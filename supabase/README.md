# Supabase de AgroControl

Proyecto Supabase: `pyhvhbenqgmgztbkmrgh` (`https://pyhvhbenqgmgztbkmrgh.supabase.co`).

## Scripts SQL (ejecutar en Supabase → SQL Editor)

Todos son **idempotentes** (se pueden correr varias veces sin romper nada). El
orden importa: los permisos asumen que la tabla ya existe.

| # | Script | Qué hace | Estado |
|---|---|---|---|
| 1 | [`lotes_permisos.sql`](lotes_permisos.sql) | Permisos y RLS (MVP) de `public.lotes` | Ya ejecutado |
| 2 | [`movimientos_permisos.sql`](movimientos_permisos.sql) | Permisos y RLS (MVP) de `public.movimientos` y `categorias` | Ya ejecutado |
| 3 | [`mensajes_chat_permisos.sql`](mensajes_chat_permisos.sql) | Crea la tabla `public.mensajes_chat` (historial del chat del Asesor IA), su índice y los permisos/RLS (MVP) | **NUEVO** |

> **`mensajes_chat_permisos.sql` es NUEVO y obligatorio para el chat:** sin él la
> tabla no existe, la app no puede guardar el historial y la conversación de la
> pestaña Asesor se pierde al cambiar de pestaña o reiniciar.

Además, el login necesita [`registro_usuarios.sql`](registro_usuarios.sql)
(tabla `public.usuarios` y su trigger): es requisito previo de la app, no de los
permisos de arriba.

Las Edge Functions (p. ej. `asesor-ia`, el proxy a Gemini) se documentan en
[`functions/README.md`](functions/README.md).
