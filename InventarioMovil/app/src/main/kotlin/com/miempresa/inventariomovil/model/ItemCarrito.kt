package com.miempresa.inventariomovil.model

// Producto dentro del carrito. Solo vive en la pantalla de venta, no se guarda en la base.
data class ItemCarrito(
    val id: Int,
    val nombre: String,
    val precio: Double,
    val icono: String,
    var cantidad: Int = 1,
    val stockDisponible: Int = 0
) {
    fun subtotal(): Double = precio * cantidad
}
