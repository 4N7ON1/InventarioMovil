<?php
// Consultas SQL de la tabla producto.

class ProductoModel
{
    private $conexion;

    // Columnas que se devuelven siempre, junto con el nombre de la categoría (JOIN).
    private $columnas = "p.id_producto, p.nombre, p.descripcion, p.precio_compra, p.precio_venta,
                         p.stock_actual, p.stock_minimo, p.unidad_medida, p.id_categoria,
                         p.fecha_registro, c.nombre AS nombre_categoria";

    public function __construct($conexion)
    {
        $this->conexion = $conexion;
    }

    // MySQL devuelve los números como texto; aquí se convierten a número para el JSON.
    private function formatear($fila)
    {
        $fila["id_producto"] = (int) $fila["id_producto"];
        $fila["precio_compra"] = (float) $fila["precio_compra"];
        $fila["precio_venta"] = (float) $fila["precio_venta"];
        $fila["stock_actual"] = (int) $fila["stock_actual"];
        $fila["stock_minimo"] = (int) $fila["stock_minimo"];
        $fila["id_categoria"] = (int) $fila["id_categoria"];
        $fila["descripcion"] = $fila["descripcion"] ?? "";
        return $fila;
    }

    // Lista los productos. Si llega un texto de búsqueda filtra por nombre.
    public function listar($buscar)
    {
        $sql = "SELECT " . $this->columnas . "
                FROM producto p INNER JOIN categoria c ON p.id_categoria = c.id_categoria
                WHERE p.nombre LIKE ? ORDER BY p.nombre";
        $consulta = $this->conexion->prepare($sql);
        $consulta->execute(["%" . $buscar . "%"]);
        return array_map([$this, "formatear"], $consulta->fetchAll());
    }

    public function obtener($id)
    {
        $sql = "SELECT " . $this->columnas . "
                FROM producto p INNER JOIN categoria c ON p.id_categoria = c.id_categoria
                WHERE p.id_producto = ?";
        $consulta = $this->conexion->prepare($sql);
        $consulta->execute([$id]);
        $fila = $consulta->fetch();
        return $fila ? $this->formatear($fila) : null;
    }

    public function insertar($p)
    {
        $sql = "INSERT INTO producto (nombre, descripcion, precio_compra, precio_venta, stock_actual,
                                      stock_minimo, unidad_medida, id_categoria, fecha_registro)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        $consulta = $this->conexion->prepare($sql);
        $consulta->execute([
            $p["nombre"], $p["descripcion"], $p["precio_compra"], $p["precio_venta"], $p["stock_actual"],
            $p["stock_minimo"], $p["unidad_medida"], $p["id_categoria"], date("Y-m-d H:i:s")
        ]);
        return (int) $this->conexion->lastInsertId();
    }

    public function actualizar($id, $p)
    {
        $sql = "UPDATE producto SET nombre = ?, descripcion = ?, precio_compra = ?, precio_venta = ?,
                                    stock_actual = ?, stock_minimo = ?, unidad_medida = ?, id_categoria = ?
                WHERE id_producto = ?";
        $consulta = $this->conexion->prepare($sql);
        $consulta->execute([
            $p["nombre"], $p["descripcion"], $p["precio_compra"], $p["precio_venta"], $p["stock_actual"],
            $p["stock_minimo"], $p["unidad_medida"], $p["id_categoria"], $id
        ]);
    }

    public function tieneVentas($id)
    {
        $consulta = $this->conexion->prepare("SELECT COUNT(*) FROM detalle_venta WHERE id_producto = ?");
        $consulta->execute([$id]);
        return $consulta->fetchColumn() > 0;
    }

    public function existeCategoria($idCategoria)
    {
        $consulta = $this->conexion->prepare("SELECT COUNT(*) FROM categoria WHERE id_categoria = ?");
        $consulta->execute([$idCategoria]);
        return $consulta->fetchColumn() > 0;
    }

    public function eliminar($id)
    {
        $consulta = $this->conexion->prepare("DELETE FROM producto WHERE id_producto = ?");
        $consulta->execute([$id]);
    }
}
