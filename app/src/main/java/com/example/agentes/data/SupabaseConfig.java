package com.example.agentes.data;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.SystemClock;
import android.provider.Settings;
import android.text.TextUtils;

/**
 * Punto único de configuración de Supabase y sesión compartida por toda la app.
 *
 * <p><b>Configuración:</b> la URL del proyecto y la clave pública ({@code anon key})
 * están declaradas una sola vez aquí. Nunca se debe escribir la {@code service_role key}
 * ni ninguna clave privada: protegen los datos las políticas RLS y el JWT.</p>
 *
 * <p><b>Sesión:</b> el módulo de autenticación llama a {@link #setSesion} tras el
 * login y a {@link #guardarSesion} según la casilla "Recordarme". El módulo de lotes
 * consulta {@link #getAccessToken()} y {@link #getUsuarioId()} antes de llamar al
 * backend; las políticas RLS de Supabase usan ese token para devolver únicamente los
 * lotes del usuario autenticado.</p>
 */
public final class SupabaseConfig {

    /**
     * URL del proyecto de Supabase.
     *
     * <p>Son credenciales <b>públicas</b> del cliente (viajan en el APK y ya son visibles para
     * quien descargue la app); protegen los datos las políticas RLS y el JWT, nunca esta clave.
     * Por eso está permitido escribirlas aquí, y por eso <b>nunca</b> debe colocarse la
     * {@code service_role key}.</p>
     */
    public static final String SUPABASE_URL = "https://pyhvhbenqgmgztbkmrgh.supabase.co";
    /**
     * Clave <b>anon</b> en formato JWT legacy ({@code eyJ...}) del mismo proyecto.
     * La usa <b>solo el módulo de lotes</b> ({@code LoteRepository}): viaja en el
     * header {@code apikey} y, cuando hay sesión de usuario, el token del usuario
     * viaja en {@code Authorization: Bearer} (ver {@link #getAccessToken()}).
     * Los flujos de autenticación (login/registro) NO usan esta clave: van con la
     * clave publicable {@code sb_publishable_...} de {@code SupabaseClient}, que
     * no debe enviarse como Bearer. Ambas claves apuntan al mismo proyecto, cuya
     * URL se declara una sola vez en {@link #SUPABASE_URL}.
     */
    public static final String SUPABASE_ANON_KEY =
            "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InB5aHZoYmV"
            + "ucWdtZ3p0YmttcmdoIiwicm9sZSI6ImFub24iLCJpYXQiOjE3OTIxODkyODAsImV4cCI6MjEwNz"
            + "c2NTI4MH0.-yGK2sFegp5bNwZ71BaSJdWcYigjHG82bV2eEedd1iA";

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
     * Registro de sesión mínimo (token + usuario) del módulo de lotes.
     * Delega en la versión completa para mantener una sola sesión en memoria.
     */
    public static void setSesion(String tokenUsuario, String idUsuario) {
        setSesion(tokenUsuario, "", idUsuario, null, null);
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

    /** Borra la sesión activa en memoria (sin contexto no se alcanzan las preferencias). */
    public static void limpiarSesion() {
        token = null;
        refreshToken = null;
        usuarioId = null;
        nombreUsuario = null;
        correoUsuario = null;
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

    /**
     * Token con el que se firman las peticiones; si no hay sesión (o el MVP
     * autentica sin JWT) se usa la anon key.
     */
    public static String getAccessToken() {
        return TextUtils.isEmpty(token) ? SUPABASE_ANON_KEY : token;
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
