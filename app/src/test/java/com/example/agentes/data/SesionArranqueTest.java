package com.example.agentes.data;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** La sesión "Recordarme" solo sobrevive mientras el celular no se reinicie. */
public class SesionArranqueTest {

    @Test
    public void mismo_contador_de_arranques_conserva_sesion() {
        assertTrue(SupabaseConfig.mismoArranque("n:12", "n:12"));
    }

    @Test
    public void celular_reiniciado_invalida_sesion() {
        assertFalse(SupabaseConfig.mismoArranque("n:12", "n:13"));
    }

    @Test
    public void sesion_guardada_por_version_anterior_se_invalida() {
        assertFalse(SupabaseConfig.mismoArranque(null, "n:12"));
    }

    @Test
    public void hora_de_encendido_con_ajuste_de_reloj_conserva_sesion() {
        assertTrue(SupabaseConfig.mismoArranque("t:1000000", "t:1030000"));
    }

    @Test
    public void hora_de_encendido_distinta_invalida_sesion() {
        assertFalse(SupabaseConfig.mismoArranque("t:1000000", "t:9000000"));
    }
}
