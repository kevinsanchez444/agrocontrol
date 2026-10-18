package com.example.agentes;

import com.example.agentes.data.SupabaseConfig;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;

public class SupabaseClient {

    /**
     * URL del proyecto de Supabase. Se declara una sola vez en {@link SupabaseConfig}
     * para que la autenticación (esta clase) y el módulo de lotes
     * ({@code LoteRepository}) apunten siempre al mismo proyecto.
     */
    private static final String SUPABASE_URL = SupabaseConfig.SUPABASE_URL;

    /**
     * Clave <b>publicable</b> del proyecto ({@code sb_publishable_...}). La usan
     * <b>solo los flujos de autenticación</b> ({@code AuthRepository}: login y
     * registro). NO es un JWT: viaja únicamente en el header {@code apikey};
     * enviarla como {@code Authorization: Bearer} provoca 401, por eso los
     * métodos de esta clase solo agregan Bearer si la clave empieza con "eyJ".
     *
     * <p>El módulo de lotes NO usa esta clave: {@code LoteRepository} trabaja con
     * la clave anon JWT de {@link SupabaseConfig} y el token de sesión del
     * usuario autenticado (fallback a la anon key cuando no hay sesión).</p>
     */
    private static final String SUPABASE_ANON_KEY = "sb_publishable_kRepyqvM-2_pF4xeEHhVgg_7GA_lDuB";

    private static final OkHttpClient client = new OkHttpClient();
    public static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    // Método para consultar datos de una tabla
    public static Request crearPeticionObtener(String nombreTabla) {
        String url = SUPABASE_URL + "/rest/v1/" + nombreTabla + "?select=*";

        Request.Builder builder = new Request.Builder()
                .url(url)
                .addHeader("apikey", SUPABASE_ANON_KEY)
                .addHeader("Accept", "application/json")
                .get();

        // Las claves nuevas "sb_publishable_..." NO son JWT: van solo en el header "apikey".
        // Enviarlas como "Authorization: Bearer" provoca 401. Solo las claves legacy (eyJ...) lo llevan.
        if (SUPABASE_ANON_KEY.startsWith("eyJ")) {
            builder.addHeader("Authorization", "Bearer " + SUPABASE_ANON_KEY);
        }

        return builder.build();
    }

    // Método para insertar datos en una tabla
    public static Request crearPeticionInsertar(String nombreTabla, String jsonDatos) {
        String url = SUPABASE_URL + "/rest/v1/" + nombreTabla;
        RequestBody body = RequestBody.create(jsonDatos, JSON);

        Request.Builder builder = new Request.Builder()
                .url(url)
                .addHeader("apikey", SUPABASE_ANON_KEY)
                .addHeader("Content-Type", "application/json")
                .addHeader("Prefer", "return=representation")
                .post(body);

        if (SUPABASE_ANON_KEY.startsWith("eyJ")) {
            builder.addHeader("Authorization", "Bearer " + SUPABASE_ANON_KEY);
        }

        return builder.build();
    }

    public static OkHttpClient getClient() {
        return client;
    }

    // Getters para el módulo de autenticación (AuthRepository / SupabaseConfig)
    public static String getSupabaseUrl() {
        return SUPABASE_URL;
    }

    public static String getAnonKey() {
        return SUPABASE_ANON_KEY;
    }
}