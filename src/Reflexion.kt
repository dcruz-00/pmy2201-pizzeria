import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KMutableProperty
import kotlin.reflect.KParameter
import kotlin.reflect.KVisibility
import kotlin.reflect.full.declaredMemberFunctions
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.superclasses

// ===================== Reflection =====================
// Analiza una clase mientras el programa se ejecuta: superclase, anotaciones,
// propiedades (incluidas las heredadas) y métodos declarados en la clase.
// No hay que mantener listas a mano: si se agrega una propiedad o un método,
// aparece automáticamente.

private fun visibilidad(v: KVisibility?): String = when (v) {
    KVisibility.PUBLIC -> "public"
    KVisibility.PROTECTED -> "protected"
    KVisibility.INTERNAL -> "internal"
    KVisibility.PRIVATE -> "private"
    null -> "local"
}

// Quita el paquete de los tipos: "kotlin.collections.List<LineaPedido>" -> "List<LineaPedido>".
private fun tipoCorto(tipo: Any): String =
    tipo.toString().replace(Regex("\\b[a-z]+(\\.[a-z]+)*\\."), "")

private fun firma(funcion: KFunction<*>): String {
    val parametros = funcion.parameters
        .filter { it.kind == KParameter.Kind.VALUE } // se omite el receptor (this)
        .joinToString(", ") { "${it.name}: ${tipoCorto(it.type)}" }
    val operador = if (funcion.isOperator) "operator " else ""
    return "$operador${funcion.name}($parametros): ${tipoCorto(funcion.returnType)}"
}

fun analizarClase(clase: KClass<*>) {
    println("\n=============== Clase ${clase.simpleName} ===============")

    val padres = clase.superclasses.filter { it != Any::class }.joinToString { it.simpleName ?: "?" }
    println("Hereda de: ${padres.ifEmpty { "Any" }}${if (clase.isAbstract) " | clase abstracta" else ""}")

    val promo = clase.findAnnotation<Promocionable>()
    println("@Promocionable: ${promo?.let { "sí (${it.porcentaje} %, ${it.motivo})" } ?: "no"}")

    val propiasNombres = clase.declaredMemberProperties.map { it.name }.toSet()
    val propiedades = clase.memberProperties.sortedBy { it.name }
    println("Propiedades (${propiedades.size}):")
    for (prop in propiedades) {
        val tipoDecl = if (prop is KMutableProperty<*>) "var" else "val"
        // Ej.: "var estado ... private set" -> se lee público, pero solo la clase lo modifica.
        val setter = if (prop is KMutableProperty<*> && prop.setter.visibility != prop.visibility)
            " (set ${visibilidad(prop.setter.visibility)})" else ""
        val origen = if (prop.name in propiasNombres) "" else " (heredada)"
        println("  - ${visibilidad(prop.visibility)} $tipoDecl ${prop.name}: ${tipoCorto(prop.returnType)}$setter$origen")
    }

    val metodos = clase.declaredMemberFunctions.sortedBy { it.name }
    println("Métodos declarados en la clase (${metodos.size}):")
    for (metodo in metodos) {
        println("  - ${visibilidad(metodo.visibility)} ${firma(metodo)}")
    }
}

// Recorre los productos del catálogo y detecta, según su clase real,
// cuáles están anotados con @Promocionable.
fun analizarCatalogo(productos: List<Producto>) {
    println("\n=============== Productos del catálogo (reflection) ===============")
    for (producto in productos) {
        val clase = producto::class
        val promo = clase.findAnnotation<Promocionable>()
        val detalle = promo?.let { "@Promocionable(${it.porcentaje} %, \"${it.motivo}\")" } ?: "sin anotación"
        println("  ${producto.nombre.padEnd(26)} clase ${clase.simpleName.orEmpty().padEnd(16)} $detalle")
    }
}