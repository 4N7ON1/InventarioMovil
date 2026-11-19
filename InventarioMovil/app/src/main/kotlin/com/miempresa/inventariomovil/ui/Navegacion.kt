package com.miempresa.inventariomovil.ui

import android.app.Activity
import android.content.Intent
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.miempresa.inventariomovil.ConfiguracionActivity
import com.miempresa.inventariomovil.HistorialActivity
import com.miempresa.inventariomovil.InventarioActivity
import com.miempresa.inventariomovil.MainActivity
import com.miempresa.inventariomovil.R
import com.miempresa.inventariomovil.VentaActivity

// La barra inferior es igual en todas las pantallas; aquí se configura una sola vez.
object Navegacion {

    // idActual es el botón de la pantalla en la que estamos (por ejemplo R.id.nav_home).
    fun configurar(activity: Activity, barra: BottomNavigationView, idActual: Int) {
        barra.selectedItemId = idActual
        barra.setOnItemSelectedListener { item ->
            if (item.itemId == idActual) {
                return@setOnItemSelectedListener true
            }
            val destino = when (item.itemId) {
                R.id.nav_home -> MainActivity::class.java
                R.id.nav_inventory -> InventarioActivity::class.java
                R.id.nav_sales -> VentaActivity::class.java
                R.id.nav_payments -> HistorialActivity::class.java
                R.id.nav_profile -> ConfiguracionActivity::class.java
                else -> null
            }
            if (destino != null) {
                activity.startActivity(Intent(activity, destino))
                activity.overridePendingTransition(0, 0)
            }
            // La pantalla de Ajustes no tiene barra, por eso su botón no queda marcado.
            item.itemId != R.id.nav_profile
        }
    }
}
