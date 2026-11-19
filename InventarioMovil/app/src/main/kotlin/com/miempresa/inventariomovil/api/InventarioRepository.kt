package com.miempresa.inventariomovil.api

import com.google.gson.Gson
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
import java.io.IOException

// Capa de datos de la app. Reemplaza al DBHelper del Hito 3:
// antes cada función hacía una consulta SQLite, ahora hace una petición a la API.
// Si algo sale mal lanza una Exception con un mensaje listo para mostrar.
class InventarioRepository {

    private fun api() = RetrofitClient.obtenerServicio()

    // Ejecuta una petición y devuelve los datos. Aquí se manejan todos los errores:
    // sin conexión (IOException) o respuesta de error del servidor (400, 404, 409, 500).
    private suspend fun <T> ejecutar(peticion: suspend () -> Response<T>): T {
        val respuesta = try {
            peticion()
        } catch (e: IOException) {
            throw Exception("No se pudo conectar con el servidor. Revisa la red y la dirección IP en Ajustes.")
        }

        val datos = respuesta.body()
        if (respuesta.isSuccessful && datos != null) {
            return datos
        }
        // 401 = el servidor ya no reconoce el token. Se borra la sesión del celular
        // y la siguiente pantalla que se abra mandará al login.
        if (respuesta.code() == 401) {
            Sesion.cerrar()
        }
        throw Exception(leerMensajeError(respuesta))
    }

    // El servidor responde los errores como {"ok": false, "mensaje": "..."}.
    private fun leerMensajeError(respuesta: Response<*>): String {
        return try {
            val texto = respuesta.errorBody()?.string()
            Gson().fromJson(texto, RespuestaApi::class.java).mensaje
        } catch (e: Exception) {
            "Error del servidor (código " + respuesta.code() + ")"
        }
    }

    // ---------- SESION ----------

    suspend fun iniciarSesion(usuario: String, clave: String): RespuestaLogin =
        ejecutar { api().iniciarSesion(Credenciales(usuario, clave)) }

    suspend fun cerrarSesion(): RespuestaApi = ejecutar { api().cerrarSesion() }

    // ---------- QR DE COBRO ----------

    // Devuelve la imagen del QR en Base64. Si todavía no se subió, lanza el error del servidor.
    suspend fun obtenerQr(): String = ejecutar { api().obtenerQr() }.imagen

    suspend fun subirQr(textoBase64: String): RespuestaApi =
        ejecutar { api().subirQr(ImagenQr(textoBase64)) }

    suspend fun probarConexion(): RespuestaApi = ejecutar { api().probarConexion() }

    suspend fun obtenerResumen(): Resumen = ejecutar { api().obtenerResumen() }

    // ---------- CATEGORIAS ----------

    suspend fun listarCategorias(): List<Categoria> = ejecutar { api().listarCategorias() }

    suspend fun crearCategoria(categoria: Categoria): RespuestaApi = ejecutar { api().crearCategoria(categoria) }

    suspend fun actualizarCategoria(categoria: Categoria): RespuestaApi =
        ejecutar { api().actualizarCategoria(categoria.id, categoria) }

    suspend fun eliminarCategoria(id: Int): RespuestaApi = ejecutar { api().eliminarCategoria(id) }

    // ---------- PRODUCTOS ----------

    suspend fun listarProductos(buscar: String): List<Producto> = ejecutar { api().listarProductos(buscar) }

    suspend fun obtenerProducto(id: Int): Producto = ejecutar { api().obtenerProducto(id) }

    suspend fun crearProducto(producto: Producto): RespuestaApi = ejecutar { api().crearProducto(producto) }

    suspend fun actualizarProducto(producto: Producto): RespuestaApi =
        ejecutar { api().actualizarProducto(producto.id, producto) }

    suspend fun eliminarProducto(id: Int): RespuestaApi = ejecutar { api().eliminarProducto(id) }

    // ---------- VENTAS ----------

    suspend fun listarVentas(): List<Venta> = ejecutar { api().listarVentas() }

    suspend fun obtenerVenta(id: Int): Venta = ejecutar { api().obtenerVenta(id) }

    suspend fun registrarVenta(venta: VentaNueva): RespuestaApi = ejecutar { api().registrarVenta(venta) }

    suspend fun actualizarVenta(id: Int, venta: VentaNueva): RespuestaApi =
        ejecutar { api().actualizarVenta(id, venta) }

    suspend fun confirmarPago(id: Int): RespuestaApi = ejecutar { api().confirmarPago(id) }

    suspend fun anularVenta(id: Int): RespuestaApi = ejecutar { api().anularVenta(id) }
}
