<?php
// Inicio y cierre de sesión.
// La app envía usuario y clave una sola vez; el servidor responde con un token.
// Desde ahí la app manda ese token en cada petición (cabecera X-Token).

require_once "models/ComercianteModel.php";

class AuthController
{
    private $modelo;

    public function __construct($conexion)
    {
        $this->modelo = new ComercianteModel($conexion);
    }

    // POST /login   Espera: {"usuario": "admin", "clave": "admin123"}
    public function iniciarSesion($metodo)
    {
        if ($metodo != "POST") {
            responderError("Método no permitido", 405);
        }
        $datos = leerJson();
        $usuario = trim($datos["usuario"] ?? "");
        $clave = $datos["clave"] ?? "";

        if ($usuario == "" || $clave == "") {
            responderError("Escribe tu usuario y tu clave");
        }

        $comerciante = $this->modelo->buscarPorUsuario($usuario);

        // El mismo mensaje si falla el usuario o la clave: así no se revela cuál de los dos existe.
        if (!$comerciante) {
            responderError("Usuario o clave incorrectos", 401);
        }
        if ($comerciante["bloqueado_hasta"] != null && $comerciante["bloqueado_hasta"] > date("Y-m-d H:i:s")) {
            responderError("Demasiados intentos. Espera 5 minutos y vuelve a probar", 429);
        }
        // password_verify compara la clave escrita con el hash guardado en la base.
        if (!password_verify($clave, $comerciante["clave_hash"])) {
            $this->modelo->registrarFallo($comerciante);
            responderError("Usuario o clave incorrectos", 401);
        }

        // Token nuevo en cada inicio de sesión: 64 caracteres al azar, imposibles de adivinar.
        // Reemplaza al anterior, así una sesión que quedó abierta en otro lado se cierra.
        $token = bin2hex(random_bytes(32));
        $this->modelo->guardarToken($comerciante["id_comerciante"], $token);

        responder([
            "ok" => true,
            "mensaje" => "Bienvenido",
            "token" => $token,
            "nombre" => $comerciante["nombre"]
        ]);
    }

    // Revisa el token de la petición. Si no es válido responde 401 y la petición termina aquí.
    // Si es válido devuelve los datos del comerciante que inició sesión.
    public function exigirSesion()
    {
        $token = $_SERVER["HTTP_X_TOKEN"] ?? "";
        $comerciante = ($token == "") ? false : $this->modelo->buscarPorToken($token);
        if (!$comerciante) {
            responderError("Tu sesión terminó. Vuelve a iniciar sesión", 401);
        }
        $this->modelo->renovarToken($comerciante["id_comerciante"]);
        return $comerciante;
    }

    // POST /logout
    public function cerrarSesion($comerciante)
    {
        $this->modelo->borrarToken($comerciante["id_comerciante"]);
        responder(["ok" => true, "mensaje" => "Sesión cerrada"]);
    }
}
