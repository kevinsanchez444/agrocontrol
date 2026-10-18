package com.example.agentes;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.example.agentes.ui.login.LoginValidador;

import org.junit.Test;

/**
 * Pruebas unitarias de las reglas de validación del formulario de login.
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

    // ---------- Contraseña ----------

    @Test
    public void contrasena_en_longitud_minima_es_valida() {
        assertTrue(LoginValidador.esContrasenaValida("abc123"));
    }

    @Test
    public void contrasena_mas_corta_que_la_minima_es_invalida() {
        assertFalse(LoginValidador.esContrasenaValida("abc12"));
    }

    @Test
    public void contrasena_vacia_es_invalida() {
        assertFalse(LoginValidador.esContrasenaValida(""));
    }

    @Test
    public void contrasena_nula_es_invalida() {
        assertFalse(LoginValidador.esContrasenaValida(null));
    }
}
