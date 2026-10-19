-- =====================================================================
-- AgroControl · Persistencia del chat del Asesor IA (tabla mensajes_chat)
-- Ejecutar completo en Supabase → SQL Editor. Es idempotente: se puede
-- correr varias veces sin romper nada.
--
-- Problema que resuelve:
--   El historial del chat vive solo en memoria en la app y se pierde al
--   cambiar de pestaña o reiniciar. La app guarda aquí cada mensaje
--   (usuario e IA) y los recarga al abrir la pestaña Asesor. Esta tabla
--   es NUEVA: sin ejecutar este script el chat funciona, pero no
--   sobrevive a los cambios de pestaña ni a un reinicio.
--
-- ADVERTENCIA MVP: las políticas permiten a cualquiera con la clave
-- pública leer/crear/borrar; la app compensa filtrando siempre por
-- usuario_id=eq.<id de la sesión>. Al migrar a Supabase Auth, reemplazar
-- por la política "to authenticated" usando auth.uid().
-- =====================================================================

create table if not exists public.mensajes_chat (
  id uuid primary key default gen_random_uuid(),
  usuario_id uuid not null,
  texto text not null,
  de_usuario boolean not null default true,
  created_at timestamptz not null default now()
);

create index if not exists mensajes_chat_usuario_fecha_idx
  on public.mensajes_chat (usuario_id, created_at);

grant usage on schema public to anon, authenticated;
grant select, insert, delete on table public.mensajes_chat to anon, authenticated;
grant update on table public.mensajes_chat to anon, authenticated;

alter table public.mensajes_chat enable row level security;

drop policy if exists "mensajes_chat_lectura_mvp" on public.mensajes_chat;
create policy "mensajes_chat_lectura_mvp" on public.mensajes_chat
  for select to anon, authenticated using (true);

drop policy if exists "mensajes_chat_escritura_mvp" on public.mensajes_chat;
create policy "mensajes_chat_escritura_mvp" on public.mensajes_chat
  for insert to anon, authenticated with check (true);

drop policy if exists "mensajes_chat_eliminacion_mvp" on public.mensajes_chat;
create policy "mensajes_chat_eliminacion_mvp" on public.mensajes_chat
  for delete to anon, authenticated using (true);

-- Comprobación: grants de la tabla y políticas activas.
select grantee, privilege_type from information_schema.role_table_grants
 where table_schema = 'public' and table_name = 'mensajes_chat'
 order by grantee, privilege_type;

select policyname, roles, cmd from pg_policies
 where schemaname = 'public' and tablename = 'mensajes_chat';
