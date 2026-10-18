package com.agrocontrol.app.asesor.data

data class MensajeChat(
    val texto: String,
    val esUsuario: Boolean,
    val estado: EstadoMensaje = EstadoMensaje.ENVIADO
)
