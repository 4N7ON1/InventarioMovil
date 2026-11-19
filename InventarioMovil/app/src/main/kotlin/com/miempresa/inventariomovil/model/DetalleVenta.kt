package com.miempresa.inventariomovil.model

import com.google.gson.annotations.SerializedName

data class DetalleVenta(
    @SerializedName("id_detalle") var id: Int = 0,
    @SerializedName("id_venta") var idVenta: Int = 0,
    @SerializedName("id_producto") var idProducto: Int = 0,
    var cantidad: Int = 0,
    @SerializedName("precio_unitario") var precioUnitario: Double = 0.0,
    var subtotal: Double = 0.0,
    @SerializedName("nombre_producto") var nombreProducto: String = ""
) {
    override fun toString(): String =
        nombreProducto + " x" + cantidad + "  -  Bs " + String.format("%.2f", subtotal)
}
