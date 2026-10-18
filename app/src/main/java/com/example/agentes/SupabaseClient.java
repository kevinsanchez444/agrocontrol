package com.example.agentes;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;

public class SupabaseClient {

    private static final String SUPABASE_URL = "https://pyhvhbenqgmgztbkmrgh.supabase.co";
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
}