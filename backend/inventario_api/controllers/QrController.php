<?php
// QR de cobro del comerciante: la imagen que le da su banco para recibir pagos.
// La app la sube una vez desde Ajustes y la muestra al cliente en cada venta por QR.
// La imagen viaja dentro del JSON como texto Base64 (así no hace falta otra librería).

require_once "models/ComercianteModel.php";

class QrController
{
    private $modelo;
    private $comerciante;   // quién inició sesión: cada comerciante tiene su propio QR

    // Tamaño máximo de la imagen ya decodificada: 1 MB.
    private $tamanoMaximo = 1048576;

    public function __construct($conexion, $comerciante)
    {
        $this->modelo = new ComercianteModel($conexion);
        $this->comerciante = $comerciante;
    }

    public function atender($metodo)
    {
        if ($metodo == "GET") {
            $this->obtener();
        } elseif ($metodo == "POST") {
            $this->guardar();
        } else {
            responderError("Método no permitido", 405);
        }
    }

    // GET /qr  ->  {"imagen": "iVBORw0KGgo..."}
    private function obtener()
    {
        $ruta = $this->comerciante["ruta_qr_cobro"];
        if ($ruta == null || !file_exists($ruta)) {
            responderError("Todavía no subiste tu QR de cobro", 404);
        }
        responder(["imagen" => base64_encode(file_get_contents($ruta))]);
    }

    // POST /qr  Espera: {"imagen": "texto Base64 de una imagen JPG o PNG"}
    private function guardar()
    {
        $datos = leerJson();
        // El segundo parámetro "true" hace que falle si el texto no es Base64 válido.
        $bytes = base64_decode($datos["imagen"] ?? "", true);
        if ($bytes === false || strlen($bytes) == 0) {
            responderError("No llegó ninguna imagen");
        }
        if (strlen($bytes) > $this->tamanoMaximo) {
            responderError("La imagen es muy grande. Máximo 1 MB");
        }

        // getimagesizefromstring revisa que de verdad sea una imagen y de qué tipo.
        $info = @getimagesizefromstring($bytes);
        if ($info === false || !in_array($info["mime"], ["image/jpeg", "image/png"])) {
            responderError("El archivo debe ser una imagen JPG o PNG");
        }
        $extension = ($info["mime"] == "image/png") ? "png" : "jpg";

        // El nombre lo pone el servidor, nunca el usuario: así nadie puede escribir otros archivos.
        $id = $this->comerciante["id_comerciante"];
        $ruta = "uploads/qr_comerciante_" . $id . "." . $extension;

        // Si tenía un QR anterior con otra extensión, se borra.
        $anterior = $this->comerciante["ruta_qr_cobro"];
        if ($anterior != null && $anterior != $ruta && file_exists($anterior)) {
            unlink($anterior);
        }

        if (file_put_contents($ruta, $bytes) === false) {
            responderError("No se pudo guardar la imagen en el servidor", 500);
        }
        $this->modelo->guardarRutaQr($id, $ruta);
        responder(["ok" => true, "mensaje" => "QR de cobro guardado"]);
    }
}
