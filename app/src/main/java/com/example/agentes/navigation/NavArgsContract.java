package com.example.agentes.navigation;

/**
 * Claves de argumentos congeladas por el Arquitecto.
 * Los módulos (Integrantes 2, 3 y 4) leen las claves desde esta clase
 * y nunca las escriben a mano. El proyecto no usa Safe Args: los
 * argumentos viajan en un Bundle normal.
 */
public final class NavArgsContract {

    /** UUID del lote (String). Destino: loteDetalleFragment. Default: "". */
    public static final String ARG_LOTE_ID = "lote_id";

    /** Indica que Home pide abrir el formulario de nuevo lote. Destino: lotesFragment. Default: false. */
    public static final String ARG_ABRIR_FORMULARIO = "abrir_formulario";

    private NavArgsContract() {
    }
}
