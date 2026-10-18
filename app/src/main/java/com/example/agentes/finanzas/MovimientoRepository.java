package com.example.agentes.finanzas;

import java.util.List;

/**
 * Contrato de acceso a datos del libro de caja.
 *
 * Hoy lo implementa {@link LocalMovimientoRepository} (almacenamiento en el dispositivo).
 * Cuando la conexión con Supabase (Integrante 1) esté lista, basta con crear una
 * implementación que llame a la tabla {@code movimientos} y cambiarla en
 * {@code FinanzasFragment}; la pantalla no necesita más cambios.
 */
public interface MovimientoRepository {

    interface Callback<T> {
        void onExito(T resultado);
        void onError(String mensaje);
    }

    /** Movimientos del lote indicado (sin orden garantizado). */
    void listarPorLote(String loteId, Callback<List<Movimiento>> callback);

    void agregar(Movimiento movimiento, Callback<Movimiento> callback);
}
