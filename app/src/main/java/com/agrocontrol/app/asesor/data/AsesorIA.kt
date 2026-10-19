package com.agrocontrol.app.asesor.data

data class ResultadoIA(
    val exito: Boolean,
    val texto: String?,
    val error: String? = null
)

interface AsesorIA {
    suspend fun preguntar(mensaje: String): ResultadoIA
}
