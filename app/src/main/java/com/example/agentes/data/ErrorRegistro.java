package com.example.agentes.data;

import java.util.Locale;

/**
 * Traduce las respuestas de error de Supabase Auth (GoTrue) a códigos de la app.
 *
 * <p>Clase pura de Java (sin Android) para poder probarla con JUnit en el host.
 * Se basa primero en {@code error_code} (campo estable de GoTrue) y, como
 * respaldo, en el texto de {@code msg}.</p>
 */
public final class ErrorRegistro {

    private ErrorRegistro() {
    }

    public static int clasificar(int codigoHttp, String cuerpo) {
        String t = cuerpo == null ? "" : cuerpo.toLowerCase(Locale.ROOT);

        if (t.contains("user_already_exists") || t.contains("email_exists")
                || t.contains("already registered") || t.contains("already been registered")) {
            return AuthRepository.ERROR_YA_REGISTRADO;
        }
        if (codigoHttp == 429 || t.contains("rate_limit") || t.contains("rate limit")) {
            return AuthRepository.ERROR_LIMITE_INTENTOS;
        }
        if (t.contains("signup_disabled") || t.contains("email_provider_disabled")
                || t.contains("signups not allowed") || t.contains("signup is disabled")
                || t.contains("email logins not allowed")) {
            return AuthRepository.ERROR_REGISTRO_DESHABILITADO;
        }
        // El trigger que copia el usuario a public.usuarios falló (o la tabla no existe).
        if (t.contains("database error saving new user")) {
            return AuthRepository.ERROR_TRIGGER_USUARIOS;
        }
        // "Confirm email" activo: el SMTP por defecto de Supabase solo envía a
        // correos del equipo del proyecto, o el envío falló.
        if (t.contains("email_address_not_authorized") || t.contains("not authorized")
                || t.contains("error sending confirmation") || t.contains("sending confirmation email")) {
            return AuthRepository.ERROR_CONFIRMACION_CORREO;
        }
        if (t.contains("email_address_invalid") || t.contains("validation_failed")
                || t.contains("is invalid")) {
            return AuthRepository.ERROR_CORREO_RECHAZADO;
        }
        if (t.contains("captcha")) {
            return AuthRepository.ERROR_CAPTCHA;
        }
        if (codigoHttp == 401 || codigoHttp == 403) {
            return AuthRepository.ERROR_PERMISOS;
        }
        return AuthRepository.ERROR_SERVIDOR;
    }
}
