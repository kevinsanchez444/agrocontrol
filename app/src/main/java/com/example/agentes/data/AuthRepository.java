package com.example.agentes.data;

import android.net.Uri;
import android.os.Handler;
import android.os.Looper;

import com.example.agentes.SupabaseClient;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.UUID;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Acceso a la tabla {@code usuarios} de Supabase (PostgREST) para login y
 * registro.
 *
 * <p>Las credenciales del MVP son <b>nombre + correo</b>: la tabla no tiene
 * columna de contraseña.</p>
 *
 * <ul>
 *   <li>Inicio de sesión → {@code GET /rest/v1/usuarios?email=eq.…&select=id,nombre}.</li>
 *   <li>Registro → {@code POST /rest/v1/usuarios} con id generado en el cliente.</li>
 * </ul>
 *
 * <p>Todos los callbacks llegan en el hilo principal.</p>
 */
public class AuthRepository {

    /** Tipos de error entregados por los callbacks. */
    public static final int ERROR_NO_REGISTRADO = 1;
    public static final int ERROR_PERMISOS = 2;
    public static final int ERROR_RED = 3;
    public static final int ERROR_SERVIDOR = 4;
    public static final int ERROR_YA_REGISTRADO = 5;

    /** Resultado de la consulta de una cuenta existente. */
    public interface CallbackConsulta {
        void onResultado(String usuarioId, String nombreRegistrado);

        void onError(int tipoError, String detalle);
    }

    public interface CallbackRegistro {
        void onRegistrado(String usuarioId);

        void onError(int tipoError, String detalle);
    }

    private final Handler hiloPrincipal = new Handler(Looper.getMainLooper());

    /** Busca la cuenta por correo; el comparador de nombres lo hace el llamador. */
    public void iniciarSesion(String correo, CallbackConsulta callback) {
        String url = SupabaseClient.getSupabaseUrl() + "/rest/v1/usuarios?email=eq."
                + Uri.encode(correo) + "&select=id,nombre";

        Request request = new Request.Builder()
                .url(url)
                .addHeader("apikey", SupabaseClient.getAnonKey())
                .addHeader("Accept", "application/json")
                .get()
                .build();

        SupabaseClient.getClient().newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                notificar(callback, ERROR_RED, e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) {
                try (Response r = response) {
                    String texto = r.body() != null ? r.body().string() : "";
                    if (!r.isSuccessful()) {
                        notificar(callback, tipoDeError(r.code()), "HTTP " + r.code() + ": " + texto);
                        return;
                    }
                    try {
                        JSONArray filas = new JSONArray(texto);
                        if (filas.length() == 0) {
                            notificar(callback, ERROR_NO_REGISTRADO, "Sin cuenta para " + correo);
                            return;
                        }
                        JSONObject fila = filas.getJSONObject(0);
                        String id = fila.optString("id");
                        String nombre = fila.optString("nombre");
                        hiloPrincipal.post(() -> callback.onResultado(id, nombre));
                    } catch (JSONException e) {
                        notificar(callback, ERROR_SERVIDOR, e.getMessage());
                    }
                } catch (IOException e) {
                    notificar(callback, ERROR_RED, e.getMessage());
                }
            }
        });
    }

    /**
     * Crea la cuenta en la tabla {@code usuarios}. El {@code id} se genera en
     * el cliente porque el default de la columna es {@code auth.uid()}, que es
     * {@code null} sin sesión de Supabase Auth.
     */
    public void registrarse(String nombre, String correo, CallbackRegistro callback) {
        String id = UUID.randomUUID().toString();

        JSONObject cuerpo = new JSONObject();
        try {
            cuerpo.put("id", id);
            cuerpo.put("email", correo);
            cuerpo.put("nombre", nombre);
        } catch (JSONException e) {
            notificar(callback, ERROR_SERVIDOR, e.getMessage());
            return;
        }

        Request request = new Request.Builder()
                .url(SupabaseClient.getSupabaseUrl() + "/rest/v1/usuarios")
                .addHeader("apikey", SupabaseClient.getAnonKey())
                .addHeader("Content-Type", "application/json")
                .addHeader("Prefer", "return=representation")
                .post(RequestBody.create(cuerpo.toString(), SupabaseClient.JSON))
                .build();

        SupabaseClient.getClient().newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                notificar(callback, ERROR_RED, e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) {
                try (Response r = response) {
                    String texto = r.body() != null ? r.body().string() : "";
                    if (r.isSuccessful()) {
                        // 201 con la fila creada; se conserva el id generado localmente.
                        hiloPrincipal.post(() -> callback.onRegistrado(id));
                        return;
                    }
                    // Correo duplicado (unique de la tabla usuarios).
                    int tipo = (r.code() == 409 || texto.contains("23505"))
                            ? ERROR_YA_REGISTRADO : tipoDeError(r.code());
                    notificar(callback, tipo, "HTTP " + r.code() + ": " + texto);
                } catch (IOException e) {
                    notificar(callback, ERROR_RED, e.getMessage());
                }
            }
        });
    }

    private static int tipoDeError(int codigoHttp) {
        if (codigoHttp == 401 || codigoHttp == 403) {
            return ERROR_PERMISOS;
        }
        return ERROR_SERVIDOR;
    }

    private void notificar(CallbackConsulta callback, int tipo, String detalle) {
        hiloPrincipal.post(() -> callback.onError(tipo, detalle));
    }

    private void notificar(CallbackRegistro callback, int tipo, String detalle) {
        hiloPrincipal.post(() -> callback.onError(tipo, detalle));
    }
}
