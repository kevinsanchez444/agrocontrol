package com.example.agentes.asesor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.agrocontrol.app.asesor.data.AsesorIAApi;
import com.agrocontrol.app.asesor.data.ResultadoIA;

/**
 * Ramas puras de {@link AsesorIAApi#interpretarRespuesta(int, String)}: la
 * traducción de la respuesta HTTP de la Edge Function {@code asesor-ia} al
 * {@code ResultadoIA} que consume el chat del Asesor.
 */
public class AsesorIAErrorTest {

    private static String error(int codigo, String cuerpo) {
        ResultadoIA r = AsesorIAApi.interpretarRespuesta(codigo, cuerpo);
        assertFalse("no debe ser exitoso con " + codigo, r.getExito());
        return r.getError();
    }

    @Test
    public void sesion_invalida_401() {
        String mensaje = error(401, "{\"error\":\"Tu sesión ya no es válida.\"}");
        assertTrue("401 debe avisar de sesión: " + mensaje, mensaje.contains("sesión"));
    }

    @Test
    public void sin_permisos_403() {
        String mensaje = error(403, "");
        assertTrue("403 debe avisar de sesión: " + mensaje, mensaje.contains("sesión"));
    }

    @Test
    public void limite_de_consultas_429() {
        String mensaje = error(429, "{\"error\":\"Demasiadas consultas seguidas, intenta en un momento.\"}");
        assertTrue("429 debe avisar del límite: " + mensaje, mensaje.contains("Demasiadas"));
    }

    @Test
    public void proveedor_caido_500() {
        String mensaje = error(500, "{\"error\":\"Servicio no disponible.\"}");
        assertTrue("5xx debe avisar de indisponibilidad: " + mensaje,
                mensaje.contains("no está disponible"));
    }

    @Test
    public void respuesta_correcta_200() {
        ResultadoIA r = AsesorIAApi.interpretarRespuesta(200, "{\"texto\":\"hola\"}");
        assertTrue(r.getExito());
        assertEquals("hola", r.getTexto());
        assertNull(r.getError());
    }

    @Test
    public void respuesta_200_sin_texto() {
        ResultadoIA r = AsesorIAApi.interpretarRespuesta(200, "{}");
        assertFalse(r.getExito());
        assertNull(r.getTexto());
        assertTrue(r.getError() != null && !r.getError().isEmpty());
    }

    @Test
    public void respuesta_200_con_cuerpo_ilegible() {
        ResultadoIA r = AsesorIAApi.interpretarRespuesta(200, "<html>error</html>");
        assertFalse(r.getExito());
        assertTrue(r.getError() != null && !r.getError().isEmpty());
    }
}
