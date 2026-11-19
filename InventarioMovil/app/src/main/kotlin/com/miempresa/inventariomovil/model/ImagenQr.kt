package com.miempresa.inventariomovil.model

// La imagen del QR de cobro convertida a texto (Base64), para que viaje dentro del JSON.
// Se usa para subirla (POST /qr) y para recibirla (GET /qr).
data class ImagenQr(
    val imagen: String = ""
)
