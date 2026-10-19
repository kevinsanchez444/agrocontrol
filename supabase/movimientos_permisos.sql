-- =====================================================================
-- AgroControl · Permisos de la tabla movimientos (rol anon/authenticated)
-- Ejecutar completo en Supabase → SQL Editor. Es idempotente: se puede
-- correr varias veces sin romper nada.
--
-- Problema que resuelve:
--   La tabla public.movimientos existe, pero el rol anon no tiene GRANT
--   sobre ella y PostgREST responde 401
--   {"code":"42501","message":"permission denied for table movimientos"}
--   en GET/POST sobre /rest/v1/movimientos. La app (libro de caja de la
--   pestaña Finanzas) necesita listar e insertar movimientos.
--
-- Nota sobre el esquema real (difiere del README del repositorio):
--   movimientos(id uuid, lote_id uuid, usuario_id uuid, categoria_id uuid,
--   tipo text, monto numeric, descripcion text, fecha, created_at,
--   updated_at). NO existe la columna "creado_en": la app traduce
--   created_at a su campo interno creadoEn.
--
-- ADVERTENCIA MVP: las políticas de abajo permiten a cualquiera con la
-- clave pública leer, crear, editar y borrar filas en movimientos (sin
-- distinguir usuarios). Se compensa porque la app añade siempre el
-- filtro usuario_id=eq.<id> en cada query (SELECT) y escribe siempre
-- usuario_id = <id de la sesión> en cada INSERT. Cuando la app migre al
-- JWT de Supabase Auth, reemplazar las políticas por la versión
-- "to authenticated" comentada en la sección 3.
-- =====================================================================

-- 1) Verificar que las tablas existen antes de tocarlas.
do $$
begin
  if to_regclass('public.movimientos') is null then
    raise exception 'No existe la tabla public.movimientos. Ejecute primero el script que la crea y luego este.';
  end if;
  if to_regclass('public.categorias') is null then
    raise exception 'No existe la tabla public.categorias. El libro de caja necesita el catálogo de categorías.';
  end if;
end $$;

-- 2) Permisos de esquema y de tabla para los roles públicos de la app.
--    La app MVP autentica sin JWT, así que las peticiones van como rol anon.
grant usage on schema public to anon, authenticated;

--    Lo que necesita la app hoy sobre movimientos: listar e insertar
--    (el módulo finanzas no edita ni borra todavía, pero se dejan las 4
--    operaciones para no repetir este script más adelante).
grant select, insert on table public.movimientos to anon, authenticated;
grant update, delete on table public.movimientos to anon, authenticated;

--    Categorías: solo lectura (el catálogo semilla ya existe; la app
--    mapea nombre → id). Idempotente: el grant ya funciona hoy, se deja
--    explícito para que el script documente el contrato completo.
grant select on table public.categorias to anon, authenticated;

-- 3) Habilitar Row Level Security.
alter table public.movimientos enable row level security;

-- 4) Políticas MVP (mismo patrón que las de public.lotes).
--    ADVERTENCIA: con la clave pública cualquiera puede leer, crear,
--    editar y borrar movimientos; la app compensa filtrando por
--    usuario_id en cada query (?usuario_id=eq.<id>) y escribiendo siempre
--    el usuario de la sesión en el INSERT.
drop policy if exists "movimientos_lectura_mvp" on public.movimientos;
create policy "movimientos_lectura_mvp"
  on public.movimientos
  for select
  to anon, authenticated
  using (true);

drop policy if exists "movimientos_escritura_mvp" on public.movimientos;
create policy "movimientos_escritura_mvp"
  on public.movimientos
  for insert
  to anon, authenticated
  with check (true);

drop policy if exists "movimientos_actualizacion_mvp" on public.movimientos;
create policy "movimientos_actualizacion_mvp"
  on public.movimientos
  for update
  to anon, authenticated
  using (true)
  with check (true);

drop policy if exists "movimientos_eliminacion_mvp" on public.movimientos;
create policy "movimientos_eliminacion_mvp"
  on public.movimientos
  for delete
  to anon, authenticated
  using (true);

--    Cuando la app migre al JWT de Supabase Auth (ya no se llama a la API
--    con la clave anon sola), reemplazar las cuatro políticas anteriores por:
--
-- drop policy if exists "movimientos_por_usuario" on public.movimientos;
-- create policy "movimientos_por_usuario"
--   on public.movimientos
--   for all
--   to authenticated
--   using (usuario_id = auth.uid())
--   with check (usuario_id = auth.uid());

-- 5) lote_id admite NULL: el libro de caja GENERAL (pestaña Finanzas sin
--    lote seleccionado) registra movimientos sin lote, igual que los
--    ingresos/gastos globales. Idempotente: repetir el ALTER no hace nada.
alter table public.movimientos alter column lote_id drop not null;

-- 6) Comprobación: grants de movimientos y categorías + políticas.
select grantee, privilege_type
from information_schema.role_table_grants
where table_schema = 'public' and table_name = 'movimientos'
order by grantee, privilege_type;

select grantee, privilege_type
from information_schema.role_table_grants
where table_schema = 'public' and table_name = 'categorias'
order by grantee, privilege_type;

select policyname, roles, cmd
from pg_policies
where schemaname = 'public' and tablename = 'movimientos';
