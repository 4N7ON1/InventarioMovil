package com.miempresa.inventariomovil.ui

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import com.miempresa.inventariomovil.LoginActivity
import com.miempresa.inventariomovil.api.Sesion

// Todas las pantallas que necesitan sesión heredan de esta clase en vez de AppCompatActivity.
// Cada vez que una pantalla se muestra (onStart) revisa dos cosas:
//   1. que exista una sesión iniciada, y
//   2. que la app no haya estado demasiado tiempo en segundo plano.
// Si algo falla, cierra la sesión y manda al login.
open class PantallaProtegida : AppCompatActivity() {

    companion object {
        // Cuántas pantallas de la app están visibles. Cuando llega a 0 la app quedó
        // en segundo plano (el usuario salió a WhatsApp, bloqueó el celular, etc.).
        private var pantallasVisibles = 0
    }

    override fun onStart() {
        super.onStart()
        pantallasVisibles++
        if (!Sesion.hayToken() || Sesion.vencio()) {
            Sesion.cerrar()
            irAlLogin()
            return
        }
        Sesion.marcarRegreso()
    }

    override fun onStop() {
        super.onStop()
        pantallasVisibles--
        if (pantallasVisibles == 0) {
            Sesion.marcarSalida()
        }
    }

    // Abre el login y cierra todas las pantallas anteriores para que "atrás" no regrese a ellas.
    fun irAlLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
