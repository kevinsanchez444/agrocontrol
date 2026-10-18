package com.example.agentes.finanzas;

/**
 * Movimiento financiero (ingreso o gasto) asociado a un lote.
 * Campos alineados con la tabla {@code movimientos} del modelo de datos.
 * El monto se guarda en pesos colombianos enteros (COP).
 */
public final class Movimiento {

    public static final String TIPO_INGRESO = "ingreso";
    public static final String TIPO_GASTO = "gasto";

    private final String id;
    private final String loteId;
    private final String tipo;
    private final String categoria;
    private final long monto;
    private final String descripcion;
    /** Fecha del movimiento en formato ISO {@code yyyy-MM-dd}. */
    private final String fecha;
    /** Momento de registro (millis) para desempatar el orden. */
    private final long creadoEn;

    public Movimiento(String id, String loteId, String tipo, String categoria,
                      long monto, String descripcion, String fecha, long creadoEn) {
        this.id = id;
        this.loteId = loteId;
        this.tipo = tipo;
        this.categoria = categoria;
        this.monto = monto;
        this.descripcion = descripcion == null ? "" : descripcion;
        this.fecha = fecha;
        this.creadoEn = creadoEn;
    }

    public String getId() { return id; }
    public String getLoteId() { return loteId; }
    public String getTipo() { return tipo; }
    public String getCategoria() { return categoria; }
    public long getMonto() { return monto; }
    public String getDescripcion() { return descripcion; }
    public String getFecha() { return fecha; }
    public long getCreadoEn() { return creadoEn; }

    public boolean esIngreso() {
        return TIPO_INGRESO.equals(tipo);
    }
}
