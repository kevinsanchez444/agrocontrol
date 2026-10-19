package com.agrocontrol.app.asesor.data

data class ResultadoIA(
    val exito: Boolean,
    val texto: String?,
    val error: String? = null
)

interface AsesorIA {
    suspend fun preguntar(mensaje: String): ResultadoIA

    /**
     * Consulta con el historial reciente de la conversación (Fase 3).
     * Por defecto delega en [preguntar] sin contexto, de modo que el mock y
     * cualquier doble de test siguen funcionando sin cambios.
     */
    suspend fun preguntar(mensaje: String, historial: List<MensajeChat>): ResultadoIA =
        preguntar(mensaje)
}
