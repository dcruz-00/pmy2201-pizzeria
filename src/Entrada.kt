import java.util.Locale

const val TELEFONO_POR_DEFECTO = "(000) 000-0000"
const val CORREO_POR_DEFECTO = "correo@invalido.com"

private val LOCALE_CL: Locale = Locale.forLanguageTag("es-CL")
private val REGEX_CORREO = Regex("^[\\w.+-]+@[\\w-]+(\\.[\\w-]+)+$")
private val REGEX_CELULAR_CL = Regex("^9\\d{8}$")

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

// ===================== Validaciones de strings =====================

// Acepta celulares chilenos (9 dígitos que empiezan con 9), con o sin +56 y espacios/guiones.
fun validarTelefono(entrada: String?): String {
    val limpio = entrada.orEmpty().replace(" ", "").replace("-", "").removePrefix("+").let {
        if (it.length == 11 && it.startsWith("56")) it.substring(2) else it
    }
    return if (REGEX_CELULAR_CL.matches(limpio)) {
        "+56 ${limpio[0]} ${limpio.substring(1, 5)} ${limpio.substring(5)}"
    } else {
        println("Teléfono vacío o inválido. Se usará: $TELEFONO_POR_DEFECTO")
        TELEFONO_POR_DEFECTO
    }
}

// null = el cliente no quiso registrar correo; formato incorrecto = valor por defecto.
fun validarCorreo(entrada: String?): String? {
    if (entrada == null) return null
    val correo = entrada.lowercase()
    return if (REGEX_CORREO.matches(correo)) {
        correo
    } else {
        println("Correo con formato inválido. Se usará: $CORREO_POR_DEFECTO")
        CORREO_POR_DEFECTO
    }
}
