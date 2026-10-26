fun main() {
    // Cliente
    val nombre: String
    val direccion: String
    val telefono: String

    // Pizza
    val nombresPizzas: List<String> = listOf("Pepperoni", "Cuatro Quesos", "Napolitana", "Española")
    val preciosPizzas: List<Double> = listOf(8990.0, 10490.0, 9490.0, 9990.0)
    var precioUnitario: Double
    var ingredientesDisponibles: Int

    // Pedido
    var idPedido: Int
    var cantidadPizzas: Int
    var totalPedido: Double
    var estadoPedido: String

    // Datos personales
    println("Ingrese su nombre: ")
    nombre = readlnOrNull().toString()
    println("Ingrese la dirección de entrega: ")
    direccion = readlnOrNull().toString()
    println("Finalmente, ingrese su número de contacto: ")
    telefono = readlnOrNull().toString()

    // Pedido cliente
    println("Ingrese la cantidad de pizzas: ")
    cantidadPizzas = readln().toInt()
    println("Seleccione el número de la pizza que desea: ")
    for (i in nombresPizzas.indices) {
        println("${i + 1}. ${nombresPizzas[i]}")
    }
    val pizzaSeleccionada: Int = readln().toInt()

    // Datos del pedido que no ingresa el usuario
    idPedido = 1001
    precioUnitario = preciosPizzas[pizzaSeleccionada - 1]
    ingredientesDisponibles = 10
    val ingredientesPorPizza = 2

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