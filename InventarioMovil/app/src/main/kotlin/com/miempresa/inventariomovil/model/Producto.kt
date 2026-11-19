package com.miempresa.inventariomovil.model

import com.google.gson.annotations.SerializedName

data class Producto(
    @SerializedName("id_producto") var id: Int = 0,
    var nombre: String = "",
    var descripcion: String = "",
    @SerializedName("precio_compra") var precioCompra: Double = 0.0,
    @SerializedName("precio_venta") var precioVenta: Double = 0.0,
    @SerializedName("stock_actual") var stockActual: Int = 0,
    @SerializedName("stock_minimo") var stockMinimo: Int = 0,
    @SerializedName("unidad_medida") var unidadMedida: String = "Unidad",
    @SerializedName("id_categoria") var idCategoria: Int = 0,
    @SerializedName("fecha_registro") var fechaRegistro: String = "",
    @SerializedName("nombre_categoria") var nombreCategoria: String = ""
) {
    fun estado(): String = when {
        stockActual == 0 -> "Agotado"
        stockActual <= stockMinimo -> "Stock bajo"
        else -> "Disponible"
    }

    fun icono(): String = when (nombreCategoria) {
        "Bebidas" -> "ic_local_drink"
        "Abarrotes" -> "ic_grocery"
        "Lácteos" -> "ic_nutrition"
        "Snacks" -> "ic_cookie"
        "Limpieza" -> "ic_water_drop"
        else -> "ic_inventory_2"
    }

    override fun toString(): String =
        nombre + "  ·  Bs " + String.format("%.2f", precioVenta) + "  ·  Stock: " + stockActual
}
