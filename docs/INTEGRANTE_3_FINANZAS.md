# Integrante 3 – Libro de caja y balance (`feature/finanzas`)

## Qué incluye
- `finanzas/FinanzasFragment` – pantalla de detalle financiero: saldo, ingresos, gastos, historial y botón **Registrar movimiento**.
- Formulario (diálogo): tipo (Gasto/Ingreso), categoría, monto en COP, fecha y descripción, con validación.
- `CalculadoraBalance` – saldo = ingresos − gastos; historial ordenado por fecha (más reciente primero).
- `MovimientoRepository` (interfaz) + `SupabaseMovimientoRepository` (activa: tabla `movimientos` de Supabase) + `LocalMovimientoRepository` (respaldo local en el dispositivo).
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

## Conexión con Supabase: lista
Los movimientos ya se leen y escriben en la tabla `movimientos` de Supabase mediante `SupabaseMovimientoRepository` (implementación activa de `MovimientoRepository`, misma capa de red que `LoteRepository`: hilo de fondo + callbacks en el hilo principal). Se cambió una sola línea en `FinanzasFragment.onViewCreated`.

Para usarlo hay que ejecutar **una vez** en el SQL Editor de Supabase el script `supabase/movimientos_permisos.sql` (grant de select/insert/update/delete a `anon` y `authenticated`, políticas RLS MVP y `lote_id drop not null` para el libro de caja general). El script es idempotente y **no se ejecuta desde la app**.

Notas del esquema real (difiere del README):
- `usuario_id`: el repo lo filtra siempre con `usuario_id=eq.<sesión>` en el SELECT y lo escribe en cada INSERT (defensa ante las políticas MVP).
- `categoria_id`: FK a `categorias`; la app guarda el nombre visible y lo traduce a uuid con el catálogo (`nombre` + `tipo`) antes de insertar. Si el nombre no existe en la BD → error "La categoría no existe en el catálogo de la base de datos.".
- `fecha`: la app envía `yyyy-MM-dd` y al leer trunca el timestamptz a `yyyy-MM-dd` (lo que espera `FormatoUtil`); `created_at` se traduce al campo interno `creadoEn` para ordenar.
- Sin lote (pestaña Finanzas desde Home) el libro de caja es **general**: se listan todos los movimientos del usuario y se inserta `lote_id = null`.

## Criterio de aceptación
Registrar un gasto de $300.000 COP en un lote sin otros movimientos → saldo **-$300.000 COP**. Agregar luego un ingreso de $1.000.000 COP → saldo **$700.000 COP**.
