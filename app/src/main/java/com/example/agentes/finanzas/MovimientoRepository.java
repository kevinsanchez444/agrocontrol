package com.example.agentes.finanzas;

import java.util.List;

/**
 * Contrato de acceso a datos del libro de caja.
 *
 * <p><b>Implementación activa:</b> {@link SupabaseMovimientoRepository}, que habla con la tabla
 * {@code movimientos} de Supabase (lectura con filtro {@code usuario_id=eq.<sesión>}, escritura
 * con {@code usuario_id} de la sesión y {@code categoria_id} resuelto contra el catálogo
 * {@code categorias}). Permisos y políticas RLS MVP en
 * {@code supabase/movimientos_permisos.sql}.</p>
 *
 * <p><b>Respaldo:</b> {@link LocalMovimientoRepository} (SharedPreferences en el dispositivo)
 * sigue disponible y documentado como alternativa sin red; la pantalla
 * ({@code FinanzasFragment}) solo cambia la línea donde instancia esta interfaz.</p>
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
