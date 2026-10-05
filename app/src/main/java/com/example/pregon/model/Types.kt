package com.example.pregon.model

import java.util.UUID

data class SimTime(
    val date: String = "2026-10-28", // YYYY-MM-DD
    val minutes: Int = 7 * 60 + 45   // Minutes from midnight (Madrid time)
) {
    val hour: Int get() = (minutes / 60) % 24
    val minute: Int get() = minutes % 60

    fun toTimeString(): String {
        return "%02d:%02d".format(hour, minute)
    }

    fun displayDateTime(): String {
        val dayName = when (dayOfWeekIndex()) {
            1 -> "lun"
            2 -> "mar"
            3 -> "mié"
            4 -> "jue"
            5 -> "vie"
            6 -> "sáb"
            else -> "dom"
        }
        val parts = date.split("-")
        val day = if (parts.size >= 3) parts[2] else ""
        val month = if (parts.size >= 3) parts[1] else ""
        return "$dayName $day/$month · ${toTimeString()}"
    }

    fun dayOfWeekIndex(): Int {
        // Deterministic day of week calculation for YYYY-MM-DD
        val parts = date.split("-").mapNotNull { it.toIntOrNull() }
        if (parts.size < 3) return 3 // default Wednesday
        val y = parts[0]
        val m = parts[1]
        val d = parts[2]
        val a = (14 - m) / 12
        val y0 = y - a
        val m0 = m + 12 * a - 2
        val d0 = (d + y0 + y0 / 4 - y0 / 100 + y0 / 400 + (31 * m0) / 12) % 7
        // d0: 0=Sun, 1=Mon, 2=Tue, 3=Wed, 4=Thu, 5=Fri, 6=Sat
        return if (d0 == 0) 7 else d0
    }

    fun dayLetter(): String {
        return when (dayOfWeekIndex()) {
            1 -> "L"
            2 -> "M"
            3 -> "X"
            4 -> "J"
            5 -> "V"
            6 -> "S"
            else -> "D"
        }
    }

    fun addMinutes(mins: Int): SimTime {
        val total = minutes + mins
        if (total < 0) {
            val daysBack = (-total + 1439) / 1440
            return SimTime(date = offsetDate(-daysBack), minutes = (total % 1440 + 1440) % 1440)
        }
        val daysFwd = total / 1440
        val remMin = total % 1440
        return if (daysFwd > 0) SimTime(date = offsetDate(daysFwd), minutes = remMin) else copy(minutes = remMin)
    }

    fun offsetDate(days: Int): String {
        val parts = date.split("-").mapNotNull { it.toIntOrNull() }
        if (parts.size < 3) return date
        var y = parts[0]
        var m = parts[1]
        var d = parts[2] + days
        // simplified day shift for October/November 2026
        while (d > 31 && (m == 10 || m == 12 || m == 1 || m == 3 || m == 5 || m == 7 || m == 8)) {
            d -= 31
            m++
        }
        while (d > 30 && (m == 4 || m == 6 || m == 9 || m == 11)) {
            d -= 30
            m++
        }
        while (d < 1) {
            m--
            val prevDays = if (m == 10) 31 else 30
            d += prevDays
        }
        return "%04d-%02d-%02d".format(y, m, d)
    }
}

data class Provenance(
    val datasetId: String,
    val title: String,
    val publisher: String,
    val license: String,
    val lastUpdated: String,
    val nature: String // "tiempo real" | "previsión" | "histórico" | "estático"
)

data class TraceStep(
    val kind: String, // "interpret" | "tool" | "compute" | "compose"
    val label: String,
    val tool: String? = null,
    val args: String? = null,
    val resultSummary: String? = null,
    val ms: Long = 0
)

data class OriginStop(
    val stopId: String,
    val stopName: String,
    val walkMin: Int
)

data class PersistOffer(
    val tool: String,
    val argsSummary: String,
    val suggestedTitle: String,
    val suggestedSchedule: Schedule
)

data class Schedule(
    val days: List<String> = listOf("L", "M", "X", "J"),
    val fromMinutes: Int = 7 * 60,       // 07:00
    val toMinutes: Int = 8 * 60 + 45,    // 08:45
    val everyMin: Int = 5,
    val onlyLective: Boolean = true
) {
    fun summary(): String {
        val daysStr = days.joinToString("–")
        val fromStr = "%02d:%02d".format(fromMinutes / 60, fromMinutes % 60)
        val toStr = "%02d:%02d".format(toMinutes / 60, toMinutes % 60)
        val lectiveStr = if (onlyLective) "solo lectivos" else "todos"
        return "$daysStr · $fromStr–$toStr · $lectiveStr"
    }
}

data class QueryLatest(
    val title: String,
    val headline: String,
    val subtitle: String,
    val status: String, // "ok" | "warning" | "alert" | "idle"
    val updatedAt: SimTime,
    val sources: List<Provenance> = emptyList(),
    val trace: List<TraceStep> = emptyList()
)

data class SavedQuery(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val tool: String,
    val argsJson: String,
    val schedule: Schedule,
    val lastResult: QueryLatest,
    val createdAt: Long = System.currentTimeMillis(),
    val isPinnedToWidget: Boolean = false
)

data class DemoState(
    val now: SimTime = SimTime("2026-10-28", 7 * 60 + 45), // mié 28/10/2026 07:45
    val mode: String = "frozen", // "frozen" | "running"
    val scenarios: Set<String> = emptySet(), // "missing_bus_L1", "rain", "accident_ronda_norte", "exam_day"
    val missingBusLine: String = "L1"
)

// Cards
sealed interface CardData {
    val cardType: String
}

data class DepartureAlternative(
    val line: String,
    val passTime: String,
    val arrivalTime: String,
    val confidence: Int
)

data class DepartureCardData(
    override val cardType: String = "departure",
    val leaveHomeTime: String,       // e.g. "8:12"
    val busLine: String,            // "L1"
    val lineName: String,           // "Nuevo Cáceres – Plaza de América – Campus"
    val originStopId: String,       // "S02"
    val originStopName: String,     // "Nuevo Cáceres - Plaza"
    val destinationStopId: String,  // "S19"
    val destinationStopName: String,// "Escuela Politécnica"
    val busPassTime: String,        // "8:18"
    val arrivalTime: String,        // "8:51"
    val targetArrivalTime: String,  // "9:00"
    val confidence: Int,            // 92 (%)
    val reason: String,             // "Día normal"
    val comparisonText: String,     // "Salida habitual" | "15 min antes de lo habitual" | "2 min antes de lo habitual"
    val walkMinutes: Int,           // 4
    val bufferMinutes: Int,         // 2
    val rainBufferMinutes: Int = 0,
    val isCancelled: Boolean = false,
    val occupancyAlert: String? = null,
    val alternatives: List<DepartureAlternative> = emptyList(),
    val computationFormula: String
) : CardData

data class BusArrival(
    val line: String,
    val destination: String,
    val minutesAway: Int,
    val occupancy: String, // "baja" | "media" | "alta"
    val isCancelled: Boolean = false
)

data class ArrivalsCardData(
    override val cardType: String = "arrivals",
    val stopId: String,
    val stopName: String,
    val arrivals: List<BusArrival>
) : CardData

data class LineStatusCardData(
    override val cardType: String = "line_status",
    val line: String,
    val name: String,
    val punctualityPercent: Int,
    val activeBuses: Int,
    val scheduledBuses: Int,
    val averageDelayMinutes: Int,
    val alerts: List<String>
) : CardData

data class PharmacyItem(
    val name: String,
    val address: String,
    val hours: String,
    val distanceKm: Double,
    val isOnDuty: Boolean
)

data class PharmacyCardData(
    override val cardType: String = "pharmacy",
    val onDutyName: String,
    val onDutyAddress: String,
    val onDutyHours: String,
    val distanceKm: Double,
    val walkMin: Int,
    val otherPharmacies: List<PharmacyItem>
) : CardData

data class ParkingStatus(
    val name: String,
    val zone: String,
    val freeSpots: Int,
    val totalSpots: Int
) {
    val occupancyPercent: Int get() = ((totalSpots - freeSpots) * 100) / totalSpots.coerceAtLeast(1)
}

data class ParkingCardData(
    override val cardType: String = "parking",
    val campusHighlight: ParkingStatus,
    val allParkings: List<ParkingStatus>
) : CardData

data class HourForecast(
    val hour: String,
    val temp: Int,
    val rainProb: Int,
    val icon: String
)

data class WeatherCardData(
    override val cardType: String = "weather",
    val condition: String,
    val tempCurrent: Int,
    val tempMax: Int,
    val tempMin: Int,
    val rainProbability: Int,
    val umbrellaAlert: String,
    val hourly: List<HourForecast>
) : CardData

data class CalendarCardData(
    override val cardType: String = "calendar",
    val date: String,
    val isLective: Boolean,
    val dayTypeTitle: String,
    val description: String,
    val nextHoliday: String
) : CardData

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val isUser: Boolean,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val cards: List<CardData> = emptyList(),
    val sources: List<Provenance> = emptyList(),
    val trace: List<TraceStep> = emptyList(),
    val persistOffer: PersistOffer? = null,
    val isThinking: Boolean = false,
    val thinkingStepIndex: Int = 0
)
