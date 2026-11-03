const val MAX_PEDIDOS_DIA = 50
const val MAX_PIZZAS_POR_LINEA = 20

// Pedidos del día: arreglo de tamaño fijo + contador de pedidos registrados.
val pedidosDelDia = arrayOfNulls<Pedido>(MAX_PEDIDOS_DIA)
var cantidadPedidos = 0
var siguienteIdPedido = 1001

val catalogo: List<Producto> = listOf(
    Producto("Pepperoni", 8990, mapOf("masa" to 1, "salsa de tomate" to 1, "mozzarella" to 2, "pepperoni" to 1)),
    Producto("Cuatro Quesos", 10490, mapOf("masa" to 1, "salsa de tomate" to 1, "mozzarella" to 1, "queso azul" to 1, "parmesano" to 1, "cheddar" to 1)),
    Producto("Napolitana", 9490, mapOf("masa" to 1, "salsa de tomate" to 1, "mozzarella" to 2, "tomate" to 1, "aceitunas" to 1)),
    Producto("Española", 9990, mapOf("masa" to 1, "salsa de tomate" to 1, "mozzarella" to 2, "chorizo" to 1, "pimentón" to 1))
)

val inventario = Inventario(
    mapOf(
        "masa" to 12, "salsa de tomate" to 12, "mozzarella" to 20, "pepperoni" to 5,
        "queso azul" to 4, "parmesano" to 4, "cheddar" to 4, "tomate" to 5,
        "aceitunas" to 5, "chorizo" to 4, "pimentón" to 4
    )
)

val repartidores: List<Repartidor> = listOf(
    Repartidor("Carlos", "+56 9 1111 2222", "moto", 1.2),
    Repartidor("Ana", "+56 9 3333 4444", "auto", 3.5),
    Repartidor("Pedro", "+56 9 5555 6666", "bicicleta", 0.8)
)

fun main() {
    println("===== Sistema de gestión - Pizzería =====")
    while (true) {
        println()
        println("1. Registrar pedido")
        println("2. Reporte de pedidos del día")
        println("3. Avanzar estado de un pedido")
        println("4. Ver inventario")
        println("5. Reabastecer ingrediente")
        println("6. Ver repartidores")
        println("0. Salir")
        when (leerEntero("Opción: ", 0, 6)) {
            1 -> registrarPedido()
            2 -> mostrarReporte()
            3 -> avanzarEstado()
            4 -> inventario.mostrar()
            5 -> reabastecerIngrediente()
            6 -> repartidores.forEach { println("  ${it.describir()}") }
            0 -> {
                println("Cerrando el sistema. ¡Hasta luego!")
                return
            }
        }
    }
}

// ===================== Registro de pedidos =====================

fun registrarCliente(): Cliente {
    println("\n--- Datos del cliente ---")
    val nombre = leerTexto("Nombre: ", "Sin nombre")
    val direccion = leerTexto("Dirección de entrega: ", "Sin dirección")
    val telefono = validarTelefono(leerTextoOpcional("Teléfono celular (9 dígitos): "))
    val correo = validarCorreo(leerTextoOpcional("Correo electrónico (opcional, Enter para omitir): "))
    return Cliente(nombre, telefono, correo, direccion)
}

fun seleccionarProductos(): List<LineaPedido> {
    val lineas = mutableListOf<LineaPedido>()
    println("\n--- Menú ---")
    catalogo.forEachIndexed { i, producto -> println("${i + 1}. ${producto.detallarProducto()}") }
    while (true) {
        val opcion = leerEntero("Número de pizza (0 para terminar): ", 0, catalogo.size)
        if (opcion == 0) break
        val cantidad = leerEntero("Cantidad: ", 1, MAX_PIZZAS_POR_LINEA)
        lineas.add(LineaPedido(catalogo[opcion - 1], cantidad))
        println("Agregado: $cantidad x ${catalogo[opcion - 1].nombre}")
    }
    return lineas
}

fun registrarPedido() {
    if (cantidadPedidos >= MAX_PEDIDOS_DIA) {
        println("Se alcanzó el máximo de $MAX_PEDIDOS_DIA pedidos del día.")
        return
    }
    val cliente = registrarCliente()
    val lineas = seleccionarProductos()
    if (lineas.isEmpty()) {
        println("No se agregaron productos. Pedido cancelado.")
        return
    }
    val notas = leerTextoOpcional("Notas del pedido (opcional): ")

    try {
        val pedido = Pedido(siguienteIdPedido, cliente, lineas, notas)
        pedido.calcularTotal() // valida que el costo sea correcto antes de tocar el inventario

        println("\nVerificando ingredientes...")
        if (!inventario.prepararLote(lineas)) {
            println("Pedido rechazado por falta de ingredientes.")
            return
        }

        pedidosDelDia[cantidadPedidos] = pedido
        cantidadPedidos++
        siguienteIdPedido++

        println("Pedido confirmado.\n")
        println(pedido.generarResumen())
        cliente.notificar("Tu pedido N° ${pedido.id} está ${pedido.estado.etiqueta.lowercase()}.")
    } catch (e: IllegalArgumentException) {
        println("Error en el pedido: ${e.message} Revise los datos e intente de nuevo.")
    }
}

// ===================== Reporte y estados =====================

fun mostrarReporte() {
    val registrados = pedidosDelDia.filterNotNull()
    if (registrados.isEmpty()) {
        println("Aún no hay pedidos registrados hoy.")
        return
    }
    println("\n================ REPORTE DEL DÍA ================")
    for (pedido in registrados) {
        val pizzas = pedido.lineas.joinToString(", ") { "${it.cantidad}x ${it.producto.nombre}" }
        println("N° ${pedido.id} | ${pedido.cliente.nombre} | $pizzas | " +
                "${formatearCLP(pedido.calcularTotal().total)} | ${pedido.estado.etiqueta}")
    }

    // Completados = Listo o En camino; pendientes = En preparación.
    val completados = registrados.filter { it.estaCompletado() }
    val pendientes = registrados.filter { !it.estaCompletado() }
    println("\nCompletados (${completados.size}): ${completados.joinToString { "N° ${it.id}" }.ifEmpty { "ninguno" }}")
    println("Pendientes (${pendientes.size}): ${pendientes.joinToString { "N° ${it.id}" }.ifEmpty { "ninguno" }}")
    println("Total vendido: ${formatearCLP(registrados.sumOf { it.calcularTotal().total })}")
}

fun buscarPedido(id: Int): Pedido? = pedidosDelDia.find { it?.id == id }

// Repartidor disponible más cercano: se ordena por distancia y find toma el primero libre.
fun asignarRepartidor(): Repartidor? =
    repartidores.sortedBy { it.distanciaKm }.find { it.disponible }

fun avanzarEstado() {
    if (cantidadPedidos == 0) {
        println("Aún no hay pedidos registrados hoy.")
        return
    }
    val id = leerEntero("N° de pedido: ", 1001, siguienteIdPedido - 1)
    val pedido = buscarPedido(id) ?: run {
        println("No existe el pedido N° $id.")
        return
    }

    when (pedido.estado) {
        EstadoPedido.EN_PREPARACION -> {
            pedido.cambiarEstado(EstadoPedido.LISTO)
            println("Pedido N° ${pedido.id} marcado como Listo.")
            pedido.cliente.notificar("Tu pedido N° ${pedido.id} está listo.")
        }
        EstadoPedido.LISTO -> {
            val repartidor = asignarRepartidor()
            repartidor?.let {
                it.entregarPedido(pedido)
                pedido.cliente.notificar("Tu pedido N° ${pedido.id} va en camino con ${it.nombre}.")
            } ?: println("No hay repartidores disponibles. El pedido sigue en estado Listo.")
        }
        EstadoPedido.EN_CAMINO -> println("El pedido N° ${pedido.id} ya va en camino.")
    }
    println()
    println(pedido.generarResumen())
}

// ===================== Inventario =====================

fun reabastecerIngrediente() {
    val ingredientes = inventario.nombresIngredientes()
    ingredientes.forEachIndexed { i, nombre -> println("${i + 1}. $nombre") }
    val opcion = leerEntero("Ingrediente: ", 1, ingredientes.size)
    // Rango amplio a propósito: una cantidad negativa o 0 se rechaza con una excepción.
    val cantidad = leerEntero("Cantidad a agregar: ", -1000, 1000)
    try {
        inventario.reabastecer(ingredientes[opcion - 1], cantidad)
    } catch (e: IllegalArgumentException) {
        println("Error: ${e.message} Ingrese una cantidad mayor que 0.")
    }
}
