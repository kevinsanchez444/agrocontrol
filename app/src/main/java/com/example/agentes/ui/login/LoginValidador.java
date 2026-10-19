package com.example.agentes.ui.login;

import java.util.regex.Pattern;

/**
 * Reglas de validación del formulario de login/registro.
 *
 * <p>Clase pura de Java (sin dependencias de Android) para poder unit-testearla
 * desde el host, siguiendo el patrón de validación manual del proyecto.</p>
 */
public final class LoginValidador {

    private static final Pattern CORREO =
            Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    private LoginValidador() {
    }

    public static boolean esNombreValido(String nombre) {
        return nombre != null && !nombre.trim().isEmpty();
    }

    public static boolean esCorreoValido(String correo) {
        return correo != null && CORREO.matcher(correo.trim()).matches();
    }

    /**
     * Compara el nombre escrito con el registrado en Supabase ignorando
     * mayúsculas/minúsculas y espacios sobrantes en los extremos.
     */
    public static boolean nombresCoinciden(String nombreEscrito, String nombreRegistrado) {
        if (nombreEscrito == null || nombreRegistrado == null) {
            return false;
        }
        return nombreEscrito.trim().equalsIgnoreCase(nombreRegistrado.trim());
    }
}
