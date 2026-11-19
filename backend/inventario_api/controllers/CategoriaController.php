<?php
// Atiende las peticiones de /categorias: valida los datos y llama al modelo.

require_once "models/CategoriaModel.php";

class CategoriaController
{
    private $modelo;

    public function __construct($conexion)
    {
        $this->modelo = new CategoriaModel($conexion);
    }

    public function atender($metodo, $id)
    {
        if ($metodo == "GET") {
            responder($this->modelo->listar());
        } elseif ($metodo == "POST") {
            $this->crear();
        } elseif ($metodo == "PUT" && $id > 0) {
            $this->actualizar($id);
        } elseif ($metodo == "DELETE" && $id > 0) {
            $this->eliminar($id);
        } else {
            responderError("Método no permitido", 405);
        }
    }

    // Revisa nombre y descripción. Si algo está mal responde el error y termina.
    private function validar($datos, $idExcluir)
    {
        $nombre = trim($datos["nombre"] ?? "");
        $descripcion = trim($datos["descripcion"] ?? "");

        if (strlen($nombre) < 3) {
            responderError("El nombre debe tener al menos 3 caracteres");
        }
        if ($this->modelo->existeNombre($nombre, $idExcluir)) {
            responderError("Ya existe una categoría llamada " . $nombre, 409);
        }
        return [$nombre, $descripcion];
    }

    private function crear()
    {
        list($nombre, $descripcion) = $this->validar(leerJson(), 0);
        $id = $this->modelo->insertar($nombre, $descripcion);
        responder(["ok" => true, "mensaje" => "Categoría registrada", "id" => $id], 201);
    }

    private function actualizar($id)
    {
        if (!$this->modelo->existe($id)) {
            responderError("Categoría no encontrada", 404);
        }
        list($nombre, $descripcion) = $this->validar(leerJson(), $id);
        $this->modelo->actualizar($id, $nombre, $descripcion);
        responder(["ok" => true, "mensaje" => "Categoría actualizada", "id" => $id]);
    }

    private function eliminar($id)
    {
        if (!$this->modelo->existe($id)) {
            responderError("Categoría no encontrada", 404);
        }
        if ($this->modelo->contarProductos($id) > 0) {
            responderError("No se puede eliminar: la categoría tiene productos", 409);
        }
        $this->modelo->eliminar($id);
        responder(["ok" => true, "mensaje" => "Categoría eliminada", "id" => $id]);
    }
}
