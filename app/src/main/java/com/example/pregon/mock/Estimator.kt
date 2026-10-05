package com.example.pregon.mock

import com.example.pregon.model.*

object Estimator {

    data class EstimationResult(
        val chosenBus: DepartureCardData,
        val normalLeaveHomeMinutes: Int, // Leave home minutes without active scenarios
        val traceSteps: List<TraceStep>,
        val persistOffer: PersistOffer
    )

    fun estimateDeparture(
        destinationStopId: String,
        targetArrivalMinutes: Int, // e.g. 9 * 60 = 540 (09:00)
        simNow: SimTime,
        scenarios: Set<String>,
        origins: List<OriginStop>
    ): EstimationResult {
        val destStop = ReferenceData.STOPS_BY_ID[destinationStopId] ?: ReferenceData.STOPS_BY_ID["S19"]!!
        val isRain = scenarios.contains("rain")
        val isAccident = scenarios.contains("accident_ronda_norte")
        val hasMissingBus = scenarios.any { it.startsWith("missing_bus") }
        val missingBusLine = scenarios.firstOrNull { it.startsWith("missing_bus") }
            ?.substringAfter("missing_bus_")?.ifEmpty { "L1" } ?: "L1"
        val isExamDay = scenarios.contains("exam_day")

        val walkBuffer = 2
        val rainBuffer = if (isRain) 2 else 0

        // 1. Calculate normal baseline (no scenarios) first for comparison
        val baselineTrip = calculateBestTrip(
            destinationStopId = destinationStopId,
            targetArrivalMinutes = targetArrivalMinutes,
            simNow = simNow,
            scenarios = emptySet(),
            origins = origins,
            walkBuffer = 2,
            rainBuffer = 0
        )

        // 2. Calculate trip with active scenarios
        val activeTrip = calculateBestTrip(
            destinationStopId = destinationStopId,
            targetArrivalMinutes = targetArrivalMinutes,
            simNow = simNow,
            scenarios = scenarios,
            origins = origins,
            walkBuffer = walkBuffer,
            rainBuffer = rainBuffer
        )

        val selectedTrip = activeTrip ?: baselineTrip!!
        val normalLeaveMinutes = baselineTrip?.leaveHomeMinutes ?: selectedTrip.leaveHomeMinutes
        val diffMin = normalLeaveMinutes - selectedTrip.leaveHomeMinutes

        val comparisonText = when {
            diffMin > 0 -> "$diffMin min antes de lo habitual"
            diffMin < 0 -> "${-diffMin} min después de lo habitual"
            else -> "Salida habitual"
        }

        // Reason formulation matching golden table
        val reason = when {
            hasMissingBus -> "La $missingBusLine va con un autobús menos"
            isAccident -> "Accidente en la Ronda Norte"
            isRain -> "Lluvia"
            isExamDay -> "Día de exámenes: mayor afluencia hacia el campus"
            else -> "Día normal"
        }

        // Exact calculation formula display
        val formula = "%02d:%02d − %d min andando − %d min colchón%s = %s".format(
            selectedTrip.busPassMinutes / 60,
            selectedTrip.busPassMinutes % 60,
            selectedTrip.origin.walkMin,
            walkBuffer,
            if (rainBuffer > 0) " − $rainBuffer min lluvia" else "",
            selectedTrip.leaveHomeTimeStr
        )

        val cardData = DepartureCardData(
            leaveHomeTime = selectedTrip.leaveHomeTimeStr,
            busLine = selectedTrip.line.id,
            lineName = selectedTrip.line.name,
            originStopId = selectedTrip.origin.stopId,
            originStopName = selectedTrip.origin.stopName,
            destinationStopId = destStop.id,
            destinationStopName = destStop.name,
            busPassTime = selectedTrip.busPassTimeStr,
            arrivalTime = selectedTrip.arrivalTimeStr,
            targetArrivalTime = "%02d:%02d".format(targetArrivalMinutes / 60, targetArrivalMinutes % 60),
            confidence = selectedTrip.confidence,
            reason = reason,
            comparisonText = comparisonText,
            walkMinutes = selectedTrip.origin.walkMin,
            bufferMinutes = walkBuffer,
            rainBufferMinutes = rainBuffer,
            occupancyAlert = if (hasMissingBus && selectedTrip.line.id == missingBusLine) "Ocupación prevista: 95% (muy lleno)" else null,
            alternatives = selectedTrip.alternatives,
            computationFormula = formula
        )

        // Trace steps simulating LLM latency and data lookups
        val traceSteps = listOf(
            TraceStep(
                kind = "interpret",
                label = "Interpretando pregunta y extrayendo entidades",
                args = "{\"destino\": \"${destStop.name} (${destStop.id})\", \"hora_llegada\": \"%02d:%02d\", \"origen\": \"${selectedTrip.origin.stopName}\"}".format(
                    targetArrivalMinutes / 60, targetArrivalMinutes % 60
                ),
                ms = 185
            ),
            TraceStep(
                kind = "tool",
                label = "Consultando flota en tiempo real y frecuencias",
                tool = "estimar_hora_salida",
                resultSummary = "Línea ${selectedTrip.line.id}: intervalo 15 min, posición expediciones activa",
                ms = 240
            ),
            TraceStep(
                kind = "tool",
                label = "Verificando tráfico e incidencias activas",
                tool = "trafico_caceres",
                resultSummary = if (scenarios.isNotEmpty()) "Incidencias: ${scenarios.joinToString(", ")}" else "Sin retenciones en Ronda Norte ni Centro",
                ms = 190
            ),
            TraceStep(
                kind = "compute",
                label = "Calculando hora óptima de salida y margen de llegada",
                resultSummary = formula,
                ms = 115
            ),
            TraceStep(
                kind = "compose",
                label = "Generando respuesta con tarjeta de salida y alternativas",
                resultSummary = "Sal a las ${selectedTrip.leaveHomeTimeStr} (${selectedTrip.confidence}% confianza)",
                ms = 95
            )
        )

        val persistOffer = PersistOffer(
            tool = "estimar_hora_salida",
            argsSummary = "Destino: ${destStop.name} a las %02d:%02d".format(targetArrivalMinutes / 60, targetArrivalMinutes % 60),
            suggestedTitle = "Salir hacia ${destStop.name.substringBefore(" -")}",
            suggestedSchedule = Schedule(
                days = listOf("L", "M", "X", "J"),
                fromMinutes = (targetArrivalMinutes - 120).coerceAtLeast(6 * 60),
                toMinutes = (targetArrivalMinutes - 15).coerceAtLeast(6 * 60),
                everyMin = 5,
                onlyLective = true
            )
        )

        return EstimationResult(
            chosenBus = cardData,
            normalLeaveHomeMinutes = normalLeaveMinutes,
            traceSteps = traceSteps,
            persistOffer = persistOffer
        )
    }

    private data class CandidateTrip(
        val line: BusLine,
        val origin: OriginStop,
        val departureFromCabecera: Int,
        val busPassMinutes: Int,
        val arrivalMinutes: Int,
        val leaveHomeMinutes: Int,
        val confidence: Int,
        val isCancelled: Boolean,
        val busPassTimeStr: String,
        val arrivalTimeStr: String,
        val leaveHomeTimeStr: String,
        val alternatives: List<DepartureAlternative> = emptyList()
    )

    private fun calculateBestTrip(
        destinationStopId: String,
        targetArrivalMinutes: Int,
        simNow: SimTime,
        scenarios: Set<String>,
        origins: List<OriginStop>,
        walkBuffer: Int,
        rainBuffer: Int
    ): CandidateTrip? {
        val isRain = scenarios.contains("rain")
        val isAccident = scenarios.contains("accident_ronda_norte")
        val hasMissingBus = scenarios.any { it.startsWith("missing_bus") }
        val missingBusLine = scenarios.firstOrNull { it.startsWith("missing_bus") }
            ?.substringAfter("missing_bus_")?.ifEmpty { "L1" } ?: "L1"

        // Search among matching lines and origins
        val candidates = mutableListOf<CandidateTrip>()

        for (origin in origins) {
            for (line in ReferenceData.LINES) {
                val originOffset = line.stopOffsets[origin.stopId] ?: continue
                val destOffset = line.stopOffsets[destinationStopId] ?: continue
                if (destOffset <= originOffset) continue // Must pass origin before destination

                // Generate expeditions around the morning window (06:45 to targetArrival)
                var dept = line.firstServiceMinutes
                while (dept <= targetArrivalMinutes) {
                    val basePassOrigin = dept + originOffset
                    var baseTravelTime = destOffset - originOffset

                    // Delay adjustments according to model specification
                    var travelDelay = 0
                    if (isRain) {
                        travelDelay += 5 // factor 1.30 on typical 18-35 min trip = +5 min
                    }
                    if (isAccident && line.id in listOf("L1", "L2")) {
                        // S15 and beyond suffers 8 min delay
                        travelDelay += 8
                    }

                    val arrivalMin = dept + destOffset + travelDelay
                    val passOriginMin = basePassOrigin // bus pass time at origin

                    // Missing bus condition: L1 08:15 expedition is cancelled
                    val isCancelled = hasMissingBus && line.id == missingBusLine && dept == (8 * 60 + 15)

                    val leaveMin = passOriginMin - origin.walkMin - walkBuffer - rainBuffer

                    // Base confidence
                    val timeKey = "%02d:%02d".format(dept / 60, dept % 60)
                    var conf = ReferenceData.HISTORICAL_CONFIDENCE_L1[timeKey] ?: 85
                    if (targetArrivalMinutes - arrivalMin < 5) conf -= 15
                    if (isRain) conf -= 10
                    if (hasMissingBus && dept == 8 * 60) conf -= 5 // crowded successor bus
                    conf = conf.coerceIn(50, 98)

                    // Target rule: Arrival must be <= target - 2 min
                    val isValidArrival = arrivalMin <= (targetArrivalMinutes - 2)

                    if (!isCancelled && isValidArrival) {
                        val passTimeStr = "%d:%02d".format(passOriginMin / 60, passOriginMin % 60)
                        val arrTimeStr = "%d:%02d".format(arrivalMin / 60, arrivalMin % 60)
                        val leaveTimeStr = "%d:%02d".format(leaveMin / 60, leaveMin % 60)

                        candidates.add(
                            CandidateTrip(
                                line = line,
                                origin = origin,
                                departureFromCabecera = dept,
                                busPassMinutes = passOriginMin,
                                arrivalMinutes = arrivalMin,
                                leaveHomeMinutes = leaveMin,
                                confidence = conf,
                                isCancelled = false,
                                busPassTimeStr = passTimeStr,
                                arrivalTimeStr = arrTimeStr,
                                leaveHomeTimeStr = leaveTimeStr
                            )
                        )
                    }

                    dept += line.headwayMinutes
                }
            }
        }

        if (candidates.isEmpty()) return null

        // Decision rule (§2.4): The latest departure that arrives on time (latest leaveHomeMinutes)
        val sorted = candidates.sortedWith(
            compareByDescending<CandidateTrip> { it.leaveHomeMinutes }
                .thenBy { it.origin.walkMin }
                .thenByDescending { it.confidence }
        )

        val best = sorted.first()

        // Alternatives: other valid options earlier
        val alts = sorted.filter { it.departureFromCabecera != best.departureFromCabecera }
            .take(2)
            .map {
                DepartureAlternative(
                    line = it.line.id,
                    passTime = it.busPassTimeStr,
                    arrivalTime = it.arrivalTimeStr,
                    confidence = it.confidence
                )
            }

        return best.copy(alternatives = alts)
    }
}
