# Integrante 3 – Libro de caja y balance (`feature/finanzas`)

## Qué incluye
- `finanzas/FinanzasFragment` – pantalla de detalle financiero: saldo, ingresos, gastos, historial y botón **Registrar movimiento**.
- Formulario (diálogo): tipo (Gasto/Ingreso), categoría, monto en COP, fecha y descripción, con validación.
- `CalculadoraBalance` – saldo = ingresos − gastos; historial ordenado por fecha (más reciente primero).
- `MovimientoRepository` (interfaz) + `LocalMovimientoRepository` (guarda en el dispositivo).
- Pruebas: `app/src/test/.../finanzas/CalculadoraBalanceTest.java`.

## Cómo abrir la pantalla desde otro módulo (Integrantes 2 y 5)
Destino `FinanzasFragment` en `nav_graph.xml`, con dos argumentos:

```java
Bundle args = new Bundle();
args.putString("loteId", lote.getId());        // uuid del lote
args.putString("loteNombre", lote.getNombre());
navController.navigate(R.id.FinanzasFragment, args);
```
`FirstFragment` trae un botón de demo que ya hace esto con un lote de ejemplo; reemplazarlo cuando exista la lista de lotes.

## Pendiente: conexión con Supabase (Integrante 1)
Hoy los movimientos se guardan localmente. Para usar la tabla `movimientos`, crear una clase que implemente `MovimientoRepository` (listar por `lote_id`, insertar) y cambiar una línea en `FinanzasFragment.onViewCreated`. Tener en cuenta que en el esquema `movimientos.categoria_id` es una FK a `categorias`, mientras que hoy la categoría se guarda como texto.

## Criterio de aceptación
Registrar un gasto de $300.000 COP en un lote sin otros movimientos → saldo **-$300.000 COP**. Agregar luego un ingreso de $1.000.000 COP → saldo **$700.000 COP**.
