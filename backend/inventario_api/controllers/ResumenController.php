<?php
// Atiende /resumen: los indicadores de la pantalla de inicio.

require_once "models/VentaModel.php";

class ResumenController
{
    private $modelo;

    public function __construct($conexion)
    {
        $this->modelo = new VentaModel($conexion);
    }

    public function atender($metodo)
    {
        if ($metodo != "GET") {
            responderError("Método no permitido", 405);
        }
        responder($this->modelo->resumen());
    }
}
