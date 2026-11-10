const val MAX_PEDIDOS_DIA = 50

// Pedidos del día: arreglo de tamaño fijo + contador de pedidos registrados.
val pedidosDelDia = arrayOfNulls<Pedido>(MAX_PEDIDOS_DIA)
var cantidadPedidos = 0
var siguienteIdPedido = 1001

// Historial de combinaciones (a + b = resultado) para el resumen general.
data class Combinacion(val primero: Pedido, val segundo: Pedido, val resultado: Pedido)
val combinaciones = mutableListOf<Combinacion>()

// Catálogo: se arma con agregar(), que rechaza productos duplicados.
val pizzaPepperoni = Pizza("Pepperoni", 8990, mapOf("masa" to 1, "salsa de tomate" to 1, "mozzarella" to 2, "pepperoni" to 1))
val bebidaCola = Bebida("Bebida cola 1,5 L", 2490, 1500)

val catalogo = Catalogo().apply {
    agregar(pizzaPepperoni)
    agregar(Pizza("Cuatro Quesos", 10490, mapOf("masa" to 1, "salsa de tomate" to 1, "mozzarella" to 1, "queso azul" to 1, "parmesano" to 1, "cheddar" to 1)))
    agregar(Pizza("Napolitana", 9490, mapOf("masa" to 1, "salsa de tomate" to 1, "mozzarella" to 2, "tomate" to 1, "aceitunas" to 1)))
    agregar(Pizza("Española", 9990, mapOf("masa" to 1, "salsa de tomate" to 1, "mozzarella" to 2, "chorizo" to 1, "pimentón" to 1)))
    agregar(bebidaCola)
    agregar(Bebida("Agua mineral 500 ml", 1290, 500))
    agregar(IngredienteExtra("Extra mozzarella", 1290, "mozzarella"))
    agregar(IngredienteExtra("Extra pepperoni", 1490, "pepperoni"))
    agregar(PizzaTemporada("Primavera", 9790, mapOf("masa" to 1, "salsa de tomate" to 1, "mozzarella" to 2, "tomate" to 1, "pimentón" to 1)))
    agregar(Combo("Combo Pepperoni + bebida", 10490, pizzaPepperoni, bebidaCola))
}

val inventario = Inventario(
    mapOf(
        "masa" to 12, "salsa de tomate" to 12, "mozzarella" to 20, "pepperoni" to 5,
        "queso azul" to 4, "parmesano" to 4, "cheddar" to 4, "tomate" to 5,
        "aceitunas" to 5, "chorizo" to 4, "pimentón" to 4
    )
)

val registroClientes = RegistroClientes()

val repartidores: List<Repartidor> = listOf(
    Repartidor("Carlos", "+56 (911) 112-222", "moto", 1.2),
    Repartidor("Ana", "+56 (933) 334-444", "auto", 3.5),
    Repartidor("Pedro", "+56 (955) 556-666", "bicicleta", 0.8)
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
        println("7. Combinar dos pedidos de un cliente")
        println("8. Analizar clases (reflection)")
        println("9. Resumen general de funcionalidades")
        println("10. Cargar datos de demostración y ver resumen")
        println("0. Salir")
        when (leerEntero("Opción: ", 0, 10)) {
            1 -> registrarPedido()
            2 -> mostrarReporte()
            3 -> avanzarEstado()
            4 -> inventario.mostrar()
            5 -> reabastecerIngrediente()
            6 -> repartidores.forEach { println("  ${it.describir()}") }
            7 -> combinarPedidos()
            8 -> menuReflection()
            9 -> mostrarResumenGeneral()
            10 -> cargarDatosDemostracion()
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
    val nombre = leerNombre("Nombre: ")
    val correo = leerCorreo("Correo electrónico: ")
    // Si ya existe un cliente igual (nombre + correo), se reutiliza y no se piden más datos.
    return registroClientes.registrar(nombre, correo) {
        val telefono = leerTelefono("Teléfono (9 dígitos): ")
        val direccion = leerTexto("Dirección de entrega: ", "Sin dirección")
        telefono to direccion
    }
}

fun seleccionarProductos(): List<LineaPedido> {
    val lineas = mutableListOf<LineaPedido>()
    println("\n--- Menú (★ = promocionable entre ${formatearFecha(INICIO_PROMOCION)} y ${formatearFecha(FIN_PROMOCION)}) ---")
    catalogo.todos().forEachIndexed { i, producto -> println("${i + 1}. ${producto.detallarProducto()}") }
    while (true) {
        val unidades = lineas.sumOf { it.cantidad }
        val disponibles = RANGO_UNIDADES.last - unidades
        if (disponibles == 0) {
            println("Se alcanzó el máximo de ${RANGO_UNIDADES.last} unidades por pedido.")
            break
        }
        val opcion = leerEntero("Número de producto (0 para terminar): ", 0, catalogo.tamano)
        if (opcion == 0) break
        val producto = catalogo[opcion - 1] // operador [] sobrecargado en Catalogo

        // Rango 1..disponibles: nunca se supera el total de 100 unidades por pedido.
        println("Puede agregar entre 1 y $disponibles unidades (máximo ${RANGO_UNIDADES.last} por pedido).")
        val cantidad = leerEntero("Cantidad: ", RANGO_UNIDADES.first, disponibles)

        // Si el producto ya está en el pedido (==, por nombre y categoría),
        // se suma a esa línea en vez de crear una línea duplicada.
        val indice = lineas.indexOfFirst { it.producto == producto }
        if (indice >= 0) {
            val nuevaCantidad = lineas[indice].cantidad + cantidad
            lineas[indice] = LineaPedido(producto, nuevaCantidad)
            println("Actualizado: $nuevaCantidad x ${producto.nombre}")
        } else {
            lineas.add(LineaPedido(producto, cantidad))
            println("Agregado: $cantidad x ${producto.nombre}")
        }
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
    val fecha = leerFechaPedido("Fecha del pedido (dd-mm-aaaa, Enter = hoy): ")
    val notas = leerTextoOpcional("Notas del pedido (opcional): ")

    try {
        val pedido = Pedido(siguienteIdPedido, cliente, lineas, fecha, notas)
        pedido.calcularTotal() // valida que el costo sea correcto antes de tocar el inventario

        // Detección dinámica (reflection) de los productos con @Promocionable.
        val promocionables = pedido.productosPromocionables()
        when {
            promocionables.isEmpty() -> println("\nEl pedido no incluye productos promocionables.")
            estaEnPeriodoPromocion(fecha) -> println("\nPromoción aplicada a: " +
                    promocionables.joinToString { "${it.nombre} (${it.promocion()?.porcentaje} %)" })
            else -> println("\nProductos promocionables (${promocionables.joinToString { it.nombre }}), " +
                    "pero la fecha ${formatearFecha(fecha)} está fuera del período de promoción.")
        }

        if (!guardarPedido(pedido)) return

        println("Pedido confirmado.\n")
        println(pedido.generarResumen())
        cliente.notificar("Tu pedido N° ${pedido.id} está ${pedido.estado.etiqueta.lowercase()}.")
    } catch (e: IllegalArgumentException) {
        println("Error en el pedido: ${e.message} Revise los datos e intente de nuevo.")
    }
}

// Verifica y descuenta ingredientes; si alcanzan, guarda el pedido en el día.
// Devuelve false si el pedido se rechazó.
fun guardarPedido(pedido: Pedido): Boolean {
    println("\nVerificando ingredientes...")
    if (!inventario.prepararLote(pedido.lineas)) {
        println("Pedido rechazado por falta de ingredientes.")
        return false
    }
    pedidosDelDia[cantidadPedidos] = pedido
    cantidadPedidos++
    siguienteIdPedido++
    return true
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
        val (cliente, productos, total) = pedido // desestructuración de Pedido
        val detalle = productos.joinToString(", ") { "${it.cantidad}x ${it.producto.nombre}" }
        println("N° ${pedido.id} | ${cliente.nombre} | $detalle | ${formatearCLP(total)} | ${pedido.estado.etiqueta}")
    }

    // Completados = Entregado; pendientes = cualquier otro estado.
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
        EstadoPedido.EN_CAMINO -> {
            val repartidor = pedido.repartidor
            if (repartidor != null && repartidor.confirmarEntrega(pedido)) {
                pedido.cliente.notificar("Tu pedido N° ${pedido.id} fue entregado. ¡Buen provecho!")
            }
        }
        EstadoPedido.ENTREGADO -> println("El pedido N° ${pedido.id} ya fue entregado.")
    }
    println()
    println(pedido.generarResumen())
}

// ===================== Combinar pedidos (operador +) =====================

fun indiceDePedido(id: Int): Int = pedidosDelDia.indexOfFirst { it?.id == id }

// Quita un pedido del arreglo y corre una posición los que estaban después.
fun eliminarPedido(indice: Int) {
    for (i in indice until cantidadPedidos - 1) {
        pedidosDelDia[i] = pedidosDelDia[i + 1]
    }
    pedidosDelDia[cantidadPedidos - 1] = null
    cantidadPedidos--
}

// Combina con + , reemplaza el primero por el resultado, elimina el segundo
// y guarda la combinación en el historial. Lanza IllegalArgumentException si no se puede.
fun aplicarCombinacion(pedidoA: Pedido, pedidoB: Pedido): Pedido {
    val combinado = pedidoA + pedidoB // operador + sobrecargado
    pedidosDelDia[indiceDePedido(pedidoA.id)] = combinado
    eliminarPedido(indiceDePedido(pedidoB.id))
    combinaciones.add(Combinacion(pedidoA, pedidoB, combinado))
    return combinado
}

fun combinarPedidos() {
    val enPreparacion = pedidosDelDia.filterNotNull().filter { it.estado == EstadoPedido.EN_PREPARACION }
    if (enPreparacion.size < 2) {
        println("Se necesitan al menos 2 pedidos en preparación para combinar.")
        return
    }
    println("\n--- Pedidos en preparación ---")
    for (pedido in enPreparacion) {
        val (cliente, _, total) = pedido // desestructuración: se omite lo que no se usa con _
        println("  N° ${pedido.id} | ${cliente.nombre} <${cliente.correo}> | ${formatearFecha(pedido.fecha)} | ${formatearCLP(total)}")
    }
    val idA = leerEntero("N° del primer pedido: ", 1001, siguienteIdPedido - 1)
    val idB = leerEntero("N° del segundo pedido: ", 1001, siguienteIdPedido - 1)
    val pedidoA = buscarPedido(idA)
    val pedidoB = buscarPedido(idB)
    if (pedidoA == null || pedidoB == null) {
        println("Uno de los pedidos no existe.")
        return
    }

    try {
        val combinado = aplicarCombinacion(pedidoA, pedidoB)
        val (cliente, productos, total) = combinado // desestructuración de Pedido
        println("\nN° ${pedidoA.id} (${formatearCLP(pedidoA.calcularTotal().total)}) + " +
                "N° ${pedidoB.id} (${formatearCLP(pedidoB.calcularTotal().total)}) = " +
                "N° ${combinado.id} con ${productos.size} productos distintos, total ${formatearCLP(total)}.")
        println(combinado.generarResumen())
        cliente.notificar("Tus pedidos N° ${pedidoA.id} y N° ${pedidoB.id} se combinaron en el pedido N° ${combinado.id}.")
    } catch (e: IllegalArgumentException) {
        println("No se pudieron combinar: ${e.message}")
    }
}

// ===================== Reflection =====================

fun menuReflection() {
    println("\n1. Pedido\n2. Producto (y sus subclases)\n3. Cliente\n4. Todas")
    when (leerEntero("Clase a analizar: ", 1, 4)) {
        1 -> analizarClase(Pedido::class)
        2 -> {
            analizarClase(Producto::class)
            analizarClase(Combo::class)
            analizarCatalogo(catalogo.todos())
        }
        3 -> analizarClase(Cliente::class)
        4 -> {
            listOf(Pedido::class, Producto::class, Cliente::class).forEach { analizarClase(it) }
            analizarCatalogo(catalogo.todos())
        }
    }
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
        println("Error: ${e.message}")
    }
}