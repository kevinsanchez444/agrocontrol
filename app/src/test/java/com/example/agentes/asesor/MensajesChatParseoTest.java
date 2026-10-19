package com.example.agentes.asesor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

import com.agrocontrol.app.asesor.data.EstadoMensaje;
import com.agrocontrol.app.asesor.data.MensajeChat;
import com.agrocontrol.app.asesor.data.MensajesChatSupabase;

/**
 * Parseo puro de la respuesta de PostgREST de la tabla
 * {@code public.mensajes_chat} a la lista de burbujas del chat
 * ({@link MensajesChatSupabase#parsear(String)}). Es el punto donde el JSON
 * remoto se convierte en el historial que ve el usuario; nunca debe lanzar.
 */
public class MensajesChatParseoTest {

    @Test
    public void json_con_dos_filas_devuelve_dos_mensajes() {
        String json = "[{\"texto\":\"¿Cómo riego mi café?\",\"de_usuario\":true},"
                + "{\"texto\":\"Riega temprano y revisa el drenaje.\",\"de_usuario\":false}]";

        List<MensajeChat> mensajes = MensajesChatSupabase.parsear(json);

        assertEquals(2, mensajes.size());

        MensajeChat primero = mensajes.get(0);
        assertEquals("¿Cómo riego mi café?", primero.getTexto());
        assertTrue("el primer mensaje lo escribió el usuario", primero.getEsUsuario());
        assertEquals(EstadoMensaje.ENVIADO, primero.getEstado());

        MensajeChat segundo = mensajes.get(1);
        assertEquals("Riega temprano y revisa el drenaje.", segundo.getTexto());
        assertFalse("el segundo mensaje lo escribió la IA", segundo.getEsUsuario());
        assertEquals(EstadoMensaje.ENVIADO, segundo.getEstado());
    }

    @Test
    public void arreglo_vacio_devuelve_lista_vacia() {
        assertTrue(MensajesChatSupabase.parsear("[]").isEmpty());
    }

    @Test
    public void cuerpo_null_devuelve_lista_vacia() {
        assertTrue(MensajesChatSupabase.parsear(null).isEmpty());
    }

    @Test
    public void json_invalido_devuelve_lista_vacia_sin_lanzar() {
        assertTrue(MensajesChatSupabase.parsear("no es json").isEmpty());
    }

    @Test
    public void fila_sin_texto_se_ignora() {
        String json = "[{\"de_usuario\":true},"
                + "{\"texto\":\"válido\",\"de_usuario\":false}]";

        List<MensajeChat> mensajes = MensajesChatSupabase.parsear(json);

        assertEquals(1, mensajes.size());
        assertEquals("válido", mensajes.get(0).getTexto());
    }

    @Test
    public void fila_con_texto_vacio_se_ignora() {
        String json = "[{\"texto\":\"   \",\"de_usuario\":true},"
                + "{\"texto\":\"válido\",\"de_usuario\":true}]";

        List<MensajeChat> mensajes = MensajesChatSupabase.parsear(json);

        assertEquals(1, mensajes.size());
        assertEquals("válido", mensajes.get(0).getTexto());
    }

    @Test
    public void de_usuario_ausente_se_asume_del_usuario() {
        List<MensajeChat> mensajes =
                MensajesChatSupabase.parsear("[{\"texto\":\"hola\"}]");

        assertEquals(1, mensajes.size());
        assertTrue("sin 'de_usuario' la fila se trata como mensaje del usuario",
                mensajes.get(0).getEsUsuario());
        assertEquals(EstadoMensaje.ENVIADO, mensajes.get(0).getEstado());
    }
}
