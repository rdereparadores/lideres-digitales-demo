package com.example.pregon.mock

import com.example.pregon.model.*

object Tools {

    fun proximasLlegadas(
        stopId: String,
        simNow: SimTime,
        scenarios: Set<String>
    ): Pair<ArrivalsCardData, List<TraceStep>> {
        val stop = ReferenceData.STOPS_BY_ID[stopId] ?: ReferenceData.STOPS_BY_ID["S02"]!!
        val hasMissingBus = scenarios.any { it.startsWith("missing_bus") }
        val missingBusLine = scenarios.firstOrNull { it.startsWith("missing_bus") }
            ?.substringAfter("missing_bus_")?.ifEmpty { "L1" } ?: "L1"

        val arrivals = mutableListOf<BusArrival>()
        for (lineId in stop.lines) {
            val line = ReferenceData.LINES_BY_ID[lineId] ?: continue
            val offset = line.stopOffsets[stopId] ?: 0

            // Find next 2 expeditions for this line
            var dept = line.firstServiceMinutes
            while (dept <= simNow.minutes + 60) {
                val passTime = dept + offset
                val minsAway = passTime - simNow.minutes
                if (minsAway >= 0) {
                    val isCancelled = hasMissingBus && lineId == missingBusLine && dept == (8 * 60 + 15)
                    val occupancy = when {
                        isCancelled -> "baja"
                        minsAway < 10 && (simNow.hour in 8..9) -> "alta"
                        minsAway < 15 -> "media"
                        else -> "baja"
                    }
                    arrivals.add(
                        BusArrival(
                            line = lineId,
                            destination = if (lineId == "L1") "Campus UEx" else "Estación / Centro",
                            minutesAway = minsAway,
                            occupancy = occupancy,
                            isCancelled = isCancelled
                        )
                    )
                }
                dept += line.headwayMinutes
            }
        }

        val sortedArrivals = arrivals.sortedBy { it.minutesAway }.take(4)
        val card = ArrivalsCardData(
            stopId = stop.id,
            stopName = stop.name,
            arrivals = sortedArrivals
        )

        val trace = listOf(
            TraceStep(
                kind = "tool",
                label = "Consultando paso estimado por ${stop.name}",
                tool = "proximas_llegadas",
                args = "{\"parada\": \"${stop.id}\", \"lineas\": \"${stop.lines.joinToString(",")}\"}",
                resultSummary = "${sortedArrivals.size} autobuses en aproximación",
                ms = 180
            )
        )

        return Pair(card, trace)
    }

    fun estadoLinea(
        lineId: String,
        simNow: SimTime,
        scenarios: Set<String>
    ): Pair<LineStatusCardData, List<TraceStep>> {
        val line = ReferenceData.LINES_BY_ID[lineId] ?: ReferenceData.LINES_BY_ID["L1"]!!
        val hasMissingBus = scenarios.any { it.startsWith("missing_bus") && it.contains(lineId) }
        val isAccident = scenarios.contains("accident_ronda_norte")
        val isRain = scenarios.contains("rain")

        val scheduled = 6
        val active = if (hasMissingBus) 5 else 6
        val punctuality = when {
            hasMissingBus -> 78
            isAccident -> 81
            isRain -> 86
            else -> 94
        }
        val delay = when {
            isAccident -> 8
            isRain -> 5
            hasMissingBus -> 4
            else -> 1
        }

        val alerts = mutableListOf<String>()
        if (hasMissingBus) alerts.add("1 servicio suprimido: expedición 08:15 no operativa")
        if (isAccident) alerts.add("Retenciones de hasta 8 min en acceso a Campus (Ronda Norte)")
        if (isRain) alerts.add("Velocidad reducida por calzada mojada")
        if (alerts.isEmpty()) alerts.add("Servicio operando con regularidad normal")

        val card = LineStatusCardData(
            line = line.id,
            name = line.name,
            punctualityPercent = punctuality,
            activeBuses = active,
            scheduledBuses = scheduled,
            averageDelayMinutes = delay,
            alerts = alerts
        )

        val trace = listOf(
            TraceStep(
                kind = "tool",
                label = "Consultando telemetría de flota línea ${line.id}",
                tool = "estado_linea",
                resultSummary = "$active de $scheduled autobuses en servicio · $punctuality% puntualidad",
                ms = 160
            )
        )

        return Pair(card, trace)
    }

    fun farmaciasGuardia(
        simNow: SimTime,
        homeLat: Double,
        homeLon: Double
    ): Pair<PharmacyCardData, List<TraceStep>> {
        val pharmacies = listOf(
            PharmacyItem("Farmacia Cánovas (simulada)", "Av. de España, 12", "09:30 a 09:30 (guardia 24h)", 0.8, true),
            PharmacyItem("Farmacia Nuevo Cáceres (simulada)", "C/ Amberes, 4", "09:00 a 22:00", 0.4, false),
            PharmacyItem("Farmacia Central Plaza Mayor", "Plaza Mayor, 18", "09:30 a 21:30", 1.2, false),
            PharmacyItem("Farmacia Colón", "C/ San Pedro de Alcántara, 3", "09:00 a 21:00", 1.0, false),
            PharmacyItem("Farmacia Mejostilla", "Av. Héroes de Baler, 22", "09:30 a 21:30", 3.1, false),
            PharmacyItem("Farmacia Ronda Norte", "C/ Dionisio Acedo, 5", "09:30 a 21:30", 2.2, false)
        )

        // Day of year rotation
        val onDuty = pharmacies.first()
        val card = PharmacyCardData(
            onDutyName = onDuty.name,
            onDutyAddress = onDuty.address,
            onDutyHours = onDuty.hours,
            distanceKm = onDuty.distanceKm,
            walkMin = (onDuty.distanceKm * 13).toInt().coerceAtLeast(4),
            otherPharmacies = pharmacies.drop(1)
        )

        val trace = listOf(
            TraceStep(
                kind = "tool",
                label = "Consultando registro oficial de guardias farmacéuticas",
                tool = "farmacias_guardia",
                resultSummary = "Guardia activa: ${onDuty.name} (${onDuty.address})",
                ms = 140
            )
        )

        return Pair(card, trace)
    }

    fun aparcamientos(
        scenarios: Set<String>
    ): Pair<ParkingCardData, List<TraceStep>> {
        val isExamDay = scenarios.contains("exam_day")

        val campusFree = if (isExamDay) 42 else 132
        val campus = ParkingStatus("Campus UEx", "Campus Universitario", campusFree, 600)
        val all = listOf(
            campus,
            ParkingStatus("Obispo Galarza", "Centro / Casco Antiguo", 84, 350),
            ParkingStatus("Plaza de América", "Zona Comercial", 45, 220),
            ParkingStatus("Estación de Tren", "Estación Renfe / Bus", 95, 180),
            ParkingStatus("San Mateo", "Ciudad Monumental", 18, 120)
        )

        val card = ParkingCardData(
            campusHighlight = campus,
            allParkings = all
        )

        val trace = listOf(
            TraceStep(
                kind = "tool",
                label = "Consultando sensores de aforo de aparcamientos municipales",
                tool = "aparcamientos",
                resultSummary = "Campus: $campusFree libres de 600 · Total red: ${all.sumOf { it.freeSpots }} libres",
                ms = 150
            )
        )

        return Pair(card, trace)
    }

    fun tiempo(
        scenarios: Set<String>
    ): Pair<WeatherCardData, List<TraceStep>> {
        val isRain = scenarios.contains("rain")

        val condition = if (isRain) "Lluvia moderada persistente" else "Parcialmente nuboso"
        val rainProb = if (isRain) 95 else 15
        val tempCurr = if (isRain) 15 else 18
        val alert = if (isRain) "Hoy 95% de probabilidad de lluvia · coge paraguas" else "Baja probabilidad de lluvia · día agradable"

        val hourly = listOf(
            HourForecast("08:00", tempCurr - 1, rainProb, if (isRain) "rain" else "cloud"),
            HourForecast("10:00", tempCurr, rainProb, if (isRain) "rain" else "sun"),
            HourForecast("12:00", tempCurr + 2, (rainProb - 10).coerceAtLeast(5), if (isRain) "rain" else "sun"),
            HourForecast("14:00", tempCurr + 4, (rainProb - 20).coerceAtLeast(5), "sun"),
            HourForecast("18:00", tempCurr + 1, 10, "sun")
        )

        val card = WeatherCardData(
            condition = condition,
            tempCurrent = tempCurr,
            tempMax = 21,
            tempMin = 12,
            rainProbability = rainProb,
            umbrellaAlert = alert,
            hourly = hourly
        )

        val trace = listOf(
            TraceStep(
                kind = "tool",
                label = "Consultando predicción meteorológica para Cáceres",
                tool = "tiempo",
                resultSummary = "$condition · $tempCurr°C · $rainProb% precipitación",
                ms = 130
            )
        )

        return Pair(card, trace)
    }

    fun calendarioUex(
        simNow: SimTime
    ): Pair<CalendarCardData, List<TraceStep>> {
        val dayInfo = CalendarUtils.getDayInfo(simNow.date)
        val nextHoliday = CalendarUtils.getNextHolidayDescription(simNow.date)

        val card = CalendarCardData(
            date = simNow.date,
            isLective = dayInfo.isLective,
            dayTypeTitle = dayInfo.title,
            description = dayInfo.description,
            nextHoliday = nextHoliday
        )

        val trace = listOf(
            TraceStep(
                kind = "tool",
                label = "Consultando calendario oficial de la Universidad de Extremadura",
                tool = "calendario_uex",
                resultSummary = "${dayInfo.title} · ${if (dayInfo.isLective) "Con clases" else "Sin clases"}",
                ms = 120
            )
        )

        return Pair(card, trace)
    }
}
