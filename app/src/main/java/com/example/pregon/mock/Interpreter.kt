package com.example.pregon.mock

import com.example.pregon.model.*

sealed interface InterpretationResult {
    data class Success(
        val tool: String,
        val args: Map<String, Any>,
        val confidence: Double
    ) : InterpretationResult

    data class Clarify(
        val question: String,
        val suggestedAnswers: List<String>
    ) : InterpretationResult

    data class Unknown(
        val message: String,
        val suggestions: List<String>
    ) : InterpretationResult
}

object Interpreter {

    fun interpret(query: String): InterpretationResult {
        val normalized = normalize(query)

        // 1. Check out of scope questions (e.g. quién ganó el mundial, recetas, etc.)
        if (isOutOfScope(normalized)) {
            return InterpretationResult.Unknown(
                message = "Todavía no sé responder a eso. Puedo ayudarte con autobuses urbanos, farmacias de guardia, aparcamientos, el tiempo y el calendario de la UEx en Cáceres.",
                suggestions = listOf(
                    "Si quiero llegar a la Politécnica a las 9, ¿a qué hora salgo?",
                    "¿Qué farmacia está de guardia hoy?",
                    "¿Hay sitio en el parking del campus?"
                )
            )
        }

        // 2. Classify intent
        return when {
            // Estimar hora salida
            matchesDeparture(normalized) -> {
                val targetStopId = extractDestinationStop(normalized) ?: "S19" // default to Politécnica
                val targetTime = extractTime(normalized)

                if (targetTime == null) {
                    val stopName = ReferenceData.STOPS_BY_ID[targetStopId]?.name ?: "la universidad"
                    InterpretationResult.Clarify(
                        question = "¿A qué hora quieres llegar a $stopName?",
                        suggestedAnswers = listOf("A las 9:00", "A las 8:30", "A las 10:00")
                    )
                } else {
                    InterpretationResult.Success(
                        tool = "estimar_hora_salida",
                        args = mapOf(
                            "destino" to targetStopId,
                            "hora_llegada_minutos" to targetTime
                        ),
                        confidence = 0.94
                    )
                }
            }

            // Próximas llegadas
            matchesArrivals(normalized) -> {
                val stopId = extractStopId(normalized) ?: "S02"
                InterpretationResult.Success(
                    tool = "proximas_llegadas",
                    args = mapOf("stop_id" to stopId),
                    confidence = 0.92
                )
            }

            // Estado línea
            matchesLineStatus(normalized) -> {
                val lineId = extractLineId(normalized) ?: "L1"
                InterpretationResult.Success(
                    tool = "estado_linea",
                    args = mapOf("linea" to lineId),
                    confidence = 0.91
                )
            }

            // Farmacias de guardia
            matchesPharmacy(normalized) -> {
                InterpretationResult.Success(
                    tool = "farmacias_guardia",
                    args = emptyMap(),
                    confidence = 0.96
                )
            }

            // Aparcamientos
            matchesParking(normalized) -> {
                InterpretationResult.Success(
                    tool = "aparcamientos",
                    args = emptyMap(),
                    confidence = 0.95
                )
            }

            // Tiempo / Lluvia
            matchesWeather(normalized) -> {
                InterpretationResult.Success(
                    tool = "tiempo",
                    args = emptyMap(),
                    confidence = 0.95
                )
            }

            // Calendario UEx
            matchesCalendar(normalized) -> {
                val isTomorrow = normalized.contains("manana")
                InterpretationResult.Success(
                    tool = "calendario_uex",
                    args = mapOf("isTomorrow" to isTomorrow),
                    confidence = 0.93
                )
            }

            else -> {
                InterpretationResult.Unknown(
                    message = "Todavía no sé responder a eso. Puedo ayudarte con autobuses, farmacias de guardia, aparcamientos, el tiempo y el calendario de la UEx.",
                    suggestions = listOf(
                        "Si quiero llegar a la Politécnica a las 9, ¿a qué hora salgo de casa?",
                        "¿Qué farmacia está de guardia hoy?",
                        "¿Hay sitio en el parking del campus?"
                    )
                )
            }
        }
    }

    private fun normalize(text: String): String {
        return text.lowercase()
            .replace("á", "a")
            .replace("é", "e")
            .replace("í", "i")
            .replace("ó", "o")
            .replace("ú", "u")
            .replace("ñ", "n")
            .replace(Regex("[^a-z0-9\\s:]"), " ")
            .trim()
            .replace(Regex("\\s+"), " ")
    }

    private fun isOutOfScope(text: String): Boolean {
        val outOfScopeKeywords = listOf(
            "mundial", "futbol", "quien gano", "chiste", "receta", "pelicula",
            "presidente", "capital de", "musica", "restaurante", "cine", "politica"
        )
        return outOfScopeKeywords.any { text.contains(it) }
    }

    private fun matchesDeparture(text: String): Boolean {
        val keywords = listOf(
            "hora salgo", "a que hora salgo", "para llegar", "llegar a", "llegar a clase",
            "a que hora salir", "hora de salida", "salir de casa", "a las 9", "a las nueve"
        )
        return keywords.any { text.contains(it) } || (text.contains("politecnica") && text.contains("9"))
    }

    private fun matchesArrivals(text: String): Boolean {
        val keywords = listOf(
            "cuando pasa", "proximo bus", "proximas llegadas", "llegadas en", "cuanto le queda", "autobus en"
        )
        return keywords.any { text.contains(it) }
    }

    private fun matchesLineStatus(text: String): Boolean {
        val keywords = listOf(
            "como va la linea", "estado de la linea", "retrasos en la", "avisos", "incidencias l", "como va l1", "como va la l"
        )
        return keywords.any { text.contains(it) }
    }

    private fun matchesPharmacy(text: String): Boolean {
        return text.contains("farmacia") || text.contains("guardia") || text.contains("medicamento")
    }

    private fun matchesParking(text: String): Boolean {
        return text.contains("parking") || text.contains("aparcar") || text.contains("aparcamiento") || text.contains("plazas libres")
    }

    private fun matchesWeather(text: String): Boolean {
        return text.contains("tiempo") || text.contains("lluvia") || text.contains("llover") || text.contains("llueve") || text.contains("paraguas") || text.contains("temperatura")
    }

    private fun matchesCalendar(text: String): Boolean {
        return text.contains("clase") || text.contains("lectivo") || text.contains("festivo") || text.contains("calendario") || text.contains("examenes")
    }

    private fun extractDestinationStop(text: String): String? {
        for (center in ReferenceData.CAMPUS_CENTERS) {
            for (alias in center.aliases) {
                if (text.contains(alias)) return center.stopId
            }
        }
        return null
    }

    private fun extractStopId(text: String): String? {
        val match = Regex("s[0-1][0-9]|s20").find(text)
        if (match != null) return match.value.uppercase()
        if (text.contains("caceres") || text.contains("amberes")) return "S02"
        if (text.contains("america")) return "S07"
        if (text.contains("estacion")) return "S03"
        if (text.contains("politecnica")) return "S19"
        return "S02"
    }

    private fun extractLineId(text: String): String? {
        val match = Regex("l[1-4]|linea [1-4]").find(text)
        if (match != null) {
            val v = match.value
            return if (v.startsWith("l")) v.uppercase() else "L" + v.substringAfter("linea ")
        }
        return "L1"
    }

    private fun extractTime(text: String): Int? {
        // Look for "a las 9", "9:00", "09:00", "8:30", "8 y media", "las 10"
        val colonMatch = Regex("(\\d{1,2}):(\\d{2})").find(text)
        if (colonMatch != null) {
            val h = colonMatch.groupValues[1].toIntOrNull() ?: 9
            val m = colonMatch.groupValues[2].toIntOrNull() ?: 0
            return h * 60 + m
        }

        if (text.contains("8 y media") || text.contains("ocho y media")) return 8 * 60 + 30
        if (text.contains("9 y media") || text.contains("nueve y media")) return 9 * 60 + 30
        if (text.contains("10 menos cuarto")) return 9 * 60 + 45
        if (text.contains("9 menos cuarto")) return 8 * 60 + 45

        val hourMatch = Regex("(?:a las|las|para las)\\s+(\\d{1,2})").find(text)
        if (hourMatch != null) {
            val h = hourMatch.groupValues[1].toIntOrNull() ?: 9
            return h * 60
        }

        if (text.contains("a las 9") || text.contains("a las nueve") || text.contains("a las 09") || text.contains("las 9")) {
            return 9 * 60
        }
        if (text.contains("a las 8") || text.contains("a las ocho")) {
            return 8 * 60
        }
        if (text.contains("a las 10") || text.contains("a las diez")) {
            return 10 * 60
        }

        // Just "9" or "9h" in question
        if (Regex("\\b9\\b|\\b9h\\b").containsMatchIn(text)) {
            return 9 * 60
        }

        return null
    }
}
