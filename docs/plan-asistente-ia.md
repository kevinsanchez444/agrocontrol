# Plan: Asistente de IA real para AgroControl

> Estado actual (oct 2026): el chat de la pestaña Asesor funciona con **`AsesorIAMock`** — respuestas predeterminadas por palabras clave (café/plátano/aguacate), sin llamadas a ningún servicio. La interfaz `AsesorIA` ya existe y está lista para enchufar una implementación real sin tocar la UI.

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
