package com.miempresa.inventariomovil.model

import com.google.gson.annotations.SerializedName

data class Venta(
    @SerializedName("id_venta") var id: Int = 0,
    @SerializedName("fecha_hora") var fechaHora: String = "",
    @SerializedName("monto_total") var montoTotal: Double = 0.0,
    @SerializedName("metodo_pago") var metodoPago: String = "EFECTIVO",
    @SerializedName("estado_pago") var estadoPago: String = "PAGADO",
    @SerializedName("id_comerciante") var idComerciante: Int = 1,
    @SerializedName("total_productos") var totalProductos: Int = 0,
    var detalles: List<DetalleVenta> = emptyList()
)
