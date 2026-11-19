package com.miempresa.inventariomovil.api

// Sesión del comerciante mientras la app está abierta.
// Se guarda SOLO en memoria (variables), no en el celular:
//   - si sales un rato a WhatsApp y vuelves, la app sigue abierta y la sesión también;
//   - si cierras la app (o Android la cierra), estas variables se borran y
//     la próxima vez hay que volver a iniciar sesión.
// La clave nunca se guarda: solo el token que entrega el servidor.
object Sesion {

    // Si la app pasa más de estos minutos en segundo plano, vuelve a pedir la clave.
    private const val MINUTOS_LIMITE = 5

    var token = ""
        private set
    var nombre = ""
        private set

    // Hora (en milisegundos) en que la app pasó a segundo plano. 0 = la app está en uso.
    private var horaSalida = 0L

    // Después de un login correcto.
    fun iniciar(tokenNuevo: String, nombreNuevo: String) {
        token = tokenNuevo
        nombre = nombreNuevo
        horaSalida = 0L
    }

    // Cerrar sesión, sesión vencida o token rechazado por el servidor.
    fun cerrar() {
        token = ""
        nombre = ""
        horaSalida = 0L
    }

    fun hayToken(): Boolean = token.isNotEmpty()

    // La app pasó a segundo plano: se anota la hora.
    fun marcarSalida() {
        horaSalida = System.currentTimeMillis()
    }

    // El usuario volvió a la app.
    fun marcarRegreso() {
        horaSalida = 0L
    }

    // true si la app estuvo fuera más tiempo del permitido.
    fun vencio(): Boolean {
        if (horaSalida == 0L) return false
        val minutosFuera = (System.currentTimeMillis() - horaSalida) / 60000
        return minutosFuera >= MINUTOS_LIMITE
    }
}
