
package com.agrocontrol.app.asesor

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agrocontrol.app.asesor.data.AsesorIA
import com.agrocontrol.app.asesor.data.AsesorIAApi
import com.agrocontrol.app.asesor.data.EstadoMensaje
import com.agrocontrol.app.asesor.data.MensajeChat
import com.agrocontrol.app.asesor.data.MensajesChatRepository
import com.agrocontrol.app.asesor.data.MensajesChatSupabase
import com.agrocontrol.app.asesor.data.ResultadoIA
import kotlinx.coroutines.launch

/**
 * Estado del chat del Asesor IA.
 *
 * <p>[asesor] se inyecta por constructor para poder sustituirlo por
 * [com.agrocontrol.app.asesor.data.AsesorIAMock] (o cualquier doble) en los tests.
 * Por defecto usa la implementación real [AsesorIAApi] (Edge Function → Gemini);
 * la clave del proveedor nunca vive en la app.</p>
 *
 * <p>[chat] persiste el historial en Supabase (fuente de verdad de la
 * conversación): al crearse el ViewModel se recarga lo guardado y cada mensaje
 * nuevo se escribe. Al tener todos los parámetros valor por defecto sigue
 * existiendo el constructor sin argumentos para Java ({@code new ChatViewModel()}).</p>
 */
class ChatViewModel(
    private val asesor: AsesorIA = AsesorIAApi,
    private val chat: MensajesChatRepository = MensajesChatSupabase
) : ViewModel() {

    private val _mensajes =
        MutableLiveData<List<MensajeChat>>(emptyList())

    val mensajes: LiveData<List<MensajeChat>> = _mensajes

    private val _error = MutableLiveData<String?>(null)
    val error: LiveData<String?> = _error

    private var ultimaConsultaFallida: String? = null

    init {
        cargarHistorial()
    }

    /**
     * Recupera el historial guardado en Supabase. Solo publica la lista si hay
     * resultados Y el chat todavía está vacío, para no pisar los mensajes que
     * el usuario ya haya escrito mientras llegaba la respuesta de red.
     */
    private fun cargarHistorial() {
        viewModelScope.launch {
            val guardados = chat.cargar(50)
            val actual = _mensajes.value ?: emptyList()
            if (guardados.isNotEmpty() && actual.isEmpty()) {
                _mensajes.value = guardados
            }
        }
    }

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

            // Persistencia (fuente de verdad): primero el mensaje del usuario y
            // después la respuesta de la IA, solo si fue exitosa. Las burbujas
            // de ERROR no se guardan.
            chat.guardar(MensajeChat(texto, true, EstadoMensaje.ENVIADO))
            if (resultado.exito) {
                chat.guardar(
                    MensajeChat(resultado.texto ?: "", false, EstadoMensaje.ENVIADO)
                )
            }

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

    /**
     * Borra la conversación: limpia el estado en memoria (burbujas, error y
     * último fallo) y elimina el historial del usuario en Supabase.
     */
    fun limpiarConversacion() {
        _error.value = null
        ultimaConsultaFallida = null
        _mensajes.value = emptyList()
        viewModelScope.launch {
            chat.borrarTodo()
        }
    }
}
