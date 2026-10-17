package com.example.agentes.data;

import android.text.TextUtils;

/**
 * Punto único de configuración de Supabase para el módulo de lotes.
 *
 * <p><b>Dependencia con el Integrante 1:</b> la URL del proyecto y la clave pública
 * ({@code anon key}) las entrega el módulo de Supabase/Auth. Nunca se debe escribir
 * aquí la {@code service_role key} ni ninguna clave privada.</p>
 *
 * <p>La sesión (token {@code JWT} e identificador del usuario) la establece el módulo de
 * autenticación al iniciar sesión y al cerrarla. Las políticas RLS de Supabase usan ese
 * token para devolver únicamente los lotes del usuario autenticado.</p>
 */
public final class SupabaseConfig {

    // TODO(Integrante 1): reemplazar por los valores reales del proyecto de Supabase.
    public static final String SUPABASE_URL = "https://TU-PROYECTO.supabase.co";
    public static final String SUPABASE_ANON_KEY = "TU_ANON_KEY";

    private static volatile String accessToken;
    private static volatile String usuarioId;

    private SupabaseConfig() {
    }

    /** Registra la sesión activa. La llama el módulo de autenticación tras el login. */
    public static void setSesion(String token, String idUsuario) {
        accessToken = token;
        usuarioId = idUsuario;
    }

    /** Borra la sesión activa. La llama el módulo de autenticación al cerrar sesión. */
    public static void limpiarSesion() {
        accessToken = null;
        usuarioId = null;
    }

    /** Token con el que se firman las peticiones; si no hay sesión se usa la anon key. */
    public static String getAccessToken() {
        return TextUtils.isEmpty(accessToken) ? SUPABASE_ANON_KEY : accessToken;
    }

    /** Identificador del usuario autenticado, o {@code null} si no hay sesión. */
    public static String getUsuarioId() {
        return usuarioId;
    }

    /** Indica si ya se reemplazaron los valores de ejemplo por los reales. */
    public static boolean estaConfigurado() {
        return !TextUtils.isEmpty(SUPABASE_URL)
                && SUPABASE_URL.startsWith("http")
                && !SUPABASE_URL.contains("TU-PROYECTO")
                && !TextUtils.isEmpty(SUPABASE_ANON_KEY)
                && !"TU_ANON_KEY".equals(SUPABASE_ANON_KEY);
    }

    /** URL base de la API REST (PostgREST) de Supabase. */
    public static String getRestUrl() {
        String base = SUPABASE_URL;
        while (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        return base + "/rest/v1";
    }
}
