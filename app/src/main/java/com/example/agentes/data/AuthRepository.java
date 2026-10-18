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
 *   <li>Registro → {@code POST /auth/v1/signup} (Supabase Auth + trigger que
 *       replica la fila a {@code public.usuarios}).</li>
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
    public static final int ERROR_REGISTRO_DESHABILITADO = 6;
    public static final int ERROR_LIMITE_INTENTOS = 7;

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
     * Crea la cuenta con Supabase Auth ({@code POST /auth/v1/signup}).
     *
     * <p>La tabla {@code usuarios} tiene clave foránea a {@code auth.users},
     * y un trigger de Supabase copia automáticamente el id, el email y el
     * nombre (enviado en {@code data}) a {@code public.usuarios}: por eso el
     * registro solo necesita llamarse a signup.</p>
     *
     * <p>La contraseña generada es interna: el MVP usa nombre + correo como
     * credenciales y nunca se muestra ni se vuelve a usar.</p>
     */
    public void registrarse(String nombre, String correo, CallbackRegistro callback) {
        String contrasenaInterna = UUID.randomUUID().toString();

        JSONObject cuerpo = new JSONObject();
        try {
            cuerpo.put("email", correo);
            cuerpo.put("password", contrasenaInterna);
            cuerpo.put("data", new JSONObject().put("nombre", nombre));
        } catch (JSONException e) {
            notificar(callback, ERROR_SERVIDOR, e.getMessage());
            return;
        }

        Request request = new Request.Builder()
                .url(SupabaseClient.getSupabaseUrl() + "/auth/v1/signup")
                .addHeader("apikey", SupabaseClient.getAnonKey())
                .addHeader("Content-Type", "application/json")
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
                        try {
                            JSONObject json = new JSONObject(texto);
                            // Sin confirmación de correo viene "id"; con sesión viene "user".
                            JSONObject usuario = json.optJSONObject("user");
                            String id = usuario != null ? usuario.optString("id")
                                    : json.optString("id");
                            if (id.isEmpty()) {
                                notificar(callback, ERROR_SERVIDOR,
                                        "Respuesta incompleta de Supabase");
                                return;
                            }
                            hiloPrincipal.post(() -> callback.onRegistrado(id));
                        } catch (JSONException e) {
                            notificar(callback, ERROR_SERVIDOR, e.getMessage());
                        }
                        return;
                    }
                    notificar(callback, clasificarErrorRegistro(r.code(), texto),
                            "HTTP " + r.code() + ": " + texto);
                } catch (IOException e) {
                    notificar(callback, ERROR_RED, e.getMessage());
                }
            }
        });
    }

    /** Traduce los errores conocidos de GoTrue (signup) a códigos de la app. */
    private static int clasificarErrorRegistro(int codigoHttp, String cuerpo) {
        String texto = cuerpo == null ? "" : cuerpo.toLowerCase(java.util.Locale.ROOT);
        if (texto.contains("user_already_exists")
                || texto.contains("already registered")
                || texto.contains("already been registered")) {
            return ERROR_YA_REGISTRADO;
        }
        if (codigoHttp == 429 || texto.contains("rate limit")) {
            return ERROR_LIMITE_INTENTOS;
        }
        if (texto.contains("signups not allowed")
                || texto.contains("signup is disabled")
                || texto.contains("email logins not allowed")) {
            return ERROR_REGISTRO_DESHABILITADO;
        }
        if (codigoHttp == 401 || codigoHttp == 403) {
            return ERROR_PERMISOS;
        }
        return ERROR_SERVIDOR;
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
