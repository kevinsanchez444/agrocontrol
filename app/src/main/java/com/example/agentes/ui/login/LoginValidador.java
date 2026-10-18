package com.example.agentes.ui.login;

import java.util.regex.Pattern;

/**
 * Reglas de validación del formulario de login.
 *
 * <p>Clase pura de Java (sin dependencias de Android) para poder unit-testearla
 * desde el host, siguiendo el patrón de validación manual del proyecto.</p>
 */
public final class LoginValidador {

    public static final int LONGITUD_MINIMA_CONTRASENA = 6;

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

    public static boolean esContrasenaValida(String contrasena) {
        return contrasena != null && contrasena.length() >= LONGITUD_MINIMA_CONTRASENA;
    }
}
