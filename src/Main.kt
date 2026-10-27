fun leerTexto(mensaje: String, porDefecto: String): String {
    println(mensaje)
    val entrada = readlnOrNull()?.trim()
    return if (entrada.isNullOrEmpty()) {
        println("Dato vacío. Se usará: $porDefecto")
        porDefecto
    } else {
        entrada
    }
}

fun leerEntero(mensaje: String, min: Int, max: Int): Int {
    while (true) {
        println(mensaje)
        val entrada = readlnOrNull()
        if (entrada == null) {
            println("Sin entrada. Se usará: $min")
            return min
        }
        val numero = entrada.trim().toIntOrNull()
        if (numero != null && numero in min..max) return numero
        println("Valor inválido. Ingrese un número entre $min y $max.")
    }
}

fun main() {
    // Cliente
    val nombre: String
    val direccion: String
    val telefono: String

    // Pizza
    val nombresPizzas: List<String> = listOf("Pepperoni", "Cuatro Quesos", "Napolitana", "Española")
    val preciosPizzas: List<Double> = listOf(8990.0, 10490.0, 9490.0, 9990.0)
    val precioUnitario: Double
    var ingredientesDisponibles = 10
    val ingredientesPorPizza = 2

    // Pedido
    val idPedido = 1001
    val cantidadPizzas: Int
    var totalPedido: Double
    val estadoPedido: String

    // Datos personales
    nombre = leerTexto("Ingrese su nombre: ", "Sin nombre")
    direccion = leerTexto("Ingrese la dirección de entrega: ", "Sin dirección")

    println("Finalmente, ingrese su número de contacto (9 dígitos): ")
    val telefonoIngresado = readlnOrNull()?.replace(" ", "") ?: ""
    telefono = if (telefonoIngresado.length == 9 && telefonoIngresado.all { it.isDigit() }) {
        telefonoIngresado
    } else {
        println("Teléfono vacío o inválido. Se usará: Sin teléfono")
        "Sin teléfono"
    }

    // Pedido cliente
    for (i in nombresPizzas.indices) {
        println("${i + 1}. ${nombresPizzas[i]}")
    }
    val pizzaSeleccionada = leerEntero("Seleccione el número de la pizza que desea: ", 1, nombresPizzas.size)
    cantidadPizzas = leerEntero("Ingrese la cantidad de pizzas: ", 1, 20)
    precioUnitario = preciosPizzas[pizzaSeleccionada - 1]

    // Cálculo del total
    totalPedido = precioUnitario * cantidadPizzas
    val aplicaDescuento: Boolean = cantidadPizzas > 3
    totalPedido = if (aplicaDescuento) totalPedido * 0.9 else totalPedido

    // Verificación de stock
    val ingredientesNecesarios = cantidadPizzas * ingredientesPorPizza
    val pedidoConfirmado: Boolean = if (ingredientesNecesarios <= ingredientesDisponibles) {
        ingredientesDisponibles -= ingredientesNecesarios
        println("Pedido confirmado. Ingredientes restantes en inventario: $ingredientesDisponibles")
        true
    } else {
        println("No hay ingredientes suficientes (se necesitan $ingredientesNecesarios y hay $ingredientesDisponibles).")
        println("El pedido no puede ser completado.")
        false
    }
    estadoPedido = if (pedidoConfirmado) "Preparación" else "Cancelado"

    // Asignación de repartidor
    val repartidor: String = when {
        !pedidoConfirmado -> "Sin asignar"
        cantidadPizzas <= 3 -> "Carlos (moto)"
        else -> "Ana (auto)"
    }

    // Imprimir resumen pedido
    println()
    if (estadoPedido == "Cancelado") {
        println("El pedido N° $idPedido fue cancelado.")
    } else {
        println("===RESUMEN===")
        println("Pedido N° $idPedido")
        println("--- Cliente ---")
        println("Nombre cliente: $nombre")
        println("Dirección de entrega: $direccion")
        println("Contacto: $telefono")
        println("--- Pizza ---")
        println("Pizza: ${nombresPizzas[pizzaSeleccionada - 1]}")
        println("Precio unitario: $${"%.0f".format(precioUnitario)}")
        println("--- Pedido ---")
        println("Cantidad pizzas: $cantidadPizzas")
        println("Descuento 10%: ${if (aplicaDescuento) "Sí" else "No"}")
        println("Total: $${"%.0f".format(totalPedido)}")
        println("Estado: $estadoPedido")
        println("Repartidor: $repartidor")
    }
}