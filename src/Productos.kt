// Unidades de un mismo ingrediente que puede llevar una pizza.
val RANGO_UNIDADES_INGREDIENTE = 1..10

// Volumen permitido para una bebida, en mililitros.
val RANGO_MILILITROS_BEBIDA = 200..3000

enum class Categoria(val etiqueta: String) {
    PIZZA("Pizza"),
    BEBIDA("Bebida"),
    INGREDIENTE("Ingrediente extra"),
    COMBO("Combo")
}

// ===================== Producto base =====================
// Dos productos son iguales si tienen el mismo nombre (sin importar
// mayúsculas ni espacios al inicio o al final) y la misma categoría.
// Al sobrescribir equals, el operador == de Kotlin usa esta regla.
abstract class Producto(
    nombre: String,
    val precio: Int, // CLP no usa decimales, por eso Int
    val categoria: Categoria
) {
    val nombre: String = nombre.trim()

    init {
        require(this.nombre.isNotEmpty()) { "El nombre del producto no puede estar vacío." }
        require(precio > 0) { "El precio de '${this.nombre}' debe ser mayor que 0 (se recibió $precio)." }
    }

    // Ingrediente del inventario -> unidades que consume una unidad del producto.
    abstract val ingredientes: Map<String, Int>

    // Cuántas pizzas cuenta este producto para el descuento por cantidad.
    open val pizzasIncluidas: Int get() = 0

    // Si la clase está anotada con @Promocionable, se indica en el menú.
    open fun detallarProducto(): String {
        val etiquetaPromo = promocion()?.let { " ★ ${it.porcentaje} % dcto. en promoción" } ?: ""
        return "$nombre - ${formatearCLP(precio)} [${categoria.etiqueta}]$etiquetaPromo"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true          // misma instancia
        if (other !is Producto) return false     // también descarta null
        return nombre.lowercase() == other.nombre.lowercase() && categoria == other.categoria
    }

    // Debe usar los mismos atributos que equals: objetos iguales => mismo hashCode.
    override fun hashCode(): Int = 31 * nombre.lowercase().hashCode() + categoria.hashCode()

    override fun toString(): String = "$nombre (${categoria.etiqueta})"
}

// ===================== Tipos de producto =====================
open class Pizza(
    nombre: String,
    precio: Int,
    ingredientes: Map<String, Int>
) : Producto(nombre, precio, Categoria.PIZZA) {

    final override val ingredientes: Map<String, Int> = ingredientes.toMap()

    init {
        require(this.ingredientes.isNotEmpty()) { "La pizza '${this.nombre}' debe tener al menos un ingrediente." }
        for ((ingrediente, unidades) in this.ingredientes) {
            require(ingrediente.isNotBlank()) { "La pizza '${this.nombre}' tiene un ingrediente sin nombre." }
            require(unidades in RANGO_UNIDADES_INGREDIENTE) {
                "'$ingrediente' en '${this.nombre}' debe estar entre ${RANGO_UNIDADES_INGREDIENTE.first} y " +
                        "${RANGO_UNIDADES_INGREDIENTE.last} unidades (se recibió $unidades)."
            }
        }
    }

    override val pizzasIncluidas: Int get() = 1

    override fun detallarProducto(): String =
        "${super.detallarProducto()} (ingredientes: ${ingredientes.keys.joinToString(", ")})"
}

// Pizza que participa en la promoción del período.
@Promocionable(porcentaje = 20, motivo = "Pizza de temporada")
class PizzaTemporada(
    nombre: String,
    precio: Int,
    ingredientes: Map<String, Int>
) : Pizza(nombre, precio, ingredientes)

class Bebida(
    nombre: String,
    precio: Int,
    val mililitros: Int
) : Producto(nombre, precio, Categoria.BEBIDA) {

    init {
        require(mililitros in RANGO_MILILITROS_BEBIDA) {
            "La bebida '${this.nombre}' debe tener entre ${RANGO_MILILITROS_BEBIDA.first} y " +
                    "${RANGO_MILILITROS_BEBIDA.last} ml (se recibió $mililitros)."
        }
    }

    // Supuesto: las bebidas no se controlan en el inventario de ingredientes.
    override val ingredientes: Map<String, Int> get() = emptyMap()
}

// Porción adicional de un ingrediente; descuenta 1 unidad del inventario.
class IngredienteExtra(
    nombre: String,
    precio: Int,
    val insumo: String
) : Producto(nombre, precio, Categoria.INGREDIENTE) {

    init {
        require(insumo.isNotBlank()) { "El ingrediente extra '${this.nombre}' debe indicar su insumo." }
    }

    override val ingredientes: Map<String, Int> get() = mapOf(insumo to 1)
}

@Promocionable(porcentaje = 15, motivo = "Combo pizza + bebida")
class Combo(
    nombre: String,
    precio: Int,
    val pizza: Pizza,
    val bebida: Bebida
) : Producto(nombre, precio, Categoria.COMBO) {

    init {
        require(precio < pizza.precio + bebida.precio) {
            "El combo '${this.nombre}' debe costar menos que comprar la pizza y la bebida por separado."
        }
    }

    override val ingredientes: Map<String, Int> get() = pizza.ingredientes
    override val pizzasIncluidas: Int get() = 1

    override fun detallarProducto(): String =
        "${super.detallarProducto()} (${pizza.nombre} + ${bebida.nombre})"
}

// ===================== Catálogo sin duplicados =====================
// LinkedHashSet usa hashCode() y equals() para rechazar duplicados
// y mantiene el orden en que se agregaron los productos.
class Catalogo {
    private val productos = linkedSetOf<Producto>()

    // Cuántas veces se intentó agregar un producto repetido.
    var duplicadosEvitados = 0
        private set

    fun agregar(producto: Producto): Boolean {
        val agregado = productos.add(producto) // false si ya existe uno igual
        if (!agregado) {
            duplicadosEvitados++
            println("Producto duplicado, no se agregó: $producto")
        }
        return agregado
    }

    val tamano: Int get() = productos.size

    // Sobrecarga del operador []: catalogo[0] devuelve el primer producto.
    operator fun get(indice: Int): Producto = productos.elementAt(indice)

    // Sobrecarga de "in": producto in catalogo.
    operator fun contains(producto: Producto): Boolean = producto in productos

    fun todos(): List<Producto> = productos.toList()
}