# Edge Functions de AgroControl

Funciones serverless del proyecto Supabase `pyhvhbenqgmgztbkmrgh`.

| Función | Qué hace |
|---|---|
| `asesor-ia` | Proxy seguro hacia **Gemini** para el chat de la pestaña Asesor. Recibe `{ mensaje, historial[], usuario_id }` y devuelve `{ texto }`. La clave del proveedor vive como **secreto de Supabase**, nunca en el APK. |

## Requisitos

```bash
npm install -g supabase   # o el instalador oficial
supabase login            # pega el access token de tu cuenta
supabase link --project-ref pyhvhbenqgmgztbkmrgh
```

> `supabase link` pedirá la contraseña de la base de datos del proyecto.

## Secretos

`SUPABASE_URL` y `SUPABASE_ANON_KEY` los inyecta Supabase automáticamente: no se
configuran a mano. Solo hacen falta los del proveedor:

```bash
supabase secrets set GEMINI_API_KEY=tu_clave_de_google_ai_studio
supabase secrets set GEMINI_MODEL=gemini-3.5-flash-lite   # ID EXACTO del modelo
```

- `GEMINI_MODEL` es opcional: si no se define, la función usa
  `gemini-3.5-flash-lite` (valor por defecto en `asesor-ia/index.ts`).
- Comprueba el ID exacto del modelo con:
  ```bash
  curl -H "x-goog-api-key: $GEMINI_API_KEY" \
       https://generativelanguage.googleapis.com/v1beta/models
  ```
  Un ID inexistente hace que Gemini responda 400 y la función devuelva
  "El modelo del asesor no está disponible".

## Desplegar

```bash
supabase functions deploy asesor-ia
```

Actualiza los secretos y vuelve a desplegar es suficiente para cambiar de clave o
de modelo (no hay que tocar la app).

## Probar con curl

Sustituye `ANON_KEY` por la clave **anon (JWT)** pública del proyecto
(`SupabaseConfig.SUPABASE_ANON_KEY`). El MVP no emite JWT de usuario, por eso
`apikey` y `Authorization` llevan la misma clave anon.

```bash
curl -i -X POST \
  "https://pyhvhbenqgmgztbkmrgh.supabase.co/functions/v1/asesor-ia" \
  -H "apikey: ANON_KEY" \
  -H "Authorization: Bearer ANON_KEY" \
  -H "Content-Type: application/json" \
  -d '{"mensaje":"¿Cómo controlo la broca en mi café?","usuario_id":"","historial":[]}'
```

Respuestas esperadas:

| Código | Cuerpo | Significado |
|---|---|---|
| 200 | `{"texto":"- ..."}` | Respuesta del asesor |
| 400 | `{"error":"Falta el mensaje."}` | Petición sin `mensaje` |
| 429 | `{"error":"Demasiadas consultas seguidas..."}` | Cuota del proveedor agotada |
| 500 | `{"error":"Falta la clave del proveedor."}` | Falta `GEMINI_API_KEY` |
| 502 | `{"error":"El asesor no está disponible."}` | Gemini caído o modelo inválido |

También sirve `supabase functions serve asesor-ia` para probarla en local con el
archivo `.env` de la carpeta.
