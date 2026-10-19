# Plan: Asistente de IA real para AgroControl

> Estado actual (oct 2026): el chat de la pestaña Asesor funciona con el **asesor IA real** — la Edge Function `asesor-ia` (Supabase) llama a **Gemini** con la clave guardada como *secreto*, nunca en el APK. El **historial de la conversación se guarda en Supabase** (tabla `public.mensajes_chat`), de modo que sobrevive al cambio de pestaña y al reinicio de la app.

## 1. Arquitectura objetivo

Nunca una clave de API dentro de la app (regla del proyecto). Opciones evaluadas:

| Opción | Seguridad | Coste/infra | Veredicto |
|---|---|---|---|
| **A. Supabase Edge Function** (app → backend serverless → LLM) | Clave del proveedor en *secrets* de Supabase; reutiliza el JWT de la sesión | Ya existe Supabase; sin servidores nuevos | **Recomendada** |
| B. Llamada directa desde la app a OpenAI/Gemini | Clave expuesta en el APK (extraíble) | Nada de infra | Descartada |
| C. Backend propio (Node/Flask) | Segura | Infra nueva que mantener | Sobredimensionada |

**Flujo:** app (`AsesorIAApi`) → `POST .../functions/v1/asesor-ia` con el Bearer JWT de la sesión → Edge Function valida el JWT → llama al LLM con su secreto → devuelve `{ texto }` → `ResultadoIA` → burbuja del chat.

Punto clave: `ChatViewModel` solo hace `private val asesor: AsesorIA = AsesorIAMock`; el cambio funcional es una línea + la nueva implementación.

## 2. Fases

### Fase 1 — Edge Function `asesor-ia` (1–2 días)
- `supabase/functions/asesor-ia/index.ts` (Deno/TS):
  - Valida el JWT (`Authorization`/`apikey`) vía `supabase.auth.getUser`; rechaza inválidos (401).
  - Recibe `{ mensaje, historial[] }`; construye el prompt con **prompt de sistema**: asesor agronómico de AgroControl, español, consejos prácticos sobre café/plátano/aguacate en viñetas, advertencias de seguridad (dosis según etiqueta, consultar agrónomo), no inventa datos.
  - Llama al proveedor con `temperature` moderada y `max_tokens` acotado.
  - Devuelve `{ texto }` o error tipado (429 rate-limit, 5xx proveedor caído).
- `supabase secrets set OPENAI_API_KEY=...` (o del proveedor).
- Prueba con `curl` antes de tocar la app.

### Fase 2 — `AsesorIAApi` en la app (0.5–1 día)
- `data/AsesorIAApi.kt` implementa `AsesorIA` con `HttpURLConnection` (mismo patrón que `LoteRepository`, sin dependencias nuevas): POST a la function con `Authorization: Bearer <sesión>` + `apikey`.
- Mapeo de errores a `ResultadoIA(exito=false, error=...)`: 401/403 → "sesión expirada", 429 → "consultas demasiado frecuentes", 5xx/red → "servicio no disponible".
- `ChatViewModel`: `AsesorIAMock` → `AsesorIAApi`; el mock queda inyectado por constructor para tests unitarios.
- Tests: ViewModel con doble de `AsesorIA` que falle/retrase → verifica estados ENVIANDO/ERROR/`reintentarUltima`.

### Fase 3 — Conversación con contexto (1 día)
- Enviar las últimas 6–10 burbujas como `historial` (continuidad de conversación).
- Tope de longitud en cliente, indicador visual mientras responde (estado `ENVIANDO`), reintento desde la burbuja de error (ya existe `reintentarUltima`).

### Fase 4 — Contexto de los datos del usuario (2–3 días, opcional)
- La Edge Function lee los **lotes** del usuario (`select cultivo, nombre, hectareas where usuario_id = ...`) y los inyecta en el prompt → respuestas personalizadas por cultivo.
- Streaming de la respuesta (SSE) para que el texto aparezca escribiéndose.
- Rate limit por usuario (p. ej. 20 consultas/día).

## 3. Seguridad y costes
- Sin claves en el cliente: secreto solo en Supabase (Edge Function).
- Rate limit + JWT: nadie consume tu cuota anónimamente.
- Coste: con un modelo pequeño (p. ej. GPT-4o-mini ≈ $0.15/M tokens de entrada) una consulta típica (<$0.001) → cientos de consultas por centavo; con cuota diaria el gasto es predecible. Alternativa con tier gratuito: Google Gemini.
- Fallo del proveedor → degradación elegante: la app ya muestra error + reintento (y se puede volver al mock como fallback).

## 4. Criterios de éxito
1. Dado un usuario logueado, cuando escribe "mi café tiene manchas amarillas", entonces recibe respuesta técnica real en <5 s (no el mock).
2. Sin sesión válida → 401 de la function y la app muestra "sesión expirada" (no "error de red").
3. Ningún APK contiene una clave de proveedor.
4. 37+ tests en verde y el flujo de la pestaña Asesor intacto.

## 5. Estimación
- Fases 1–3: **~4–6 días** → asistente real funcional.
- Fase 4: mejora de valor (prioridad baja).

## 6. Estado de implementación (oct 2026)

Rama `feature/app-completa`. El chat de la pestaña Asesor ya habla con Gemini a
través de una Edge Function; la clave del proveedor **no** está en el APK.

### Implementado
| Fase | Estado | Dónde |
|---|---|---|
| 1. Edge Function `asesor-ia` (proxy al LLM, prompt de sistema, errores tipados 429/502) | ✅ | `supabase/functions/asesor-ia/index.ts` |
| 2. `AsesorIAApi` en la app (`HttpURLConnection`, mapeo 401/403/429/5xx/red a `ResultadoIA`) | ✅ | `app/src/main/java/com/agrocontrol/app/asesor/data/AsesorIAApi.kt` |
| 3. Conversación con contexto (últimas 8 burbujas como `historial`; estados ENVIANDO/ENVIADO/ERROR y reintento intactos) | ✅ | `ChatViewModel` + `AsesorIA.preguntar(mensaje, historial)` |
| 4. Contexto de lotes del usuario inyectado en el prompt | ✅ (en la función) | `contextoLotes()` en `asesor-ia/index.ts` (best-effort: si falla, responde igual) |

- `ChatViewModel(private val asesor: AsesorIA = AsesorIAApi)` permite inyectar
  `AsesorIAMock` o cualquier doble en los tests.
- Proveedor: **Gemini**, modelo configurable por el secreto `GEMINI_MODEL`
  (default en código: `gemini-3.5-flash-lite`).
- Tests: `AsesorIAErrorTest` cubre las ramas puras de
  `AsesorIAApi.interpretarRespuesta(...)` (401/403/429/500 y 200 con y sin texto).

### Pendiente (para activar en el entorno real)
1. **Desplegar** la función y fijar secretos (ver `supabase/functions/README.md`):
   ```bash
   supabase secrets set GEMINI_API_KEY=...      # clave del productor
   supabase secrets set GEMINI_MODEL=...        # ID EXACTO del modelo
   supabase functions deploy asesor-ia
   ```
2. **Verificar el ID exacto del modelo** antes de desplegar; si el ID no existe,
   la function devuelve 502 y la app muestra "El asesor no está disponible":
   ```bash
   curl -H "x-goog-api-key: CLAVE" https://generativelanguage.googleapis.com/v1beta/models
   ```
3. **Rate limit por usuario** (p. ej. 20 consultas/día) como igualmente siguiente
   paso: hoy cualquiera con la anon key (pública) puede consumir la cuota.
4. Fallback opcional al `AsesorIAMock` si el proveedor está caído.

## 7. Persistencia del chat (oct 2026)

Problema: `ChatViewModel` vivía solo en memoria y el fragment lo obtenía con
`new ViewModelProvider(this)` (ámbito de fragment), así que la conversación se
perdía al cambiar de pestaña o reiniciar la app. Ahora el historial se guarda en
**Supabase**, igual que los lotes y los movimientos.

| Pieza | Dónde | Qué hace |
|---|---|---|
| Tabla `public.mensajes_chat` | Supabase (`id`, `usuario_id`, `texto`, `de_usuario`, `created_at`) | Fuente de verdad del historial: guarda cada burbuja (usuario e IA) |
| Script `supabase/mensajes_chat_permisos.sql` | `supabase/` | Crea la tabla, el índice y los permisos/RLS MVP. **Debe ejecutarse en Supabase → SQL Editor** (es idempotente; sin él la tabla no existe y el chat solo dura en memoria) |
| `MensajesChatRepository` / `MensajesChatSupabase` | `app/src/main/java/com/agrocontrol/app/asesor/data/MensajesChatRepository.kt` | `cargar(50)`, `guardar(mensaje)` y `borrarTodo()` con `HttpURLConnection`; nunca lanza y sin sesión no toca la red |
| `ChatViewModel` | `app/src/main/java/com/agrocontrol/app/asesor/ChatViewModel.kt` | Recarga el historial al arrancar, persiste cada mensaje (las burbujas de ERROR no) y expone `limpiarConversacion()` |

Además, `AsesorFragment` obtiene el ViewModel con `new ViewModelProvider(requireActivity())`:
el chat tiene **ámbito de actividad**, por lo que la conversación sobrevive al
cambio de pestaña y a la recreación de la vista. La UI añade un botón
**Limpiar** (visible solo cuando hay mensajes) que pide confirmación antes de
borrar la conversación en memoria y en Supabase.
