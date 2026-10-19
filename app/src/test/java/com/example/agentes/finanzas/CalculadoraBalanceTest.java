package com.example.agentes.finanzas;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class CalculadoraBalanceTest {

    private static Movimiento mov(String tipo, long monto, String fecha, long creadoEn) {
        return new Movimiento("id" + creadoEn, "lote1", tipo, "Cat", monto, "", fecha, creadoEn);
    }

    @Test
    public void sinMovimientos_saldoCero() {
        Balance b = CalculadoraBalance.calcular(new ArrayList<Movimiento>());
        assertEquals(0, b.getSaldo());
    }

    @Test
    public void gastoDe300000_saldoNegativo() {
        List<Movimiento> lista = new ArrayList<>();
        lista.add(mov(Movimiento.TIPO_GASTO, 300000, "2026-10-18", 1));
        Balance b = CalculadoraBalance.calcular(lista);
        assertEquals(-300000, b.getSaldo());
        assertEquals("-$300.000 COP", FormatoUtil.moneda(b.getSaldo()));
    }

    @Test
    public void ingresoDespuesDeGasto_saldoSeRecalcula() {
        List<Movimiento> lista = new ArrayList<>();
        lista.add(mov(Movimiento.TIPO_GASTO, 300000, "2026-10-18", 1));
        lista.add(mov(Movimiento.TIPO_INGRESO, 1000000, "2026-10-19", 2));
        Balance b = CalculadoraBalance.calcular(lista);
        assertEquals(1000000, b.getIngresos());
        assertEquals(300000, b.getGastos());
        assertEquals(700000, b.getSaldo());
    }

    @Test
    public void historial_ordenadoPorFechaDescendente() {
        List<Movimiento> lista = new ArrayList<>();
        lista.add(mov(Movimiento.TIPO_GASTO, 1, "2026-10-01", 1));
        lista.add(mov(Movimiento.TIPO_GASTO, 2, "2026-10-20", 2));
        lista.add(mov(Movimiento.TIPO_GASTO, 3, "2026-10-10", 3));
        List<Movimiento> orden = CalculadoraBalance.ordenarPorFechaDesc(lista);
        assertEquals("2026-10-20", orden.get(0).getFecha());
        assertEquals("2026-10-10", orden.get(1).getFecha());
        assertEquals("2026-10-01", orden.get(2).getFecha());
    }

    @Test
    public void mismaFecha_desempataPorRegistroMasReciente() {
        List<Movimiento> lista = new ArrayList<>();
        lista.add(mov(Movimiento.TIPO_GASTO, 1, "2026-10-10", 5));
        lista.add(mov(Movimiento.TIPO_GASTO, 2, "2026-10-10", 9));
        assertEquals(9, CalculadoraBalance.ordenarPorFechaDesc(lista).get(0).getCreadoEn());
    }

    @Test
    public void formatoFechaYMoneda() {
        assertEquals("18/10/2026", FormatoUtil.fechaVisible("2026-10-18"));
        assertEquals("$1.000.000 COP", FormatoUtil.moneda(1000000));
        assertEquals("$0 COP", FormatoUtil.moneda(0));
        assertTrue(FormatoUtil.hoyIso().matches("\\d{4}-\\d{2}-\\d{2}"));
        assertEquals("2026-10-18", FormatoUtil.isoDesdeUtcMillis(1792281600000L));
    }
}
