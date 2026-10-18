package com.example.agentes.ui.lotes;

import android.os.Bundle;

import com.example.agentes.model.Lote;

/**
 * Contrato acordado con el Integrante 5 (interfaz principal y navegación).
 *
 * <p>Define los nombres de las pantallas del módulo de lotes y el parámetro que se envía
 * al abrir el detalle de un lote. El Integrante 5 es quien crea los destinos y las
 * acciones en {@code nav_graph.xml}; el Integrante 3 es quien implementa el detalle.</p>
 *
 * <p>Ver {@code docs/contrato-lotes.md} para el fragmento de navegación listo para pegar.</p>
 */
public final class LotesContrato {

    /** Fragment que muestra el listado de lotes (Integrante 2). */
    public static final String DESTINO_LOTES =
            "com.example.agentes.ui.lotes.LotesFragment";

    /** Formulario de nuevo lote (Integrante 2). */
    public static final String DESTINO_NUEVO_LOTE =
            "com.example.agentes.ui.lotes.NuevoLoteFragment";

    /** Pantalla de detalle del lote (la implementa el Integrante 3). */
    public static final String DESTINO_DETALLE_LOTE =
            "com.example.agentes.ui.finanzas.DetalleLoteFragment";

    /** Identificadores de acción sugeridos para el nav_graph. */
    public static final String ACCION_LOTES_A_NUEVO_LOTE = "accionLotesANuevoLote";
    public static final String ACCION_LOTES_A_DETALLE_LOTE = "accionLotesADetalleLote";

    /** Clave del argumento obligatorio que recibe la pantalla de detalle. */
    public static final String ARG_LOTE_ID = "lote_id";

    /** Clave informativa con el nombre del lote (para el título del detalle). */
    public static final String ARG_LOTE_NOMBRE = "lote_nombre";

    /**
     * Clave informativa con el cultivo del lote (se muestra en una etiqueta estilo chip,
     * igual que en la tarjeta de la lista).
     */
    public static final String ARG_LOTE_CULTIVO = "lote_cultivo";

    /**
     * Clave informativa con las hectáreas del lote como texto crudo (sin formatear);
     * el formateo se hace en la vista del detalle.
     */
    public static final String ARG_LOTE_HECTARES = "lote_hectareas";

    /** Clave con la que el formulario avisa que guardó un lote y hay que recargar la lista. */
    public static final String RESULT_NUEVO_LOTE = "resultado_nuevo_lote";

    /**
     * Clave del argumento (Serializable) con el que la hoja inferior recibe el {@link Lote}
     * a editar; si está presente, el formulario abre en modo edición prellenado.
     */
    public static final String ARG_LOTE_EDITAR = "lote_editar";

    /**
     * Clave del FragmentResult con el que la hoja inferior devuelve a la pantalla de
     * detalle el {@link Lote} ya actualizado en el servidor.
     */
    public static final String ARG_RESULT_LOTE = "resultado_lote_actualizado";

    private LotesContrato() {
    }

    /** Arma los argumentos que debe recibir la pantalla de detalle al seleccionar un lote. */
    public static Bundle argumentosDetalleLote(Lote lote) {
        Bundle argumentos = new Bundle();
        argumentos.putString(ARG_LOTE_ID, lote.getId());
        argumentos.putString(ARG_LOTE_NOMBRE, lote.getNombre());
        argumentos.putString(ARG_LOTE_CULTIVO, lote.getCultivo());
        argumentos.putString(ARG_LOTE_HECTARES, String.valueOf(lote.getHectareas()));
        return argumentos;
    }

    /** Instancia la hoja inferior en modo edición, prellenada con el lote recibido. */
    public static NuevoLoteFragment nuevaHojaEdicion(Lote lote) {
        NuevoLoteFragment hoja = new NuevoLoteFragment();
        Bundle argumentos = new Bundle();
        argumentos.putSerializable(ARG_LOTE_EDITAR, lote);
        hoja.setArguments(argumentos);
        return hoja;
    }
}
