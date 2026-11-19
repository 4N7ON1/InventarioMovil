package com.miempresa.inventariomovil.api

import android.content.Context

// Guarda la dirección del servidor. BASE_URL es la única variable que hay que
// cambiar para que la app apunte a otra computadora.
object ApiConfig {

    private const val PREFERENCIAS = "config_servidor"
    private const val CLAVE_IP = "direccion_ip"
    const val IP_EMULADOR = "10.0.2.2"
    const val CARPETA_API = "inventario_api"

    var direccionIp = IP_EMULADOR
        private set

    var BASE_URL = armarUrl(IP_EMULADOR)
        private set

    fun armarUrl(ip: String): String = "http://$ip/$CARPETA_API/"

    // Lee la IP guardada. Se llama una vez al abrir la app (InventarioApp).
    fun cargar(context: Context) {
        val preferencias = context.getSharedPreferences(PREFERENCIAS, Context.MODE_PRIVATE)
        aplicar(preferencias.getString(CLAVE_IP, IP_EMULADOR) ?: IP_EMULADOR)
    }

    // Guarda una IP nueva y reinicia Retrofit para que use la nueva dirección.
    fun guardar(context: Context, ip: String) {
        val preferencias = context.getSharedPreferences(PREFERENCIAS, Context.MODE_PRIVATE)
        preferencias.edit().putString(CLAVE_IP, ip).apply()
        aplicar(ip)
    }

    private fun aplicar(ip: String) {
        direccionIp = ip
        BASE_URL = armarUrl(ip)
        RetrofitClient.reiniciar()
    }
}
