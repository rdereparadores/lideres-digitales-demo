package com.example.pregon.mock

import com.example.pregon.model.SimTime

data class CalendarDayInfo(
    val date: String,
    val isLective: Boolean,
    val title: String,
    val description: String,
    val holidayName: String? = null
)

object CalendarUtils {
    // Holidays / non-lective dates for 2026-2027 in UEx Cáceres
    private val HOLIDAYS = mapOf(
        "2026-10-12" to "Fiesta Nacional de España",
        "2026-11-01" to "Día de Todos los Santos",
        "2026-11-02" to "Traslado Todos los Santos",
        "2026-12-06" to "Día de la Constitución",
        "2026-12-08" to "Inmaculada Concepción",
        "2026-12-23" to "Vacaciones de Navidad",
        "2026-12-24" to "Vacaciones de Navidad",
        "2026-12-25" to "Navidad"
    )

    fun getDayInfo(date: String): CalendarDayInfo {
        val simTime = SimTime(date, 720)
        val dow = simTime.dayOfWeekIndex() // 1=Mon, ..., 6=Sat, 7=Sun

        val holiday = HOLIDAYS[date]
        if (holiday != null) {
            return CalendarDayInfo(
                date = date,
                isLective = false,
                title = "Día Festivo / No lectivo",
                description = "No hay clases en la Universidad de Extremadura ($holiday).",
                holidayName = holiday
            )
        }

        if (dow == 6 || dow == 7) {
            val dayName = if (dow == 6) "Sábado" else "Domingo"
            return CalendarDayInfo(
                date = date,
                isLective = false,
                title = "Fin de semana ($dayName)",
                description = "No hay docencia presencial durante el fin de semana.",
                holidayName = null
            )
        }

        return CalendarDayInfo(
            date = date,
            isLective = true,
            title = "Día lectivo ordinario",
            description = "Actividad académica regular en todos los centros y facultades del Campus de Cáceres.",
            holidayName = null
        )
    }

    fun isLective(date: String): Boolean {
        return getDayInfo(date).isLective
    }

    fun getNextLectiveDay(fromDate: String): String {
        var current = fromDate
        for (i in 1..14) {
            val next = SimTime(current, 720).offsetDate(1)
            if (isLective(next)) {
                return next
            }
            current = next
        }
        return fromDate
    }

    fun getNextHolidayDescription(fromDate: String): String {
        return "Lunes 2 de Noviembre (Todos los Santos)"
    }
}
