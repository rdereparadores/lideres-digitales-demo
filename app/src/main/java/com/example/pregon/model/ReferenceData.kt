package com.example.pregon.model

data class BusStop(
    val id: String,
    val name: String,
    val lat: Double,
    val lon: Double,
    val lines: List<String>
)

data class BusLine(
    val id: String,
    val name: String,
    val colorHex: String,
    val headwayMinutes: Int,
    val firstServiceMinutes: Int, // 06:45 = 405
    val lastServiceMinutes: Int,  // 23:00 = 1380
    // stopId to offset in minutes from cabecera
    val stopOffsets: Map<String, Int>
)

data class CampusCenter(
    val id: String,
    val name: String,
    val stopId: String,
    val aliases: List<String>
)

object ReferenceData {
    val STOPS = listOf(
        BusStop("S01", "Aldea Moret - Av. Constitución", 39.4542, -6.3981, listOf("L1", "L3")),
        BusStop("S02", "Nuevo Cáceres - Plaza", 39.4589, -6.3789, listOf("L1", "L2")),
        BusStop("S03", "Estación de Tren / Autobuses", 39.4623, -6.3812, listOf("L1", "L2", "L4")),
        BusStop("S04", "Av. Alemania", 39.4682, -6.3778, listOf("L1", "L2")),
        BusStop("S05", "Av. de España - Múltiples", 39.4711, -6.3752, listOf("L1", "L3")),
        BusStop("S06", "Cruz de los Caídos", 39.4725, -6.3740, listOf("L1", "L2", "L3")),
        BusStop("S07", "Plaza de América", 39.4741, -6.3732, listOf("L1", "L2", "L4")),
        BusStop("S08", "Paseo de Cánovas", 39.4735, -6.3721, listOf("L1", "L3")),
        BusStop("S09", "Av. Hernán Cortés", 39.4768, -6.3689, listOf("L1", "L4")),
        BusStop("S10", "Plaza de Toros", 39.4802, -6.3645, listOf("L1", "L4")),
        BusStop("S11", "Mejostilla - Héroes de Baler", 39.4892, -6.3698, listOf("L2", "L3")),
        BusStop("S12", "Mejostilla - Ronda Norte", 39.4915, -6.3654, listOf("L2")),
        BusStop("S13", "R-66 - Coubertin", 39.4789, -6.3912, listOf("L3")),
        BusStop("S14", "R-66 - Delicias", 39.4812, -6.3889, listOf("L3")),
        BusStop("S15", "Ronda Norte - Acceso Campus", 39.4823, -6.3541, listOf("L1", "L2")),
        BusStop("S16", "Residencial Universidad", 39.4815, -6.3482, listOf("L1", "L2")),
        BusStop("S17", "Hospital Universitario", 39.4805, -6.3421, listOf("L1", "L4")),
        BusStop("S18", "Campus UEx - Ciencias Central", 39.4795, -6.3402, listOf("L1", "L2")),
        BusStop("S19", "Escuela Politécnica", 39.4782, -6.3389, listOf("L1", "L2")),
        BusStop("S20", "Facultad de Derecho", 39.4771, -6.3375, listOf("L1"))
    )

    val STOPS_BY_ID = STOPS.associateBy { it.id }

    val LINES = listOf(
        BusLine(
            id = "L1",
            name = "Nuevo Cáceres – Plaza de América – Campus UEx",
            colorHex = "#E53E3E", // Rojo Línea 1
            headwayMinutes = 15,
            firstServiceMinutes = 6 * 60 + 45, // 06:45
            lastServiceMinutes = 23 * 60,
            stopOffsets = mapOf(
                "S01" to 0,
                "S02" to 3,
                "S03" to 6,
                "S05" to 11,
                "S07" to 14,
                "S08" to 17,
                "S09" to 20,
                "S10" to 22,
                "S15" to 25,
                "S16" to 28,
                "S18" to 31,
                "S19" to 36,
                "S20" to 39
            )
        ),
        BusLine(
            id = "L2",
            name = "Mejostilla – Ronda Norte – Campus UEx",
            colorHex = "#3182CE", // Azul Línea 2
            headwayMinutes = 20,
            firstServiceMinutes = 6 * 60 + 50,
            lastServiceMinutes = 22 * 60 + 40,
            stopOffsets = mapOf(
                "S11" to 0,
                "S12" to 4,
                "S15" to 15,
                "S16" to 18,
                "S18" to 22,
                "S19" to 26
            )
        ),
        BusLine(
            id = "L3",
            name = "Aldea Moret – Centro – Mejostilla",
            colorHex = "#38A169", // Verde Línea 3
            headwayMinutes = 20,
            firstServiceMinutes = 7 * 60,
            lastServiceMinutes = 22 * 60 + 30,
            stopOffsets = mapOf(
                "S01" to 0,
                "S05" to 12,
                "S06" to 15,
                "S08" to 18,
                "S13" to 26,
                "S14" to 30,
                "S11" to 38
            )
        ),
        BusLine(
            id = "L4",
            name = "Macondo – Plaza América – Hospital Universitario",
            colorHex = "#DD6B20", // Naranja Línea 4
            headwayMinutes = 25,
            firstServiceMinutes = 7 * 60 + 10,
            lastServiceMinutes = 22 * 60,
            stopOffsets = mapOf(
                "S03" to 0,
                "S07" to 10,
                "S09" to 16,
                "S10" to 19,
                "S17" to 29
            )
        )
    )

    val LINES_BY_ID = LINES.associateBy { it.id }

    val CAMPUS_CENTERS = listOf(
        CampusCenter("politecnica", "Escuela Politécnica", "S19", listOf("politecnica", "la politecnica", "poli", "la poli", "escuela politecnica", "ep")),
        CampusCenter("derecho", "Facultad de Derecho", "S20", listOf("derecho", "facultad de derecho", "filosofia", "letras")),
        CampusCenter("ciencias", "Campus UEx - Ciencias", "S18", listOf("ciencias", "campus", "campus uex", "el campus", "universidad")),
        CampusCenter("hospital", "Hospital Universitario", "S17", listOf("hospital", "el hospital", "hospital universitario", "universitario", "enfermeria")),
        CampusCenter("plaza_america", "Plaza de América", "S07", listOf("plaza de america", "plaza america", "america")),
        CampusCenter("estacion", "Estación de Tren / Autobuses", "S03", listOf("estacion", "la estacion", "tren", "autobuses")),
        CampusCenter("canovas", "Paseo de Cánovas - Centro", "S08", listOf("canovas", "centro", "paseo de canovas", "el centro"))
    )

    // Pre-calculated Historical Confidence table for L1 S02 -> S19 (Wednesday)
    val HISTORICAL_CONFIDENCE_L1 = mapOf(
        "07:45" to 97,
        "08:00" to 95,
        "08:15" to 92,
        "08:30" to 61
    )

    fun createSources(simNow: SimTime): List<Provenance> {
        val updatedRealtime = simNow.addMinutes(-2).toTimeString()
        val updatedYesterday = "Ayer 23:59"
        val updatedStatic = "01/09/2026"

        return listOf(
            Provenance(
                datasetId = "buses_red",
                title = "Red de autobuses urbanos",
                publisher = "Ayuntamiento de Cáceres (simulado)",
                license = "CC0",
                lastUpdated = updatedStatic,
                nature = "estático"
            ),
            Provenance(
                datasetId = "buses_flota",
                title = "Flota en tiempo real",
                publisher = "Ayuntamiento de Cáceres (simulado)",
                license = "CC0",
                lastUpdated = "hace 2 min ($updatedRealtime)",
                nature = "tiempo real"
            ),
            Provenance(
                datasetId = "buses_afluencia",
                title = "Afluencia en autobuses",
                publisher = "Ayuntamiento de Cáceres – sensores (simulado)",
                license = "CC0",
                lastUpdated = "hace 1 min ($updatedRealtime)",
                nature = "tiempo real"
            ),
            Provenance(
                datasetId = "buses_historico",
                title = "Histórico de trayectos",
                publisher = "Ayuntamiento de Cáceres (simulado)",
                license = "CC0",
                lastUpdated = updatedYesterday,
                nature = "histórico, hasta ayer"
            ),
            Provenance(
                datasetId = "trafico_caceres",
                title = "Tráfico",
                publisher = "Ayuntamiento de Cáceres (simulado)",
                license = "CC0",
                lastUpdated = "hace 2 min ($updatedRealtime)",
                nature = "tiempo real"
            ),
            Provenance(
                datasetId = "calendario_academico",
                title = "Calendario académico",
                publisher = "Universidad de Extremadura (simulado)",
                license = "CC BY 4.0",
                lastUpdated = updatedStatic,
                nature = "estático"
            ),
            Provenance(
                datasetId = "aemet_clima",
                title = "Meteorología",
                publisher = "Agencia meteorológica (simulado)",
                license = "CC BY 4.0",
                lastUpdated = "hace 15 min",
                nature = "previsión"
            ),
            Provenance(
                datasetId = "farmacias_colegio",
                title = "Farmacias y guardias",
                publisher = "Colegio de Farmacéuticos de Cáceres (simulado)",
                license = "CC BY 4.0",
                lastUpdated = updatedStatic,
                nature = "estático"
            ),
            Provenance(
                datasetId = "parkings_ciudad",
                title = "Aparcamientos",
                publisher = "Ayuntamiento de Cáceres (simulado)",
                license = "CC0",
                lastUpdated = "hace 3 min ($updatedRealtime)",
                nature = "tiempo real"
            )
        )
    }
}
