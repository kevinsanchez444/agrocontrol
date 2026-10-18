package com.example.agentes.data;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.SystemClock;
import android.provider.Settings;

/**
 * Sesión de Supabase compartida por toda la app.
 *
 * <p>El módulo de autenticación llama a {@link #setSesion} tras el login y a
 * {@link #guardarSesion} según la casilla "Recordarme". El resto de la app
 * (p. ej. {@code LoteRepository}) consulta {@link #getToken()} y
 * {@link #getUsuarioId()} antes de llamar al backend.</p>
 */
public final class SupabaseConfig {

    private static final String PREFS_SESION = "agro_sesion";
    private static final String CLAVE_TOKEN = "access_token";
    private static final String CLAVE_REFRESH_TOKEN = "refresh_token";
    private static final String CLAVE_USUARIO_ID = "usuario_id";
    private static final String CLAVE_NOMBRE = "usuario_nombre";
    private static final String CLAVE_CORREO = "usuario_correo";
    private static final String CLAVE_ARRANQUE = "id_arranque";

    /** Diferencia tolerada al comparar la hora de encendido (los relojes se ajustan). */
    private static final long TOLERANCIA_ARRANQUE_MS = 60_000L;

    // Sesión en memoria (siempre presente mientras viva el proceso)
    private static String token;
    private static String refreshToken;
    private static String usuarioId;
    private static String nombreUsuario;
    private static String correoUsuario;

    private SupabaseConfig() {
    }

    /** La llama el módulo de autenticación tras un login exitoso. */
    public static void setSesion(String nuevoToken, String nuevoRefreshToken, String nuevoUsuarioId,
                                 String nombre, String correo) {
        token = nuevoToken;
        refreshToken = nuevoRefreshToken;
        usuarioId = nuevoUsuarioId;
        nombreUsuario = nombre;
        correoUsuario = correo;
    }

    /**
     * Persiste la sesión solo si el usuario marcó "Recordarme"; si no, borra
     * cualquier sesión guardada previamente para que no se restaure al volver.
     */
    public static void guardarSesion(Context contexto, boolean recordarme) {
        SharedPreferences.Editor editor = prefs(contexto).edit().clear();
        if (recordarme && haySesion()) {
            editor.putString(CLAVE_ARRANQUE, idArranque(contexto))
                    .putString(CLAVE_TOKEN, token)
                    .putString(CLAVE_REFRESH_TOKEN, refreshToken)
                    .putString(CLAVE_USUARIO_ID, usuarioId)
                    .putString(CLAVE_NOMBRE, nombreUsuario)
                    .putString(CLAVE_CORREO, correoUsuario);
        }
        editor.apply();
    }

    /**
     * Restaura en memoria la sesión persistida con "Recordarme".
     *
     * <p>Si el celular se reinició desde que se guardó, la sesión se borra y
     * se devuelve {@code false}, de modo que vuelve a aparecer el login.</p>
     */
    public static boolean restaurarSesion(Context contexto) {
        SharedPreferences prefs = prefs(contexto);
        if (!mismoArranque(prefs.getString(CLAVE_ARRANQUE, null), idArranque(contexto))) {
            limpiarSesion(contexto);
            return false;
        }
        token = prefs.getString(CLAVE_TOKEN, null);
        refreshToken = prefs.getString(CLAVE_REFRESH_TOKEN, null);
        usuarioId = prefs.getString(CLAVE_USUARIO_ID, null);
        nombreUsuario = prefs.getString(CLAVE_NOMBRE, null);
        correoUsuario = prefs.getString(CLAVE_CORREO, null);
        return haySesion();
    }

    /** Cierra la sesión en memoria y en preferencias. */
    public static void limpiarSesion(Context contexto) {
        token = null;
        refreshToken = null;
        usuarioId = null;
        nombreUsuario = null;
        correoUsuario = null;
        prefs(contexto).edit().clear().apply();
    }

    public static boolean haySesion() {
        // El MVP autentica con nombre + correo contra la tabla usuarios, sin JWT:
        // la sesión se considera activa cuando hay usuarioId.
        return usuarioId != null && !usuarioId.isEmpty();
    }

    /** Token JWT actual o cadena vacía si no hay sesión. */
    public static String getToken() {
        return token != null ? token : "";
    }

    public static String getRefreshToken() {
        return refreshToken != null ? refreshToken : "";
    }

    /** UUID del usuario autenticado o cadena vacía si no hay sesión. */
    public static String getUsuarioId() {
        return usuarioId != null ? usuarioId : "";
    }

    public static String getNombreUsuario() {
        return nombreUsuario != null ? nombreUsuario : "";
    }

    public static String getCorreoUsuario() {
        return correoUsuario != null ? correoUsuario : "";
    }

    /**
     * Identifica el encendido actual del celular: "n:<contador>" con el
     * contador de arranques del sistema, o "t:<hora de encendido>" si no está
     * disponible.
     */
    private static String idArranque(Context contexto) {
        try {
            int arranques = Settings.Global.getInt(contexto.getContentResolver(),
                    Settings.Global.BOOT_COUNT);
            return "n:" + arranques;
        } catch (Settings.SettingNotFoundException | SecurityException e) {
            long encendido = System.currentTimeMillis() - SystemClock.elapsedRealtime();
            return "t:" + encendido;
        }
    }

    static boolean mismoArranque(String guardado, String actual) {
        if (guardado == null || actual == null) {
            return false;
        }
        if (guardado.startsWith("t:") && actual.startsWith("t:")) {
            try {
                long a = Long.parseLong(guardado.substring(2));
                long b = Long.parseLong(actual.substring(2));
                return Math.abs(a - b) <= TOLERANCIA_ARRANQUE_MS;
            } catch (NumberFormatException e) {
                return false;
            }
        }
        return guardado.equals(actual);
    }

    private static SharedPreferences prefs(Context contexto) {
        return contexto.getApplicationContext()
                .getSharedPreferences(PREFS_SESION, Context.MODE_PRIVATE);
    }
}
