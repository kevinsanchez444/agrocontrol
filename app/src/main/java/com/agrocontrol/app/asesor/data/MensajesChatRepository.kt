package com.agrocontrol.app.asesor.data

import com.example.agentes.data.SupabaseConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets

/**
 * Persistencia del historial del chat del Asesor IA.
 *
 * <p>Es la fuente de verdad de la conversación (igual que lotes y
 * movimientos): al abrir la pestaña Asesor se recarga el historial guardado
 * y cada mensaje nuevo se escribe en Supabase. La interfaz permite
 * sustituir la implementación por un doble en los tests.</p>
 */
interface MensajesChatRepository {

    /** Devuelve los últimos mensajes del usuario, del más antiguo al más nuevo. */
    suspend fun cargar(limite: Int = 50): List<MensajeChat>

    /** Guarda un mensaje del historial. Devuelve {@code true} si se persistió. */
    suspend fun guardar(mensaje: MensajeChat): Boolean

    /** Borra todo el historial del usuario de la sesión. */
    suspend fun borrarTodo(): Boolean
}

/**
 * Implementación real de [MensajesChatRepository] contra la tabla
 * {@code public.mensajes_chat} de Supabase (PostgREST).
 *
 * <p><b>Seguridad:</b> la app solo usa la {@code anon key} pública y el token
 * de la sesión; nunca una clave privada. Mismo patrón de red que
 * [AsesorIAApi] y [com.example.agentes.data.LoteRepository]:
 * [HttpURLConnection] en un hilo de IO, sin dependencias nuevas. Ningún
 * método propaga excepciones: si algo falla, el chat sigue funcionando en
 * memoria y simplemente no se persiste.</p>
 *
 * <p>Sin sesión ([SupabaseConfig.getUsuarioId] vacío) no se llama a la red:
 * no hay usuario al que asociar el historial.</p>
 */
object MensajesChatSupabase : MensajesChatRepository {

    private const val RUTA = "/rest/v1/mensajes_chat"
    private const val COLUMNAS = "texto,de_usuario"
    private const val TIEMPO_MS = 15_000

    override suspend fun cargar(limite: Int): List<MensajeChat> =
        withContext(Dispatchers.IO) {
            val usuarioId = SupabaseConfig.getUsuarioId()
            if (usuarioId.isNullOrEmpty()) {
                return@withContext emptyList()
            }
            var conexion: HttpURLConnection? = null
            try {
                val ruta = RUTA +
                        "?select=" + COLUMNAS +
                        "&usuario_id=eq." + usuarioId +
                        "&order=created_at.asc" +
                        "&limit=" + limite
                conexion = abrirConexion("GET", ruta)
                val codigo = conexion.responseCode
                if (codigo in 200..299) {
                    parsear(leerRespuesta(conexion.inputStream))
                } else {
                    emptyList()
                }
            } catch (e: Exception) {
                // Sin historial en pantalla, pero la conversación sigue en memoria.
                emptyList()
            } finally {
                conexion?.disconnect()
            }
        }

    override suspend fun guardar(mensaje: MensajeChat): Boolean =
        withContext(Dispatchers.IO) {
            val usuarioId = SupabaseConfig.getUsuarioId()
            if (usuarioId.isNullOrEmpty()) {
                return@withContext false
            }
            var conexion: HttpURLConnection? = null
            try {
                val cuerpo = JSONObject()
                cuerpo.put("usuario_id", usuarioId)
                cuerpo.put("texto", mensaje.texto)
                cuerpo.put("de_usuario", mensaje.esUsuario)

                conexion = abrirConexion("POST", RUTA)
                conexion.doOutput = true
                // No se necesita la fila insertada: solo confirmar la escritura.
                conexion.setRequestProperty("Prefer", "return=minimal")

                val salida: OutputStream = conexion.outputStream
                salida.write(cuerpo.toString().toByteArray(StandardCharsets.UTF_8))
                salida.flush()
                salida.close()

                conexion.responseCode in 200..299
            } catch (e: Exception) {
                false
            } finally {
                conexion?.disconnect()
            }
        }

    override suspend fun borrarTodo(): Boolean = withContext(Dispatchers.IO) {
        val usuarioId = SupabaseConfig.getUsuarioId()
        if (usuarioId.isNullOrEmpty()) {
            return@withContext false
        }
        var conexion: HttpURLConnection? = null
        try {
            val ruta = RUTA + "?usuario_id=eq." + usuarioId
            conexion = abrirConexion("DELETE", ruta)
            conexion.setRequestProperty("Prefer", "return=minimal")
            conexion.responseCode in 200..299
        } catch (e: Exception) {
            false
        } finally {
            conexion?.disconnect()
        }
    }

    /**
     * Convierte el JSON de PostgREST (arreglo de filas con {@code texto} y
     * {@code de_usuario}) en la lista de mensajes del chat. Función pura y sin
     * dependencias de Android: testeable desde Java. Ignora filas sin texto e
     * interpreta un JSON inválido como historial vacío (nunca lanza).
     */
    @JvmStatic
    fun parsear(cuerpo: String?): List<MensajeChat> {
        if (cuerpo == null) {
            return emptyList()
        }
        return try {
            val arreglo = JSONArray(cuerpo)
            val mensajes = ArrayList<MensajeChat>(arreglo.length())
            for (i in 0 until arreglo.length()) {
                val fila = arreglo.optJSONObject(i) ?: continue
                val texto = fila.optString("texto", "").trim()
                if (texto.isEmpty()) {
                    continue
                }
                mensajes.add(
                    MensajeChat(
                        texto = texto,
                        esUsuario = fila.optBoolean("de_usuario", true),
                        estado = EstadoMensaje.ENVIADO
                    )
                )
            }
            mensajes
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun abrirConexion(metodo: String, ruta: String): HttpURLConnection {
        val url = URL(SupabaseConfig.SUPABASE_URL + ruta)
        val conexion = url.openConnection() as HttpURLConnection
        conexion.requestMethod = metodo
        // apikey: clave anon pública del proyecto.
        // Authorization: token de la sesión (o la anon key como fallback).
        conexion.setRequestProperty("apikey", SupabaseConfig.SUPABASE_ANON_KEY)
        conexion.setRequestProperty(
            "Authorization",
            "Bearer " + SupabaseConfig.getAccessToken()
        )
        conexion.setRequestProperty("Content-Type", "application/json")
        conexion.setRequestProperty("Accept", "application/json")
        conexion.connectTimeout = TIEMPO_MS
        conexion.readTimeout = TIEMPO_MS
        return conexion
    }

    private fun leerRespuesta(flujo: InputStream?): String {
        if (flujo == null) return ""
        val texto = StringBuilder()
        val lector = BufferedReader(InputStreamReader(flujo, StandardCharsets.UTF_8))
        try {
            var linea = lector.readLine()
            while (linea != null) {
                texto.append(linea)
                linea = lector.readLine()
            }
        } finally {
            lector.close()
        }
        return texto.toString()
    }
}
