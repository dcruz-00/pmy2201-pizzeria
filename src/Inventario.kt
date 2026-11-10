// Inventario de ingredientes: Map con clave = nombre del ingrediente, valor = cantidad disponible.
class Inventario(stockInicial: Map<String, Int>) {

    private val stock: MutableMap<String, Int> = stockInicial.toMutableMap()

    init {
        for ((ingrediente, cantidad) in stockInicial) {
            require(cantidad >= 0) { "La cantidad inicial de '$ingrediente' no puede ser negativa." }
        }
    }

    // Suma lo que necesita cada línea: unidades por pizza x cantidad de pizzas.
    fun calcularNecesarios(lineas: List<LineaPedido>): Map<String, Int> {
        val necesarios = mutableMapOf<String, Int>()
        for (linea in lineas) {
            for ((ingrediente, porPizza) in linea.producto.ingredientes) {
                necesarios[ingrediente] = (necesarios[ingrediente] ?: 0) + porPizza * linea.cantidad
            }
        }
        return necesarios
    }

    // Revisa cada ingrediente con when. Devuelve los faltantes (vacío si alcanza todo).
    fun verificarDisponibilidad(necesarios: Map<String, Int>): Map<String, Int> {
        val faltantes = mutableMapOf<String, Int>()
        for ((ingrediente, cantidadNecesaria) in necesarios) {
            val disponible = stock[ingrediente] ?: 0
            when {
                disponible >= cantidadNecesaria -> Unit // alcanza
                disponible == 0 -> {
                    println("  - $ingrediente: AGOTADO (se necesitan $cantidadNecesaria).")
                    faltantes[ingrediente] = cantidadNecesaria
                }
                else -> {
                    val falta = cantidadNecesaria - disponible
                    println("  - $ingrediente: insuficiente (hay $disponible, se necesitan $cantidadNecesaria, faltan $falta).")
                    faltantes[ingrediente] = falta
                }
            }
        }
        return faltantes
    }

    // Dado el lote de pizzas del pedido, calcula lo necesario y, si alcanza,
    // descuenta automáticamente del inventario. Devuelve true si se pudo preparar.
    fun prepararLote(lineas: List<LineaPedido>): Boolean {
        val necesarios = calcularNecesarios(lineas)
        val faltantes = verificarDisponibilidad(necesarios)

        if (faltantes.isNotEmpty()) {
            mostrarPasosReabastecimiento(faltantes)
            return false
        }
        for ((ingrediente, cantidad) in necesarios) {
            stock[ingrediente] = (stock[ingrediente] ?: 0) - cantidad
        }
        return true
    }

    fun reabastecer(ingrediente: String, cantidad: Int) {
        if (cantidad <= 0) {
            throw IllegalArgumentException("La cantidad a reabastecer debe ser positiva (se recibió $cantidad).")
        }
        if (ingrediente !in stock) {
            throw IllegalArgumentException("El ingrediente '$ingrediente' no existe en el inventario.")
        }
        val actual = stock[ingrediente] ?: 0
        // Math.addExact lanza ArithmeticException si la suma supera Int.MAX_VALUE,
        // en vez de "dar la vuelta" a un número negativo sin avisar.
        val nuevoTotal = try {
            Math.addExact(actual, cantidad)
        } catch (e: ArithmeticException) {
            throw IllegalArgumentException(
                "No se puede agregar $cantidad a '$ingrediente': se superaría el máximo permitido (${Int.MAX_VALUE})."
            )
        }
        stock[ingrediente] = nuevoTotal
        println("Reabastecido: $ingrediente +$cantidad (total: ${stock[ingrediente]}).")
    }

    fun nombresIngredientes(): List<String> = stock.keys.toList()

    fun mostrar() {
        println("--- Inventario actual ---")
        for ((ingrediente, cantidad) in stock) {
            val alerta = when {
                cantidad == 0 -> " <- AGOTADO"
                cantidad <= 3 -> " <- stock bajo"
                else -> ""
            }
            println("  ${ingrediente.padEnd(18)} $cantidad$alerta")
        }
    }

    private fun mostrarPasosReabastecimiento(faltantes: Map<String, Int>) {
        println("Pasos para reabastecer:")
        println("  1. Contactar al proveedor y solicitar: " +
                faltantes.entries.joinToString(", ") { "${it.key} (mín. ${it.value})" } + ".")
        println("  2. Recibir y revisar la mercadería.")
        println("  3. Registrar el ingreso en el menú 'Reabastecer ingrediente'.")
        println("  4. Volver a ingresar el pedido.")
    }
}
