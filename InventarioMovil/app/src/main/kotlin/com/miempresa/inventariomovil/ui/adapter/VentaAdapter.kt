package com.miempresa.inventariomovil.ui.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.miempresa.inventariomovil.R
import com.miempresa.inventariomovil.model.Venta

class VentaAdapter(private val context: Context, private var lista: List<Venta>) : BaseAdapter() {

    fun actualizar(nuevaLista: List<Venta>) {
        lista = nuevaLista
        notifyDataSetChanged()
    }

    override fun getCount(): Int = lista.size

    override fun getItem(position: Int): Venta = lista[position]

    override fun getItemId(position: Int): Long = lista[position].id.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val vista = convertView ?: LayoutInflater.from(context).inflate(R.layout.item_venta, parent, false)
        val venta = lista[position]

        vista.findViewById<TextView>(R.id.txtVentaId).text = "Venta #" + venta.id
        vista.findViewById<TextView>(R.id.txtVentaFecha).text = venta.fechaHora
        vista.findViewById<TextView>(R.id.txtVentaTotal).text = "Bs " + String.format("%.2f", venta.montoTotal)

        val txtEstado = vista.findViewById<TextView>(R.id.txtVentaEstado)
        txtEstado.text = venta.metodoPago + " · " + venta.estadoPago
        if (venta.estadoPago == "PENDIENTE") {
            txtEstado.setTextColor(ContextCompat.getColor(context, R.color.warn_fg))
        } else {
            txtEstado.setTextColor(ContextCompat.getColor(context, R.color.ok_fg))
        }
        return vista
    }
}
