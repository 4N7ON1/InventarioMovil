package com.miempresa.inventariomovil

import android.app.Application
import com.miempresa.inventariomovil.api.ApiConfig

// Se ejecuta una sola vez al abrir la app, antes que cualquier pantalla.
// La sesión NO se carga aquí: empieza vacía cada vez que se abre la app,
// así que siempre se pide el login al volver a abrirla.
class InventarioApp : Application() {
    override fun onCreate() {
        super.onCreate()
        ApiConfig.cargar(this)
    }
}
