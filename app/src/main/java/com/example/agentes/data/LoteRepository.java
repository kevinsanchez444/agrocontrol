package com.example.agentes.data;

import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;

import com.example.agentes.model.Lote;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Acceso a la tabla {@code lotes} de Supabase mediante la API REST (PostgREST).
 *
 * <p><b>Contrato de la tabla</b> (la administra el Integrante 1):
 * {@code lotes(id uuid pk, usuario_id uuid default auth.uid() fk -> usuarios, nombre text,
 * cultivo text, hectareas numeric, activo boolean, created_at timestamptz,
 * updated_at timestamptz)}. El orden usa {@code created_at.desc}, que es el nombre real de la
 * columna en Supabase; no existe ninguna columna llamada {@code creado_en}.</p>
 *
 * <p>Las peticiones se ejecutan en un hilo de fondo y el resultado se entrega en el hilo
 * principal, para que los Fragment actualicen la interfaz sin manejar hilos a mano.</p>
 */
public class LoteRepository {

    /** Devuelve el resultado de una operación remota. */
    public interface Callback<T> {
        void onExito(T datos);

        void onError(String mensaje);
    }

    private static final String TAG = "LoteRepository";
    private static final String TABLA = "lotes";
    private static final String COLUMNAS = "id,usuario_id,nombre,cultivo,hectareas";
    private static final String ORDEN = "created_at.desc";

    private static final String MSG_SIN_CONFIGURAR =
            "Falta configurar la conexión con Supabase.";
    private static final String MSG_SIN_SESION =
            "No hay una sesión activa. Inicia sesión para gestionar tus lotes.";
    private static final String MSG_RED =
            "No se pudo conectar con el servidor. Revisa tu conexión e intenta de nuevo.";

    private final ExecutorService ejecutor = Executors.newSingleThreadExecutor();
    private final Handler principal = new Handler(Looper.getMainLooper());

    /** Lista los lotes del usuario autenticado (las políticas RLS filtran por usuario). */
    public void listarLotes(final Callback<List<Lote>> callback) {
        if (!SupabaseConfig.estaConfigurado()) {
            publicarError(callback, MSG_SIN_CONFIGURAR);
            return;
        }
        ejecutor.execute(new Runnable() {
            @Override
            public void run() {
                HttpURLConnection conexion = null;
                try {
                    String ruta = TABLA + "?select=" + COLUMNAS + "&order=" + ORDEN;
                    conexion = abrirConexion("GET", ruta);
                    int codigo = conexion.getResponseCode();
                    String cuerpo = leerRespuesta(codigo < 400 ? conexion.getInputStream() : conexion.getErrorStream());
                    if (codigo >= 400) {
                        publicarError(callback, mensajeDeError(codigo, cuerpo));
                        return;
                    }
                    JSONArray arreglo = new JSONArray(cuerpo);
                    List<Lote> lotes = new ArrayList<>();
                    for (int i = 0; i < arreglo.length(); i++) {
                        lotes.add(desdeJson(arreglo.getJSONObject(i)));
                    }
                    publicarExito(callback, lotes);
                } catch (Exception e) {
                    publicarError(callback, mensajeDeExcepcion(e));
                } finally {
                    if (conexion != null) {
                        conexion.disconnect();
                    }
                }
            }
        });
    }

    /** Inserta un lote nuevo a nombre del usuario autenticado. */
    public void crearLote(final Lote lote, final Callback<Lote> callback) {
        if (!SupabaseConfig.estaConfigurado()) {
            publicarError(callback, MSG_SIN_CONFIGURAR);
            return;
        }
        final String usuarioId = SupabaseConfig.getUsuarioId();
        if (TextUtils.isEmpty(usuarioId)) {
            publicarError(callback, MSG_SIN_SESION);
            return;
        }
        ejecutor.execute(new Runnable() {
            @Override
            public void run() {
                HttpURLConnection conexion = null;
                try {
                    JSONObject cuerpo = new JSONObject();
                    cuerpo.put("usuario_id", usuarioId);
                    cuerpo.put("nombre", lote.getNombre());
                    cuerpo.put("cultivo", lote.getCultivo());
                    cuerpo.put("hectareas", lote.getHectareas());

                    conexion = abrirConexion("POST", TABLA);
                    conexion.setDoOutput(true);
                    conexion.setRequestProperty("Prefer", "return=representation");

                    OutputStream salida = conexion.getOutputStream();
                    salida.write(cuerpo.toString().getBytes(StandardCharsets.UTF_8));
                    salida.flush();
                    salida.close();

                    int codigo = conexion.getResponseCode();
                    String respuesta = leerRespuesta(codigo < 400 ? conexion.getInputStream() : conexion.getErrorStream());
                    if (codigo >= 400) {
                        publicarError(callback, mensajeDeError(codigo, respuesta));
                        return;
                    }
                    JSONArray arreglo = new JSONArray(respuesta);
                    Lote creado = arreglo.length() > 0 ? desdeJson(arreglo.getJSONObject(0)) : lote;
                    publicarExito(callback, creado);
                } catch (Exception e) {
                    publicarError(callback, mensajeDeExcepcion(e));
                } finally {
                    if (conexion != null) {
                        conexion.disconnect();
                    }
                }
            }
        });
    }

    private HttpURLConnection abrirConexion(String metodo, String ruta) throws IOException {
        URL url = new URL(SupabaseConfig.getRestUrl() + "/" + ruta);
        HttpURLConnection conexion = (HttpURLConnection) url.openConnection();
        conexion.setRequestMethod(metodo);
        conexion.setRequestProperty("apikey", SupabaseConfig.SUPABASE_ANON_KEY);
        conexion.setRequestProperty("Authorization", "Bearer " + SupabaseConfig.getAccessToken());
        conexion.setRequestProperty("Content-Type", "application/json");
        conexion.setRequestProperty("Accept", "application/json");
        conexion.setConnectTimeout(15000);
        conexion.setReadTimeout(15000);
        return conexion;
    }

    private static Lote desdeJson(JSONObject json) throws JSONException {
        Lote lote = new Lote();
        lote.setId(json.optString("id", null));
        lote.setUsuarioId(json.optString("usuario_id", null));
        lote.setNombre(json.optString("nombre", ""));
        lote.setCultivo(json.optString("cultivo", ""));
        lote.setHectareas(json.optDouble("hectareas", 0));
        return lote;
    }

    private static String leerRespuesta(InputStream flujo) throws IOException {
        if (flujo == null) {
            return "";
        }
        StringBuilder texto = new StringBuilder();
        BufferedReader lector = new BufferedReader(new InputStreamReader(flujo, StandardCharsets.UTF_8));
        try {
            String linea;
            while ((linea = lector.readLine()) != null) {
                texto.append(linea);
            }
        } finally {
            lector.close();
        }
        return texto.toString();
    }

    private static String mensajeDeError(int codigo, String cuerpo) {
        Log.w(TAG, "Supabase respondió " + codigo + ": " + cuerpo);
        if (codigo == 401 || codigo == 403) {
            return MSG_SIN_SESION;
        }
        return "El servidor respondió con un error (" + codigo + "). Intenta de nuevo.";
    }

    private static String mensajeDeExcepcion(Exception e) {
        Log.e(TAG, "Error de conexión con Supabase", e);
        if (e instanceof IOException) {
            return MSG_RED;
        }
        return "Ocurrió un error inesperado. Intenta de nuevo.";
    }

    private <T> void publicarExito(final Callback<T> callback, final T datos) {
        principal.post(new Runnable() {
            @Override
            public void run() {
                callback.onExito(datos);
            }
        });
    }

    private <T> void publicarError(final Callback<T> callback, final String mensaje) {
        principal.post(new Runnable() {
            @Override
            public void run() {
                callback.onError(mensaje);
            }
        });
    }
}
