# Sistema de gestión de pizzería — Semana 3

Desarrollo App Móviles I (PMY2201) — Actividad sumativa "Creando soluciones con Kotlin" (Forma B).

Aplicación de consola en Kotlin que gestiona clientes, productos (pizzas, bebidas, ingredientes extra y combos), pedidos, inventario y repartidores. Esta versión agrega Regex, Ranges, anotaciones, reflection, sobrecarga de operadores, desestructuración e igualdad de objetos.

## Requisitos

- IntelliJ IDEA con JDK 17 o superior.
- Kotlin (versión del proyecto, librería `KotlinJavaRuntime`).
- **kotlin-reflect** de la **misma versión de Kotlin**, necesario para `findAnnotation`, `memberProperties` y `declaredMemberFunctions`.

### Configurar kotlin-reflect (si el proyecto no compila con "Unresolved reference: full")

1. File > Project Structure > Modules > pizza > Dependencies > `+` > Library > New Library > From Maven.
2. Escribir `org.jetbrains.kotlin:kotlin-reflect:<versión de Kotlin>` (por ejemplo `2.1.0`).
3. Marcar "Download to" con la carpeta `lib` del proyecto y aceptar.

## Ejecución

Abrir el proyecto en IntelliJ y ejecutar `main()` en `src/Main.kt`.

**Prueba rápida:** elegir la opción **10. Cargar datos de demostración y ver resumen**. Registra clientes y pedidos de ejemplo, combina pedidos y muestra el resumen con todas las funcionalidades, sin escribir datos.

## Menú

| Opción | Función |
|---|---|
| 1 | Registrar pedido (cliente, productos, fecha, notas) |
| 2 | Reporte de pedidos del día |
| 3 | Avanzar estado: En preparación → Listo → En camino → Entregado |
| 4 | Ver inventario |
| 5 | Reabastecer ingrediente |
| 6 | Ver repartidores |
| 7 | Combinar dos pedidos de un cliente (operador `+`) |
| 8 | Analizar clases con reflection (Pedido, Producto, Cliente) |
| 9 | Resumen general de funcionalidades |
| 10 | Cargar datos de demostración y ver resumen |

## Estructura del código (`src/`)

| Archivo | Contenido |
|---|---|
| `Main.kt` | Datos iniciales (catálogo, inventario, repartidores), menú y flujo de cada opción |
| `Usuarios.kt` | `Usuario`, `Cliente` (equals/hashCode, `component1..3`), `RegistroClientes`, `Repartidor` |
| `Productos.kt` | `Categoria`, `Producto` (equals/hashCode), `Pizza`, `PizzaTemporada`, `Bebida`, `IngredienteExtra`, `Combo`, `Catalogo` |
| `Pedido.kt` | `EstadoPedido`, `LineaPedido`, `Totales`, cálculo de totales, `Pedido` (`plus`, `component1..3`) |
| `Promociones.kt` | Anotación `@Promocionable`, período de promoción (rango de fechas), lectura de la anotación |
| `Reflexion.kt` | Listado de propiedades y métodos de una clase; análisis del catálogo |
| `Entrada.kt` | Lectura por consola y validaciones con Regex (nombre, correo, teléfono, fecha) |
| `Inventario.kt` | Stock de ingredientes, verificación, descuento y reabastecimiento |
| `Resumen.kt` | Resumen general integrado y datos de demostración |

## Requerimientos y dónde se implementan

| Requerimiento | Implementación |
|---|---|
| Regex: correo `nombre@dominio.com`, se vuelve a pedir si es inválido | `Entrada.kt`: `REGEX_CORREO`, `leerCorreo()` |
| Regex: formato uniforme de teléfono | `Entrada.kt`: `formatearTelefono()` → `+56 (XXX) XXX-XXX` |
| Range: fechas de promoción con descuento | `Promociones.kt`: `PERIODO_PROMOCION`; `Pedido.kt`: `calcularTotalPedido()` |
| Range: cantidad de productos 1–100 | `Pedido.kt`: `RANGO_UNIDADES`; `Main.kt`: `seleccionarProductos()` |
| Anotación `@Promocionable` | `Promociones.kt`; aplicada a `PizzaTemporada` y `Combo` |
| Detección dinámica de promocionables | `Producto.promocion()` con `findAnnotation`; `Pedido.productosPromocionables()` |
| Reflection de Pedido, Producto y Cliente | `Reflexion.kt`: `analizarClase()`; menú opción 8 |
| Operador `+` en Pedido | `Pedido.plus()`; menú opción 7 |
| `==` en Producto por nombre y categoría | `Producto.equals()` / `hashCode()` |
| Desestructuración de Cliente | `Cliente.component1..3()` → `(nombre, correo, telefono)` |
| Desestructuración de Pedido | `Pedido.component1..3()` → `(cliente, productos, total)` |
| equals/hashCode en Cliente y Producto | `Cliente` (nombre + correo), `Producto` (nombre + categoría) |
| Evitar duplicados | `RegistroClientes` y `Catalogo` (`LinkedHashSet`); fusión de líneas repetidas en un pedido |
| Resumen en consola | `Resumen.kt`: `mostrarResumenGeneral()`; opciones 9 y 10 |

### Mejoras por la retroalimentación de la Semana 2

- Estado **Entregado** y `Repartidor.confirmarEntrega()`, que libera al repartidor.
- `Inventario.reabastecer()` usa `Math.addExact()` para evitar desbordamientos.
- Validación de unidades de cada ingrediente por pizza (1–10) y del total de unidades por pedido (1–100).

## Decisiones y supuestos

- **Teléfono:** se adaptó a Chile. Un número chileno tiene 9 dígitos, por lo que el formato `+XX (XXX) XXX-XXXX` de la instrucción no aplica; se usa `+56 (XXX) XXX-XXX`. Se aceptan entradas con o sin `+56`, espacios, guiones, paréntesis o puntos.
- **Correo obligatorio** para clientes, porque forma parte de la clave de igualdad (nombre + correo).
- **Precios netos:** el IVA (19 %) se suma después de los descuentos.
- **Descuentos:** 10 % con más de 3 pizzas (los combos cuentan como pizza) y descuento de promoción solo sobre productos `@Promocionable` cuando la fecha está en el período (01-11-2026 al 30-11-2026). Ambos se calculan sobre el subtotal original y se suman.
- **Fecha del pedido:** desde hoy hasta 30 días después (pedidos programados). Para probar la promoción fuera de noviembre de 2026, usar la opción 10, que usa el 01-11-2026 como fecha de los datos de ejemplo.
- **Combinar pedidos:** solo del mismo cliente, con la misma fecha y en preparación. El resultado conserva el N° del primero; el inventario no se vuelve a descontar.
- **Bebidas:** no se controlan en el inventario de ingredientes.
- Los datos se guardan en memoria: se pierden al cerrar el programa.