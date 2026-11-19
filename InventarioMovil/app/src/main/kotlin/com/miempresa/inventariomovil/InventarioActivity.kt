package com.miempresa.inventariomovil

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.miempresa.inventariomovil.api.InventarioRepository
import com.miempresa.inventariomovil.databinding.ActivityInventarioBinding
import com.miempresa.inventariomovil.model.Producto
import com.miempresa.inventariomovil.ui.EstadoVista
import com.miempresa.inventariomovil.ui.Navegacion
import com.miempresa.inventariomovil.ui.PantallaProtegida
import com.miempresa.inventariomovil.ui.adapter.ProductoAdapter
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class InventarioActivity : PantallaProtegida() {

    private lateinit var binding: ActivityInventarioBinding
    private lateinit var adapter: ProductoAdapter
    private lateinit var estado: EstadoVista
    private val repositorio = InventarioRepository()
    private var filtroActivo = "Todos"
    private var busqueda: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityInventarioBinding.inflate(layoutInflater)
        setContentView(binding.root)

        estado = EstadoVista(binding.estado)

        adapter = ProductoAdapter(ArrayList(), { abrirFormulario(it) }, { confirmarEliminar(it) })
        binding.recyclerView.adapter = adapter
        binding.recyclerView.layoutManager = LinearLayoutManager(this)

        crearFiltros()

        binding.searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                cargarLista(400)
            }
        })

        binding.addButton.setOnClickListener {
            val opciones = arrayOf("Nuevo producto", "Gestionar categorías")
            AlertDialog.Builder(this)
                .setTitle("Agregar")
                .setItems(opciones) { _, opcion ->
                    if (opcion == 0) {
                        startActivity(Intent(this, ProductoFormActivity::class.java))
                    } else {
                        startActivity(Intent(this, CategoriasActivity::class.java))
                    }
                }
                .show()
        }

        binding.backButton.setOnClickListener { finish() }
        Navegacion.configurar(this, binding.bottomNavigation, R.id.nav_inventory)
    }

    override fun onResume() {
        super.onResume()
        binding.bottomNavigation.selectedItemId = R.id.nav_inventory
        cargarLista(0)
    }

    // Pide los productos a la API (GET /productos?buscar=texto).
    // "espera" son los milisegundos que se aguarda antes de consultar: mientras el
    // usuario sigue escribiendo se cancela la búsqueda anterior y no se satura el servidor.
    private fun cargarLista(espera: Long) {
        busqueda?.cancel()
        busqueda = lifecycleScope.launch {
            delay(espera)
            if (adapter.total() == 0) estado.cargando()
            try {
                val texto = binding.searchInput.text.toString().trim()
                val lista = repositorio.listarProductos(texto)
                adapter.actualizar(lista)
                actualizarContador()
            } catch (e: Exception) {
                adapter.actualizar(ArrayList())
                binding.counterText.text = ""
                estado.error(e.message) { cargarLista(0) }
            }
        }
    }

    private fun abrirFormulario(producto: Producto) {
        val intent = Intent(this, ProductoFormActivity::class.java)
        intent.putExtra("id_producto", producto.id)
        startActivity(intent)
    }

    private fun confirmarEliminar(producto: Producto) {
        AlertDialog.Builder(this)
            .setTitle("Eliminar producto")
            .setMessage("¿Seguro que deseas eliminar \"" + producto.nombre + "\"?")
            .setPositiveButton("Eliminar") { _, _ -> eliminar(producto) }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    // DELETE /productos/{id}. Si el producto tiene ventas el servidor lo rechaza con un mensaje.
    private fun eliminar(producto: Producto) {
        lifecycleScope.launch {
            try {
                val respuesta = repositorio.eliminarProducto(producto.id)
                Toast.makeText(this@InventarioActivity, respuesta.mensaje, Toast.LENGTH_SHORT).show()
                cargarLista(0)
            } catch (e: Exception) {
                Toast.makeText(this@InventarioActivity, e.message, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun crearFiltros() {
        val filtros = listOf("Todos", "Disponibles", "Stock bajo", "Agotados")
        for (filtro in filtros) {
            binding.filtrosContainer.addView(crearChip(filtro))
        }
        pintarFiltros()
    }

    private fun crearChip(texto: String): View {
        val chip = LayoutInflater.from(this)
            .inflate(R.layout.chip_filtro, binding.filtrosContainer, false) as TextView
        chip.text = texto
        chip.tag = texto
        chip.setOnClickListener {
            filtroActivo = texto
            adapter.filtrar(texto)
            actualizarContador()
            pintarFiltros()
        }
        return chip
    }

    private fun pintarFiltros() {
        for (i in 0 until binding.filtrosContainer.childCount) {
            val chip = binding.filtrosContainer.getChildAt(i) as? TextView ?: continue
            if (chip.tag == filtroActivo) {
                chip.setBackgroundResource(R.drawable.shape_chip_selected)
                chip.setTextColor(getColor(R.color.white))
            } else {
                chip.setBackgroundResource(R.drawable.shape_chip_unselected)
                chip.setTextColor(getColor(R.color.text_secondary))
            }
        }
    }

    private fun actualizarContador() {
        binding.counterText.text = adapter.itemCount.toString() + " de " + adapter.total() + " productos"
        if (adapter.itemCount == 0) {
            estado.vacio("No hay productos para mostrar")
        } else {
            estado.ocultar()
        }
    }
}
