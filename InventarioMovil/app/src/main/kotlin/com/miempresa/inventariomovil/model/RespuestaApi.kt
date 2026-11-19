package com.miempresa.inventariomovil.model

// Respuesta de la API al crear, actualizar o eliminar: {"ok": true, "mensaje": "...", "id": 5}
data class RespuestaApi(
    val ok: Boolean = false,
    val mensaje: String = "",
    val id: Int = 0,
    // Avisos de stock que manda el servidor después de una venta. Puede no venir (null).
    val alertas: List<String>? = null
)
