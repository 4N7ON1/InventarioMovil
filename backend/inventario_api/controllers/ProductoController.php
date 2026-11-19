<?php
// Atiende las peticiones de /productos: valida los datos y llama al modelo.

require_once "models/ProductoModel.php";

class ProductoController
{
    private $modelo;

    public function __construct($conexion)
    {
        $this->modelo = new ProductoModel($conexion);
    }

    public function atender($metodo, $id)
    {
        if ($metodo == "GET" && $id > 0) {
            $this->obtener($id);
        } elseif ($metodo == "GET") {
            $buscar = trim($_GET["buscar"] ?? "");
            responder($this->modelo->listar($buscar));
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

    private function obtener($id)
    {
        $producto = $this->modelo->obtener($id);
        if ($producto == null) {
            responderError("Producto no encontrado", 404);
        }
        responder($producto);
    }

    // Revisa los datos del producto. Son las mismas reglas que valida la app.
    private function validar($datos)
    {
        $producto = [
            "nombre" => trim($datos["nombre"] ?? ""),
            "descripcion" => trim($datos["descripcion"] ?? ""),
            "precio_compra" => $datos["precio_compra"] ?? null,
            "precio_venta" => $datos["precio_venta"] ?? null,
            "stock_actual" => $datos["stock_actual"] ?? null,
            "stock_minimo" => $datos["stock_minimo"] ?? null,
            "unidad_medida" => trim($datos["unidad_medida"] ?? "Unidad"),
            "id_categoria" => intval($datos["id_categoria"] ?? 0)
        ];

        if ($producto["nombre"] == "") {
            responderError("El nombre es obligatorio");
        }
        if (!is_numeric($producto["precio_compra"]) || $producto["precio_compra"] < 0) {
            responderError("El precio de compra no es válido");
        }
        if (!is_numeric($producto["precio_venta"]) || $producto["precio_venta"] <= 0) {
            responderError("El precio de venta debe ser mayor a 0");
        }
        if ($producto["precio_venta"] < $producto["precio_compra"]) {
            responderError("El precio de venta no puede ser menor al precio de compra");
        }
        if (!is_numeric($producto["stock_actual"]) || $producto["stock_actual"] < 0) {
            responderError("El stock actual no es válido");
        }
        if (!is_numeric($producto["stock_minimo"]) || $producto["stock_minimo"] < 0) {
            responderError("El stock mínimo no es válido");
        }
        if (!$this->modelo->existeCategoria($producto["id_categoria"])) {
            responderError("La categoría seleccionada no existe");
        }
        return $producto;
    }

    private function crear()
    {
        $producto = $this->validar(leerJson());
        $id = $this->modelo->insertar($producto);
        responder(["ok" => true, "mensaje" => "Producto registrado", "id" => $id], 201);
    }

    private function actualizar($id)
    {
        if ($this->modelo->obtener($id) == null) {
            responderError("Producto no encontrado", 404);
        }
        $producto = $this->validar(leerJson());
        $this->modelo->actualizar($id, $producto);
        responder(["ok" => true, "mensaje" => "Producto actualizado", "id" => $id]);
    }

    private function eliminar($id)
    {
        if ($this->modelo->obtener($id) == null) {
            responderError("Producto no encontrado", 404);
        }
        if ($this->modelo->tieneVentas($id)) {
            responderError("No se puede eliminar: el producto tiene ventas registradas", 409);
        }
        $this->modelo->eliminar($id);
        responder(["ok" => true, "mensaje" => "Producto eliminado", "id" => $id]);
    }
}
