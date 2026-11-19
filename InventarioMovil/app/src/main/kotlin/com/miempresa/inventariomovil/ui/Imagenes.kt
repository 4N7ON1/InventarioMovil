package com.miempresa.inventariomovil.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import java.io.ByteArrayOutputStream

// Funciones para convertir la imagen del QR de cobro: imagen <-> texto Base64.
object Imagenes {

    // Lado más largo con el que se guarda la imagen. Suficiente para que el QR se lea bien
    // y la imagen pese poco (una foto del celular puede tener 4000 px y varios MB).
    private const val TAMANO_MAXIMO = 800

    // Lee la imagen que eligió el usuario, la achica y la convierte en texto Base64.
    fun aTextoBase64(context: Context, uri: Uri): String {
        val entrada = context.contentResolver.openInputStream(uri)
            ?: throw Exception("No se pudo abrir la imagen")
        val original = entrada.use { BitmapFactory.decodeStream(it) }
            ?: throw Exception("El archivo elegido no es una imagen")

        val imagen = achicar(original)
        val salida = ByteArrayOutputStream()
        imagen.compress(Bitmap.CompressFormat.JPEG, 90, salida)
        return Base64.encodeToString(salida.toByteArray(), Base64.NO_WRAP)
    }

    // Convierte el texto Base64 que manda el servidor en una imagen para mostrar.
    fun aBitmap(textoBase64: String): Bitmap? {
        val bytes = Base64.decode(textoBase64, Base64.DEFAULT)
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }

    // Si la imagen es más grande que TAMANO_MAXIMO, la reduce manteniendo la proporción.
    private fun achicar(imagen: Bitmap): Bitmap {
        val ladoMayor = maxOf(imagen.width, imagen.height)
        if (ladoMayor <= TAMANO_MAXIMO) return imagen
        val escala = TAMANO_MAXIMO.toFloat() / ladoMayor
        val ancho = (imagen.width * escala).toInt()
        val alto = (imagen.height * escala).toInt()
        return Bitmap.createScaledBitmap(imagen, ancho, alto, true)
    }
}
