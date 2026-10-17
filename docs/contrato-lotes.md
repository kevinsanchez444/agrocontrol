# Integrante 2 – Registro y listado de lotes

Rama de trabajo: `feature/lotes`.

Este documento resume lo entregado en el módulo de lotes y los contratos acordados con los
demás integrantes (sobre todo con el Integrante 5, según la tarea 6 del plan de trabajo).

## Qué se implementó

| Tarea del plan | Dónde quedó |
|---|---|
| Lista de lotes en XML con tarjeta por lote (nombre, cultivo, hectáreas) | `app/src/main/res/layout/fragment_lotes.xml` y `app/src/main/res/layout/item_lote.xml` |
| Formulario de nuevo lote | `app/src/main/res/layout/fragment_nuevo_lote.xml` + `NuevoLoteFragment` |
| Validación de nombre, cultivo y hectáreas | `NuevoLoteFragment.validar(...)` |
| Guardar y consultar los lotes en Supabase | `data/LoteRepository.java` (REST/PostgREST) |
| Estado vacío | `fragment_lotes.xml` (`texto_vacio`) gestionado por `LotesFragment` |
| Contrato con el Integrante 5 (fragment y parámetro) | `ui/lotes/LotesContrato.java` y este documento |

Clases nuevas:

```
app/src/main/java/com/example/agentes/
├── model/Lote.java                      # modelo del lote (café, plátano, aguacate)
├── data/SupabaseConfig.java             # URL, anon key y sesión (las llena el Integrante 1)
├── data/LoteRepository.java             # listarLotes() y crearLote() contra Supabase
└── ui/lotes/
    ├── LotesFragment.java               # pantalla de listado + estado vacío
    ├── NuevoLoteFragment.java           # formulario con validaciones
    ├── LotesAdapter.java                # tarjetas de la lista
    └── LotesContrato.java               # nombres de pantallas y argumentos (contrato)
```

## Validaciones implementadas

El formulario **no guarda** si los datos son inválidos y muestra el error en el campo:

- **Nombre**: obligatorio (no puede quedar vacío).
- **Cultivo**: debe ser `Café`, `Plátano` o `Aguacate` (los únicos del MVP).
- **Hectáreas**: número mayor que 0 (se acepta coma o punto decimal).

Con datos válidos el lote se guarda en Supabase y la lista se recarga. El estado vacío muestra
el mensaje *"Aún no tienes lotes. ¡Crea el primero!"*.

---

## Contrato con el Integrante 5 (interfaz principal y navegación)

Estos son los nombres y parámetros acordados. Todo vive en `LotesContrato.java`.

| Concepto | Valor |
|---|---|
| Pantalla de listado | `com.example.agentes.ui.lotes.LotesFragment` |
| Formulario de nuevo lote | `com.example.agentes.ui.lotes.NuevoLoteFragment` |
| Pantalla de detalle (Integrante 3) | `com.example.agentes.ui.finanzas.DetalleLoteFragment` (propuesto) |
| Acción listado → nuevo lote | `accionLotesANuevoLote` |
| Acción listado → detalle | `accionLotesADetalleLote` |
| **Parámetro al seleccionar un lote** | **`lote_id`** (obligatorio) + `lote_nombre` (opcional, para el título) |
| Resultado al guardar un lote | `resultado_nuevo_lote` |

### Qué falta integrar (responsabilidad del Integrante 5)

El módulo funciona de forma aislada: el formulario se abre como hoja inferior, por lo que
**no** se modificó `nav_graph.xml`. Para dejar el listado accesible desde la interfaz principal,
el Integrante 5 debe agregar los destinos. Fragmento listo para pegar dentro de la etiqueta
`<navigation ...>`:

```xml
<fragment
    android:id="@+id/lotesFragment"
    android:name="com.example.agentes.ui.lotes.LotesFragment"
    android:label="@string/lotes_titulo"
    tools:layout="@layout/fragment_lotes">

    <action
        android:id="@+id/accionLotesANuevoLote"
        app:destination="@id/nuevoLoteFragment" />
</fragment>

<fragment
    android:id="@+id/nuevoLoteFragment"
    android:name="com.example.agentes.ui.lotes.NuevoLoteFragment"
    android:label="@string/nuevo_lote_titulo" />
```

Al tocar una tarjeta, `LotesFragment.abrirDetalleLote(...)` ya arma el `Bundle` con `lote_id`
usando `LotesContrato.argumentosDetalleLote(lote)`. Cuando exista el destino de detalle, basta
reemplazar el `Snackbar` provisional por:

```java
findNavController().navigate(R.id.accionLotesADetalleLote, argumentos);
```

---

## Contrato con el Integrante 1 (Supabase)

`LoteRepository` usa la API REST (PostgREST) de Supabase y espera la tabla:

```
lotes(
  id          uuid primary key default gen_random_uuid(),
  usuario_id  uuid not null references usuarios(id),  -- o perfiles(id)
  nombre      text not null,
  cultivo     text not null,      -- 'Café' | 'Plátano' | 'Aguacate'
  hectareas   numeric(8,2) not null check (hectareas > 0),
  creado_en   timestamptz not null default now()
)
```

- Las consultas ordenan por `creado_en.desc`.
- Las políticas RLS deben permitir al usuario autenticado leer/insertar solo sus propios lotes
  (`usuario_id = auth.uid()`).
- Cuando el Integrante 1 tenga los valores reales, debe reemplazar los marcadores de
  `SupabaseConfig.SUPABASE_URL` y `SupabaseConfig.SUPABASE_ANON_KEY`, y llamar a
  `SupabaseConfig.setSesion(token, usuarioId)` tras el login (y `limpiarSesion()` al salir).
  **Nunca** se versiona la `service_role key`.

> Mientras los valores sigan siendo marcadores, el repositorio devuelve el mensaje
> "Falta configurar la conexión con Supabase." en lugar de intentar la petición.

## Criterios de aceptación (del plan)

- Con datos inválidos el formulario muestra errores y **no** guarda.
- Con datos válidos el lote aparece en la lista y **persiste** al cerrar y volver a abrir la app
  (los datos viven en Supabase, no en memoria).
