package com.miempresa.inventariomovil.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.miempresa.inventariomovil.R
import com.miempresa.inventariomovil.databinding.ItemCarritoBinding
import com.miempresa.inventariomovil.model.ItemCarrito

class CarritoAdapter(
    private val items: MutableList<ItemCarrito>,
    private val alCambiarCantidad: (Double) -> Unit
) : RecyclerView.Adapter<CarritoAdapter.ViewHolder>() {

    // Cuando la venta ya fue registrada se bloquean los botones + y -.
    var bloqueado = false

    override fun getItemCount() = items.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        ViewHolder(ItemCarritoBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.mostrar(items[position])

    inner class ViewHolder(private val binding: ItemCarritoBinding) : RecyclerView.ViewHolder(binding.root) {

        fun mostrar(item: ItemCarrito) {
            binding.nombreItem.text = item.nombre
            binding.detalleItem.text = item.cantidad.toString() + " × Bs " + String.format("%.2f", item.precio)
            binding.subtotalItem.text = "Bs " + String.format("%.2f", item.subtotal())
            binding.cantidadItem.text = item.cantidad.toString()
            binding.iconoItem.setImageResource(buscarIcono(item.icono))

            binding.btnMenos.setOnClickListener {
                val posicion = bindingAdapterPosition
                if (bloqueado || posicion == RecyclerView.NO_POSITION) return@setOnClickListener
                item.cantidad--
                if (item.cantidad <= 0) {
                    items.removeAt(posicion)
                    notifyItemRemoved(posicion)
                } else {
                    notifyItemChanged(posicion)
                }
                alCambiarCantidad(calcularTotal())
            }

            binding.btnMas.setOnClickListener {
                val posicion = bindingAdapterPosition
                if (bloqueado || posicion == RecyclerView.NO_POSITION) return@setOnClickListener
                if (item.cantidad >= item.stockDisponible) {
                    Toast.makeText(binding.root.context, "Solo hay " + item.stockDisponible + " en stock", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                item.cantidad++
                notifyItemChanged(posicion)
                alCambiarCantidad(calcularTotal())
            }
        }

        private fun buscarIcono(nombre: String): Int {
            val contexto = binding.root.context
            val id = contexto.resources.getIdentifier(nombre, "drawable", contexto.packageName)
            return if (id != 0) id else R.drawable.ic_inventory_2
        }
    }

    private fun calcularTotal() = items.sumOf { it.subtotal() }
}
