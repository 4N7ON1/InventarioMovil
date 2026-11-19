package com.miempresa.inventariomovil.model

import com.google.gson.annotations.SerializedName

// @SerializedName indica el nombre que tiene el campo en el JSON de la API.
data class Categoria(
    @SerializedName("id_categoria") var id: Int = 0,
    var nombre: String = "",
    var descripcion: String = "",
    @SerializedName("total_productos") var totalProductos: Int = 0
) {
    override fun toString(): String = nombre
}
