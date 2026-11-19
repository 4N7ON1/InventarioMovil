package com.miempresa.inventariomovil.api

import com.miempresa.inventariomovil.model.Categoria
import com.miempresa.inventariomovil.model.Credenciales
import com.miempresa.inventariomovil.model.ImagenQr
import com.miempresa.inventariomovil.model.Producto
import com.miempresa.inventariomovil.model.RespuestaApi
import com.miempresa.inventariomovil.model.RespuestaLogin
import com.miempresa.inventariomovil.model.Resumen
import com.miempresa.inventariomovil.model.Venta
import com.miempresa.inventariomovil.model.VentaNueva
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

// Contrato de la API: cada función es una ruta del servidor.
interface ApiService {

    @GET(".")
    suspend fun probarConexion(): Response<RespuestaApi>

    // ---------- SESION ----------
    @POST("login")
    suspend fun iniciarSesion(@Body credenciales: Credenciales): Response<RespuestaLogin>

    @POST("logout")
    suspend fun cerrarSesion(): Response<RespuestaApi>

    // ---------- QR DE COBRO ----------
    @GET("qr")
    suspend fun obtenerQr(): Response<ImagenQr>

    @POST("qr")
    suspend fun subirQr(@Body imagen: ImagenQr): Response<RespuestaApi>

    @GET("resumen")
    suspend fun obtenerResumen(): Response<Resumen>

    // ---------- CATEGORIAS ----------
    @GET("categorias")
    suspend fun listarCategorias(): Response<List<Categoria>>

    @POST("categorias")
    suspend fun crearCategoria(@Body categoria: Categoria): Response<RespuestaApi>

    @PUT("categorias/{id}")
    suspend fun actualizarCategoria(@Path("id") id: Int, @Body categoria: Categoria): Response<RespuestaApi>

    @DELETE("categorias/{id}")
    suspend fun eliminarCategoria(@Path("id") id: Int): Response<RespuestaApi>

    // ---------- PRODUCTOS ----------
    @GET("productos")
    suspend fun listarProductos(@Query("buscar") buscar: String): Response<List<Producto>>

    @GET("productos/{id}")
    suspend fun obtenerProducto(@Path("id") id: Int): Response<Producto>

    @POST("productos")
    suspend fun crearProducto(@Body producto: Producto): Response<RespuestaApi>

    @PUT("productos/{id}")
    suspend fun actualizarProducto(@Path("id") id: Int, @Body producto: Producto): Response<RespuestaApi>

    @DELETE("productos/{id}")
    suspend fun eliminarProducto(@Path("id") id: Int): Response<RespuestaApi>

    // ---------- VENTAS ----------
    @GET("ventas")
    suspend fun listarVentas(): Response<List<Venta>>

    @GET("ventas/{id}")
    suspend fun obtenerVenta(@Path("id") id: Int): Response<Venta>

    @POST("ventas")
    suspend fun registrarVenta(@Body venta: VentaNueva): Response<RespuestaApi>

    // PUT con la lista de productos: cambia los productos de la venta.
    @PUT("ventas/{id}")
    suspend fun actualizarVenta(@Path("id") id: Int, @Body venta: VentaNueva): Response<RespuestaApi>

    // PUT sin cuerpo: confirma el pago de una venta pendiente.
    @PUT("ventas/{id}")
    suspend fun confirmarPago(@Path("id") id: Int): Response<RespuestaApi>

    @DELETE("ventas/{id}")
    suspend fun anularVenta(@Path("id") id: Int): Response<RespuestaApi>
}
