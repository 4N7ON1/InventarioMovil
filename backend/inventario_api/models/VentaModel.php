<?php
// Consultas SQL de las tablas venta y detalle_venta.

class VentaModel
{
    private $conexion;

    public function __construct($conexion)
    {
        $this->conexion = $conexion;
    }

    private function formatear($fila)
    {
        $fila["id_venta"] = (int) $fila["id_venta"];
        $fila["monto_total"] = (float) $fila["monto_total"];
        $fila["id_comerciante"] = (int) $fila["id_comerciante"];
        $fila["total_productos"] = (int) $fila["total_productos"];
        return $fila;
    }

    // Lista las ventas de la más nueva a la más antigua.
    public function listar()
    {
        $sql = "SELECT v.id_venta, v.fecha_hora, v.monto_total, v.metodo_pago, v.estado_pago, v.id_comerciante,
                       (SELECT COUNT(*) FROM detalle_venta d WHERE d.id_venta = v.id_venta) AS total_productos
                FROM venta v ORDER BY v.id_venta DESC";
        return array_map([$this, "formatear"], $this->conexion->query($sql)->fetchAll());
    }

    // Devuelve una venta con su lista de detalles, o null si no existe.
    public function obtener($id)
    {
        $sql = "SELECT v.id_venta, v.fecha_hora, v.monto_total, v.metodo_pago, v.estado_pago, v.id_comerciante,
                       (SELECT COUNT(*) FROM detalle_venta d WHERE d.id_venta = v.id_venta) AS total_productos
                FROM venta v WHERE v.id_venta = ?";
        $consulta = $this->conexion->prepare($sql);
        $consulta->execute([$id]);
        $venta = $consulta->fetch();
        if (!$venta) {
            return null;
        }
        $venta = $this->formatear($venta);
        $venta["detalles"] = $this->listarDetalles($id);
        return $venta;
    }

    public function listarDetalles($idVenta)
    {
        $sql = "SELECT d.id_detalle, d.id_venta, d.id_producto, d.cantidad, d.precio_unitario, d.subtotal,
                       p.nombre AS nombre_producto
                FROM detalle_venta d INNER JOIN producto p ON d.id_producto = p.id_producto
                WHERE d.id_venta = ?";
        $consulta = $this->conexion->prepare($sql);
        $consulta->execute([$idVenta]);
        $filas = $consulta->fetchAll();
        foreach ($filas as &$fila) {
            $fila["id_detalle"] = (int) $fila["id_detalle"];
            $fila["id_venta"] = (int) $fila["id_venta"];
            $fila["id_producto"] = (int) $fila["id_producto"];
            $fila["cantidad"] = (int) $fila["cantidad"];
            $fila["precio_unitario"] = (float) $fila["precio_unitario"];
            $fila["subtotal"] = (float) $fila["subtotal"];
        }
        return $filas;
    }

    // ------------------------------------------------------------------
    // TRANSACCIONES
    // Una venta toca varias tablas (venta, detalle_venta y producto).
    // beginTransaction() abre la transacción, commit() guarda todo junto y
    // rollBack() deshace todo. Si un producto no tiene stock suficiente se
    // lanza una Exception, se llega al catch y el rollBack deja la base
    // exactamente como estaba: no se descuenta ninguna unidad del inventario.
    // ------------------------------------------------------------------

    // Registra una venta nueva. Devuelve el id de la venta.
    public function registrar($metodoPago, $estadoPago, $items, $idComerciante)
    {
        $this->conexion->beginTransaction();
        try {
            $consulta = $this->conexion->prepare(
                "INSERT INTO venta (fecha_hora, monto_total, metodo_pago, estado_pago, id_comerciante)
                 VALUES (?, 0, ?, ?, ?)"
            );
            $consulta->execute([date("Y-m-d H:i:s"), $metodoPago, $estadoPago, $idComerciante]);
            $idVenta = (int) $this->conexion->lastInsertId();

            $total = $this->guardarDetalles($idVenta, $items);
            $this->guardarTotal($idVenta, $total);

            $this->conexion->commit();
            return $idVenta;
        } catch (Exception $e) {
            $this->conexion->rollBack();   // <-- aquí se deshace todo si algo falló
            throw $e;
        }
    }

    // Cambia los productos de una venta ya registrada (el cliente cambió su compra).
    // 1) devuelve al inventario lo que tenía la venta, 2) borra sus detalles,
    // 3) guarda los detalles nuevos descontando stock y 4) actualiza el total.
    public function actualizar($idVenta, $items)
    {
        $this->conexion->beginTransaction();
        try {
            $this->devolverStock($idVenta);

            $consulta = $this->conexion->prepare("DELETE FROM detalle_venta WHERE id_venta = ?");
            $consulta->execute([$idVenta]);

            $total = $this->guardarDetalles($idVenta, $items);
            $this->guardarTotal($idVenta, $total);

            $this->conexion->commit();
        } catch (Exception $e) {
            $this->conexion->rollBack();   // la venta queda igual que antes de editarla
            throw $e;
        }
    }

    // Anula la venta: devuelve el stock de cada producto y borra la venta.
    // Los detalles se borran solos por el ON DELETE CASCADE.
    public function anular($idVenta)
    {
        $this->conexion->beginTransaction();
        try {
            $this->devolverStock($idVenta);
            $consulta = $this->conexion->prepare("DELETE FROM venta WHERE id_venta = ?");
            $consulta->execute([$idVenta]);
            $this->conexion->commit();
        } catch (Exception $e) {
            $this->conexion->rollBack();
            throw $e;
        }
    }

    // Guarda los detalles de una venta y descuenta el stock. Devuelve el total.
    // Se usa siempre dentro de una transacción (registrar y actualizar).
    private function guardarDetalles($idVenta, $items)
    {
        $total = 0;
        foreach ($items as $item) {
            // FOR UPDATE bloquea la fila para que dos ventas al mismo tiempo no usen el mismo stock.
            $consulta = $this->conexion->prepare(
                "SELECT nombre, precio_venta, stock_actual FROM producto WHERE id_producto = ? FOR UPDATE"
            );
            $consulta->execute([$item["id_producto"]]);
            $producto = $consulta->fetch();

            if (!$producto) {
                throw new Exception("Uno de los productos ya no existe");
            }
            if ($producto["stock_actual"] < $item["cantidad"]) {
                throw new Exception(
                    "Stock insuficiente para " . $producto["nombre"] . ". Quedan " . $producto["stock_actual"]
                );
            }

            $subtotal = $producto["precio_venta"] * $item["cantidad"];
            $total += $subtotal;

            $consulta = $this->conexion->prepare(
                "INSERT INTO detalle_venta (id_venta, id_producto, cantidad, precio_unitario, subtotal)
                 VALUES (?, ?, ?, ?, ?)"
            );
            $consulta->execute([$idVenta, $item["id_producto"], $item["cantidad"], $producto["precio_venta"], $subtotal]);

            $consulta = $this->conexion->prepare(
                "UPDATE producto SET stock_actual = stock_actual - ? WHERE id_producto = ?"
            );
            $consulta->execute([$item["cantidad"], $item["id_producto"]]);
        }
        return $total;
    }

    // Suma de vuelta al inventario las cantidades de una venta.
    private function devolverStock($idVenta)
    {
        foreach ($this->listarDetalles($idVenta) as $detalle) {
            $consulta = $this->conexion->prepare(
                "UPDATE producto SET stock_actual = stock_actual + ? WHERE id_producto = ?"
            );
            $consulta->execute([$detalle["cantidad"], $detalle["id_producto"]]);
        }
    }

    private function guardarTotal($idVenta, $total)
    {
        $consulta = $this->conexion->prepare("UPDATE venta SET monto_total = ? WHERE id_venta = ?");
        $consulta->execute([$total, $idVenta]);
    }

    public function confirmarPago($id)
    {
        $consulta = $this->conexion->prepare("UPDATE venta SET estado_pago = 'PAGADO' WHERE id_venta = ?");
        $consulta->execute([$id]);
    }

    // ALERTA DE STOCK: avisos de los productos que se agotaron o están por agotarse
    // (stock_actual menor o igual al stock_minimo).
    // Con $idVenta revisa solo los productos de esa venta; con 0 revisa todo el inventario.
    public function alertasStock($idVenta = 0)
    {
        $sql = "SELECT nombre, stock_actual FROM producto WHERE stock_actual <= stock_minimo";
        $parametros = [];
        if ($idVenta > 0) {
            $sql .= " AND id_producto IN (SELECT id_producto FROM detalle_venta WHERE id_venta = ?)";
            $parametros[] = $idVenta;
        }
        $sql .= " ORDER BY stock_actual, nombre";

        $consulta = $this->conexion->prepare($sql);
        $consulta->execute($parametros);

        $alertas = [];
        foreach ($consulta->fetchAll() as $producto) {
            if ($producto["stock_actual"] == 0) {
                $alertas[] = $producto["nombre"] . ": agotado";
            } else {
                $alertas[] = $producto["nombre"] . ": quedan " . $producto["stock_actual"];
            }
        }
        return $alertas;
    }

    // Datos de la pantalla de inicio en una sola consulta por cada indicador.
    public function resumen()
    {
        $hoy = date("Y-m-d") . "%";
        return [
            "ingreso_hoy" => (float) $this->valor(
                "SELECT IFNULL(SUM(monto_total), 0) FROM venta WHERE fecha_hora LIKE ? AND estado_pago = 'PAGADO'", [$hoy]
            ),
            "ventas_hoy" => (int) $this->valor("SELECT COUNT(*) FROM venta WHERE fecha_hora LIKE ?", [$hoy]),
            "utilidad_hoy" => (float) $this->valor(
                "SELECT IFNULL(SUM((d.precio_unitario - p.precio_compra) * d.cantidad), 0)
                 FROM detalle_venta d
                 INNER JOIN venta v ON d.id_venta = v.id_venta
                 INNER JOIN producto p ON d.id_producto = p.id_producto
                 WHERE v.fecha_hora LIKE ? AND v.estado_pago = 'PAGADO'", [$hoy]
            ),
            "total_productos" => (int) $this->valor("SELECT COUNT(*) FROM producto", []),
            "stock_bajo" => (int) $this->valor(
                "SELECT COUNT(*) FROM producto WHERE stock_actual > 0 AND stock_actual <= stock_minimo", []
            ),
            "agotados" => (int) $this->valor("SELECT COUNT(*) FROM producto WHERE stock_actual = 0", []),
            "alertas" => $this->alertasStock()
        ];
    }

    // Ejecuta una consulta que devuelve un solo valor (COUNT o SUM).
    private function valor($sql, $parametros)
    {
        $consulta = $this->conexion->prepare($sql);
        $consulta->execute($parametros);
        return $consulta->fetchColumn();
    }
}
