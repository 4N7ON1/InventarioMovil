<?php
// Consultas SQL de la tabla categoria.

class CategoriaModel
{
    private $conexion;

    public function __construct($conexion)
    {
        $this->conexion = $conexion;
    }

    // Lista las categorías con la cantidad de productos que tiene cada una.
    public function listar()
    {
        $sql = "SELECT c.id_categoria, c.nombre, c.descripcion, COUNT(p.id_producto) AS total_productos
                FROM categoria c
                LEFT JOIN producto p ON p.id_categoria = c.id_categoria
                GROUP BY c.id_categoria, c.nombre, c.descripcion
                ORDER BY c.nombre";
        $filas = $this->conexion->query($sql)->fetchAll();
        foreach ($filas as &$fila) {
            $fila["id_categoria"] = (int) $fila["id_categoria"];
            $fila["total_productos"] = (int) $fila["total_productos"];
            $fila["descripcion"] = $fila["descripcion"] ?? "";
        }
        return $filas;
    }

    // Devuelve true si ya hay otra categoría con ese nombre.
    public function existeNombre($nombre, $idExcluir)
    {
        $sql = "SELECT COUNT(*) FROM categoria WHERE LOWER(nombre) = LOWER(?) AND id_categoria <> ?";
        $consulta = $this->conexion->prepare($sql);
        $consulta->execute([$nombre, $idExcluir]);
        return $consulta->fetchColumn() > 0;
    }

    public function insertar($nombre, $descripcion)
    {
        $consulta = $this->conexion->prepare("INSERT INTO categoria (nombre, descripcion) VALUES (?, ?)");
        $consulta->execute([$nombre, $descripcion]);
        return (int) $this->conexion->lastInsertId();
    }

    public function actualizar($id, $nombre, $descripcion)
    {
        $consulta = $this->conexion->prepare("UPDATE categoria SET nombre = ?, descripcion = ? WHERE id_categoria = ?");
        $consulta->execute([$nombre, $descripcion, $id]);
    }

    public function existe($id)
    {
        $consulta = $this->conexion->prepare("SELECT COUNT(*) FROM categoria WHERE id_categoria = ?");
        $consulta->execute([$id]);
        return $consulta->fetchColumn() > 0;
    }

    public function contarProductos($id)
    {
        $consulta = $this->conexion->prepare("SELECT COUNT(*) FROM producto WHERE id_categoria = ?");
        $consulta->execute([$id]);
        return (int) $consulta->fetchColumn();
    }

    public function eliminar($id)
    {
        $consulta = $this->conexion->prepare("DELETE FROM categoria WHERE id_categoria = ?");
        $consulta->execute([$id]);
    }
}
