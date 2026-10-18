package com.example.agentes.data;

import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.example.agentes.SupabaseClient;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Locale;
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
    public static final int ERROR_TRIGGER_USUARIOS = 8;
    public static final int ERROR_CONFIRMACION_CORREO = 9;
    public static final int ERROR_CORREO_RECHAZADO = 10;
    public static final int ERROR_CAPTCHA = 11;

    private static final String TAG = "AUTH";

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
    public void iniciarSesion(String correoEscrito, CallbackConsulta callback) {
        // Supabase Auth guarda el correo en minúsculas y "eq." es sensible a mayúsculas.
        String correo = normalizarCorreo(correoEscrito);
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
    public void registrarse(String nombre, String correoEscrito, CallbackRegistro callback) {
        String correo = normalizarCorreo(correoEscrito);
        // Cumple cualquier política de contraseñas de Supabase (minúsculas,
        // mayúsculas, dígitos y símbolos; > 8 caracteres).
        String contrasenaInterna = "Ag1!" + UUID.randomUUID();

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
                            JSONObject datosUsuario = usuario != null ? usuario : json;
                            // Con "Confirm email" activo, un correo ya registrado devuelve
                            // 200 con un usuario ficticio sin identidades.
                            JSONArray identidades = datosUsuario.optJSONArray("identities");
                            if (identidades != null && identidades.length() == 0) {
                                notificar(callback, ERROR_YA_REGISTRADO,
                                        "Correo ya registrado (identities vacío)");
                                return;
                            }
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
                    notificar(callback, ErrorRegistro.clasificar(r.code(), texto),
                            "HTTP " + r.code() + ": " + texto);
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

    static String normalizarCorreo(String correo) {
        return correo == null ? "" : correo.trim().toLowerCase(Locale.ROOT);
    }

    private void notificar(CallbackConsulta callback, int tipo, String detalle) {
        Log.e(TAG, "Login falló (tipo " + tipo + "): " + detalle);
        hiloPrincipal.post(() -> callback.onError(tipo, detalle));
    }

    private void notificar(CallbackRegistro callback, int tipo, String detalle) {
        Log.e(TAG, "Registro falló (tipo " + tipo + "): " + detalle);
        hiloPrincipal.post(() -> callback.onError(tipo, detalle));
    }
}
