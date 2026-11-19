package com.miempresa.inventariomovil.api

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

// Crea una sola vez el cliente Retrofit que usa toda la app.
object RetrofitClient {

    private var servicio: ApiService? = null

    fun reiniciar() {
        servicio = null
    }

    fun obtenerServicio(): ApiService {
        val actual = servicio
        if (actual != null) return actual

        // El interceptor agrega el token de la sesión a todas las peticiones.
        // Así el servidor sabe quién las envía y rechaza a quien no inició sesión.
        val cliente = OkHttpClient.Builder()
            .addInterceptor { cadena ->
                val peticion = cadena.request().newBuilder()
                    .addHeader("X-Token", Sesion.token)
                    .build()
                cadena.proceed(peticion)
            }
            .connectTimeout(8, TimeUnit.SECONDS)
            .readTimeout(8, TimeUnit.SECONDS)
            .build()

        val nuevo = Retrofit.Builder()
            .baseUrl(ApiConfig.BASE_URL)
            .client(cliente)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)

        servicio = nuevo
        return nuevo
    }
}
