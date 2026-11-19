package com.miempresa.inventariomovil.ui

import android.view.View
import com.miempresa.inventariomovil.databinding.VistaEstadoBinding

// Controla el layout vista_estado.xml: cargando, error con "Reintentar" o lista vacía.
class EstadoVista(private val binding: VistaEstadoBinding) {

    fun cargando() {
        binding.root.visibility = View.VISIBLE
        binding.progreso.visibility = View.VISIBLE
        binding.txtMensaje.text = "Cargando..."
        binding.btnReintentar.visibility = View.GONE
    }

    fun error(mensaje: String?, alReintentar: () -> Unit) {
        binding.root.visibility = View.VISIBLE
        binding.progreso.visibility = View.GONE
        binding.txtMensaje.text = mensaje ?: "Ocurrió un error"
        binding.btnReintentar.visibility = View.VISIBLE
        binding.btnReintentar.setOnClickListener { alReintentar() }
    }

    fun vacio(mensaje: String) {
        binding.root.visibility = View.VISIBLE
        binding.progreso.visibility = View.GONE
        binding.txtMensaje.text = mensaje
        binding.btnReintentar.visibility = View.GONE
    }

    fun ocultar() {
        binding.root.visibility = View.GONE
    }
}
