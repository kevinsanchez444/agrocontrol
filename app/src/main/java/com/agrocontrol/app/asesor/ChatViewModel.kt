
package com.agrocontrol.app.asesor

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agrocontrol.app.asesor.data.AsesorIA
import com.agrocontrol.app.asesor.data.AsesorIAApi
import com.agrocontrol.app.asesor.data.EstadoMensaje
import com.agrocontrol.app.asesor.data.MensajeChat
import com.agrocontrol.app.asesor.data.ResultadoIA
import kotlinx.coroutines.launch

/**
 * Estado del chat del Asesor IA.
 *
 * <p>[asesor] se inyecta por constructor para poder sustituirlo por
 * [com.agrocontrol.app.asesor.data.AsesorIAMock] (o cualquier doble) en los tests.
 * Por defecto usa la implementación real [AsesorIAApi] (Edge Function → Gemini);
 * la clave del proveedor nunca vive en la app.</p>
 */
class ChatViewModel(private val asesor: AsesorIA = AsesorIAApi) : ViewModel() {

    private val _mensajes =
        MutableLiveData<List<MensajeChat>>(emptyList())

    val mensajes: LiveData<List<MensajeChat>> = _mensajes

    private val _error = MutableLiveData<String?>(null)
    val error: LiveData<String?> = _error

    private var ultimaConsultaFallida: String? = null

    fun enviar(texto: String) {
        _error.value = null

        val actual = _mensajes.value ?: emptyList()
        // Contexto (Fase 3): se captura ANTES de añadir el mensaje nuevo, sin las
        // burbujas de error, y acotado a las últimas 8 para limitar el prompt.
        val historial = actual
            .filter { it.estado != EstadoMensaje.ERROR }
            .takeLast(8)
        val nuevo = actual +
                listOf(MensajeChat(texto, true, EstadoMensaje.ENVIANDO))

        _mensajes.value = nuevo

        viewModelScope.launch {
            val resultado: ResultadoIA = asesor.preguntar(texto, historial)

            val actual2 = _mensajes.value ?: emptyList()
            val mutable = actual2.toMutableList()

            val ultimoIndex = mutable.size - 1
            mutable[ultimoIndex] =
                MensajeChat(texto, true, EstadoMensaje.ENVIADO)

            if (resultado.exito) {
                mutable.add(
                    MensajeChat(
                        resultado.texto ?: "",
                        false,
                        EstadoMensaje.ENVIADO
                    )
                )
                ultimaConsultaFallida = null
            } else {
                mutable.add(
                    MensajeChat(
                        resultado.error ?: "Error desconocido",
                        false,
                        EstadoMensaje.ERROR
                    )
                )
                _error.value = resultado.error
                ultimaConsultaFallida = texto
            }

            _mensajes.value = mutable
        }
    }

    fun reintentarUltima() {
        _error.value = null
        ultimaConsultaFallida?.let { enviar(it) }
    }
}
