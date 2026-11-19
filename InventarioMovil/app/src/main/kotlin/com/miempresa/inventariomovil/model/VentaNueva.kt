package com.miempresa.inventariomovil.model

import com.google.gson.annotations.SerializedName

// Datos que la app envía a la API para registrar una venta (POST /ventas).
// El precio y el total no se envían: los calcula el servidor.
data class VentaNueva(
    @SerializedName("metodo_pago") val metodoPago: String,
    val detalles: List<DetalleNuevo>
)

data class DetalleNuevo(
    @SerializedName("id_producto") val idProducto: Int,
    val cantidad: Int
)
