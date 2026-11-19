<?php
// Funciones de ayuda para responder y leer JSON.

// Envía la respuesta en formato JSON con su código HTTP y termina la petición.
function responder($datos, $codigo = 200)
{
    http_response_code($codigo);
    echo json_encode($datos, JSON_UNESCAPED_UNICODE);
    exit;
}

// Respuesta de error con un mensaje para mostrar en la app.
function responderError($mensaje, $codigo = 400)
{
    responder(["ok" => false, "mensaje" => $mensaje], $codigo);
}

// Lee el cuerpo de la petición (JSON) y lo convierte en un arreglo de PHP.
function leerJson()
{
    $datos = json_decode(file_get_contents("php://input"), true);
    if (!is_array($datos)) {
        responderError("El cuerpo de la petición no es un JSON válido");
    }
    return $datos;
}
