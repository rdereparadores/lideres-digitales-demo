package com.example.pregon.mock

import com.example.pregon.model.*

class PregonEngine {

    var demoState: DemoState = DemoState()
        private set

    fun updateDemoState(newState: DemoState) {
        demoState = newState
    }

    fun setClock(now: SimTime, mode: String) {
        demoState = demoState.copy(now = now, mode = mode)
    }

    fun toggleScenario(scenario: String) {
        val current = demoState.scenarios.toMutableSet()
        if (current.contains(scenario)) {
            current.remove(scenario)
        } else {
            // Remove other conflicting line missing scenarios if adding one
            if (scenario.startsWith("missing_bus")) {
                current.removeAll { it.startsWith("missing_bus") }
            }
            current.add(scenario)
        }
        demoState = demoState.copy(scenarios = current)
    }

    fun clearScenarios() {
        demoState = demoState.copy(scenarios = emptySet())
    }

    fun simulateTomorrow(): DemoState {
        val nextDate = CalendarUtils.getNextLectiveDay(demoState.now.date)
        demoState = DemoState(
            now = SimTime(date = nextDate, minutes = 7 * 60 + 30), // 07:30
            mode = "frozen",
            scenarios = emptySet(),
            missingBusLine = "L1"
        )
        return demoState
    }

    fun resetDemo(): DemoState {
        demoState = DemoState()
        return demoState
    }

    data class EngineResponse(
        val answer: String,
        val cards: List<CardData>,
        val sources: List<Provenance>,
        val trace: List<TraceStep>,
        val persistOffer: PersistOffer?,
        val thinkingSteps: List<String>
    )

    fun processQuery(
        query: String,
        homePreset: NeighborhoodPreset
    ): EngineResponse {
        val simNow = demoState.now
        val scenarios = demoState.scenarios
        val interpretation = Interpreter.interpret(query)

        val sourcesAll = ReferenceData.createSources(simNow)

        return when (interpretation) {
            is InterpretationResult.Unknown -> {
                EngineResponse(
                    answer = interpretation.message,
                    cards = emptyList(),
                    sources = emptyList(),
                    trace = listOf(
                        TraceStep(
                            kind = "interpret",
                            label = "Análisis de intención",
                            args = "{\"consulta\": \"$query\"}",
                            resultSummary = "Sin correspondencia con herramientas urbanas disponibles",
                            ms = 110
                        )
                    ),
                    persistOffer = null,
                    thinkingSteps = listOf("Entendiendo la pregunta…", "Verificando dominios de datos…", "Respuesta directa")
                )
            }

            is InterpretationResult.Clarify -> {
                EngineResponse(
                    answer = interpretation.question,
                    cards = emptyList(),
                    sources = emptyList(),
                    trace = listOf(
                        TraceStep(
                            kind = "interpret",
                            label = "Falta entidad obligatoria",
                            args = "{\"parametro_faltante\": \"hora_objetivo\"}",
                            resultSummary = "Solicitando aclaración al usuario",
                            ms = 95
                        )
                    ),
                    persistOffer = null,
                    thinkingSteps = listOf("Entendiendo la pregunta…", "Identificando destino…", "Solicitando hora de llegada")
                )
            }

            is InterpretationResult.Success -> {
                when (interpretation.tool) {
                    "estimar_hora_salida" -> {
                        val destId = (interpretation.args["destino"] as? String) ?: "S19"
                        val targetMin = (interpretation.args["hora_llegada_minutos"] as? Int) ?: (9 * 60)
                        val est = Estimator.estimateDeparture(
                            destinationStopId = destId,
                            targetArrivalMinutes = targetMin,
                            simNow = simNow,
                            scenarios = scenarios,
                            origins = homePreset.originStops
                        )

                        val diff = est.normalLeaveHomeMinutes - (est.chosenBus.busPassTime.substringBefore(":").toInt() * 60 +
                                est.chosenBus.busPassTime.substringAfter(":").toInt() - est.chosenBus.walkMinutes - est.chosenBus.bufferMinutes - est.chosenBus.rainBufferMinutes)

                        val answerText = "Sal a las ${est.chosenBus.leaveHomeTime} para llegar a ${est.chosenBus.destinationStopName} a las ${est.chosenBus.targetArrivalTime}." +
                                if (est.chosenBus.comparisonText != "Salida habitual") " (${est.chosenBus.comparisonText}: ${est.chosenBus.reason})." else " La ${est.chosenBus.busLine} pasa a las ${est.chosenBus.busPassTime} por ${est.chosenBus.originStopName}."

                        val relSources = sourcesAll.filter {
                            it.datasetId in listOf("buses_red", "buses_flota", "buses_afluencia", "buses_historico", "trafico_caceres")
                        }

                        val destName = ReferenceData.STOPS_BY_ID[destId]?.name ?: "Campus"
                        EngineResponse(
                            answer = answerText,
                            cards = listOf(est.chosenBus),
                            sources = relSources,
                            trace = est.traceSteps,
                            persistOffer = est.persistOffer,
                            thinkingSteps = listOf(
                                "Entendiendo la pregunta…",
                                "Buscando '$destName' → parada $destId",
                                "Consultando flota en tiempo real y frecuencias…",
                                "Calculando hora óptima de salida…"
                            )
                        )
                    }

                    "proximas_llegadas" -> {
                        val stopId = (interpretation.args["stop_id"] as? String) ?: homePreset.originStops.firstOrNull()?.stopId ?: "S02"
                        val (card, trace) = Tools.proximasLlegadas(stopId, simNow, scenarios)
                        val firstArrival = card.arrivals.firstOrNull()
                        val answerText = if (firstArrival != null) {
                            "Próximo autobús en ${card.stopName}: ${firstArrival.line} en ${firstArrival.minutesAway} min hacia ${firstArrival.destination}."
                        } else {
                            "No hay autobuses previstos en los próximos minutos para ${card.stopName}."
                        }
                        val relSources = sourcesAll.filter { it.datasetId in listOf("buses_red", "buses_flota", "buses_afluencia") }
                        EngineResponse(
                            answer = answerText,
                            cards = listOf(card),
                            sources = relSources,
                            trace = trace,
                            persistOffer = PersistOffer(
                                tool = "proximas_llegadas",
                                argsSummary = "Parada ${card.stopName}",
                                suggestedTitle = "Llegadas en ${card.stopName}",
                                suggestedSchedule = Schedule(fromMinutes = 7 * 60, toMinutes = 22 * 60)
                            ),
                            thinkingSteps = listOf("Entendiendo la pregunta…", "Identificando parada ${card.stopName}…", "Consultando tiempos de paso en vivo")
                        )
                    }

                    "estado_linea" -> {
                        val lineId = (interpretation.args["linea"] as? String) ?: "L1"
                        val (card, trace) = Tools.estadoLinea(lineId, simNow, scenarios)
                        val answerText = "Estado de la ${card.line}: ${card.activeBuses} de ${card.scheduledBuses} autobuses activos. ${card.alerts.firstOrNull() ?: "Servicio normal"}."
                        val relSources = sourcesAll.filter { it.datasetId in listOf("buses_red", "buses_flota", "trafico_caceres") }
                        EngineResponse(
                            answer = answerText,
                            cards = listOf(card),
                            sources = relSources,
                            trace = trace,
                            persistOffer = PersistOffer(
                                tool = "estado_linea",
                                argsSummary = "Línea ${card.line}",
                                suggestedTitle = "Incidencias ${card.line}",
                                suggestedSchedule = Schedule(fromMinutes = 7 * 60, toMinutes = 21 * 60)
                            ),
                            thinkingSteps = listOf("Entendiendo la pregunta…", "Consultando línea ${card.line}…", "Verificando puntualidad y alertas")
                        )
                    }

                    "farmacias_guardia" -> {
                        val (card, trace) = Tools.farmaciasGuardia(simNow, homePreset.lat, homeLon = homePreset.lon)
                        val answerText = "La farmacia de guardia hoy en Cáceres es ${card.onDutyName} (${card.onDutyAddress}), abierta hasta las ${card.onDutyHours}. Está a ${card.distanceKm} km de tu ubicación."
                        val relSources = sourcesAll.filter { it.datasetId == "farmacias_colegio" }
                        EngineResponse(
                            answer = answerText,
                            cards = listOf(card),
                            sources = relSources,
                            trace = trace,
                            persistOffer = PersistOffer(
                                tool = "farmacias_guardia",
                                argsSummary = "Farmacia de guardia hoy",
                                suggestedTitle = "Farmacia de guardia",
                                suggestedSchedule = Schedule(fromMinutes = 8 * 60, toMinutes = 23 * 60)
                            ),
                            thinkingSteps = listOf("Entendiendo la pregunta…", "Consultando calendario del Colegio de Farmacéuticos…", "Calculando distancia desde Mi Casa")
                        )
                    }

                    "aparcamientos" -> {
                        val (card, trace) = Tools.aparcamientos(scenarios)
                        val answerText = "En el Campus UEx hay ${card.campusHighlight.freeSpots} plazas libres de ${card.campusHighlight.totalSpots} (${card.campusHighlight.occupancyPercent}% de ocupación)."
                        val relSources = sourcesAll.filter { it.datasetId == "parkings_ciudad" }
                        EngineResponse(
                            answer = answerText,
                            cards = listOf(card),
                            sources = relSources,
                            trace = trace,
                            persistOffer = PersistOffer(
                                tool = "aparcamientos",
                                argsSummary = "Aforo Parking Campus UEx",
                                suggestedTitle = "Plazas Campus UEx",
                                suggestedSchedule = Schedule(fromMinutes = 7 * 60 + 30, toMinutes = 14 * 60)
                            ),
                            thinkingSteps = listOf("Entendiendo la pregunta…", "Consultando sensores de aparcamiento…", "Calculando disponibilidad")
                        )
                    }

                    "tiempo" -> {
                        val (card, trace) = Tools.tiempo(scenarios)
                        val answerText = "Tiempo en Cáceres: ${card.tempCurrent}°C, ${card.condition.lowercase()}. ${card.umbrellaAlert}."
                        val relSources = sourcesAll.filter { it.datasetId == "aemet_clima" }
                        EngineResponse(
                            answer = answerText,
                            cards = listOf(card),
                            sources = relSources,
                            trace = trace,
                            persistOffer = PersistOffer(
                                tool = "tiempo",
                                argsSummary = "Previsión y lluvia Cáceres",
                                suggestedTitle = "El tiempo en Cáceres",
                                suggestedSchedule = Schedule(fromMinutes = 7 * 60, toMinutes = 21 * 60)
                            ),
                            thinkingSteps = listOf("Entendiendo la pregunta…", "Consultando previsión AEMET…", "Evaluando probabilidad de lluvia")
                        )
                    }

                    "calendario_uex" -> {
                        val (card, trace) = Tools.calendarioUex(simNow)
                        val answerText = if (card.isLective) {
                            "Sí, hoy ${card.date} es día lectivo en la Universidad de Extremadura. Hay docencia normal."
                        } else {
                            "Hoy no hay clases en la Universidad de Extremadura (${card.dayTypeTitle}). Próximo festivo: ${card.nextHoliday}."
                        }
                        val relSources = sourcesAll.filter { it.datasetId == "calendario_academico" }
                        EngineResponse(
                            answer = answerText,
                            cards = listOf(card),
                            sources = relSources,
                            trace = trace,
                            persistOffer = null, // No persistence required for calendar
                            thinkingSteps = listOf("Entendiendo la pregunta…", "Consultando calendario oficial UEx…", "Verificando día lectivo")
                        )
                    }

                    else -> {
                        EngineResponse(
                            answer = "No he podido procesar la solicitud.",
                            cards = emptyList(),
                            sources = emptyList(),
                            trace = emptyList(),
                            persistOffer = null,
                            thinkingSteps = listOf("Entendiendo la pregunta…")
                        )
                    }
                }
            }
        }
    }

    /**
     * Executes a persistent query without using LLM / without NLP interpreter!
     */
    fun runPersistentQuery(
        savedQuery: SavedQuery,
        homePreset: NeighborhoodPreset
    ): QueryLatest {
        val simNow = demoState.now
        val sched = savedQuery.schedule
        val todayDowLetter = simNow.dayLetter()

        val isTodayScheduled = sched.days.contains(todayDowLetter)
        val isLectiveToday = CalendarUtils.isLective(simNow.date)

        val sourcesAll = ReferenceData.createSources(simNow)

        // 1. Outside scheduled days
        if (!isTodayScheduled) {
            return QueryLatest(
                title = savedQuery.title,
                headline = "Hoy no te toca",
                subtitle = "Programada para días ${sched.days.joinToString("–")}",
                status = "idle",
                updatedAt = simNow,
                sources = sourcesAll.take(2),
                trace = listOf(
                    TraceStep(
                        kind = "compute",
                        label = "Ejecutada sin LLM: filtro de horario programado",
                        resultSummary = "Hoy ($todayDowLetter) no coincide con los días activos (${sched.days.joinToString(",")})",
                        ms = 8
                    )
                )
            )
        }

        // 2. Only lective check
        if (sched.onlyLective && !isLectiveToday) {
            val dayInfo = CalendarUtils.getDayInfo(simNow.date)
            return QueryLatest(
                title = savedQuery.title,
                headline = "Hoy no hay clase",
                subtitle = dayInfo.title + (if (dayInfo.holidayName != null) " (${dayInfo.holidayName})" else ""),
                status = "idle",
                updatedAt = simNow,
                sources = sourcesAll.filter { it.datasetId == "calendario_academico" },
                trace = listOf(
                    TraceStep(
                        kind = "compute",
                        label = "Ejecutada sin LLM: verificación calendario UEx",
                        resultSummary = "Día no lectivo detectado en ${simNow.date}",
                        ms = 9
                    )
                )
            )
        }

        // 3. Execute tool deterministically according to tool type
        return when (savedQuery.tool) {
            "estimar_hora_salida" -> {
                val est = Estimator.estimateDeparture(
                    destinationStopId = "S19",
                    targetArrivalMinutes = 9 * 60,
                    simNow = simNow,
                    scenarios = demoState.scenarios,
                    origins = homePreset.originStops
                )
                val status = when {
                    est.chosenBus.isCancelled -> "alert"
                    est.chosenBus.comparisonText != "Salida habitual" -> "warning"
                    else -> "ok"
                }

                val subtitle = if (est.chosenBus.comparisonText != "Salida habitual") {
                    "${est.chosenBus.comparisonText} · ${est.chosenBus.reason.lowercase()}"
                } else {
                    "${est.chosenBus.busLine} desde ${est.chosenBus.originStopName.substringBefore(" -")} · día normal · ${est.chosenBus.confidence}%"
                }

                QueryLatest(
                    title = savedQuery.title,
                    headline = "Sal a las ${est.chosenBus.leaveHomeTime}",
                    subtitle = subtitle,
                    status = status,
                    updatedAt = simNow,
                    sources = sourcesAll.filter { it.datasetId in listOf("buses_flota", "buses_red", "trafico_caceres") },
                    trace = listOf(
                        TraceStep(
                            kind = "tool",
                            label = "Ejecutada sin LLM: llamada congelada estimar_hora_salida({\"destino\":\"S19\",\"hora\":\"09:00\"})",
                            resultSummary = est.chosenBus.computationFormula,
                            ms = 14
                        )
                    )
                )
            }

            "proximas_llegadas" -> {
                val (card, _) = Tools.proximasLlegadas("S02", simNow, demoState.scenarios)
                val first = card.arrivals.firstOrNull()
                val headline = if (first != null) "${first.line} en ${first.minutesAway} min" else "Sin buses próximos"
                val second = card.arrivals.getOrNull(1)
                val sub = if (second != null) "Siguiente: ${second.line} en ${second.minutesAway} min" else "Paso por ${card.stopName}"
                QueryLatest(
                    title = savedQuery.title,
                    headline = headline,
                    subtitle = sub,
                    status = if (first?.isCancelled == true) "alert" else "ok",
                    updatedAt = simNow,
                    sources = sourcesAll.filter { it.datasetId == "buses_flota" }
                )
            }

            "farmacias_guardia" -> {
                val (card, _) = Tools.farmaciasGuardia(simNow, homePreset.lat, homePreset.lon)
                QueryLatest(
                    title = savedQuery.title,
                    headline = card.onDutyName.substringBefore(" ("),
                    subtitle = "Guardia hoy · ${card.onDutyAddress} (hasta 09:30)",
                    status = "ok",
                    updatedAt = simNow,
                    sources = sourcesAll.filter { it.datasetId == "farmacias_colegio" }
                )
            }

            "aparcamientos" -> {
                val (card, _) = Tools.aparcamientos(demoState.scenarios)
                val campus = card.campusHighlight
                val status = if (campus.freeSpots < 50) "warning" else "ok"
                QueryLatest(
                    title = savedQuery.title,
                    headline = "${campus.freeSpots} de ${campus.totalSpots} libres",
                    subtitle = "Campus UEx · ${campus.occupancyPercent}% ocupación",
                    status = status,
                    updatedAt = simNow,
                    sources = sourcesAll.filter { it.datasetId == "parkings_ciudad" }
                )
            }

            "tiempo" -> {
                val (card, _) = Tools.tiempo(demoState.scenarios)
                val status = if (card.rainProbability > 50) "warning" else "ok"
                QueryLatest(
                    title = savedQuery.title,
                    headline = "${card.tempCurrent}°C · ${card.rainProbability}% lluvia",
                    subtitle = card.umbrellaAlert,
                    status = status,
                    updatedAt = simNow,
                    sources = sourcesAll.filter { it.datasetId == "aemet_clima" }
                )
            }

            else -> {
                QueryLatest(
                    title = savedQuery.title,
                    headline = "Consulta activa",
                    subtitle = "Actualizado ${simNow.toTimeString()}",
                    status = "ok",
                    updatedAt = simNow
                )
            }
        }
    }
}
