package com.example.agentes.finanzas;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/** Lógica pura (sin Android) del libro de caja: saldo y orden del historial. */
public final class CalculadoraBalance {

    private CalculadoraBalance() { }

    public static Balance calcular(List<Movimiento> movimientos) {
        long ingresos = 0;
        long gastos = 0;
        for (Movimiento m : movimientos) {
            if (m.esIngreso()) {
                ingresos += m.getMonto();
            } else {
                gastos += m.getMonto();
            }
        }
        return new Balance(ingresos, gastos);
    }

    /** Historial ordenado por fecha, del más reciente al más antiguo. */
    public static List<Movimiento> ordenarPorFechaDesc(List<Movimiento> movimientos) {
        List<Movimiento> copia = new ArrayList<>(movimientos);
        Collections.sort(copia, new Comparator<Movimiento>() {
            @Override
            public int compare(Movimiento a, Movimiento b) {
                int porFecha = b.getFecha().compareTo(a.getFecha());
                if (porFecha != 0) {
                    return porFecha;
                }
                return Long.compare(b.getCreadoEn(), a.getCreadoEn());
            }
        });
        return copia;
    }
}
