-- =====================================================================
-- AgroControl · Registro de usuarios (nombre + correo)
-- Ejecutar completo en Supabase → SQL Editor. Es idempotente: se puede
-- correr varias veces sin romper nada.
--
-- Flujo de la app:
--   Registro → POST /auth/v1/signup  (crea la fila en auth.users)
--            → este trigger copia id, email y nombre a public.usuarios
--   Login    → GET /rest/v1/usuarios?email=eq.<correo>  (rol anon)
-- =====================================================================

-- 1) Tabla usuarios (según el diccionario de datos del README)
create table if not exists public.usuarios (
  id         uuid primary key references auth.users (id) on delete cascade,
  email      text not null unique,
  nombre     text not null,
  creado_en  timestamptz not null default now()
);

-- 2) Función del trigger.
--    SECURITY DEFINER: el trigger corre como el rol de Supabase Auth, que
--    NO tiene permiso de escritura sobre public.usuarios; sin esto, signup
--    responde 500 "Database error saving new user".
--    search_path = '': exigido por Supabase para funciones SECURITY DEFINER.
create or replace function public.crear_usuario_desde_auth()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
begin
  insert into public.usuarios (id, email, nombre)
  values (
    new.id,
    lower(new.email),
    coalesce(nullif(trim(new.raw_user_meta_data ->> 'nombre'), ''),
             split_part(new.email, '@', 1))   -- nombre nunca NULL
  )
  on conflict do nothing;
  return new;
end;
$$;

drop trigger if exists on_auth_user_created on auth.users;
create trigger on_auth_user_created
  after insert on auth.users
  for each row execute function public.crear_usuario_desde_auth();

-- 3) Usuarios que ya existen en auth.users pero no en public.usuarios
--    (p. ej. registros de pruebas anteriores al trigger)
insert into public.usuarios (id, email, nombre)
select u.id, lower(u.email),
       coalesce(nullif(trim(u.raw_user_meta_data ->> 'nombre'), ''), split_part(u.email, '@', 1))
from auth.users u
where u.email is not null
on conflict do nothing;

-- 4) Permisos para que el login (clave publicable = rol anon) pueda leer.
--    ADVERTENCIA MVP: esto permite a cualquiera con la clave pública leer
--    nombres y correos. Cuando la app use el JWT de Supabase Auth, cambie
--    la política a "to authenticated using (id = auth.uid())".
grant usage on schema public to anon, authenticated;
grant select on table public.usuarios to anon, authenticated;

alter table public.usuarios enable row level security;

drop policy if exists "usuarios_lectura_login" on public.usuarios;
create policy "usuarios_lectura_login"
  on public.usuarios
  for select
  to anon, authenticated
  using (true);

-- 5) Comprobación: debe listar el trigger y la política
select tgname from pg_trigger where tgname = 'on_auth_user_created';
select policyname from pg_policies where tablename = 'usuarios';
