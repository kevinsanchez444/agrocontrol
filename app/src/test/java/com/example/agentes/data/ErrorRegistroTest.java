package com.example.agentes.data;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/** Respuestas reales de Supabase Auth (POST /auth/v1/signup) y su clasificación. */
public class ErrorRegistroTest {

    @Test
    public void trigger_que_falla() {
        assertEquals(AuthRepository.ERROR_TRIGGER_USUARIOS, ErrorRegistro.clasificar(500,
                "{\"code\":500,\"error_code\":\"unexpected_failure\",\"msg\":\"Database error saving new user\"}"));
    }

    @Test
    public void smtp_por_defecto_no_autoriza_el_correo() {
        assertEquals(AuthRepository.ERROR_CONFIRMACION_CORREO, ErrorRegistro.clasificar(400,
                "{\"code\":400,\"error_code\":\"email_address_not_authorized\",\"msg\":\"Email address \\\"juan@gmail.com\\\" cannot be used as it is not authorized\"}"));
    }

    @Test
    public void fallo_enviando_confirmacion() {
        assertEquals(AuthRepository.ERROR_CONFIRMACION_CORREO, ErrorRegistro.clasificar(500,
                "{\"code\":500,\"error_code\":\"unexpected_failure\",\"msg\":\"Error sending confirmation email\"}"));
    }

    @Test
    public void correo_rechazado() {
        assertEquals(AuthRepository.ERROR_CORREO_RECHAZADO, ErrorRegistro.clasificar(400,
                "{\"code\":400,\"error_code\":\"email_address_invalid\",\"msg\":\"Email address \\\"juan@test.com\\\" is invalid\"}"));
    }

    @Test
    public void captcha_activo() {
        assertEquals(AuthRepository.ERROR_CAPTCHA, ErrorRegistro.clasificar(400,
                "{\"code\":400,\"error_code\":\"captcha_failed\",\"msg\":\"captcha protection: request disallowed\"}"));
    }

    @Test
    public void limite_de_correos() {
        assertEquals(AuthRepository.ERROR_LIMITE_INTENTOS, ErrorRegistro.clasificar(429,
                "{\"code\":429,\"error_code\":\"over_email_send_rate_limit\",\"msg\":\"email rate limit exceeded\"}"));
    }

    @Test
    public void registro_deshabilitado() {
        assertEquals(AuthRepository.ERROR_REGISTRO_DESHABILITADO, ErrorRegistro.clasificar(422,
                "{\"code\":422,\"error_code\":\"signup_disabled\",\"msg\":\"Signups not allowed for this instance\"}"));
    }

    @Test
    public void usuario_ya_existe() {
        assertEquals(AuthRepository.ERROR_YA_REGISTRADO, ErrorRegistro.clasificar(422,
                "{\"code\":422,\"error_code\":\"user_already_exists\",\"msg\":\"User already registered\"}"));
    }

    @Test
    public void api_key_invalida() {
        assertEquals(AuthRepository.ERROR_PERMISOS, ErrorRegistro.clasificar(401,
                "{\"message\":\"Invalid API key\"}"));
    }

    @Test
    public void correo_se_normaliza_a_minusculas() {
        assertEquals("juan@gmail.com", AuthRepository.normalizarCorreo("  Juan@Gmail.com "));
    }
}
