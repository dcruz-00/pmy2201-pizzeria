import java.time.LocalDate
import kotlin.reflect.full.declaredMemberFunctions
import kotlin.reflect.full.memberProperties

// ===================== Resumen general (Paso 6 y 7 de la instrucción) =====================
// Integra en una sola salida: operador +, desestructuración, anotaciones,
// igualdad de objetos, validaciones (Regex y Ranges) y reflection.

private fun titulo(texto: String) {
    println("\n------------------------------------------------------------")
    println(" $texto")
    println("------------------------------------------------------------")
}

private fun siNo(valor: Boolean): String = if (valor) "sí" else "no"

fun mostrarResumenGeneral() {
    println("\n############################################################")
    println("#        RESUMEN GENERAL - SISTEMA DE LA PIZZERÍA          #")
    println("############################################################")
    println("Pedidos activos: $cantidadPedidos | Clientes registrados: ${registroClientes.cantidad} | " +
            "Productos en catálogo: ${catalogo.tamano}")

    resumenPedidosCombinados()
    resumenClientesDesestructurados()
    resumenPromocionables()
    resumenIgualdad()
    resumenValidaciones()
    resumenReflection()

    println("\n############################ FIN ############################")
}

// 1. Pedidos combinados mediante la sobrecarga del operador +.
private fun resumenPedidosCombinados() {
    titulo("1. Pedidos combinados (operador +)")
    if (combinaciones.isEmpty()) {
        println("Aún no se han combinado pedidos (opción 7 del menú o datos de demostración).")
        return
    }
    for ((primero, segundo, resultado) in combinaciones) { // desestructuración de data class
        val (_, productosA, totalA) = primero
        val (_, productosB, totalB) = segundo
        val (cliente, productos, total) = resultado
        println("N° ${primero.id} (${productosA.size} productos, ${formatearCLP(totalA)}) + " +
                "N° ${segundo.id} (${productosB.size} productos, ${formatearCLP(totalB)})")
        println("  = N° ${resultado.id} para ${cliente.nombre}: ${productos.size} productos, total ${formatearCLP(total)} " +
                "(diferencia por descuentos recalculados: ${formatearCLP(totalA + totalB - total)})")
    }
    // Confirmación completa del último pedido combinado (para el cliente y la pizzería).
    println("\nConfirmación del último pedido combinado:")
    println(combinaciones.last().resultado.generarResumen())
}

// 2. Información de clientes desestructurados (nombre, correo, teléfono).
private fun resumenClientesDesestructurados() {
    titulo("2. Clientes desestructurados: val (nombre, correo, telefono) = cliente")
    val clientes = registroClientes.todos()
    if (clientes.isEmpty()) {
        println("Aún no hay clientes registrados.")
        return
    }
    for (cliente in clientes) {
        val (nombre, correo, telefono) = cliente // operator component1..3
        println("  Nombre: ${nombre.padEnd(20)} Correo: ${correo.padEnd(24)} Teléfono: $telefono")
    }
}

// 3. Productos identificados como promocionables mediante la anotación.
private fun resumenPromocionables() {
    titulo("3. Productos promocionables (@Promocionable + reflection)")
    val hoy = LocalDate.now()
    println("Período: ${formatearFecha(INICIO_PROMOCION)} al ${formatearFecha(FIN_PROMOCION)} | " +
            "Hoy (${formatearFecha(hoy)}) dentro del período: ${siNo(estaEnPeriodoPromocion(hoy))}")
    val promocionables = catalogo.todos().filter { it.esPromocionable() }
    for (producto in promocionables) {
        val promo = producto.promocion()!!
        println("  ★ ${producto.nombre.padEnd(26)} clase ${producto::class.simpleName.orEmpty().padEnd(15)} " +
                "${promo.porcentaje} % - ${promo.motivo}")
    }
    println("No promocionables: ${catalogo.todos().filterNot { it.esPromocionable() }.joinToString { it.nombre }}")
}

// 4. Validación de igualdad de objetos (clientes y productos).
private fun resumenIgualdad() {
    titulo("4. Igualdad de objetos (equals / hashCode / == / ===)")

    println("Productos (iguales si coinciden nombre y categoría):")
    val original = catalogo[0]
    val copia = Pizza("  ${original.nombre.uppercase()} ", 1, mapOf("masa" to 1)) // otro precio e ingredientes
    val otraCategoria = IngredienteExtra(original.nombre, 1490, "pepperoni")
    println("  '${original.nombre}' == '${copia.nombre}' (otro precio): ${siNo(original == copia)}")
    println("  '${original.nombre}' === '${copia.nombre}' (misma instancia): ${siNo(original === copia)}")
    println("  hashCode iguales: ${siNo(original.hashCode() == copia.hashCode())}")
    println("  Pizza '${original.nombre}' == Ingrediente extra '${otraCategoria.nombre}': ${siNo(original == otraCategoria)}")
    println("  ¿El catálogo ya contiene la copia? ${siNo(copia in catalogo)} -> no se agregaría como nuevo producto")

    println("Clientes (iguales si coinciden nombre y correo):")
    val primerCliente = registroClientes.todos().firstOrNull()
    if (primerCliente == null) {
        println("  (Registre un cliente para ver esta comparación)")
    } else {
        val copiaCliente = Cliente(
            primerCliente.nombre.uppercase(), "+56 (900) 000-000", primerCliente.correo.uppercase(), "Otra dirección"
        )
        val otroCliente = Cliente(primerCliente.nombre, primerCliente.telefono, "otro@correo.cl", primerCliente.direccion)
        println("  $primerCliente == ${copiaCliente.nombre} <${copiaCliente.correo}>: ${siNo(primerCliente == copiaCliente)}")
        println("  $primerCliente === copia: ${siNo(primerCliente === copiaCliente)}")
        println("  Mismo nombre, otro correo (${otroCliente.correo}): ${siNo(primerCliente == otroCliente)}")
        println("  ¿El registro ya contiene la copia? ${siNo(copiaCliente in registroClientes)}")
    }
    println("Duplicados evitados en esta sesión: clientes ${registroClientes.duplicadosEvitados}, " +
            "productos ${catalogo.duplicadosEvitados}")
}

// 5. Validaciones con Regex y Ranges sobre datos de ejemplo.
private fun resumenValidaciones() {
    titulo("5. Validaciones (Regex y Ranges)")
    for (correo in listOf("ana@mail.cl", "ana@mail", "ana.mail.cl")) {
        println("  Correo ${"'$correo'".padEnd(22)} válido: ${siNo(esCorreoValido(correo))}")
    }
    for (telefono in listOf("9 1234 5678", "+56 2 2345 6789", "12345")) {
        println("  Teléfono ${"'$telefono'".padEnd(20)} -> ${formatearTelefono(telefono) ?: "inválido"}")
    }
    for (texto in listOf("20-11-2026", "31-02-2026", "2026-11-20")) {
        val fecha = convertirFecha(texto)
        val detalle = fecha?.let { "válida, en promoción: ${siNo(estaEnPeriodoPromocion(it))}" } ?: "inválida"
        println("  Fecha ${"'$texto'".padEnd(23)} -> $detalle")
    }
    for (cantidad in listOf(0, 50, 101)) {
        println("  Cantidad ${cantidad.toString().padEnd(20)} en rango ${RANGO_UNIDADES}: ${siNo(cantidad in RANGO_UNIDADES)}")
    }
}

// 6. Reflection: cantidad de miembros de las clases clave.
private fun resumenReflection() {
    titulo("6. Reflection (detalle completo en la opción 8 del menú)")
    for (clase in listOf(Pedido::class, Producto::class, Cliente::class)) {
        println("  ${clase.simpleName.orEmpty().padEnd(10)} propiedades: ${clase.memberProperties.size.toString().padEnd(3)} " +
                "métodos declarados: ${clase.declaredMemberFunctions.size}")
    }
}

// ===================== Datos de demostración =====================
// Carga clientes y pedidos de ejemplo para mostrar todas las funcionalidades
// sin tener que escribir datos. Solo se puede cargar una vez por ejecución.
private var demoCargada = false

fun cargarDatosDemostracion() {
    if (demoCargada) {
        println("Los datos de demostración ya fueron cargados.")
        mostrarResumenGeneral()
        return
    }
    if (cantidadPedidos + 3 > MAX_PEDIDOS_DIA) {
        println("No hay espacio para los pedidos de demostración.")
        return
    }
    demoCargada = true
    println("\n=== Cargando datos de demostración ===")

    // Si hoy está fuera del período de promoción, se usa su fecha de inicio
    // para que la demostración muestre el descuento por @Promocionable.
    val hoy = LocalDate.now()
    val fecha = if (estaEnPeriodoPromocion(hoy)) hoy else INICIO_PROMOCION

    val ana = registroClientes.registrar(
        Cliente("Ana Pérez", formatearTelefono("9 1234 5678")!!, "ana@mail.cl", "Av. Providencia 123")
    )
    val luis = registroClientes.registrar(
        Cliente("Luis Soto", formatearTelefono("+56 2 2345 6789")!!, "luis@mail.cl", "Los Leones 456")
    )
    // Mismo nombre y correo que Ana, con otras mayúsculas: equals detecta el duplicado.
    val anaRepetida = registroClientes.registrar(
        Cliente("  ana pérez ", formatearTelefono("987654321")!!, "ANA@MAIL.CL", "Otra dirección")
    )
    // Producto repetido: el catálogo lo rechaza.
    catalogo.agregar(Pizza("pepperoni", 9990, mapOf("masa" to 1)))

    val producto = { nombre: String -> catalogo.todos().first { it.nombre == nombre } }
    val pedido1 = Pedido(siguienteIdPedido, ana,
        listOf(LineaPedido(producto("Pepperoni"), 2), LineaPedido(producto("Bebida cola 1,5 L"), 1)), fecha, "sin cebolla")
    if (!guardarPedido(pedido1)) return
    val pedido2 = Pedido(siguienteIdPedido, anaRepetida,
        listOf(LineaPedido(producto("Combo Pepperoni + bebida"), 1), LineaPedido(producto("Primavera"), 1)), fecha, "tocar timbre")
    if (!guardarPedido(pedido2)) return
    val pedido3 = Pedido(siguienteIdPedido, luis,
        listOf(LineaPedido(producto("Napolitana"), 1)), fecha, null)
    if (!guardarPedido(pedido3)) return
    println("Pedidos creados: N° ${pedido1.id} y N° ${pedido2.id} (Ana), N° ${pedido3.id} (Luis).")

    try {
        val combinado = aplicarCombinacion(pedido1, pedido2)
        println("Pedidos N° ${pedido1.id} + N° ${pedido2.id} combinados en N° ${combinado.id}.")
    } catch (e: IllegalArgumentException) {
        println("No se pudieron combinar: ${e.message}")
    }
    try {
        aplicarCombinacion(pedido3, buscarPedido(pedido1.id)!!)
    } catch (e: IllegalArgumentException) {
        println("Combinación de prueba rechazada (clientes distintos): ${e.message}")
    }

    mostrarResumenGeneral()
}