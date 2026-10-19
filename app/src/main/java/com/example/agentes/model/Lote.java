package com.example.agentes.model;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Lote de cultivo de un productor.
 *
 * <p>Espeja la tabla {@code lotes} de Supabase:
 * {@code id, usuario_id, nombre, cultivo, hectareas}.</p>
 */
public class Lote implements Serializable {

    public static final String CULTIVO_CAFE = "Café";
    public static final String CULTIVO_PLATANO = "Plátano";
    public static final String CULTIVO_AGUACATE = "Aguacate";

    /** Cultivos permitidos por el MVP. */
    public static final List<String> CULTIVOS = Collections.unmodifiableList(
            Arrays.asList(CULTIVO_CAFE, CULTIVO_PLATANO, CULTIVO_AGUACATE));

    private String id;
    private String usuarioId;
    private String nombre;
    private String cultivo;
    private double hectareas;

    public Lote() {
    }

    public Lote(String nombre, String cultivo, double hectareas) {
        this.nombre = nombre;
        this.cultivo = cultivo;
        this.hectareas = hectareas;
    }

    /** Indica si un texto corresponde a uno de los cultivos soportados (café, plátano o aguacate). */
    public static boolean esCultivoValido(String cultivo) {
        return cultivo != null && CULTIVOS.contains(cultivo);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUsuarioId() {
        return usuarioId;
    }

    public void setUsuarioId(String usuarioId) {
        this.usuarioId = usuarioId;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getCultivo() {
        return cultivo;
    }

    public void setCultivo(String cultivo) {
        this.cultivo = cultivo;
    }

    public double getHectareas() {
        return hectareas;
    }

    public void setHectareas(double hectareas) {
        this.hectareas = hectareas;
    }
}
