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
 * Implementación real de [AsesorIA]: habla con la Edge Function `asesor-ia`
 * de Supabase, que a su vez llama al proveedor (Gemini) con su clave secreta.
 *
 * <p><b>Seguridad:</b> la app NUNCA contiene la clave del proveedor; solo usa la
 * `anon key` pública del proyecto y el token de la sesión. El mismo patrón de red
 * de [com.example.agentes.data.LoteRepository]: [HttpURLConnection] en un hilo de
 * IO, sin dependencias nuevas.</p>
 */
object AsesorIAApi : AsesorIA {

    private const val RUTA = "/functions/v1/asesor-ia"
    private const val TIEMPO_MS = 20_000

    /** Mensajes mostrados en la burbuja de error / aviso de la UI. */
    const val MSG_SIN_SESION = "Tu sesión ya no es válida. Vuelve a iniciar sesión."
    const val MSG_LIMITE = "Demasiadas consultas seguidas. Intenta en un momento."
    const val MSG_SERVICIO =
        "El asesor no está disponible. Revisa tu conexión e intenta de nuevo."
    const val MSG_VACIO = "El asesor no devolvió respuesta."

    /** Consulta sin historial (contrato base de [AsesorIA]). */
    override suspend fun preguntar(mensaje: String): ResultadoIA =
        preguntar(mensaje, emptyList())

    override suspend fun preguntar(
        mensaje: String,
        historial: List<MensajeChat>
    ): ResultadoIA = withContext(Dispatchers.IO) {
        var conexion: HttpURLConnection? = null
        try {
            val url = URL(SupabaseConfig.SUPABASE_URL + RUTA)
            conexion = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                // apikey: clave anon pública del proyecto.
                // Authorization: token de la sesión (o la anon key como fallback).
                setRequestProperty("apikey", SupabaseConfig.SUPABASE_ANON_KEY)
                setRequestProperty(
                    "Authorization",
                    "Bearer " + SupabaseConfig.getAccessToken()
                )
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Accept", "application/json")
                connectTimeout = TIEMPO_MS
                readTimeout = TIEMPO_MS
                doOutput = true
            }

            val salida: OutputStream = conexion.outputStream
            salida.write(
                cuerpoPeticion(mensaje, historial).toString()
                    .toByteArray(StandardCharsets.UTF_8)
            )
            salida.flush()
            salida.close()

            val codigo = conexion.responseCode
            val cuerpo = leerRespuesta(
                if (codigo < 400) conexion.inputStream else conexion.errorStream
            )
            interpretarRespuesta(codigo, cuerpo)
        } catch (e: Exception) {
            // Nunca se lanza una excepción hacia el ViewModel.
            ResultadoIA(exito = false, texto = null, error = MSG_SERVICIO)
        } finally {
            conexion?.disconnect()
        }
    }

    /**
     * Traduce un código HTTP y el cuerpo de la respuesta a [ResultadoIA].
     * Función pura y sin dependencias de Android: testeable desde Java.
     */
    @JvmStatic
    fun interpretarRespuesta(codigo: Int, cuerpo: String?): ResultadoIA {
        if (codigo in 200..299) {
            val texto = try {
                JSONObject(cuerpo ?: "").optString("texto", "").trim()
            } catch (e: Exception) {
                ""
            }
            return if (texto.isEmpty()) {
                ResultadoIA(exito = false, texto = null, error = MSG_VACIO)
            } else {
                ResultadoIA(exito = true, texto = texto)
            }
        }
        val error = when (codigo) {
            401, 403 -> MSG_SIN_SESION
            429 -> MSG_LIMITE
            else -> MSG_SERVICIO
        }
        return ResultadoIA(exito = false, texto = null, error = error)
    }

    /** Cuerpo JSON de la consulta: mensaje, usuario e historial reciente. */
    private fun cuerpoPeticion(mensaje: String, historial: List<MensajeChat>): JSONObject {
        val cuerpo = JSONObject()
        cuerpo.put("mensaje", mensaje)
        cuerpo.put("usuario_id", SupabaseConfig.getUsuarioId())
        val arreglo = JSONArray()
        historial.forEach { m ->
            val item = JSONObject()
            item.put("texto", m.texto)
            item.put("deUsuario", m.esUsuario)
            arreglo.put(item)
        }
        cuerpo.put("historial", arreglo)
        return cuerpo
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
