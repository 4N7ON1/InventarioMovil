package com.miempresa.inventariomovil

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import com.miempresa.inventariomovil.api.InventarioRepository
import com.miempresa.inventariomovil.databinding.ActivityHistorialBinding
import com.miempresa.inventariomovil.model.Venta
import com.miempresa.inventariomovil.ui.EstadoVista
import com.miempresa.inventariomovil.ui.Navegacion
import com.miempresa.inventariomovil.ui.PantallaProtegida
import com.miempresa.inventariomovil.ui.adapter.VentaAdapter
import kotlinx.coroutines.launch

class HistorialActivity : PantallaProtegida() {

    private lateinit var binding: ActivityHistorialBinding
    private lateinit var adapter: VentaAdapter
    private lateinit var estado: EstadoVista
    private val repositorio = InventarioRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHistorialBinding.inflate(layoutInflater)
        setContentView(binding.root)

        estado = EstadoVista(binding.estado)
        adapter = VentaAdapter(this, ArrayList())
        binding.listVentas.adapter = adapter

        binding.listVentas.setOnItemClickListener { _, _, posicion, _ ->
            mostrarOpciones(adapter.getItem(posicion))
        }

        binding.backButton.setOnClickListener { finish() }
        Navegacion.configurar(this, binding.bottomNavigation, R.id.nav_payments)
    }

    override fun onResume() {
        super.onResume()
        binding.bottomNavigation.selectedItemId = R.id.nav_payments
        cargarLista()
    }

    // GET /ventas y GET /resumen
    private fun cargarLista() {
        if (adapter.count == 0) estado.cargando()
        lifecycleScope.launch {
            try {
                val lista = repositorio.listarVentas()
                val resumen = repositorio.obtenerResumen()
                adapter.actualizar(lista)
                binding.txtResumenHistorial.text = "Hoy: Bs " + String.format("%.2f", resumen.ingresoHoy) +
                        "  ·  Utilidad: Bs " + String.format("%.2f", resumen.utilidadHoy)
                if (lista.isEmpty()) {
                    estado.vacio("Todavía no hay ventas registradas")
                } else {
                    estado.ocultar()
                }
            } catch (e: Exception) {
                adapter.actualizar(ArrayList())
                binding.txtResumenHistorial.text = ""
                estado.error(e.message) { cargarLista() }
            }
        }
    }

    // Al tocar una venta se pregunta qué se quiere hacer con ella.
    private fun mostrarOpciones(venta: Venta) {
        val opciones = ArrayList<String>()
        opciones.add("Ver detalle")
        opciones.add("Cambiar productos")
        if (venta.estadoPago == "PENDIENTE") {
            opciones.add("Confirmar pago")
        }
        opciones.add("Anular venta")

        AlertDialog.Builder(this)
            .setTitle("Venta #" + venta.id + " · Bs " + String.format("%.2f", venta.montoTotal))
            .setItems(opciones.toTypedArray()) { _, posicion ->
                when (opciones[posicion]) {
                    "Ver detalle" -> cargarDetalle(venta)
                    "Cambiar productos" -> editar(venta)
                    "Confirmar pago" -> confirmarPago(venta)
                    "Anular venta" -> confirmarAnular(venta)
                }
            }
            .setNegativeButton("Cerrar", null)
            .show()
    }

    // Abre la pantalla de venta en modo edición con el número de la venta.
    private fun editar(venta: Venta) {
        val intent = Intent(this, VentaActivity::class.java)
        intent.putExtra("id_venta", venta.id)
        startActivity(intent)
    }

    // GET /ventas/{id}: trae la venta con sus detalles.
    private fun cargarDetalle(venta: Venta) {
        lifecycleScope.launch {
            try {
                mostrarDetalle(repositorio.obtenerVenta(venta.id))
            } catch (e: Exception) {
                Toast.makeText(this@HistorialActivity, e.message, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun mostrarDetalle(venta: Venta) {
        var texto = "Fecha: " + venta.fechaHora + "\n"
        texto += "Método: " + venta.metodoPago + "\n"
        texto += "Estado: " + venta.estadoPago + "\n\n"
        for (detalle in venta.detalles) {
            texto += detalle.toString() + "\n"
        }
        texto += "\nTOTAL: Bs " + String.format("%.2f", venta.montoTotal)

        AlertDialog.Builder(this)
            .setTitle("Venta #" + venta.id)
            .setMessage(texto)
            .setPositiveButton("Cerrar", null)
            .show()
    }

    // PUT /ventas/{id}: cambia el estado de PENDIENTE a PAGADO.
    private fun confirmarPago(venta: Venta) {
        lifecycleScope.launch {
            try {
                val respuesta = repositorio.confirmarPago(venta.id)
                Toast.makeText(this@HistorialActivity, respuesta.mensaje, Toast.LENGTH_SHORT).show()
                cargarLista()
            } catch (e: Exception) {
                Toast.makeText(this@HistorialActivity, e.message, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun confirmarAnular(venta: Venta) {
        AlertDialog.Builder(this)
            .setTitle("Anular venta")
            .setMessage("Se eliminará la venta #" + venta.id + " y se devolverá el stock. ¿Continuar?")
            .setPositiveButton("Anular") { _, _ -> anular(venta) }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    // DELETE /ventas/{id}: el servidor devuelve el stock y borra la venta.
    private fun anular(venta: Venta) {
        lifecycleScope.launch {
            try {
                val respuesta = repositorio.anularVenta(venta.id)
                Toast.makeText(this@HistorialActivity, respuesta.mensaje, Toast.LENGTH_SHORT).show()
                cargarLista()
            } catch (e: Exception) {
                Toast.makeText(this@HistorialActivity, e.message, Toast.LENGTH_LONG).show()
            }
        }
    }
}
