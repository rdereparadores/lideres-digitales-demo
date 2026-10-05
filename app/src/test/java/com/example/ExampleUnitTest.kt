package com.example

import com.example.pregon.mock.CalendarUtils
import com.example.pregon.mock.Estimator
import com.example.pregon.mock.Interpreter
import com.example.pregon.mock.InterpretationResult
import com.example.pregon.mock.PregonEngine
import com.example.pregon.model.HomePresets
import com.example.pregon.model.SimTime
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {

    private val simNow = SimTime(date = "2026-10-28", minutes = 7 * 60 + 45) // mié 28/10 07:45
    private val origins = HomePresets.DEFAULT_HOME.originStops // S02 (4m), S03 (7m)
    private val destStopId = "S19" // Escuela Politécnica
    private val targetArrival = 9 * 60 // 09:00

    @Test
    fun testGoldenRow1_NoScenario() {
        val result = Estimator.estimateDeparture(
            destinationStopId = destStopId,
            targetArrivalMinutes = targetArrival,
            simNow = simNow,
            scenarios = emptySet(),
            origins = origins
        )

        assertEquals("L1", result.chosenBus.busLine)
        assertEquals("8:18", result.chosenBus.busPassTime)
        assertEquals("8:51", result.chosenBus.arrivalTime)
        assertEquals("8:12", result.chosenBus.leaveHomeTime)
        assertEquals("Día normal", result.chosenBus.reason)
        assertEquals("Salida habitual", result.chosenBus.comparisonText)
        assertEquals(92, result.chosenBus.confidence)
    }

    @Test
    fun testGoldenRow2_MissingBusL1() {
        val result = Estimator.estimateDeparture(
            destinationStopId = destStopId,
            targetArrivalMinutes = targetArrival,
            simNow = simNow,
            scenarios = setOf("missing_bus_L1"),
            origins = origins
        )

        assertEquals("L1", result.chosenBus.busLine)
        assertEquals("8:03", result.chosenBus.busPassTime)
        assertEquals("8:36", result.chosenBus.arrivalTime)
        assertEquals("7:57", result.chosenBus.leaveHomeTime)
        assertEquals("La L1 va con un autobús menos", result.chosenBus.reason)
        assertEquals("15 min antes de lo habitual", result.chosenBus.comparisonText)
    }

    @Test
    fun testGoldenRow3_Rain() {
        val result = Estimator.estimateDeparture(
            destinationStopId = destStopId,
            targetArrivalMinutes = targetArrival,
            simNow = simNow,
            scenarios = setOf("rain"),
            origins = origins
        )

        assertEquals("L1", result.chosenBus.busLine)
        assertEquals("8:18", result.chosenBus.busPassTime)
        assertEquals("8:56", result.chosenBus.arrivalTime)
        assertEquals("8:10", result.chosenBus.leaveHomeTime)
        assertEquals("Lluvia", result.chosenBus.reason)
        assertEquals("2 min antes de lo habitual", result.chosenBus.comparisonText)
    }

    @Test
    fun testGoldenRow4_AccidentRondaNorte() {
        val result = Estimator.estimateDeparture(
            destinationStopId = destStopId,
            targetArrivalMinutes = targetArrival,
            simNow = simNow,
            scenarios = setOf("accident_ronda_norte"),
            origins = origins
        )

        assertEquals("L1", result.chosenBus.busLine)
        assertEquals("8:03", result.chosenBus.busPassTime)
        assertEquals("8:44", result.chosenBus.arrivalTime)
        assertEquals("7:57", result.chosenBus.leaveHomeTime)
        assertEquals("Accidente en la Ronda Norte", result.chosenBus.reason)
        assertEquals("15 min antes de lo habitual", result.chosenBus.comparisonText)
    }

    @Test
    fun testDeterminism_SameInputYieldsSameOutput() {
        val engine1 = PregonEngine()
        val engine2 = PregonEngine()

        val resp1 = engine1.processQuery("Si quiero llegar a la Politécnica a las 9, ¿a qué hora salgo de casa?", HomePresets.DEFAULT_HOME)
        val resp2 = engine2.processQuery("Si quiero llegar a la Politécnica a las 9, ¿a qué hora salgo de casa?", HomePresets.DEFAULT_HOME)

        assertEquals(resp1.answer, resp2.answer)
        assertEquals(resp1.cards.size, resp2.cards.size)
        assertEquals(resp1.trace.size, resp2.trace.size)
    }

    @Test
    fun testInterpreter_AllIntents() {
        val departure = Interpreter.interpret("Si quiero llegar a la Politécnica a las 9, ¿a qué hora salgo de casa?")
        assertTrue(departure is InterpretationResult.Success)
        assertEquals("estimar_hora_salida", (departure as InterpretationResult.Success).tool)

        val pharmacy = Interpreter.interpret("¿Qué farmacia está de guardia hoy?")
        assertTrue(pharmacy is InterpretationResult.Success)
        assertEquals("farmacias_guardia", (pharmacy as InterpretationResult.Success).tool)

        val parking = Interpreter.interpret("¿Hay sitio en el parking del campus?")
        assertTrue(parking is InterpretationResult.Success)
        assertEquals("aparcamientos", (parking as InterpretationResult.Success).tool)

        val calendar = Interpreter.interpret("¿Hay clase mañana?")
        assertTrue(calendar is InterpretationResult.Success)
        assertEquals("calendario_uex", (calendar as InterpretationResult.Success).tool)

        val outOfScope = Interpreter.interpret("¿Quién ganó el Mundial de fútbol?")
        assertTrue(outOfScope is InterpretationResult.Unknown)
    }

    @Test
    fun testCalendar_LectiveAndHolidays() {
        assertTrue(CalendarUtils.isLective("2026-10-28")) // Miércoles
        assertTrue(CalendarUtils.isLective("2026-10-29")) // Jueves
        assertTrue(CalendarUtils.isLective("2026-10-30")) // Viernes
        assertFalse(CalendarUtils.isLective("2026-10-31")) // Sábado
        assertFalse(CalendarUtils.isLective("2026-11-01")) // Domingo
        assertFalse(CalendarUtils.isLective("2026-11-02")) // Festivo (Todos los Santos)
        assertEquals("2026-10-29", CalendarUtils.getNextLectiveDay("2026-10-28"))
        assertEquals("2026-11-03", CalendarUtils.getNextLectiveDay("2026-10-30"))
    }
}
