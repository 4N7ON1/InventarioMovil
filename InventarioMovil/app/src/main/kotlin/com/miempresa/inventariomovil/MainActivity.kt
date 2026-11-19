package com.miempresa.inventariomovil

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import com.miempresa.inventariomovil.api.ApiConfig
import com.miempresa.inventariomovil.api.InventarioRepository
import com.miempresa.inventariomovil.api.Sesion
import com.miempresa.inventariomovil.databinding.ActivityMainBinding
import com.miempresa.inventariomovil.model.Venta
import com.miempresa.inventariomovil.ui.Navegacion
import com.miempresa.inventariomovil.ui.PantallaProtegida
import kotlinx.coroutines.launch

class MainActivity : PantallaProtegida() {

    private lateinit var binding: ActivityMainBinding
    private val repositorio = InventarioRepository()

    // Avisos de stock que llegaron del servidor: "Arroz 1 kg: quedan 3".
    private var alertas: List<String> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        Navegacion.configurar(this, binding.bottomNavigation, R.id.nav_home)

        // Saludo con el nombre de quien inició sesión y su inicial en el círculo.
        binding.txtSaludo.text = "Hola, " + Sesion.nombre + " 👋"
        binding.txtInicial.text = Sesion.nombre.take(1).uppercase()

        binding.btnAlertas.setOnClickListener { mostrarAlertas() }
        binding.tarjetaAlerta.setOnClickListener {
            startActivity(Intent(this, InventarioActivity::class.java))
        }

        binding.actionAddProduct.setOnClickListener {
            startActivity(Intent(this, ProductoFormActivity::class.java))
        }
        binding.actionNewSale.setOnClickListener {
            startActivity(Intent(this, VentaActivity::class.java))
        }
        binding.actionCategories.setOnClickListener {
            startActivity(Intent(this, CategoriasActivity::class.java))
        }
        binding.actionServer.setOnClickListener {
            startActivity(Intent(this, ConfiguracionActivity::class.java))
        }
        binding.inventoryLink.setOnClickListener {
            startActivity(Intent(this, InventarioActivity::class.java))
        }
        binding.salesLink.setOnClickListener {
            startActivity(Intent(this, HistorialActivity::class.java))
        }
        binding.txtEstadoServidor.setOnClickListener {
            startActivity(Intent(this, ConfiguracionActivity::class.java))
        }

        // Botón "atrás" en el inicio: en vez de salir sin más, pregunta y cierra la sesión.
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                confirmarSalida()
            }
        })
    }

    private fun confirmarSalida() {
        AlertDialog.Builder(this)
            .setTitle("Salir de la app")
            .setMessage("Se cerrará tu sesión. Para volver a entrar tendrás que escribir tu usuario y tu clave.")
            .setPositiveButton("Salir") { _, _ -> salir() }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    // Avisa al servidor (POST /logout) para que el token deje de servir,
    // borra la sesión del celular y cierra la app.
    private fun salir() {
        lifecycleScope.launch {
            try {
                repositorio.cerrarSesion()
            } catch (e: Exception) {
                // Sin conexión igual se sale: el token vence solo en el servidor a los 30 minutos.
            }
            Sesion.cerrar()
            finishAffinity()
        }
    }

    override fun onResume() {
        super.onResume()
        binding.bottomNavigation.selectedItemId = R.id.nav_home
        cargarResumen()
    }

    // Pide a la API los indicadores del día y las últimas ventas.
    private fun cargarResumen() {
        binding.txtEstadoServidor.text = "Conectando con " + ApiConfig.direccionIp + "..."
        lifecycleScope.launch {
            try {
                val resumen = repositorio.obtenerResumen()
                val ventas = repositorio.listarVentas()

                binding.txtEstadoServidor.text = "Conectado al servidor " + ApiConfig.direccionIp
                binding.txtIngresoHoy.text = "Bs " + String.format("%.2f", resumen.ingresoHoy)
                binding.txtVentasHoy.text = resumen.ventasHoy.toString()
                binding.txtTotalProductos.text = resumen.totalProductos.toString()
                binding.txtStockBajo.text = resumen.stockBajo.toString()
                binding.txtAgotados.text = resumen.agotados.toString()
                mostrarUltimasVentas(ventas)
                pintarAlertas(resumen.alertas ?: emptyList())
            } catch (e: Exception) {
                mostrarSinConexion(e.message)
            }
        }
    }

    // ALERTA DE STOCK: muestra la tarjeta roja solo si hay productos por agotarse.
    private fun pintarAlertas(lista: List<String>) {
        alertas = lista
        if (lista.isEmpty()) {
            binding.tarjetaAlerta.visibility = View.GONE
            binding.puntoAlerta.visibility = View.GONE
            return
        }
        binding.tarjetaAlerta.visibility = View.VISIBLE
        binding.puntoAlerta.visibility = View.VISIBLE
        binding.txtAlertaTitulo.text = "Productos por agotarse (" + lista.size + ")"

        // En la tarjeta caben 3; el resto se ve tocando la campana.
        var texto = lista.take(3).joinToString("\n")
        if (lista.size > 3) {
            texto += "\ny " + (lista.size - 3) + " más..."
        }
        binding.txtAlertaDetalle.text = texto
    }

    // La campana abre la lista completa de avisos.
    private fun mostrarAlertas() {
        val mensaje = if (alertas.isEmpty()) {
            "Todo bien: ningún producto está por agotarse."
        } else {
            alertas.joinToString("\n")
        }
        AlertDialog.Builder(this)
            .setTitle("Avisos de stock")
            .setMessage(mensaje)
            .setPositiveButton("Entendido", null)
            .show()
    }

    private fun mostrarUltimasVentas(ventas: List<Venta>) {
        if (ventas.isEmpty()) {
            binding.txtVenta1Titulo.text = "Todavía no hay ventas"
            binding.txtVenta1Detalle.text = "Registra tu primera venta"
            binding.txtVenta1Monto.text = ""
        } else {
            binding.txtVenta1Titulo.text = tituloVenta(ventas[0])
            binding.txtVenta1Detalle.text = detalleVenta(ventas[0])
            binding.txtVenta1Monto.text = "Bs " + String.format("%.2f", ventas[0].montoTotal)
        }

        if (ventas.size >= 2) {
            binding.filaVenta2.visibility = View.VISIBLE
            binding.divisorVentas.visibility = View.VISIBLE
            binding.txtVenta2Titulo.text = tituloVenta(ventas[1])
            binding.txtVenta2Detalle.text = detalleVenta(ventas[1])
            binding.txtVenta2Monto.text = "Bs " + String.format("%.2f", ventas[1].montoTotal)
        } else {
            binding.filaVenta2.visibility = View.GONE
            binding.divisorVentas.visibility = View.GONE
        }
    }

    private fun tituloVenta(venta: Venta): String = "Venta #" + venta.id + " · " + venta.metodoPago

    // La fecha llega como "2026-11-15 14:30:00"; se muestra solo la hora (14:30).
    private fun detalleVenta(venta: Venta): String {
        val hora = if (venta.fechaHora.length >= 16) venta.fechaHora.substring(11, 16) else venta.fechaHora
        return hora + " · " + venta.totalProductos + " producto(s) · " + venta.estadoPago
    }

    private fun mostrarSinConexion(mensaje: String?) {
        binding.txtEstadoServidor.text = "Sin conexión con el servidor. Toca aquí para configurar."
        binding.txtIngresoHoy.text = "Bs --"
        binding.txtVentasHoy.text = "-"
        binding.txtTotalProductos.text = "-"
        binding.txtStockBajo.text = "-"
        binding.txtAgotados.text = "-"
        pintarAlertas(emptyList())
        binding.txtVenta1Titulo.text = "No se pudieron cargar las ventas"
        binding.txtVenta1Detalle.text = mensaje ?: ""
        binding.txtVenta1Monto.text = ""
        binding.filaVenta2.visibility = View.GONE
        binding.divisorVentas.visibility = View.GONE
    }
}
