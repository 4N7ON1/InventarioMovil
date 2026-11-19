<?php
// Conexión a MySQL con PDO. Es el único archivo donde están los datos de conexión.

class Database
{
    private $host = "127.0.0.1";
    private $puerto = "3306";   // XAMPP usa 3306. Si tu MySQL está en otro puerto (ej. 3307) cámbialo aquí.
    private $baseDatos = "db_inventario";
    private $usuario = "root";
    private $clave = "";

    public function conectar()
    {
        $dsn = "mysql:host=" . $this->host . ";port=" . $this->puerto
             . ";dbname=" . $this->baseDatos . ";charset=utf8mb4";
        $conexion = new PDO($dsn, $this->usuario, $this->clave);
        $conexion->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);
        $conexion->setAttribute(PDO::ATTR_DEFAULT_FETCH_MODE, PDO::FETCH_ASSOC);
        return $conexion;
    }
}
