package com.example.agentes.data;

import android.os.Handler;
import android.os.Looper;

import com.example.agentes.SupabaseClient;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Acceso a la API de Supabase Auth (GoTrue) con OkHttp, sin dependencias extra.
 *
 * <p>Endpoints usados:</p>
 * <ul>
 *   <li>{@code POST /auth/v1/token?grant_type=password} → inicio de sesión.</li>
 *   <li>{@code POST /auth/v1/recover} → enlace de recuperación de contraseña.</li>
 * </ul>
 *
 * <p>Todos los callbacks llegan en el hilo principal.</p>
 */
public class AuthRepository {

    /** Tipos de error entregados por {@link CallbackSesion#onError}. */
    public static final int ERROR_CREDENCIALES = 1;
    public static final int ERROR_RED = 2;
    public static final int ERROR_SERVIDOR = 3;

    public interface CallbackSesion {
        void onSesionIniciada(String accessToken, String refreshToken, String usuarioId);

        void onError(int tipoError, String detalle);
    }

    public interface CallbackEnvio {
        void onEnviado();

        void onError(int tipoError, String detalle);
    }

    private final Handler hiloPrincipal = new Handler(Looper.getMainLooper());

    /** Intenta iniciar sesión con correo y contraseña. */
    public void iniciarSesion(String correo, String contrasena, CallbackSesion callback) {
        JSONObject cuerpo = new JSONObject();
        try {
            cuerpo.put("email", correo);
            cuerpo.put("password", contrasena);
        } catch (JSONException e) {
            notificarErrorSesion(callback, ERROR_SERVIDOR, e.getMessage());
            return;
        }

        Request request = new Request.Builder()
                .url(SupabaseClient.getSupabaseUrl() + "/auth/v1/token?grant_type=password")
                .addHeader("apikey", SupabaseClient.getAnonKey())
                .addHeader("Content-Type", "application/json")
                .post(RequestBody.create(cuerpo.toString(), SupabaseClient.JSON))
                .build();

        SupabaseClient.getClient().newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                notificarErrorSesion(callback, ERROR_RED, e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) {
                try (Response r = response) {
                    String texto = r.body() != null ? r.body().string() : "";
                    if (r.isSuccessful()) {
                        try {
                            JSONObject json = new JSONObject(texto);
                            String accessToken = json.optString("access_token");
                            String refreshToken = json.optString("refresh_token");
                            JSONObject usuario = json.optJSONObject("user");
                            String usuarioId = usuario != null ? usuario.optString("id") : "";
                            if (accessToken.isEmpty() || usuarioId.isEmpty()) {
                                notificarErrorSesion(callback, ERROR_SERVIDOR,
                                        "Respuesta incompleta de Supabase");
                                return;
                            }
                            hiloPrincipal.post(() ->
                                    callback.onSesionIniciada(accessToken, refreshToken, usuarioId));
                        } catch (JSONException e) {
                            notificarErrorSesion(callback, ERROR_SERVIDOR, e.getMessage());
                        }
                    } else {
                        // 400/401: credenciales inválidas (o política de contraseña);
                        // el resto son fallos del servidor.
                        int tipo = (r.code() == 400 || r.code() == 401)
                                ? ERROR_CREDENCIALES : ERROR_SERVIDOR;
                        notificarErrorSesion(callback, tipo, "HTTP " + r.code() + ": " + texto);
                    }
                } catch (IOException e) {
                    notificarErrorSesion(callback, ERROR_RED, e.getMessage());
                }
            }
        });
    }

    /**
     * Solicita el correo de recuperación de contraseña. Supabase responde 200
     * aunque el correo no exista (por privacidad), así que siempre es éxito.
     */
    public void recuperarContrasena(String correo, CallbackEnvio callback) {
        JSONObject cuerpo = new JSONObject();
        try {
            cuerpo.put("email", correo);
        } catch (JSONException e) {
            notificarErrorEnvio(callback, ERROR_SERVIDOR, e.getMessage());
            return;
        }

        Request request = new Request.Builder()
                .url(SupabaseClient.getSupabaseUrl() + "/auth/v1/recover")
                .addHeader("apikey", SupabaseClient.getAnonKey())
                .addHeader("Content-Type", "application/json")
                .post(RequestBody.create(cuerpo.toString(), SupabaseClient.JSON))
                .build();

        SupabaseClient.getClient().newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                notificarErrorEnvio(callback, ERROR_RED, e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) {
                try (Response r = response) {
                    if (r.isSuccessful()) {
                        hiloPrincipal.post(callback::onEnviado);
                    } else {
                        String texto = r.body() != null ? r.body().string() : "";
                        notificarErrorEnvio(callback, ERROR_SERVIDOR, "HTTP " + r.code() + ": " + texto);
                    }
                } catch (IOException e) {
                    notificarErrorEnvio(callback, ERROR_RED, e.getMessage());
                }
            }
        });
    }

    private void notificarErrorSesion(CallbackSesion callback, int tipo, String detalle) {
        hiloPrincipal.post(() -> callback.onError(tipo, detalle));
    }

    private void notificarErrorEnvio(CallbackEnvio callback, int tipo, String detalle) {
        hiloPrincipal.post(() -> callback.onError(tipo, detalle));
    }
}
