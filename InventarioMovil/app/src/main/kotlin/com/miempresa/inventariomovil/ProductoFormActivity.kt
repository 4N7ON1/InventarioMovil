package com.miempresa.inventariomovil

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.text.InputType
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import com.miempresa.inventariomovil.api.InventarioRepository
import com.miempresa.inventariomovil.databinding.ActivityProductoFormBinding
import com.miempresa.inventariomovil.model.Categoria
import com.miempresa.inventariomovil.model.Producto
import com.miempresa.inventariomovil.ui.PantallaProtegida
import kotlinx.coroutines.launch

class ProductoFormActivity : PantallaProtegida() {

    private lateinit var binding: ActivityProductoFormBinding
    private val repositorio = InventarioRepository()
    private var listaCategorias: List<Categoria> = ArrayList()
    private val unidades = arrayOf("Unidad", "Kg", "Litro", "Docena", "Paquete", "Caja")
    private var idProducto = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProductoFormBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.spnUnidad.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, unidades)

        // Si llega un id es edición; si no, es un producto nuevo.
        idProducto = intent.getIntExtra("id_producto", 0)
        if (idProducto > 0) {
            binding.txtTituloForm.text = "Editar producto"
            binding.btnGuardarProducto.text = "Guardar cambios"
        }

        // Cada vez que cambia un precio se vuelve a calcular la ganancia.
        val alEscribirPrecio = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                mostrarGanancia()
            }
        }
        binding.edtPrecioCompra.addTextChangedListener(alEscribirPrecio)
        binding.edtPrecioVenta.addTextChangedListener(alEscribirPrecio)

        binding.btnNuevaCategoria.setOnClickListener { crearCategoria() }
        binding.backButton.setOnClickListener { finish() }
        binding.btnGuardarProducto.setOnClickListener { guardar() }
        binding.btnCancelarProducto.setOnClickListener { finish() }

        cargarDatos()
    }

    // Carga las categorías (GET /categorias) y, si es edición, el producto (GET /productos/{id}).
    private fun cargarDatos() {
        binding.btnGuardarProducto.isEnabled = false
        lifecycleScope.launch {
            try {
                listaCategorias = repositorio.listarCategorias()
                if (listaCategorias.isEmpty()) {
                    Toast.makeText(this@ProductoFormActivity, "Primero registra una categoría", Toast.LENGTH_LONG).show()
                    finish()
                    return@launch
                }
                mostrarCategorias(0)
                if (idProducto > 0) {
                    mostrarProducto(repositorio.obtenerProducto(idProducto))
                }
                binding.btnGuardarProducto.isEnabled = true
            } catch (e: Exception) {
                Toast.makeText(this@ProductoFormActivity, e.message, Toast.LENGTH_LONG).show()
                finish()
            }
        }
    }

    private fun mostrarCategorias(idSeleccionar: Int) {
        binding.spnCategoria.adapter =
            ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, listaCategorias)
        for (i in listaCategorias.indices) {
            if (listaCategorias[i].id == idSeleccionar) binding.spnCategoria.setSelection(i)
        }
    }

    private fun mostrarProducto(producto: Producto) {
        binding.edtNombreProducto.setText(producto.nombre)
        binding.edtDescripcionProducto.setText(producto.descripcion)
        binding.edtPrecioCompra.setText(producto.precioCompra.toString())
        binding.edtPrecioVenta.setText(producto.precioVenta.toString())
        binding.edtStockActual.setText(producto.stockActual.toString())
        binding.edtStockMinimo.setText(producto.stockMinimo.toString())

        for (i in listaCategorias.indices) {
            if (listaCategorias[i].id == producto.idCategoria) binding.spnCategoria.setSelection(i)
        }
        val posicionUnidad = unidades.indexOf(producto.unidadMedida)
        if (posicionUnidad >= 0) binding.spnUnidad.setSelection(posicionUnidad)
    }

    // Muestra cuánto se gana por cada unidad (precio de venta menos precio de compra).
    private fun mostrarGanancia() {
        val compra = binding.edtPrecioCompra.text.toString().toDoubleOrNull()
        val venta = binding.edtPrecioVenta.text.toString().toDoubleOrNull()
        if (compra == null || venta == null) {
            binding.txtGanancia.text = ""
            return
        }
        val ganancia = venta - compra
        if (ganancia >= 0) {
            binding.txtGanancia.setTextColor(getColor(R.color.ok_fg))
            binding.txtGanancia.text = "Ganas Bs " + String.format("%.2f", ganancia) + " por cada uno"
        } else {
            binding.txtGanancia.setTextColor(getColor(R.color.err_fg))
            binding.txtGanancia.text = "Cuidado: lo estás vendiendo más barato de lo que te cuesta"
        }
    }

    // Diálogo para crear una categoría sin salir del formulario (POST /categorias).
    private fun crearCategoria() {
        val edtNombre = EditText(this)
        edtNombre.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
        edtNombre.hint = "Nombre de la categoría"

        AlertDialog.Builder(this)
            .setTitle("Nueva categoría")
            .setView(edtNombre)
            .setPositiveButton("Agregar") { _, _ ->
                val nombre = edtNombre.text.toString().trim()
                if (nombre.isEmpty()) {
                    Toast.makeText(this, "El nombre es obligatorio", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                lifecycleScope.launch {
                    try {
                        val respuesta = repositorio.crearCategoria(Categoria(0, nombre, ""))
                        listaCategorias = repositorio.listarCategorias()
                        mostrarCategorias(respuesta.id)
                        Toast.makeText(this@ProductoFormActivity, "Categoría \"" + nombre + "\" agregada", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        Toast.makeText(this@ProductoFormActivity, e.message, Toast.LENGTH_LONG).show()
                    }
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    // Valida el formulario y envía el producto: POST si es nuevo, PUT si es edición.
    private fun guardar() {
        val nombre = binding.edtNombreProducto.text.toString().trim()
        val descripcion = binding.edtDescripcionProducto.text.toString().trim()
        val precioCompra = binding.edtPrecioCompra.text.toString().toDoubleOrNull()
        val precioVenta = binding.edtPrecioVenta.text.toString().toDoubleOrNull()
        val stockActual = binding.edtStockActual.text.toString().toIntOrNull()
        val stockMinimo = binding.edtStockMinimo.text.toString().toIntOrNull()

        if (nombre.isEmpty()) {
            binding.edtNombreProducto.error = "Escribe el nombre del producto"
            return
        }
        if (precioCompra == null || precioCompra < 0) {
            binding.edtPrecioCompra.error = "Escribe a cuánto lo compras"
            return
        }
        if (precioVenta == null || precioVenta <= 0) {
            binding.edtPrecioVenta.error = "Escribe a cuánto lo vendes"
            return
        }
        if (precioVenta < precioCompra) {
            binding.edtPrecioVenta.error = "No puede ser menor al precio de compra"
            return
        }
        if (stockActual == null || stockActual < 0) {
            binding.edtStockActual.error = "Escribe cuántos tienes"
            return
        }
        if (stockMinimo == null || stockMinimo < 0) {
            binding.edtStockMinimo.error = "Escribe con cuántos quieres que te avise"
            return
        }

        val categoria = binding.spnCategoria.selectedItem as Categoria
        val producto = Producto(
            id = idProducto,
            nombre = nombre,
            descripcion = descripcion,
            precioCompra = precioCompra,
            precioVenta = precioVenta,
            stockActual = stockActual,
            stockMinimo = stockMinimo,
            unidadMedida = binding.spnUnidad.selectedItem.toString(),
            idCategoria = categoria.id
        )

        binding.btnGuardarProducto.isEnabled = false
        lifecycleScope.launch {
            try {
                val respuesta = if (idProducto == 0) {
                    repositorio.crearProducto(producto)
                } else {
                    repositorio.actualizarProducto(producto)
                }
                Toast.makeText(this@ProductoFormActivity, respuesta.mensaje, Toast.LENGTH_SHORT).show()
                finish()
            } catch (e: Exception) {
                binding.btnGuardarProducto.isEnabled = true
                Toast.makeText(this@ProductoFormActivity, e.message, Toast.LENGTH_LONG).show()
            }
        }
    }
}
