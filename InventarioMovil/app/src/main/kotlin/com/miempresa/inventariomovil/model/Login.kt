package com.miempresa.inventariomovil.model

// Lo que la app envía al iniciar sesión (POST /login).
data class Credenciales(
    val usuario: String,
    val clave: String
)

// Lo que responde el servidor cuando el usuario y la clave son correctos.
data class RespuestaLogin(
    val ok: Boolean = false,
    val mensaje: String = "",
    val token: String = "",
    val nombre: String = ""
)
