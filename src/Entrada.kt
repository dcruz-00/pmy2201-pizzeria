import java.time.DateTimeException
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val LOCALE_CL: Locale = Locale.forLanguageTag("es-CL")

// ===================== Expresiones regulares =====================
// Correo: nombre@dominio.extension (puede haber subdominios: a@mail.empresa.cl).
private val REGEX_CORREO = Regex("^[\\w.+-]+@[\\w-]+(\\.[\\w-]+)+$")

// Teléfono chileno: 9 dígitos que empiezan entre 2 y 9, con +56 o 56 opcional.
// El grupo 1 captura los 9 dígitos sin el código de país.
private val REGEX_TELEFONO_CL = Regex("^(?:\\+?56)?([2-9]\\d{8})$")

// Separa los 9 dígitos en tres bloques de 3 para darles formato.
private val REGEX_BLOQUES_TELEFONO = Regex("^(\\d{3})(\\d{3})(\\d{3})$")

// Caracteres que el usuario suele escribir como separadores: espacios, guiones, paréntesis y puntos.
private val REGEX_SEPARADORES = Regex("[\\s\\-().]")

// Nombre: letras (incluye tildes y ñ), espacios, apóstrofo, punto o guion; de 2 a 50 caracteres.
private val REGEX_NOMBRE = Regex("^[\\p{L} .'-]{2,50}$")

fun formatearCLP(monto: Int): String = "$" + String.format(LOCALE_CL, "%,d", monto)

// ===================== Lectura por consola =====================

fun leerTexto(mensaje: String, porDefecto: String): String {
    print(mensaje)
    val entrada = readlnOrNull()?.trim()
    return if (entrada.isNullOrEmpty()) {
        println("Dato vacío. Se usará: $porDefecto")
        porDefecto
    } else {
        entrada
    }
}

// Devuelve null si el usuario no escribe nada (campo opcional).
fun leerTextoOpcional(mensaje: String): String? {
    print(mensaje)
    return readlnOrNull()?.trim()?.ifEmpty { null }
}

// Lee un entero dentro de un rango. Usa try-catch para capturar texto no numérico.
fun leerEntero(mensaje: String, min: Int, max: Int): Int {
    while (true) {
        print(mensaje)
        val entrada = readlnOrNull() ?: run {
            println("\nSin entrada. Se usará: $min")
            return min
        }
        try {
            val numero = entrada.trim().toInt() // lanza NumberFormatException si no es número
            if (numero in min..max) return numero
            println("Fuera de rango. Ingrese un número entre $min y $max.")
        } catch (e: NumberFormatException) {
            println("'$entrada' no es un número válido. Intente de nuevo.")
        }
    }
}

// Lee una línea; si la entrada terminó (EOF) no hay forma de seguir pidiendo datos.
private fun leerLinea(mensaje: String): String {
    print(mensaje)
    return readlnOrNull()?.trim() ?: throw IllegalStateException("No hay más entrada disponible.")
}

// ===================== Validaciones con Regex =====================

fun esCorreoValido(correo: String): Boolean = REGEX_CORREO.matches(correo)

// Devuelve el teléfono con formato +56 (XXX) XXX-XXX, o null si no es un número chileno válido.
// Ej.: "9 1234 5678", "+56912345678" y "56-9-1234-5678" -> "+56 (912) 345-678".
fun formatearTelefono(entrada: String): String? {
    val limpio = entrada.replace(REGEX_SEPARADORES, "")
    val numero = REGEX_TELEFONO_CL.find(limpio)?.groupValues?.get(1) ?: return null
    val (bloque1, bloque2, bloque3) = REGEX_BLOQUES_TELEFONO.find(numero)!!.destructured
    return "+56 ($bloque1) $bloque2-$bloque3"
}

fun leerNombre(mensaje: String): String {
    while (true) {
        val nombre = leerLinea(mensaje).replace(Regex("\\s+"), " ")
        if (REGEX_NOMBRE.matches(nombre)) return nombre
        println("Nombre inválido: use solo letras y espacios (entre 2 y 50 caracteres).")
    }
}

fun leerCorreo(mensaje: String): String {
    while (true) {
        val correo = leerLinea(mensaje).lowercase()
        when {
            correo.isEmpty() -> println("El correo es obligatorio.")
            esCorreoValido(correo) -> return correo
            else -> println("Correo '$correo' con formato inválido. Use el formato nombre@dominio.com.")
        }
    }
}

fun leerTelefono(mensaje: String): String {
    while (true) {
        val entrada = leerLinea(mensaje)
        val telefono = formatearTelefono(entrada)
        if (telefono != null) {
            println("Teléfono registrado como: $telefono")
            return telefono
        }
        println("Teléfono '$entrada' inválido. Ingrese 9 dígitos (ej.: 9 1234 5678), con o sin +56.")
    }
}


// ===================== Fechas (Regex + Range) =====================
// Formato dd-mm-aaaa (también se acepta dd/mm/aaaa).
private val REGEX_FECHA = Regex("^(\\d{2})[-/](\\d{2})[-/](\\d{4})$")
private val FORMATO_FECHA: DateTimeFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy")

// Un pedido puede ser para hoy o programarse hasta 30 días después.
const val DIAS_MAX_PROGRAMACION = 30L

fun formatearFecha(fecha: LocalDate): String = fecha.format(FORMATO_FECHA)

// Convierte "dd-mm-aaaa" en LocalDate. Devuelve null si el formato no calza
// o si la fecha no existe (por ejemplo, 31-02-2026).
fun convertirFecha(texto: String): LocalDate? {
    val (dia, mes, anio) = REGEX_FECHA.find(texto)?.destructured ?: return null
    return try {
        LocalDate.of(anio.toInt(), mes.toInt(), dia.toInt())
    } catch (e: DateTimeException) {
        null
    }
}

fun leerFechaPedido(mensaje: String): LocalDate {
    val hoy = LocalDate.now()
    val rangoPermitido = hoy..hoy.plusDays(DIAS_MAX_PROGRAMACION)
    while (true) {
        val entrada = leerLinea(mensaje)
        if (entrada.isEmpty()) return hoy
        val fecha = convertirFecha(entrada)
        when {
            fecha == null -> println("Fecha '$entrada' inválida. Use el formato dd-mm-aaaa con una fecha real.")
            fecha !in rangoPermitido -> println(
                "La fecha debe estar entre ${formatearFecha(rangoPermitido.start)} y " +
                        "${formatearFecha(rangoPermitido.endInclusive)} (hoy o hasta $DIAS_MAX_PROGRAMACION días después)."
            )
            else -> return fecha
        }
    }
}