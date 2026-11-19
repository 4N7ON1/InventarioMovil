<?php
// Consultas SQL de la tabla comerciante que usa el inicio de sesión.

class ComercianteModel
{
    private $conexion;

    public function __construct($conexion)
    {
        $this->conexion = $conexion;
    }

    public function buscarPorUsuario($usuario)
    {
        $consulta = $this->conexion->prepare("SELECT * FROM comerciante WHERE usuario = ?");
        $consulta->execute([$usuario]);
        return $consulta->fetch();
    }

    // Devuelve el comerciante dueño del token, o false si el token no existe o ya venció.
    public function buscarPorToken($token)
    {
        $consulta = $this->conexion->prepare(
            "SELECT * FROM comerciante WHERE token = ? AND token_vence > ?"
        );
        $consulta->execute([$token, date("Y-m-d H:i:s")]);
        return $consulta->fetch();
    }

    // Cada vez que la app usa el token, se le dan otros 30 minutos de vida.
    // Si la app se cierra y nadie lo vuelve a usar, el token muere solo a los 30 minutos.
    public function renovarToken($idComerciante)
    {
        $consulta = $this->conexion->prepare("UPDATE comerciante SET token_vence = ? WHERE id_comerciante = ?");
        $consulta->execute([$this->horaVencimiento(), $idComerciante]);
    }

    private function horaVencimiento()
    {
        return date("Y-m-d H:i:s", time() + 30 * 60);
    }

    // Suma un intento fallido. Al llegar a 5 bloquea la cuenta por 5 minutos.
    public function registrarFallo($comerciante)
    {
        $intentos = $comerciante["intentos_fallidos"] + 1;
        $bloqueadoHasta = null;
        if ($intentos >= 5) {
            $bloqueadoHasta = date("Y-m-d H:i:s", time() + 5 * 60);
            $intentos = 0;
        }
        $consulta = $this->conexion->prepare(
            "UPDATE comerciante SET intentos_fallidos = ?, bloqueado_hasta = ? WHERE id_comerciante = ?"
        );
        $consulta->execute([$intentos, $bloqueadoHasta, $comerciante["id_comerciante"]]);
    }

    // Guarda el token de la sesión nueva (con 30 minutos de vida) y limpia los intentos fallidos.
    // El token anterior se reemplaza, así que cualquier sesión vieja deja de servir.
    public function guardarToken($idComerciante, $token)
    {
        $consulta = $this->conexion->prepare(
            "UPDATE comerciante SET token = ?, token_vence = ?, intentos_fallidos = 0, bloqueado_hasta = NULL
             WHERE id_comerciante = ?"
        );
        $consulta->execute([$token, $this->horaVencimiento(), $idComerciante]);
    }

    // Guarda dónde quedó la imagen del QR de cobro.
    public function guardarRutaQr($idComerciante, $ruta)
    {
        $consulta = $this->conexion->prepare("UPDATE comerciante SET ruta_qr_cobro = ? WHERE id_comerciante = ?");
        $consulta->execute([$ruta, $idComerciante]);
    }

    // Cerrar sesión: se borra el token y deja de servir.
    public function borrarToken($idComerciante)
    {
        $consulta = $this->conexion->prepare(
            "UPDATE comerciante SET token = NULL, token_vence = NULL WHERE id_comerciante = ?"
        );
        $consulta->execute([$idComerciante]);
    }
}
