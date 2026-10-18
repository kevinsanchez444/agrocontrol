package com.example.agentes;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.example.agentes.ui.login.LoginValidador;

import org.junit.Test;

/**
 * Pruebas unitarias de las reglas de validación del formulario de login/registro.
 */
public class LoginValidadorTest {

    // ---------- Nombre ----------

    @Test
    public void nombre_valido_con_espacios() {
        assertTrue(LoginValidador.esNombreValido("María Fernanda"));
    }

    @Test
    public void nombre_vacio_es_invalido() {
        assertFalse(LoginValidador.esNombreValido(""));
    }

    @Test
    public void nombre_solo_espacios_es_invalido() {
        assertFalse(LoginValidador.esNombreValido("   "));
    }

    @Test
    public void nombre_nulo_es_invalido() {
        assertFalse(LoginValidador.esNombreValido(null));
    }

    // ---------- Correo ----------

    @Test
    public void correo_valido() {
        assertTrue(LoginValidador.esCorreoValido("productor@agrocontrol.com"));
    }

    @Test
    public void correo_valido_con_subdominio_y_plus() {
        assertTrue(LoginValidador.esCorreoValido("finca.la.albania+agro@correo.coop"));
    }

    @Test
    public void correo_sin_arroba_es_invalido() {
        assertFalse(LoginValidador.esCorreoValido("productor.agrocontrol.com"));
    }

    @Test
    public void correo_sin_dominio_es_invalido() {
        assertFalse(LoginValidador.esCorreoValido("productor@"));
    }

    @Test
    public void correo_vacio_es_invalido() {
        assertFalse(LoginValidador.esCorreoValido(""));
    }

    @Test
    public void correo_nulo_es_invalido() {
        assertFalse(LoginValidador.esCorreoValido(null));
    }

    // ---------- Coincidencia de nombres (credencial junto al correo) ----------

    @Test
    public void nombres_coinciden_ignorando_mayusculas() {
        assertTrue(LoginValidador.nombresCoinciden("juan Pérez", "Juan Pérez"));
    }

    @Test
    public void nombres_coinciden_con_espacios_en_los_extremos() {
        assertTrue(LoginValidador.nombresCoinciden("  Juan Pérez  ", "Juan Pérez"));
    }

    @Test
    public void nombres_distintos_no_coinciden() {
        assertFalse(LoginValidador.nombresCoinciden("Pedro", "Juan Pérez"));
    }

    @Test
    public void nombre_vacio_no_coincide_con_registrado() {
        assertFalse(LoginValidador.nombresCoinciden("", "Juan Pérez"));
    }

    @Test
    public void nombres_nulos_no_coinciden() {
        assertFalse(LoginValidador.nombresCoinciden(null, "Juan Pérez"));
        assertFalse(LoginValidador.nombresCoinciden("Juan Pérez", null));
    }
}
