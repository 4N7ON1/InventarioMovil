<?php
// Punto de entrada de la API. Todas las peticiones llegan aquí (ver .htaccess).
// Lee la URL, revisa la sesión, decide qué controlador usar y le pasa el método HTTP.

// Los errores de PHP no se muestran al cliente: así no se filtran rutas ni datos internos.
ini_set("display_errors", "0");
header("Content-Type: application/json; charset=utf-8");
date_default_timezone_set("America/La_Paz");

require_once "config/Database.php";
require_once "helpers/respuesta.php";
require_once "controllers/AuthController.php";
require_once "controllers/CategoriaController.php";
require_once "controllers/ProductoController.php";
require_once "controllers/VentaController.php";
require_once "controllers/ResumenController.php";
require_once "controllers/QrController.php";

$metodo = $_SERVER["REQUEST_METHOD"];

// Ejemplo: /inventario_api/productos/5  ->  recurso = "productos", id = 5
$ruta = parse_url($_SERVER["REQUEST_URI"], PHP_URL_PATH);
$carpeta = dirname($_SERVER["SCRIPT_NAME"]);
if ($carpeta != "/" && $carpeta != "\\" && strpos($ruta, $carpeta) === 0) {
    $ruta = substr($ruta, strlen($carpeta));
}
$partes = array_values(array_filter(explode("/", $ruta), function ($parte) {
    return $parte != "" && $parte != "index.php";
}));

$recurso = isset($partes[0]) ? $partes[0] : "";
$id = isset($partes[1]) ? intval($partes[1]) : 0;

try {
    // Ruta pública 1: sirve para probar que la API está encendida.
    if ($recurso == "") {
        responder(["ok" => true, "mensaje" => "API InventarioMovil funcionando"]);
    }

    $conexion = (new Database())->conectar();
    $auth = new AuthController($conexion);

    // Ruta pública 2: el inicio de sesión.
    if ($recurso == "login") {
        $auth->iniciarSesion($metodo);
    }

    // Todo lo demás necesita haber iniciado sesión (token válido).
    $comerciante = $auth->exigirSesion();

    if ($recurso == "logout") {
        $auth->cerrarSesion($comerciante);
    } elseif ($recurso == "categorias") {
        (new CategoriaController($conexion))->atender($metodo, $id);
    } elseif ($recurso == "productos") {
        (new ProductoController($conexion))->atender($metodo, $id);
    } elseif ($recurso == "ventas") {
        (new VentaController($conexion, $comerciante["id_comerciante"]))->atender($metodo, $id);
    } elseif ($recurso == "qr") {
        (new QrController($conexion, $comerciante))->atender($metodo);
    } elseif ($recurso == "resumen") {
        (new ResumenController($conexion))->atender($metodo);
    } else {
        responderError("Recurso no encontrado", 404);
    }
} catch (PDOException $e) {
    responderError("Error de base de datos. Revisa que MySQL esté encendido y que exista db_inventario", 500);
} catch (Throwable $e) {
    responderError("Error interno del servidor", 500);
}
