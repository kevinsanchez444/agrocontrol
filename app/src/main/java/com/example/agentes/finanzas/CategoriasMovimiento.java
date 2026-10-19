package com.example.agentes.finanzas;

import java.util.Arrays;
import java.util.List;

/** Categorías iniciales (coinciden con el catálogo semilla de la tabla {@code categorias}). */
public final class CategoriasMovimiento {

    private static final List<String> GASTOS = Arrays.asList(
            "Fertilizante", "Abono", "Fungicida", "Jornal", "Otro gasto");
    private static final List<String> INGRESOS = Arrays.asList(
            "Venta de cosecha", "Otro ingreso");

    private CategoriasMovimiento() { }

    public static List<String> paraTipo(String tipo) {
        return Movimiento.TIPO_INGRESO.equals(tipo) ? INGRESOS : GASTOS;
    }
}
