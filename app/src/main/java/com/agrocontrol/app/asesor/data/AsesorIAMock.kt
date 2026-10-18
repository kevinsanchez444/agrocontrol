package com.agrocontrol.app.asesor.data

import kotlinx.coroutines.delay

object AsesorIAMock : AsesorIA {

    override suspend fun preguntar(mensaje: String): ResultadoIA {
        // Simula tiempo de red
        delay(1200)

        val texto = mensaje.lowercase()

        return when {
            texto.contains("café") || texto.contains("cafe") -> {
                ResultadoIA(
                    exito = true,
                    texto = """Las principales plagas del café son la broca del café (Hypothenemus hampei) y la roya (Hemileia vastatrix).
- Broca: revisa los frutos periódicamente y usa trampas atractivas.
- roya: aplica fungicidas preventivos en épocas de humedad y poda para mejorar la aireación.
Siempre sigue la etiqueta del producto y consulta a un ingeniero agrónomo."""
                )
            }
            texto.contains("plátano") || texto.contains("platano") -> {
                ResultadoIA(
                    exito = true,
                    texto = """Para la fertilización del plátano se recomienda un abono balanceado con énfasis en potasio.
- Aplica NPK según análisis de suelo.
- El potasio mejora el tamaño y calidad del racimo.
- Divide las aplicaciones en 3-4 veces durante el ciclo."""
                )
            }
            texto.contains("aguacate") -> {
                ResultadoIA(
                    exito = true,
                    texto = """El aguacate necesita riego regular pero sin encharcar.
- Riega 2-3 veces por semana en verano, menos en invierno.
- Asegura buen drenaje; el exceso de agua provoca pudrición de raíces.
- Mulch (cubierta orgánica) ayuda a conservar la humedad."""
                )
            }
            else -> {
                ResultadoIA(
                    exito = true,
                    texto = "Lo siento, solo atiendo consultas técnicas sobre café, plátano y aguacate. ¿Tienes alguna duda sobre uno de esos cultivos?"
                )
            }
        }
    }
}
