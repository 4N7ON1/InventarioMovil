package com.miempresa.inventariomovil.model

import com.google.gson.annotations.SerializedName

// Indicadores de la pantalla de inicio (GET /resumen).
data class Resumen(
    @SerializedName("ingreso_hoy") val ingresoHoy: Double = 0.0,
    @SerializedName("ventas_hoy") val ventasHoy: Int = 0,
    @SerializedName("utilidad_hoy") val utilidadHoy: Double = 0.0,
    @SerializedName("total_productos") val totalProductos: Int = 0,
    @SerializedName("stock_bajo") val stockBajo: Int = 0,
    val agotados: Int = 0,
    // Productos agotados o por agotarse, ya como texto: "Arroz 1 kg: quedan 3".
    val alertas: List<String>? = null
)
