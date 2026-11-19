package com.miempresa.inventariomovil

import android.content.Context
import android.os.Bundle
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import com.miempresa.inventariomovil.api.InventarioRepository
import com.miempresa.inventariomovil.databinding.ActivityCategoriasBinding
import com.miempresa.inventariomovil.model.Categoria
import com.miempresa.inventariomovil.ui.EstadoVista
import com.miempresa.inventariomovil.ui.PantallaProtegida
import kotlinx.coroutines.launch

class CategoriasActivity : PantallaProtegida() {

    private lateinit var binding: ActivityCategoriasBinding
    private lateinit var estado: EstadoVista
    private val repositorio = InventarioRepository()
    private var listaCategorias: List<Categoria> = ArrayList()
    private var idEditando = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCategoriasBinding.inflate(layoutInflater)
        setContentView(binding.root)

        estado = EstadoVista(binding.estado)

        binding.backButton.setOnClickListener { finish() }
        binding.btnGuardarCategoria.setOnClickListener { guardar() }
        binding.btnCancelarCategoria.setOnClickListener { limpiar() }
        binding.btnNuevaCategoria.setOnClickListener { nuevaCategoria() }

        // Tocar una categoría la carga en el formulario para editarla.
        binding.listCategorias.setOnItemClickListener { _, _, posicion, _ ->
            val categoria = listaCategorias[posicion]
            idEditando = categoria.id
            binding.edtNombreCategoria.setText(categoria.nombre)
            binding.edtDescripcionCategoria.setText(categoria.descripcion)
            binding.txtModoCategoria.text = "Editando: " + categoria.nombre
            binding.btnGuardarCategoria.text = "Guardar cambios"
            binding.btnCancelarCategoria.isEnabled = true
        }

        // Mantener presionado pide confirmación para eliminar.
        binding.listCategorias.setOnItemLongClickListener { _, _, posicion, _ ->
            confirmarEliminar(listaCategorias[posicion])
            true
        }

        cargarLista()
    }

    // GET /categorias: cada categoría llega con su cantidad de productos.
    private fun cargarLista() {
        if (listaCategorias.isEmpty()) estado.cargando()
        lifecycleScope.launch {
            try {
                listaCategorias = repositorio.listarCategorias()
                val textos = listaCategorias.map {
                    it.nombre + "  (" + it.totalProductos + " productos)\n" + it.descripcion
                }
                binding.listCategorias.adapter =
                    ArrayAdapter(this@CategoriasActivity, android.R.layout.simple_list_item_1, textos)
                if (listaCategorias.isEmpty()) {
                    estado.vacio("No hay categorías registradas")
                } else {
                    estado.ocultar()
                }
            } catch (e: Exception) {
                estado.error(e.message) { cargarLista() }
            }
        }
    }

    // POST /categorias si es nueva, PUT /categorias/{id} si se está editando.
    private fun guardar() {
        val nombre = binding.edtNombreCategoria.text.toString().trim()
        val descripcion = binding.edtDescripcionCategoria.text.toString().trim()

        if (nombre.isEmpty()) {
            binding.edtNombreCategoria.error = "El nombre es obligatorio"
            return
        }
        if (nombre.length < 3) {
            binding.edtNombreCategoria.error = "Mínimo 3 caracteres"
            return
        }

        binding.btnGuardarCategoria.isEnabled = false
        lifecycleScope.launch {
            try {
                val categoria = Categoria(idEditando, nombre, descripcion)
                val respuesta = if (idEditando == 0) {
                    repositorio.crearCategoria(categoria)
                } else {
                    repositorio.actualizarCategoria(categoria)
                }
                Toast.makeText(this@CategoriasActivity, respuesta.mensaje, Toast.LENGTH_SHORT).show()
                limpiar()
                cargarLista()
            } catch (e: Exception) {
                // Por ejemplo: "Ya existe una categoría llamada Bebidas" (lo valida el servidor).
                binding.edtNombreCategoria.error = e.message
            }
            binding.btnGuardarCategoria.isEnabled = true
        }
    }

    private fun confirmarEliminar(categoria: Categoria) {
        if (categoria.totalProductos > 0) {
            Toast.makeText(this, "No se puede eliminar: la categoría tiene productos", Toast.LENGTH_LONG).show()
            return
        }
        AlertDialog.Builder(this)
            .setTitle("Eliminar categoría")
            .setMessage("¿Seguro que deseas eliminar \"" + categoria.nombre + "\"?")
            .setPositiveButton("Eliminar") { _, _ -> eliminar(categoria) }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    // DELETE /categorias/{id}
    private fun eliminar(categoria: Categoria) {
        lifecycleScope.launch {
            try {
                val respuesta = repositorio.eliminarCategoria(categoria.id)
                Toast.makeText(this@CategoriasActivity, respuesta.mensaje, Toast.LENGTH_SHORT).show()
                limpiar()
                cargarLista()
            } catch (e: Exception) {
                Toast.makeText(this@CategoriasActivity, e.message, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun limpiar() {
        idEditando = 0
        binding.edtNombreCategoria.setText("")
        binding.edtDescripcionCategoria.setText("")
        binding.edtNombreCategoria.error = null
        binding.txtModoCategoria.text = "Nueva categoría"
        binding.btnGuardarCategoria.text = "Agregar categoría"
        binding.btnCancelarCategoria.isEnabled = false
    }

    private fun nuevaCategoria() {
        limpiar()
        binding.edtNombreCategoria.requestFocus()
        val teclado = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        teclado.showSoftInput(binding.edtNombreCategoria, InputMethodManager.SHOW_IMPLICIT)
    }
}
