// ===================== Usuarios (herencia y polimorfismo) =====================
open class Usuario(
    val nombre: String,
    val telefono: String,
    open val correo: String? // el repartidor no registra correo
) {
    open fun describir(): String =
        "$nombre | Tel: $telefono | Correo: ${correo ?: "Sin correo"}"
}

// El cliente siempre tiene correo: se valida con regex al registrarlo.
// Dos clientes son el mismo si coinciden nombre y correo
// (sin distinguir mayúsculas ni espacios al inicio o al final).
class Cliente(
    nombre: String,
    telefono: String,
    override val correo: String,
    val direccion: String
) : Usuario(nombre, telefono, correo) {

    override fun describir(): String = "Cliente: ${super.describir()} | Dirección: $direccion"

    fun notificar(mensaje: String) {
        println("Notificación enviada a $correo: $mensaje")
    }

    // Desestructuración: val (nombre, correo, telefono) = cliente
    operator fun component1(): String = nombre
    operator fun component2(): String = correo
    operator fun component3(): String = telefono

    private val claveNombre: String get() = nombre.trim().lowercase()
    private val claveCorreo: String get() = correo.trim().lowercase()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Cliente) return false
        return claveNombre == other.claveNombre && claveCorreo == other.claveCorreo
    }

    // Mismos atributos que equals, para que funcione en HashSet y HashMap.
    override fun hashCode(): Int = 31 * claveNombre.hashCode() + claveCorreo.hashCode()

    override fun toString(): String = "$nombre <$correo>"
}

// Registro de clientes sin duplicados (usa equals y hashCode de Cliente).
class RegistroClientes {
    private val clientes = linkedSetOf<Cliente>()

    // Cuántas veces se evitó registrar un cliente repetido.
    var duplicadosEvitados = 0
        private set

    // Registro desde el menú: primero se compara por nombre y correo, y solo si el
    // cliente es nuevo se ejecuta pedirDatos() para solicitar teléfono y dirección.
    fun registrar(nombre: String, correo: String, pedirDatos: () -> Pair<String, String>): Cliente {
        // Cliente "clave": solo sirve para comparar con equals/hashCode (nombre + correo).
        val clave = Cliente(nombre, "", correo, "")
        if (clave in clientes) {
            duplicadosEvitados++
            val existente = clientes.first { it == clave }
            println("Cliente ya registrado: $existente. Se usarán sus datos " +
                    "(teléfono ${existente.telefono}, dirección ${existente.direccion}).")
            return existente
        }
        val (telefono, direccion) = pedirDatos() // desestructuración de Pair
        return registrar(Cliente(nombre, telefono, correo, direccion))
    }

    // Devuelve el cliente que queda registrado: el nuevo o el que ya existía.
    fun registrar(nuevo: Cliente): Cliente {
        if (clientes.add(nuevo)) {
            println("Cliente registrado: $nuevo")
            return nuevo
        }
        duplicadosEvitados++
        val existente = clientes.first { it == nuevo }
        println("Ya existe un cliente con ese nombre y correo. Se usará el registro existente " +
                "(dirección: ${existente.direccion}).")
        return existente
    }

    // Permite escribir "cliente in registroClientes".
    operator fun contains(cliente: Cliente): Boolean = cliente in clientes

    val cantidad: Int get() = clientes.size

    fun todos(): List<Cliente> = clientes.toList()
}

class Repartidor(
    nombre: String,
    telefono: String,
    val vehiculo: String,
    val distanciaKm: Double,
    var disponible: Boolean = true
) : Usuario(nombre, telefono, null) {

    override fun describir(): String =
        "Repartidor: $nombre ($vehiculo) | a $distanciaKm km | ${if (disponible) "Disponible" else "Ocupado"}"

    fun entregarPedido(pedido: Pedido) {
        disponible = false
        pedido.repartidor = this
        pedido.cambiarEstado(EstadoPedido.EN_CAMINO)
        println("$nombre ($vehiculo) salió a entregar el pedido N° ${pedido.id} a ${pedido.cliente.direccion}.")
    }

    // Cierra la entrega y libera al repartidor. Solo puede confirmarla
    // el repartidor asignado (=== compara que sea la misma instancia).
    fun confirmarEntrega(pedido: Pedido): Boolean {
        if (pedido.repartidor !== this) {
            println("$nombre no es el repartidor asignado al pedido N° ${pedido.id}.")
            return false
        }
        if (!pedido.cambiarEstado(EstadoPedido.ENTREGADO)) return false
        disponible = true
        println("$nombre entregó el pedido N° ${pedido.id} y quedó disponible.")
        return true
    }
}