import java.time.LocalDate
import kotlin.math.roundToInt

// ===================== Constantes de negocio =====================
// Supuestos:
// - Los precios del catálogo son netos y el IVA (19 %) se suma después de los descuentos.
// - El descuento por cantidad y el de promoción se calculan sobre el subtotal
//   original y se suman (no se aplican uno encima del otro).
const val TASA_IVA = 0.19
const val TASA_DESCUENTO = 0.10
const val MIN_PIZZAS_DESCUENTO = 3 // el descuento aplica con MÁS de 3 pizzas

// Unidades permitidas por línea y por pedido completo (pizzas, bebidas y extras).
val RANGO_UNIDADES = 1..100

// ===================== Estado del pedido =====================
enum class EstadoPedido(val etiqueta: String) {
    EN_PREPARACION("En preparación"),
    LISTO("Listo"),
    EN_CAMINO("En camino"),
    ENTREGADO("Entregado")
}

// Una línea del pedido: un producto y cuántas unidades se piden de él.
class LineaPedido(val producto: Producto, val cantidad: Int) {
    init {
        require(cantidad in RANGO_UNIDADES) {
            "La cantidad debe estar entre ${RANGO_UNIDADES.first} y ${RANGO_UNIDADES.last} (se recibió $cantidad)."
        }
    }

    val subtotal: Int get() = producto.precio * cantidad
    val pizzas: Int get() = producto.pizzasIncluidas * cantidad
}

// ===================== Totales =====================
data class Totales(
    val subtotal: Int,
    val descuentoCantidad: Int,
    val descuentoPromocion: Int,
    val impuesto: Int,
    val total: Int
)

// Calcula subtotal, descuentos, IVA y total.
// - Descuento por cantidad: 10 % del subtotal con más de 3 pizzas (pizzas y combos cuentan como pizza).
// - Descuento de promoción: solo si la fecha está en el período, y solo sobre los
//   productos cuya clase tiene @Promocionable (se detecta con reflection).
fun calcularTotalPedido(lineas: List<LineaPedido>, fecha: LocalDate): Totales {
    val subtotal = lineas.sumOf { it.subtotal }
    val cantidadPizzas = lineas.sumOf { it.pizzas }

    val descuentoCantidad =
        if (cantidadPizzas > MIN_PIZZAS_DESCUENTO) (subtotal * TASA_DESCUENTO).roundToInt() else 0

    val descuentoPromocion = if (estaEnPeriodoPromocion(fecha)) {
        lineas.sumOf { linea ->
            val promo = linea.producto.promocion()
            if (promo == null) 0 else (linea.subtotal * promo.porcentaje / 100.0).roundToInt()
        }
    } else 0

    val base = subtotal - descuentoCantidad - descuentoPromocion
    val impuesto = (base * TASA_IVA).roundToInt()
    val total = base + impuesto

    require(total > 0) { "Costo de pedido no válido: el total debe ser mayor que 0." }
    return Totales(subtotal, descuentoCantidad, descuentoPromocion, impuesto, total)
}

// ===================== Pedido =====================
class Pedido(
    val id: Int,
    val cliente: Cliente,
    val lineas: List<LineaPedido>,
    val fecha: LocalDate,
    val notas: String?, // opcional
    val pedidosOrigen: List<Int> = emptyList() // N° de los pedidos que se combinaron (vacío si es original)
) {
    var estado: EstadoPedido = EstadoPedido.EN_PREPARACION
        private set
    var repartidor: Repartidor? = null

    init {
        require(lineas.isNotEmpty()) { "El pedido debe tener al menos un producto." }
        require(totalUnidades in RANGO_UNIDADES) {
            "El pedido debe tener entre ${RANGO_UNIDADES.first} y ${RANGO_UNIDADES.last} unidades (tiene $totalUnidades)."
        }
    }

    val totalUnidades: Int get() = lineas.sumOf { it.cantidad }
    val cantidadPizzas: Int get() = lineas.sumOf { it.pizzas }

    fun calcularTotal(): Totales = calcularTotalPedido(lineas, fecha)

    val esCombinado: Boolean get() = pedidosOrigen.isNotEmpty()

    // ===================== Sobrecarga del operador + =====================
    // pedidoA + pedidoB devuelve un pedido nuevo con los productos de ambos.
    // - Solo se combinan pedidos del mismo cliente (== usa Cliente.equals),
    //   con la misma fecha y que sigan en preparación.
    // - Las líneas del mismo producto se agrupan (groupBy usa Producto.equals/hashCode)
    //   y se suman sus cantidades.
    // - El total se recalcula con calcularTotal(), incluidos los descuentos.
    // - El pedido resultante conserva el N° del primero.
    operator fun plus(otro: Pedido): Pedido {
        require(this !== otro) { "No se puede combinar un pedido consigo mismo." }
        require(cliente == otro.cliente) { "Solo se pueden combinar pedidos del mismo cliente." }
        require(fecha == otro.fecha) {
            "Solo se pueden combinar pedidos con la misma fecha (${formatearFecha(fecha)} y ${formatearFecha(otro.fecha)})."
        }
        require(estado == EstadoPedido.EN_PREPARACION && otro.estado == EstadoPedido.EN_PREPARACION) {
            "Solo se pueden combinar pedidos que estén en preparación."
        }

        val lineasCombinadas = (lineas + otro.lineas)
            .groupBy { it.producto }
            .map { (producto, lineasDelProducto) -> // desestructuración de Map.Entry
                LineaPedido(producto, lineasDelProducto.sumOf { it.cantidad })
            }
        val notasCombinadas = listOfNotNull(notas, otro.notas).joinToString(" | ").ifEmpty { null }
        val origen = (if (esCombinado) pedidosOrigen else listOf(id)) +
                (if (otro.esCombinado) otro.pedidosOrigen else listOf(otro.id))

        return Pedido(id, cliente, lineasCombinadas, fecha, notasCombinadas, origen)
    }

    // ===================== Desestructuración =====================
    // val (cliente, productos, total) = pedido
    operator fun component1(): Cliente = cliente
    operator fun component2(): List<LineaPedido> = lineas
    operator fun component3(): Int = calcularTotal().total

    // Productos del pedido cuya clase está anotada con @Promocionable.
    fun productosPromocionables(): List<Producto> =
        lineas.map { it.producto }.filter { it.esPromocionable() }

    // Solo permite avanzar al estado siguiente:
    // En preparación -> Listo -> En camino -> Entregado.
    fun cambiarEstado(nuevo: EstadoPedido): Boolean {
        val permitido = when (estado) {
            EstadoPedido.EN_PREPARACION -> nuevo == EstadoPedido.LISTO
            EstadoPedido.LISTO -> nuevo == EstadoPedido.EN_CAMINO
            EstadoPedido.EN_CAMINO -> nuevo == EstadoPedido.ENTREGADO
            EstadoPedido.ENTREGADO -> false
        }
        if (permitido) {
            estado = nuevo
        } else {
            println("No se puede pasar de '${estado.etiqueta}' a '${nuevo.etiqueta}'.")
        }
        return permitido
    }

    fun estaCompletado(): Boolean = estado == EstadoPedido.ENTREGADO

    fun generarResumen(): String {
        val (subtotal, descuentoCantidad, descuentoPromocion, impuesto, total) = calcularTotal() // desestructuración de Totales
        val (nombreCliente, correoCliente, telefonoCliente) = cliente // desestructuración de Cliente
        val detalleProductos = lineas.joinToString("\n") { linea ->
            val marca = if (linea.producto.esPromocionable()) " ★" else ""
            "  ${linea.producto.nombre.padEnd(26)} ${formatearCLP(linea.producto.precio).padStart(9)} x ${linea.cantidad}" +
                    " = ${formatearCLP(linea.subtotal)}$marca"
        }
        val estadoPromocion = if (estaEnPeriodoPromocion(fecha)) "vigente" else "no vigente para esta fecha"
        return """
            |============ RESUMEN DEL PEDIDO N° $id ============
            |  Fecha:     ${formatearFecha(fecha)}
            |  Origen:    ${if (esCombinado) "combinado de " + pedidosOrigen.joinToString(" + ") { "N° $it" } else "pedido original"}
            |--- Cliente ---
            |  Nombre:    $nombreCliente
            |  Dirección: ${cliente.direccion}
            |  Teléfono:  $telefonoCliente
            |  Correo:    $correoCliente
            |--- Productos (★ = promocionable) ---
            |$detalleProductos
            |--- Totales ---
            |  Pizzas en el pedido:   $cantidadPizzas
            |  Subtotal:              ${formatearCLP(subtotal)}
            |  Dcto. cantidad (${(TASA_DESCUENTO * 100).roundToInt()} %):  -${formatearCLP(descuentoCantidad)}
            |  Dcto. promoción:       -${formatearCLP(descuentoPromocion)} ($estadoPromocion)
            |  IVA (${(TASA_IVA * 100).roundToInt()} %):              ${formatearCLP(impuesto)}
            |  TOTAL:                 ${formatearCLP(total)}
            |--- Estado ---
            |  Estado:     ${estado.etiqueta}
            |  Repartidor: ${repartidor?.nombre ?: "Sin asignar"}
            |  Notas:      ${notas ?: "Sin notas"}
            |=================================================
        """.trimMargin()
    }
}