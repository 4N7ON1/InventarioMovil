package com.miempresa.inventariomovil

import android.content.res.ColorStateList
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.miempresa.inventariomovil.api.InventarioRepository
import com.miempresa.inventariomovil.databinding.ActivityVentaBinding
import com.miempresa.inventariomovil.model.DetalleNuevo
import com.miempresa.inventariomovil.model.ItemCarrito
import com.miempresa.inventariomovil.model.Producto
import com.miempresa.inventariomovil.model.VentaNueva
import com.miempresa.inventariomovil.ui.Navegacion
import com.miempresa.inventariomovil.ui.Imagenes
import com.miempresa.inventariomovil.ui.PantallaProtegida
import com.miempresa.inventariomovil.ui.adapter.CarritoAdapter
import kotlinx.coroutines.launch

class VentaActivity : PantallaProtegida() {

    private lateinit var binding: ActivityVentaBinding
    private lateinit var adapter: CarritoAdapter
    private val repositorio = InventarioRepository()
    private val carrito = mutableListOf<ItemCarrito>()

    // Método elegido: "EFECTIVO", "QR" u "OTRO" (los mismos valores que guarda la base).
    private var metodoPago = "QR"

    // Paso de la venta: "nueva" (armando el carrito), "esperando" (QR sin confirmar),
    // "pagada" o "editando" (cambiando los productos de una venta ya registrada).
    private var paso = "nueva"
    private var idVentaActual = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVentaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        adapter = CarritoAdapter(carrito) { total -> mostrarTotal(total) }
        binding.carritoRecyclerView.adapter = adapter
        binding.carritoRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.carritoRecyclerView.isNestedScrollingEnabled = false

        binding.qrBox.setLayerType(View.LAYER_TYPE_SOFTWARE, null)

        binding.btnEfectivo.setOnClickListener { elegirMetodo("EFECTIVO") }
        binding.btnQR.setOnClickListener { elegirMetodo("QR") }
        binding.btnOtro.setOnClickListener { elegirMetodo("OTRO") }

        binding.addLink.setOnClickListener { elegirProducto() }
        binding.botonPago.setOnClickListener { procesarPago() }

        binding.backButton.setOnClickListener { finish() }
        cargarQr()
        Navegacion.configurar(this, binding.bottomNavigation, R.id.nav_sales)

        pintarMetodos()
        pintarBotonPago()
        mostrarTotal(calcularTotal())

        // Si desde el historial llega el número de una venta, la pantalla se abre para cambiarla.
        idVentaActual = intent.getIntExtra("id_venta", 0)
        if (idVentaActual > 0) {
            cargarVentaParaEditar()
        }
    }

    // Trae el QR de cobro que el comerciante subió en Ajustes (GET /qr).
    // Si todavía no lo subió, se queda el dibujo de ejemplo con un aviso.
    private fun cargarQr() {
        lifecycleScope.launch {
            try {
                val imagen = Imagenes.aBitmap(repositorio.obtenerQr())
                binding.imgQrCobro.setImageBitmap(imagen)
                binding.imgQrCobro.visibility = View.VISIBLE
                binding.icoQrEjemplo.visibility = View.GONE
                binding.txtNotaQr.text = "Cuando tu cliente pague, revisa tu banco y toca «Confirmar pago recibido»."
            } catch (e: Exception) {
                binding.txtNotaQr.text = getString(R.string.qr_prototype_note)
            }
        }
    }

    // MODO EDICIÓN: trae la venta (GET /ventas/{id}) y pone sus productos en el carrito.
    private fun cargarVentaParaEditar() {
        paso = "editando"
        binding.txtTituloVenta.text = "Cambiar venta #" + idVentaActual
        binding.tituloMetodoPago.visibility = View.GONE
        binding.opcionesPago.visibility = View.GONE
        binding.qrSection.visibility = View.GONE
        pintarBotonPago()

        lifecycleScope.launch {
            try {
                val venta = repositorio.obtenerVenta(idVentaActual)
                val productos = repositorio.listarProductos("")
                for (detalle in venta.detalles) {
                    val producto = productos.find { it.id == detalle.idProducto }
                    // Lo máximo que se puede llevar = lo que queda en el inventario
                    // + lo que esta venta ya tenía (porque eso vuelve al inventario al editar).
                    val maximo = (producto?.stockActual ?: 0) + detalle.cantidad
                    carrito.add(
                        ItemCarrito(
                            detalle.idProducto,
                            detalle.nombreProducto,
                            producto?.precioVenta ?: detalle.precioUnitario,
                            producto?.icono() ?: "ic_inventory_2",
                            detalle.cantidad,
                            maximo
                        )
                    )
                }
                adapter.notifyDataSetChanged()
                mostrarTotal(calcularTotal())
            } catch (e: Exception) {
                Toast.makeText(this@VentaActivity, e.message, Toast.LENGTH_LONG).show()
                finish()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        binding.bottomNavigation.selectedItemId = R.id.nav_sales
    }

    // Pide a la API los productos (GET /productos) y muestra los que tienen stock.
    private fun elegirProducto() {
        if (paso != "nueva" && paso != "editando") {
            Toast.makeText(this, "Termina la venta actual primero", Toast.LENGTH_SHORT).show()
            return
        }
        lifecycleScope.launch {
            try {
                // Se ofrecen los productos con stock y los que ya están en el carrito.
                val productos = repositorio.listarProductos("").filter { producto ->
                    producto.stockActual > 0 || carrito.any { it.id == producto.id }
                }
                if (productos.isEmpty()) {
                    Toast.makeText(this@VentaActivity, "No hay productos con stock disponible", Toast.LENGTH_LONG).show()
                    return@launch
                }
                val nombres = productos.map { it.toString() }.toTypedArray()
                AlertDialog.Builder(this@VentaActivity)
                    .setTitle("Elegir producto")
                    .setItems(nombres) { _, posicion -> pedirCantidad(productos[posicion]) }
                    .setNegativeButton("Cancelar", null)
                    .show()
            } catch (e: Exception) {
                Toast.makeText(this@VentaActivity, e.message, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun pedirCantidad(producto: Producto) {
        val edtCantidad = EditText(this)
        edtCantidad.inputType = InputType.TYPE_CLASS_NUMBER
        edtCantidad.hint = "Cantidad (máx. " + producto.stockActual + ")"
        edtCantidad.setText("1")

        AlertDialog.Builder(this)
            .setTitle(producto.nombre)
            .setView(edtCantidad)
            .setPositiveButton("Agregar") { _, _ ->
                val cantidad = edtCantidad.text.toString().toIntOrNull()
                if (cantidad == null || cantidad <= 0) {
                    Toast.makeText(this, "Ingresa una cantidad mayor a 0", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                agregarAlCarrito(producto, cantidad)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    // Si el producto ya está en el carrito suma la cantidad; nunca deja pasar del stock.
    private fun agregarAlCarrito(producto: Producto, cantidad: Int) {
        val existente = carrito.find { it.id == producto.id }
        val enCarrito = existente?.cantidad ?: 0
        val maximo = existente?.stockDisponible ?: producto.stockActual
        if (cantidad + enCarrito > maximo) {
            Toast.makeText(this, "No alcanza. Solo puedes agregar " + (maximo - enCarrito) + " más", Toast.LENGTH_LONG).show()
            return
        }
        if (existente != null) {
            existente.cantidad += cantidad
        } else {
            carrito.add(
                ItemCarrito(producto.id, producto.nombre, producto.precioVenta, producto.icono(), cantidad, producto.stockActual)
            )
        }
        adapter.notifyDataSetChanged()
        mostrarTotal(calcularTotal())
    }

    private fun elegirMetodo(metodo: String) {
        if (paso != "nueva") {
            Toast.makeText(this, "La venta ya fue registrada", Toast.LENGTH_SHORT).show()
            return
        }
        metodoPago = metodo
        pintarMetodos()
        pintarBotonPago()
    }

    // El botón principal hace algo distinto según el paso de la venta.
    private fun procesarPago() {
        when (paso) {
            "nueva" -> registrarVenta()
            "esperando" -> confirmarPagoQr()
            "pagada" -> nuevaVenta()
            "editando" -> guardarCambios()
        }
    }

    // PUT /ventas/{id}: envía la lista nueva de productos de una venta ya registrada.
    // El servidor devuelve al inventario lo anterior y descuenta lo nuevo en una sola
    // transacción; si no alcanza el stock responde con error y la venta queda como estaba.
    private fun guardarCambios() {
        if (carrito.isEmpty()) {
            Toast.makeText(this, "La venta necesita al menos un producto. Para borrarla usa \"Anular venta\" en el historial.", Toast.LENGTH_LONG).show()
            return
        }
        val detalles = carrito.map { DetalleNuevo(it.id, it.cantidad) }

        binding.botonPago.isEnabled = false
        lifecycleScope.launch {
            try {
                val respuesta = repositorio.actualizarVenta(idVentaActual, VentaNueva(metodoPago, detalles))
                Toast.makeText(this@VentaActivity, respuesta.mensaje, Toast.LENGTH_SHORT).show()
                avisarStock(respuesta.alertas) { finish() }
            } catch (e: Exception) {
                binding.botonPago.isEnabled = true
                Toast.makeText(this@VentaActivity, e.message, Toast.LENGTH_LONG).show()
            }
        }
    }

    // ALERTA DE STOCK: si después de la venta algún producto quedó agotado o por agotarse,
    // el servidor manda sus nombres y aquí se muestran en un aviso.
    private fun avisarStock(alertas: List<String>?, alCerrar: () -> Unit) {
        if (alertas.isNullOrEmpty()) {
            alCerrar()
            return
        }
        AlertDialog.Builder(this)
            .setTitle("¡Atención! Se está agotando")
            .setMessage(alertas.joinToString("\n"))
            .setCancelable(false)
            .setPositiveButton("Entendido") { _, _ -> alCerrar() }
            .show()
    }

    // POST /ventas: envía el método de pago y los productos del carrito.
    // El servidor revisa el stock, calcula el total y descuenta el inventario.
    private fun registrarVenta() {
        if (carrito.isEmpty()) {
            Toast.makeText(this, "Agrega al menos un producto con \"+ Agregar\"", Toast.LENGTH_SHORT).show()
            return
        }
        val detalles = carrito.map { DetalleNuevo(it.id, it.cantidad) }

        binding.botonPago.isEnabled = false
        lifecycleScope.launch {
            try {
                val respuesta = repositorio.registrarVenta(VentaNueva(metodoPago, detalles))
                idVentaActual = respuesta.id
                paso = if (metodoPago == "QR") "esperando" else "pagada"
                adapter.bloqueado = true
                Toast.makeText(this@VentaActivity, "Venta #" + idVentaActual + " registrada", Toast.LENGTH_SHORT).show()
                pintarBotonPago()
                avisarStock(respuesta.alertas) { }
            } catch (e: Exception) {
                Toast.makeText(this@VentaActivity, e.message, Toast.LENGTH_LONG).show()
            }
            binding.botonPago.isEnabled = true
        }
    }

    // PUT /ventas/{id}: el comerciante confirma que recibió el pago por QR.
    private fun confirmarPagoQr() {
        binding.botonPago.isEnabled = false
        lifecycleScope.launch {
            try {
                repositorio.confirmarPago(idVentaActual)
                paso = "pagada"
                Toast.makeText(this@VentaActivity, "Pago QR confirmado", Toast.LENGTH_SHORT).show()
                pintarBotonPago()
            } catch (e: Exception) {
                Toast.makeText(this@VentaActivity, e.message, Toast.LENGTH_LONG).show()
            }
            binding.botonPago.isEnabled = true
        }
    }

    private fun nuevaVenta() {
        carrito.clear()
        idVentaActual = 0
        paso = "nueva"
        metodoPago = "QR"
        adapter.bloqueado = false
        adapter.notifyDataSetChanged()
        pintarMetodos()
        mostrarTotal(calcularTotal())
        pintarBotonPago()
    }

    private fun mostrarTotal(total: Double) {
        val unidades = carrito.sumOf { it.cantidad }
        binding.totalTexto.text = "Bs " + String.format("%.2f", total)
        binding.resumenTexto.text = getString(R.string.items_summary, unidades, carrito.size)
        binding.txtCarritoVacio.visibility = if (carrito.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun calcularTotal() = carrito.sumOf { it.subtotal() }

    private fun pintarMetodos() {
        pintarMetodo(binding.btnEfectivo, binding.iconEfectivo, binding.labelEfectivo, metodoPago == "EFECTIVO")
        pintarMetodo(binding.btnQR, binding.iconQR, binding.labelQR, metodoPago == "QR")
        pintarMetodo(binding.btnOtro, binding.iconOtro, binding.labelOtro, metodoPago == "OTRO")
        binding.qrSection.visibility = if (metodoPago == "QR") View.VISIBLE else View.GONE
    }

    private fun pintarMetodo(contenedor: View, icono: ImageView, etiqueta: TextView, seleccionado: Boolean) {
        if (seleccionado) {
            contenedor.setBackgroundResource(R.drawable.venta_payment_selected_bg)
            val color = ContextCompat.getColor(this, R.color.primary_dark)
            icono.setColorFilter(color)
            etiqueta.setTextColor(color)
        } else {
            contenedor.setBackgroundResource(R.drawable.venta_payment_unselected_bg)
            val color = ContextCompat.getColor(this, R.color.text_secondary)
            icono.setColorFilter(color)
            etiqueta.setTextColor(color)
        }
    }

    private fun pintarBotonPago() {
        when (paso) {
            "nueva" -> {
                binding.botonPago.text = when (metodoPago) {
                    "QR" -> getString(R.string.generate_qr)
                    "EFECTIVO" -> getString(R.string.charge_cash)
                    else -> getString(R.string.charge_other)
                }
                binding.botonPago.backgroundTintList =
                    ColorStateList.valueOf(ContextCompat.getColor(this, R.color.accent))
                binding.qrStatusBadge.text = getString(R.string.qr_ready)
                binding.qrOverlay.visibility = View.GONE
            }
            "esperando" -> {
                binding.botonPago.text = "Confirmar pago recibido"
                binding.botonPago.backgroundTintList =
                    ColorStateList.valueOf(ContextCompat.getColor(this, R.color.warn_fg))
                binding.qrStatusBadge.text = getString(R.string.qr_waiting)
                binding.qrOverlay.visibility = View.GONE
            }
            "editando" -> {
                binding.botonPago.text = "Guardar cambios"
                binding.botonPago.backgroundTintList =
                    ColorStateList.valueOf(ContextCompat.getColor(this, R.color.accent))
            }
            "pagada" -> {
                binding.botonPago.text = getString(R.string.sale_registered)
                binding.botonPago.backgroundTintList =
                    ColorStateList.valueOf(ContextCompat.getColor(this, R.color.ok_fg))
                binding.qrStatusBadge.text = "Pago recibido · Bs " + String.format("%.2f", calcularTotal())
                binding.qrOverlay.visibility = if (metodoPago == "QR") View.VISIBLE else View.GONE
            }
        }
    }
}
