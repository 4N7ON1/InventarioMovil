package com.miempresa.inventariomovil.ui.adapter

import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.miempresa.inventariomovil.R
import com.miempresa.inventariomovil.databinding.ItemProductoBinding
import com.miempresa.inventariomovil.model.Producto

class ProductoAdapter(
    private var productos: List<Producto>,
    private val alEditar: (Producto) -> Unit,
    private val alEliminar: (Producto) -> Unit
) : RecyclerView.Adapter<ProductoAdapter.ViewHolder>() {

    private var filtrados: List<Producto> = productos
    private var filtroActual = "Todos"

    fun actualizar(nuevaLista: List<Producto>) {
        productos = nuevaLista
        filtrar(filtroActual)
    }

    fun filtrar(filtro: String) {
        filtroActual = filtro
        filtrados = when (filtro) {
            "Disponibles" -> productos.filter { it.estado() == "Disponible" }
            "Stock bajo" -> productos.filter { it.estado() == "Stock bajo" }
            "Agotados" -> productos.filter { it.estado() == "Agotado" }
            else -> productos
        }
        notifyDataSetChanged()
    }

    fun total(): Int = productos.size

    override fun getItemCount() = filtrados.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        ViewHolder(ItemProductoBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.mostrar(filtrados[position])

    inner class ViewHolder(private val binding: ItemProductoBinding) : RecyclerView.ViewHolder(binding.root) {

        fun mostrar(producto: Producto) {
            val contexto = binding.root.context
            binding.productoNombre.text = producto.nombre
            binding.productoCategoria.text = producto.nombreCategoria + " · se vende por " + producto.unidadMedida
            binding.productoStock.text = if (producto.stockActual == 0) {
                "No queda ninguno"
            } else {
                "Quedan " + producto.stockActual
            }
            binding.productoPrecio.text = "Bs " + String.format("%.2f", producto.precioVenta)
            binding.productoIcono.setImageResource(buscarIcono(producto.icono()))

            // Color de la etiqueta según el estado del stock.
            val (colorTexto, colorFondo) = when (producto.estado()) {
                "Agotado" -> Pair(R.color.err_fg, R.color.err_bg)
                "Stock bajo" -> Pair(R.color.warn_fg, R.color.warn_bg)
                else -> Pair(R.color.ok_fg, R.color.ok_bg)
            }
            val texto = ContextCompat.getColor(contexto, colorTexto)
            val fondo = ContextCompat.getColor(contexto, colorFondo)
            (binding.estadoBadge.background?.mutate() as? GradientDrawable)?.setColor(fondo)
            (binding.estadoPunto.background?.mutate() as? GradientDrawable)?.setColor(texto)
            binding.estadoTexto.setTextColor(texto)
            binding.estadoTexto.text = producto.estado()

            binding.btnEditarProducto.setOnClickListener { alEditar(producto) }
            binding.btnEliminarProducto.setOnClickListener { alEliminar(producto) }
        }

        // Convierte el nombre del ícono ("ic_grocery") en su recurso drawable.
        private fun buscarIcono(nombre: String): Int {
            val contexto = binding.root.context
            val id = contexto.resources.getIdentifier(nombre, "drawable", contexto.packageName)
            return if (id != 0) id else R.drawable.ic_inventory_2
        }
    }
}
