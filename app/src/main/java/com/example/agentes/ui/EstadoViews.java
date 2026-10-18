package com.example.agentes.ui;

import android.view.View;

/**
 * Alterna los 4 contenedores de estado congelados por el Arquitecto:
 * vacío, carga, error y contenido. Los módulos (Integrantes 2, 3 y 4)
 * conservan los 4 IDs de sus layouts y usan este helper para cambiar
 * de estado sin tocar la navegación ni los estilos comunes.
 */
public final class EstadoViews {

    public static final String VACIO = "vacio";
    public static final String CARGA = "carga";
    public static final String ERROR = "error";
    public static final String CONTENIDO = "contenido";

    private final View vacio;
    private final View carga;
    private final View error;
    private final View contenido;

    public EstadoViews(View vacio, View carga, View error, View contenido) {
        this.vacio = vacio;
        this.carga = carga;
        this.error = error;
        this.contenido = contenido;
    }

    public void mostrar(String estado) {
        vacio.setVisibility(VACIO.equals(estado) ? View.VISIBLE : View.GONE);
        carga.setVisibility(CARGA.equals(estado) ? View.VISIBLE : View.GONE);
        error.setVisibility(ERROR.equals(estado) ? View.VISIBLE : View.GONE);
        contenido.setVisibility(CONTENIDO.equals(estado) ? View.VISIBLE : View.GONE);
    }
}
