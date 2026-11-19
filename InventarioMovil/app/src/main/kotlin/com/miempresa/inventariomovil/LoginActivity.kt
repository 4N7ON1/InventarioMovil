package com.miempresa.inventariomovil

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.miempresa.inventariomovil.api.InventarioRepository
import com.miempresa.inventariomovil.api.Sesion
import com.miempresa.inventariomovil.databinding.ActivityLoginBinding
import kotlinx.coroutines.launch

// Primera pantalla de la app: pide usuario y clave.
class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private val repositorio = InventarioRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Si ya hay una sesión guardada y no venció, se entra directo sin pedir la clave.
        if (Sesion.hayToken() && !Sesion.vencio()) {
            abrirInicio()
            return
        }
        Sesion.cerrar()

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnEntrar.setOnClickListener { iniciarSesion() }
        binding.btnServidor.setOnClickListener {
            startActivity(Intent(this, ConfiguracionActivity::class.java))
        }
    }

    // Valida los campos y envía usuario y clave al servidor (POST /login).
    private fun iniciarSesion() {
        val usuario = binding.edtUsuario.text.toString().trim()
        val clave = binding.edtClave.text.toString()

        if (usuario.isEmpty()) {
            binding.edtUsuario.error = "Escribe tu usuario"
            return
        }
        if (clave.isEmpty()) {
            binding.edtClave.error = "Escribe tu clave"
            return
        }

        mostrarCargando(true)
        binding.txtErrorLogin.text = ""
        lifecycleScope.launch {
            try {
                val respuesta = repositorio.iniciarSesion(usuario, clave)
                Sesion.iniciar(respuesta.token, respuesta.nombre)
                abrirInicio()
            } catch (e: Exception) {
                mostrarCargando(false)
                binding.edtClave.setText("")
                binding.txtErrorLogin.text = e.message
            }
        }
    }

    private fun mostrarCargando(cargando: Boolean) {
        binding.btnEntrar.isEnabled = !cargando
        binding.progresoLogin.visibility = if (cargando) View.VISIBLE else View.GONE
    }

    private fun abrirInicio() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
