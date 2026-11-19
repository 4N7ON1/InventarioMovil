package com.miempresa.inventariomovil

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.miempresa.inventariomovil.api.ApiConfig
import com.miempresa.inventariomovil.api.InventarioRepository
import com.miempresa.inventariomovil.api.Sesion
import com.miempresa.inventariomovil.databinding.ActivityConfiguracionBinding
import com.miempresa.inventariomovil.ui.Imagenes
import kotlinx.coroutines.launch

// Pantalla de ajustes: en qué computadora está la API, probar la conexión y cerrar sesión.
// No hereda de PantallaProtegida porque también se abre desde el login (antes de tener sesión).
class ConfiguracionActivity : AppCompatActivity() {

    private lateinit var binding: ActivityConfiguracionBinding
    private val repositorio = InventarioRepository()

    // Abre la galería del celular para elegir una imagen. Cuando el usuario elige una,
    // se llama a subirQr con su dirección (Uri). Si cancela, llega null.
    private val elegirImagen = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) subirQr(uri)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityConfiguracionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.edtDireccionIp.setText(ApiConfig.direccionIp)
        mostrarUrl()

        binding.edtDireccionIp.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                mostrarUrl()
            }
        })

        binding.backButton.setOnClickListener { finish() }
        binding.btnGuardarServidor.setOnClickListener {
            if (guardar()) {
                Toast.makeText(this, "Servidor guardado", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
        binding.btnProbarConexion.setOnClickListener {
            if (guardar()) probarConexion()
        }

        // "Mi QR de cobro" y "Cerrar sesión" solo aparecen si hay alguien con sesión iniciada.
        if (Sesion.hayToken()) {
            binding.txtSesion.text = "Sesión iniciada: " + Sesion.nombre
            binding.btnCerrarSesion.setOnClickListener { confirmarCerrarSesion() }
            binding.btnSubirQr.setOnClickListener { elegirImagen.launch("image/*") }
            cargarQr()
        } else {
            binding.seccionQr.visibility = View.GONE
            binding.seccionSesion.visibility = View.GONE
        }
    }

    private fun mostrarUrl() {
        val ip = binding.edtDireccionIp.text.toString().trim()
        binding.txtUrlApi.text = "La app usará:\n" + ApiConfig.armarUrl(ip)
    }

    // Valida la IP y la guarda. Devuelve false si el campo está vacío o tiene espacios.
    private fun guardar(): Boolean {
        val ip = binding.edtDireccionIp.text.toString().trim()
        if (ip.isEmpty() || ip.contains(" ") || ip.contains("/")) {
            binding.edtDireccionIp.error = "Escribe solo la IP, por ejemplo 192.168.1.10 (o 192.168.1.10:90)"
            return false
        }
        ApiConfig.guardar(this, ip)
        return true
    }

    // GET /qr: si el comerciante ya subió su QR, se muestra.
    private fun cargarQr() {
        lifecycleScope.launch {
            try {
                mostrarQr(repositorio.obtenerQr())
            } catch (e: Exception) {
                binding.imgQr.visibility = View.GONE
                binding.txtEstadoQr.text = e.message
            }
        }
    }

    // Convierte la imagen elegida a texto Base64 y la envía al servidor (POST /qr).
    private fun subirQr(uri: Uri) {
        binding.btnSubirQr.isEnabled = false
        binding.txtEstadoQr.text = "Subiendo..."
        lifecycleScope.launch {
            try {
                val texto = Imagenes.aTextoBase64(this@ConfiguracionActivity, uri)
                val respuesta = repositorio.subirQr(texto)
                Toast.makeText(this@ConfiguracionActivity, respuesta.mensaje, Toast.LENGTH_SHORT).show()
                mostrarQr(texto)
            } catch (e: Exception) {
                binding.txtEstadoQr.text = e.message
            }
            binding.btnSubirQr.isEnabled = true
        }
    }

    private fun mostrarQr(textoBase64: String) {
        binding.imgQr.setImageBitmap(Imagenes.aBitmap(textoBase64))
        binding.imgQr.visibility = View.VISIBLE
        binding.txtEstadoQr.text = "Este es tu QR de cobro"
        binding.btnSubirQr.text = "Cambiar imagen del QR"
    }

    private fun confirmarCerrarSesion() {
        AlertDialog.Builder(this)
            .setTitle("Cerrar sesión")
            .setMessage("Tendrás que escribir tu usuario y tu clave para volver a entrar.")
            .setPositiveButton("Cerrar sesión") { _, _ -> cerrarSesion() }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    // Avisa al servidor (POST /logout) para que el token deje de servir,
    // borra la sesión del celular y vuelve al login.
    private fun cerrarSesion() {
        lifecycleScope.launch {
            try {
                repositorio.cerrarSesion()
            } catch (e: Exception) {
                // Sin conexión igual se cierra la sesión en el celular.
            }
            Sesion.cerrar()
            val intent = Intent(this@ConfiguracionActivity, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }

    private fun probarConexion() {
        binding.progresoConexion.visibility = View.VISIBLE
        binding.txtResultadoConexion.text = ""
        lifecycleScope.launch {
            try {
                val respuesta = repositorio.probarConexion()
                binding.txtResultadoConexion.setTextColor(ContextCompat.getColor(this@ConfiguracionActivity, R.color.ok_fg))
                binding.txtResultadoConexion.text = "Conexión correcta: " + respuesta.mensaje
            } catch (e: Exception) {
                binding.txtResultadoConexion.setTextColor(ContextCompat.getColor(this@ConfiguracionActivity, R.color.err_fg))
                binding.txtResultadoConexion.text = e.message
            }
            binding.progresoConexion.visibility = View.GONE
        }
    }
}
