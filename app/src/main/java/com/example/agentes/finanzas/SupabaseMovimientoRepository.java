package com.example.agentes.finanzas;

import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;

import com.example.agentes.data.SupabaseConfig;

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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

/**
 * Acceso a la tabla {@code movimientos} de Supabase mediante la API REST (PostgREST).
 * Implementación remota de {@link MovimientoRepository} para el libro de caja.
 *
 * <p><b>Contrato de la tabla</b> (esquema real verificado, difiere del README):
 * {@code movimientos(id uuid pk, lote_id uuid nullable, usuario_id uuid, categoria_id uuid fk ->
 * categorias, tipo text, monto numeric, descripcion text, fecha, created_at, updated_at)}.
 * No existe la columna {@code creado_en}: la app traduce {@code created_at} al campo interno
 * {@code creadoEn} de {@link Movimiento}.</p>
 *
 * <p><b>Filtro de seguridad:</b> cada petición de lectura añade
 * {@code usuario_id=eq.<id de la sesión>} y cada INSERT escribe siempre el {@code usuario_id}
 * de la sesión: es la defensa en profundidad que compensa las políticas RLS MVP
 * (ver {@code supabase/movimientos_permisos.sql}).</p>
 *
 * <p><b>Libro de caja general:</b> ver {@link #listarPorLote}. Las peticiones se ejecutan en
 * un hilo de fondo y el resultado se entrega en el hilo principal con
 * {@code Handler(Looper.getMainLooper())}, igual que {@code LoteRepository}.</p>
 */
public class SupabaseMovimientoRepository implements MovimientoRepository {

    private static final String TAG = "SupabaseMovRepo";
    private static final String TABLA = "movimientos";
    private static final String TABLA_CATEGORIAS = "categorias";
    private static final String COLUMNAS =
            "id,lote_id,tipo,categoria_id,monto,descripcion,fecha,created_at";
    private static final String COLUMNAS_CATEGORIAS = "id,nombre,tipo";
    /** Máximo de filas por consulta (la UI no necesita más; acota memoria y coste). */
    private static final int LIMITE = 500;

    private static final String MSG_SIN_CONFIGURAR =
            "Falta configurar la conexión con Supabase.";
    private static final String MSG_SIN_SESION =
            "No hay una sesión activa. Inicia sesión para ver los movimientos.";
    private static final String MSG_RED =
            "No se pudo conectar con el servidor. Revisa tu conexión e intenta de nuevo.";
    private static final String MSG_SIN_PERMISOS =
            "El servidor no autoriza la tabla de movimientos (permisos pendientes en Supabase). "
                    + "Avisa al equipo técnico.";
    private static final String MSG_CATEGORIA_INEXISTENTE =
            "La categoría no existe en el catálogo de la base de datos.";

    /** Placeholder cuando el id de categoría no está en el catálogo (nunca rompe la lista). */
    private static final String CATEGORIA_DESCONOCIDA = "—";

    private final Handler principal = new Handler(Looper.getMainLooper());

    /**
     * Lista los movimientos del usuario autenticado.
     *
     * <p><b>Decisión de producto:</b> si {@code loteId} viene <b>vacío</b> NO se aplica filtro de
     * lote y se devuelven <b>todos</b> los movimientos del usuario: es el <b>libro de caja
     * GENERAL</b> de la pestaña Finanzas (Home sin lote seleccionado). Con un lote no vacío se
     * filtra por {@code lote_id=eq.<loteId>} (libro de caja del lote).</p>
     *
     * <p>Dos pasos en el mismo hilo: 1) movimientos del usuario (orden
     * {@code fecha.desc,created_at.desc}, tope 500); 2) catálogo {@code categorias} para traducir
     * {@code categoria_id} → nombre. El paso 2 es best-effort: si falla, la lista se devuelve igual
     * con los ids cortos (nunca se falla la lista por el catálogo).</p>
     */
    @Override
    public void listarPorLote(final String loteId, final Callback<List<Movimiento>> callback) {
        if (!SupabaseConfig.estaConfigurado()) {
            publicarError(callback, MSG_SIN_CONFIGURAR);
            return;
        }
        final String usuarioId = SupabaseConfig.getUsuarioId();
        if (TextUtils.isEmpty(usuarioId)) {
            publicarError(callback, MSG_SIN_SESION);
            return;
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    // 1) Movimientos del usuario (filtro explícito por la sesión: no depende
                    //    solo de RLS, que con la clave anon no distingue usuarios).
                    String ruta = TABLA + "?select=" + COLUMNAS
                            + "&usuario_id=eq." + usuarioId
                            + (TextUtils.isEmpty(loteId) ? "" : "&lote_id=eq." + loteId)
                            + "&order=fecha.desc,created_at.desc&limit=" + LIMITE;
                    JSONArray arreglo = pedirArreglo("GET", ruta, null);

                    // 2) Catálogo de categorías (best-effort, ya funciona hoy).
                    Map<String, String> nombres = new HashMap<>();
                    try {
                        JSONArray categorias = pedirArreglo("GET",
                                TABLA_CATEGORIAS + "?select=" + COLUMNAS_CATEGORIAS, null);
                        for (int i = 0; i < categorias.length(); i++) {
                            JSONObject c = categorias.getJSONObject(i);
                            nombres.put(c.optString("id", ""), c.optString("nombre", ""));
                        }
                    } catch (Exception e) {
                        Log.w(TAG, "No se pudo leer el catálogo de categorías; se usan los ids", e);
                    }

                    List<Movimiento> movimientos = new ArrayList<>();
                    for (int i = 0; i < arreglo.length(); i++) {
                        movimientos.add(desdeJson(arreglo.getJSONObject(i), nombres));
                    }
                    publicarExito(callback, movimientos);
                } catch (ErrorServidor e) {
                    publicarError(callback, mensajeDeError(e.codigo, e.cuerpo));
                } catch (Exception e) {
                    publicarError(callback, mensajeDeExcepcion(e));
                }
            }
        }).start();
    }

    /**
     * Inserta un movimiento en nombre del usuario autenticado (POST con
     * {@code Prefer: return=representation}).
     *
     * <p>El {@code categoria_id} se resuelve contra el catálogo real: primero por
     * nombre + tipo, luego por nombre exacto; si no existe → error tipado
     * {@link #MSG_CATEGORIA_INEXISTENTE}. Si el lote está vacío se envía
     * {@code lote_id: null} (libro de caja general); si la BD aún tuviera
     * {@code NOT NULL} en esa columna, el error lo verá el usuario por Snackbar hasta que se
     * ejecute {@code supabase/movimientos_permisos.sql}.</p>
     *
     * <p>No se envían {@code created_at}/{@code updated_at}: los pone la base de datos.</p>
     */
    @Override
    public void agregar(final Movimiento movimiento, final Callback<Movimiento> callback) {
        if (!SupabaseConfig.estaConfigurado()) {
            publicarError(callback, MSG_SIN_CONFIGURAR);
            return;
        }
        final String usuarioId = SupabaseConfig.getUsuarioId();
        if (TextUtils.isEmpty(usuarioId)) {
            publicarError(callback, MSG_SIN_SESION);
            return;
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    // 1) Catálogo de categorías → resolver nombre+tipo a uuid.
                    JSONArray categorias = pedirArreglo("GET",
                            TABLA_CATEGORIAS + "?select=" + COLUMNAS_CATEGORIAS, null);
                    String categoriaId = resolverCategoriaId(categorias,
                            movimiento.getCategoria(), movimiento.getTipo());
                    if (categoriaId == null) {
                        publicarError(callback, MSG_CATEGORIA_INEXISTENTE);
                        return;
                    }
                    Map<String, String> nombres = new HashMap<>();
                    for (int i = 0; i < categorias.length(); i++) {
                        JSONObject c = categorias.getJSONObject(i);
                        nombres.put(c.optString("id", ""), c.optString("nombre", ""));
                    }

                    // 2) INSERT del movimiento.
                    String loteId = movimiento.getLoteId();
                    JSONObject cuerpo = new JSONObject();
                    cuerpo.put("id", movimiento.getId());
                    cuerpo.put("usuario_id", usuarioId);
                    cuerpo.put("lote_id", TextUtils.isEmpty(loteId) ? JSONObject.NULL : loteId);
                    cuerpo.put("categoria_id", categoriaId);
                    cuerpo.put("tipo", movimiento.getTipo());
                    cuerpo.put("monto", movimiento.getMonto());
                    cuerpo.put("descripcion", movimiento.getDescripcion());
                    cuerpo.put("fecha", aFechaIso(movimiento.getFecha()));

                    HttpURLConnection conexion = null;
                    try {
                        conexion = abrirConexion("POST", TABLA);
                        conexion.setDoOutput(true);
                        conexion.setRequestProperty("Prefer", "return=representation");

                        OutputStream salida = conexion.getOutputStream();
                        salida.write(cuerpo.toString().getBytes(StandardCharsets.UTF_8));
                        salida.flush();
                        salida.close();

                        int codigo = conexion.getResponseCode();
                        String respuesta = leerRespuesta(
                                codigo < 400 ? conexion.getInputStream() : conexion.getErrorStream());
                        if (codigo >= 400) {
                            publicarError(callback, mensajeDeError(codigo, respuesta));
                            return;
                        }
                        JSONArray devuelto = new JSONArray(respuesta);
                        Movimiento guardado = devuelto.length() > 0
                                ? desdeJson(devuelto.getJSONObject(0), nombres) : movimiento;
                        publicarExito(callback, guardado);
                    } finally {
                        if (conexion != null) {
                            conexion.disconnect();
                        }
                    }
                } catch (ErrorServidor e) {
                    publicarError(callback, mensajeDeError(e.codigo, e.cuerpo));
                } catch (Exception e) {
                    publicarError(callback, mensajeDeExcepcion(e));
                }
            }
        }).start();
    }

    // ------------------------------------------------------------------
    // Red (mismo patrón que LoteRepository: apikey + Bearer de la sesión)
    // ------------------------------------------------------------------

    private static HttpURLConnection abrirConexion(String metodo, String ruta) throws IOException {
        URL url = new URL(SupabaseConfig.getRestUrl() + "/" + ruta);
        HttpURLConnection conexion = (HttpURLConnection) url.openConnection();
        conexion.setRequestMethod(metodo);
        // apikey: la clave anon del proyecto (rol anon/invitado).
        // Authorization: el token del usuario autenticado si existe; sin sesión,
        // la misma clave anon como fallback (SupabaseConfig.getAccessToken()).
        conexion.setRequestProperty("apikey", SupabaseConfig.SUPABASE_ANON_KEY);
        conexion.setRequestProperty("Authorization", "Bearer " + SupabaseConfig.getAccessToken());
        conexion.setRequestProperty("Content-Type", "application/json");
        conexion.setRequestProperty("Accept", "application/json");
        conexion.setConnectTimeout(15000);
        conexion.setReadTimeout(15000);
        return conexion;
    }

    /** GET simple que devuelve el arreglo JSON o lanza {@link ErrorServidor} en 4xx/5xx. */
    private static JSONArray pedirArreglo(String metodo, String ruta, JSONObject cuerpo)
            throws IOException, ErrorServidor, JSONException {
        HttpURLConnection conexion = null;
        try {
            conexion = abrirConexion(metodo, ruta);
            if (cuerpo != null) {
                conexion.setDoOutput(true);
                OutputStream salida = conexion.getOutputStream();
                salida.write(cuerpo.toString().getBytes(StandardCharsets.UTF_8));
                salida.flush();
                salida.close();
            }
            int codigo = conexion.getResponseCode();
            String respuesta = leerRespuesta(
                    codigo < 400 ? conexion.getInputStream() : conexion.getErrorStream());
            if (codigo >= 400) {
                throw new ErrorServidor(codigo, respuesta);
            }
            return new JSONArray(respuesta);
        } finally {
            if (conexion != null) {
                conexion.disconnect();
            }
        }
    }

    // ------------------------------------------------------------------
    // Mapeo de filas
    // ------------------------------------------------------------------

    /**
     * Convierte una fila de {@code movimientos} en {@link Movimiento}.
     * {@code fecha} se trunca a {@code yyyy-MM-dd} (formato que espera {@link FormatoUtil});
     * {@code categoria} se traduce por {@code categoria_id} y, si no está en el catálogo,
     * se muestra el id corto o "—" sin romper nunca la lista.
     */
    private static Movimiento desdeJson(JSONObject json, Map<String, String> categorias)
            throws JSONException {
        String categoriaId = json.isNull("categoria_id") ? "" : json.optString("categoria_id", "");
        String categoria = categorias.get(categoriaId);
        if (TextUtils.isEmpty(categoria)) {
            if (TextUtils.isEmpty(categoriaId)) {
                categoria = CATEGORIA_DESCONOCIDA;
            } else {
                categoria = categoriaId.length() > 8 ? categoriaId.substring(0, 8) : categoriaId;
            }
        }
        String loteId = json.isNull("lote_id") ? "" : json.optString("lote_id", "");
        return new Movimiento(
                json.optString("id", null),
                loteId,
                json.optString("tipo", ""),
                categoria,
                montoDe(json),
                json.optString("descripcion", ""),
                aFechaIso(json.optString("fecha", "")),
                creadoEnDe(json.optString("created_at", "")));
    }

    /** El monto puede venir como número o como texto con decimales (numeric de PostgREST). */
    private static long montoDe(JSONObject json) {
        try {
            return json.getLong("monto");
        } catch (JSONException e) {
            try {
                return (long) json.getDouble("monto");
            } catch (JSONException e2) {
                return 0L;
            }
        }
    }

    /** Trunca a {@code yyyy-MM-dd}: PostgREST devuelve timestamptz tipo ISO con "T". */
    private static String aFechaIso(String fecha) {
        if (fecha == null) {
            return "";
        }
        int posicion = fecha.indexOf('T');
        if (posicion < 0) {
            posicion = fecha.indexOf(' ');
        }
        if (posicion > 0) {
            return fecha.substring(0, Math.min(posicion, 10));
        }
        return fecha.length() > 10 ? fecha.substring(0, 10) : fecha;
    }

    /** {@code created_at} ISO8601 → millis (UTC). Best-effort: si no se parsea → 0. */
    private static long creadoEnDe(String iso) {
        if (TextUtils.isEmpty(iso)) {
            return 0L;
        }
        try {
            String limpio = iso;
            int fraccion = limpio.indexOf('.');
            if (fraccion >= 0) {
                limpio = limpio.substring(0, fraccion);
            }
            int offset = limpio.indexOf('+');
            if (offset >= 0) {
                limpio = limpio.substring(0, offset);
            }
            if (limpio.endsWith("Z")) {
                limpio = limpio.substring(0, limpio.length() - 1);
            }
            SimpleDateFormat formato = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
            formato.setTimeZone(TimeZone.getTimeZone("UTC"));
            Date fecha = formato.parse(limpio);
            return fecha != null ? fecha.getTime() : 0L;
        } catch (Exception e) {
            return 0L;
        }
    }

    /**
     * Resuelve el uuid de una categoría a partir de su nombre: primero nombre + tipo,
     * luego nombre exacto. Devuelve {@code null} si no está en el catálogo.
     */
    private static String resolverCategoriaId(JSONArray categorias, String nombre, String tipo) {
        String soloNombre = null;
        try {
            for (int i = 0; i < categorias.length(); i++) {
                JSONObject c = categorias.getJSONObject(i);
                String n = c.optString("nombre", "");
                String t = c.optString("tipo", "");
                if (n.equals(nombre) && t.equals(tipo)) {
                    return c.optString("id", null);
                }
                if (n.equals(nombre) && soloNombre == null) {
                    soloNombre = c.optString("id", null);
                }
            }
        } catch (JSONException e) {
            return null;
        }
        return soloNombre;
    }

    // ------------------------------------------------------------------
    // Errores y publicación en el hilo principal (duplicado de LoteRepository
    // porque allí los helpers son private: no se toca esa clase)
    // ------------------------------------------------------------------

    /** Error 4xx/5xx de PostgREST, con cuerpo para distinguir sesión de permisos. */
    private static final class ErrorServidor extends Exception {
        final int codigo;
        final String cuerpo;

        ErrorServidor(int codigo, String cuerpo) {
            this.codigo = codigo;
            this.cuerpo = cuerpo;
        }
    }

    private static String mensajeDeError(int codigo, String cuerpo) {
        Log.w(TAG, "Supabase respondió " + codigo + ": " + cuerpo);
        // PostgREST devuelve 401 con code "42501" (permission denied) cuando la
        // tabla no tiene GRANT para el rol anon: NO es un problema de sesión del
        // usuario, así que se avisa antes del check de 401/403 para no engañar.
        if (cuerpo != null && (cuerpo.contains("permission denied") || cuerpo.contains("42501"))) {
            return MSG_SIN_PERMISOS;
        }
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
