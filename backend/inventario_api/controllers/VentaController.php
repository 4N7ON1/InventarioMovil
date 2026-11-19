<?php
// Atiende las peticiones de /ventas.

require_once "models/VentaModel.php";

class VentaController
{
    private $modelo;
    private $idComerciante;   // quién inició sesión: la venta queda a su nombre

    public function __construct($conexion, $idComerciante)
    {
        $this->modelo = new VentaModel($conexion);
        $this->idComerciante = $idComerciante;
    }

    public function atender($metodo, $id)
    {
        if ($metodo == "GET" && $id > 0) {
            $this->obtener($id);
        } elseif ($metodo == "GET") {
            responder($this->modelo->listar());
        } elseif ($metodo == "POST") {
            $this->registrar();
        } elseif ($metodo == "PUT" && $id > 0) {
            $this->actualizar($id);
        } elseif ($metodo == "DELETE" && $id > 0) {
            $this->anular($id);
        } else {
            responderError("Método no permitido", 405);
        }
    }

    private function obtener($id)
    {
        $venta = $this->modelo->obtener($id);
        if ($venta == null) {
            responderError("Venta no encontrada", 404);
        }
        responder($venta);
    }

    // POST /ventas
    // Espera: {"metodo_pago": "QR", "detalles": [{"id_producto": 1, "cantidad": 2}]}
    // El precio y el total los calcula el servidor con los datos de la base.
    private function registrar()
    {
        $datos = leerJson();
        $metodoPago = strtoupper(trim($datos["metodo_pago"] ?? ""));
        if (!in_array($metodoPago, ["EFECTIVO", "QR", "OTRO"])) {
            responderError("El método de pago debe ser EFECTIVO, QR u OTRO");
        }
        $items = $this->validarDetalles($datos["detalles"] ?? []);

        // El pago por QR queda pendiente hasta que el comerciante lo confirma.
        $estadoPago = ($metodoPago == "QR") ? "PENDIENTE" : "PAGADO";

        try {
            $idVenta = $this->modelo->registrar($metodoPago, $estadoPago, $items, $this->idComerciante);
        } catch (PDOException $e) {
            throw $e;
        } catch (Exception $e) {
            // Stock insuficiente: el modelo ya hizo rollBack, aquí solo se avisa a la app.
            responderError($e->getMessage(), 409);
        }
        responder([
            "ok" => true,
            "mensaje" => "Venta registrada",
            "id" => $idVenta,
            "alertas" => $this->modelo->alertasStock($idVenta)
        ], 201);
    }

    // PUT /ventas/{id}
    // Con {"detalles": [...]} cambia los productos de la venta.
    // Sin detalles confirma el pago de una venta pendiente (QR).
    private function actualizar($id)
    {
        if ($this->modelo->obtener($id) == null) {
            responderError("Venta no encontrada", 404);
        }
        $datos = json_decode(file_get_contents("php://input"), true);
        if (!is_array($datos) || !isset($datos["detalles"])) {
            $this->confirmarPago($id);
        }
        $items = $this->validarDetalles($datos["detalles"]);

        try {
            $this->modelo->actualizar($id, $items);
        } catch (PDOException $e) {
            throw $e;
        } catch (Exception $e) {
            responderError($e->getMessage(), 409);
        }
        responder([
            "ok" => true,
            "mensaje" => "Venta actualizada",
            "id" => $id,
            "alertas" => $this->modelo->alertasStock($id)
        ]);
    }

    // Revisa la lista de productos que manda la app y la devuelve limpia.
    private function validarDetalles($detalles)
    {
        if (!is_array($detalles) || count($detalles) == 0) {
            responderError("La venta debe tener al menos un producto");
        }
        $items = [];
        foreach ($detalles as $detalle) {
            $idProducto = intval($detalle["id_producto"] ?? 0);
            $cantidad = intval($detalle["cantidad"] ?? 0);
            if ($idProducto <= 0 || $cantidad <= 0) {
                responderError("Cada detalle necesita id_producto y una cantidad mayor a 0");
            }
            $items[] = ["id_producto" => $idProducto, "cantidad" => $cantidad];
        }
        return $items;
    }

    private function confirmarPago($id)
    {
        $this->modelo->confirmarPago($id);
        responder(["ok" => true, "mensaje" => "Pago confirmado", "id" => $id]);
    }

    private function anular($id)
    {
        if ($this->modelo->obtener($id) == null) {
            responderError("Venta no encontrada", 404);
        }
        $this->modelo->anular($id);
        responder(["ok" => true, "mensaje" => "Venta anulada y stock devuelto", "id" => $id]);
    }
}
