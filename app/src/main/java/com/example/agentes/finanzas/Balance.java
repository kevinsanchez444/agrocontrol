package com.example.agentes.finanzas;

/** Resultado del cálculo financiero de un lote. */
public final class Balance {

    private final long ingresos;
    private final long gastos;

    public Balance(long ingresos, long gastos) {
        this.ingresos = ingresos;
        this.gastos = gastos;
    }

    public long getIngresos() { return ingresos; }
    public long getGastos() { return gastos; }

    /** Saldo = ingresos - gastos (puede ser negativo). */
    public long getSaldo() { return ingresos - gastos; }
}
