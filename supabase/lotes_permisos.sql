-- =====================================================================
-- AgroControl · Permisos de la tabla lotes (rol anon/authenticated)
-- Ejecutar completo en Supabase → SQL Editor. Es idempotente: se puede
-- correr varias veces sin romper nada.
--
-- Problema que resuelve:
--   Con la clave anon, PostgREST responde 401
--   {"code":"42501","message":"permission denied for table lotes"}
--   en GET/POST/PATCH/DELETE sobre /rest/v1/lotes, porque la tabla no
--   tenía GRANT para los roles anon/authenticated (sí lo tenía usuarios).
--   La app traducía ese 401 como "no hay sesión", que es engañoso.
--   Este script cubre las 4 operaciones del módulo: listar, crear,
--   editar y eliminar lotes.
--
-- ADVERTENCIA MVP: las políticas de abajo permiten a cualquiera con la
-- clave pública leer, crear, editar y borrar filas en lotes (sin
-- distinguir usuarios). Se compensa porque la app añade siempre el
-- filtro usuario_id=eq.<id> en cada query (SELECT/PATCH/DELETE),
-- además de que el PATCH nunca reasigna usuario_id. Cuando la app
-- migre al JWT de Supabase Auth, reemplazar las políticas por la
-- versión "to authenticated" comentada en la sección 4.
-- =====================================================================

-- 1) Verificar que la tabla public.lotes existe antes de tocarla.
do $$
begin
  if to_regclass('public.lotes') is null then
    raise exception 'No existe la tabla public.lotes. Ejecute primero el script que la crea (contrato en docs/contrato-lotes.md) y luego este.';
  end if;
end $$;

-- 2) Permisos de esquema y de tabla para los roles públicos de la app.
--    La app MVP autentica sin JWT, así que las peticiones van como rol anon.
grant usage on schema public to anon, authenticated;

--    Lo que necesita la app hoy: listar, crear, editar y eliminar lotes.
grant select, insert on table public.lotes to anon, authenticated;
grant update, delete on table public.lotes to anon, authenticated;

-- 3) Habilitar Row Level Security.
alter table public.lotes enable row level security;

-- 4) Políticas MVP (mismo estilo que las de public.usuarios).
--    ADVERTENCIA: con la clave pública cualquiera puede leer, crear,
--    editar y borrar lotes; la app compensa filtrando por usuario_id
--    en cada query (?usuario_id=eq.<id>), de modo que cada sesión solo
--    opera sobre los suyos.
drop policy if exists "lotes_lectura_mvp" on public.lotes;
create policy "lotes_lectura_mvp"
  on public.lotes
  for select
  to anon, authenticated
  using (true);

drop policy if exists "lotes_escritura_mvp" on public.lotes;
create policy "lotes_escritura_mvp"
  on public.lotes
  for insert
  to anon, authenticated
  with check (true);

drop policy if exists "lotes_actualizacion_mvp" on public.lotes;
create policy "lotes_actualizacion_mvp"
  on public.lotes
  for update
  to anon, authenticated
  using (true)
  with check (true);

drop policy if exists "lotes_eliminacion_mvp" on public.lotes;
create policy "lotes_eliminacion_mvp"
  on public.lotes
  for delete
  to anon, authenticated
  using (true);

--    Cuando la app migre al JWT de Supabase Auth (ya no se llama a la API
--    con la clave anon sola), reemplazar las cuatro políticas anteriores por:
--
-- drop policy if exists "lotes_por_usuario" on public.lotes;
-- create policy "lotes_por_usuario"
--   on public.lotes
--   for all
--   to authenticated
--   using (usuario_id = auth.uid())
--   with check (usuario_id = auth.uid());

-- 5) Comprobación: debe listar los grants de la tabla y las cuatro políticas.
select grantee, privilege_type
from information_schema.role_table_grants
where table_schema = 'public' and table_name = 'lotes'
order by grantee, privilege_type;

select policyname, roles, cmd
from pg_policies
where schemaname = 'public' and tablename = 'lotes';
