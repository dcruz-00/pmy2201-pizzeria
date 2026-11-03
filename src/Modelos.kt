import kotlin.math.roundToInt

// ===================== Constantes de negocio =====================
// Supuesto: los precios del catálogo son netos y el IVA (19 %) se suma
// después de aplicar el descuento.
const val TASA_IVA = 0.19
const val TASA_DESCUENTO = 0.10
const val MIN_PIZZAS_DESCUENTO = 3 // el descuento aplica con MÁS de 3 pizzas

// ===================== Estado del pedido =====================
enum class EstadoPedido(val etiqueta: String) {
    EN_PREPARACION("En preparación"),
    LISTO("Listo"),
    EN_CAMINO("En camino")
}

// ===================== Usuarios (herencia y polimorfismo) =====================
open class Usuario(
    val nombre: String,
    val telefono: String,
    val correo: String? // opcional: puede no registrarse
) {
    open fun describir(): String =
        "$nombre | Tel: $telefono | Correo: ${correo ?: "Sin correo"}"
}

class Cliente(
    nombre: String,
    telefono: String,
    correo: String?,
    val direccion: String
) : Usuario(nombre, telefono, correo) {

    override fun describir(): String = "Cliente: ${super.describir()} | Dirección: $direccion"

    // Solo notifica si el cliente registró un correo válido (uso de let).
    fun notificar(mensaje: String) {
        correo?.takeIf { it != CORREO_POR_DEFECTO }?.let { destino ->
            println("Notificación enviada a $destino: $mensaje")
        } ?: println("(El cliente no tiene correo válido registrado; no se envía notificación)")
    }
}

class Repartidor(
    nombre: String,
    telefono: String,
    val vehiculo: String,
    val distanciaKm: Double,
    var disponible: Boolean = true
) : Usuario(nombre, telefono, null) {

    override fun describir(): String =
        "Repartidor: $nombre ($vehiculo) | a $distanciaKm km | ${if (disponible) "Disponible" else "Ocupado"}"

    fun entregarPedido(pedido: Pedido) {
        disponible = false
        pedido.repartidor = this
        pedido.cambiarEstado(EstadoPedido.EN_CAMINO)
        println("$nombre ($vehiculo) salió a entregar el pedido N° ${pedido.id} a ${pedido.cliente.direccion}.")
    }
}

// ===================== Productos =====================
class Producto(
    val nombre: String,
    val precio: Int, // CLP no usa decimales, por eso Int
    val ingredientes: Map<String, Int> // ingrediente -> unidades por pizza
) {
    init {
        require(precio > 0) { "El precio de '$nombre' debe ser mayor que 0 (se recibió $precio)." }
    }

    fun detallarProducto(): String {
        val listaIngredientes = ingredientes.keys.joinToString(", ")
        return "$nombre - ${formatearCLP(precio)} (ingredientes: $listaIngredientes)"
    }
}

// Una línea del pedido: un producto y cuántas unidades se piden de él.
class LineaPedido(val producto: Producto, val cantidad: Int) {
    init {
        require(cantidad > 0) { "La cantidad debe ser mayor que 0." }
    }

    val subtotal: Int get() = producto.precio * cantidad
}

// ===================== Totales =====================
data class Totales(val subtotal: Int, val descuento: Int, val impuesto: Int, val total: Int)

// Calcula subtotal, descuento (10 % con más de 3 pizzas), IVA y total.
fun calcularTotalPedido(lineas: List<LineaPedido>): Totales {
    val subtotal = lineas.sumOf { it.subtotal }
    val cantidadPizzas = lineas.sumOf { it.cantidad }

    val descuento = if (cantidadPizzas > MIN_PIZZAS_DESCUENTO) (subtotal * TASA_DESCUENTO).roundToInt() else 0
    val base = subtotal - descuento
    val impuesto = (base * TASA_IVA).roundToInt()
    val total = base + impuesto

    require(total > 0) { "Costo de pedido no válido: el total debe ser mayor que 0." }
    return Totales(subtotal, descuento, impuesto, total)
}

// ===================== Pedido =====================
class Pedido(
    val id: Int,
    val cliente: Cliente,
    val lineas: List<LineaPedido>,
    val notas: String? // opcional
) {
    var estado: EstadoPedido = EstadoPedido.EN_PREPARACION
        private set
    var repartidor: Repartidor? = null

    val cantidadPizzas: Int get() = lineas.sumOf { it.cantidad }

    fun calcularTotal(): Totales = calcularTotalPedido(lineas)

    // Solo permite avanzar al estado siguiente: En preparación -> Listo -> En camino.
    fun cambiarEstado(nuevo: EstadoPedido): Boolean {
        val permitido = when (estado) {
            EstadoPedido.EN_PREPARACION -> nuevo == EstadoPedido.LISTO
            EstadoPedido.LISTO -> nuevo == EstadoPedido.EN_CAMINO
            EstadoPedido.EN_CAMINO -> false
        }
        if (permitido) {
            estado = nuevo
        } else {
            println("No se puede pasar de '${estado.etiqueta}' a '${nuevo.etiqueta}'.")
        }
        return permitido
    }

    fun estaCompletado(): Boolean = estado != EstadoPedido.EN_PREPARACION

    fun generarResumen(): String {
        val t = calcularTotal()
        val detalleProductos = lineas.joinToString("\n") { linea ->
            "  ${linea.producto.nombre.padEnd(15)} ${formatearCLP(linea.producto.precio).padStart(9)} x ${linea.cantidad}" +
                    " = ${formatearCLP(linea.subtotal)}"
        }
        return """
            |============ RESUMEN DEL PEDIDO N° $id ============
            |--- Cliente ---
            |  Nombre:    ${cliente.nombre}
            |  Dirección: ${cliente.direccion}
            |  Teléfono:  ${cliente.telefono}
            |  Correo:    ${cliente.correo ?: "Sin correo"}
            |--- Productos ---
            |$detalleProductos
            |--- Totales ---
            |  Subtotal:              ${formatearCLP(t.subtotal)}
            |  Descuento (${(TASA_DESCUENTO * 100).roundToInt()} %):        -${formatearCLP(t.descuento)}
            |  IVA (${(TASA_IVA * 100).roundToInt()} %):              ${formatearCLP(t.impuesto)}
            |  TOTAL:                 ${formatearCLP(t.total)}
            |--- Estado ---
            |  Estado:     ${estado.etiqueta}
            |  Repartidor: ${repartidor?.nombre ?: "Sin asignar"}
            |  Notas:      ${notas ?: "Sin notas"}
            |=================================================
        """.trimMargin()
    }
}
