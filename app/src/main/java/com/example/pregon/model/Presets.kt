package com.example.pregon.model

data class NeighborhoodPreset(
    val name: String,
    val description: String,
    val lat: Double,
    val lon: Double,
    val originStops: List<OriginStop>
)

object HomePresets {
    val PRESETS = listOf(
        NeighborhoodPreset(
            name = "Nuevo Cáceres",
            description = "Zona sur · Paradas S02 y S03",
            lat = 39.4582,
            lon = -6.3792,
            originStops = listOf(
                OriginStop("S02", "Nuevo Cáceres - Plaza", 4),
                OriginStop("S03", "Estación de Tren / Autobuses", 7)
            )
        ),
        NeighborhoodPreset(
            name = "Mejostilla",
            description = "Zona norte · Paradas S11 y S12",
            lat = 39.4895,
            lon = -6.3685,
            originStops = listOf(
                OriginStop("S11", "Mejostilla - Héroes de Baler", 3),
                OriginStop("S12", "Mejostilla - Ronda Norte", 6)
            )
        ),
        NeighborhoodPreset(
            name = "Aldea Moret",
            description = "Zona suroeste · Parada S01",
            lat = 39.4538,
            lon = -6.3985,
            originStops = listOf(
                OriginStop("S01", "Aldea Moret - Av. Constitución", 4)
            )
        ),
        NeighborhoodPreset(
            name = "Centro – Cánovas",
            description = "Centro de la ciudad · Paradas S08 y S07",
            lat = 39.4731,
            lon = -6.3725,
            originStops = listOf(
                OriginStop("S08", "Paseo de Cánovas", 3),
                OriginStop("S07", "Plaza de América", 5)
            )
        ),
        NeighborhoodPreset(
            name = "R-66",
            description = "Zona oeste · Paradas S14 y S13",
            lat = 39.4815,
            lon = -6.3892,
            originStops = listOf(
                OriginStop("S14", "R-66 - Delicias", 4),
                OriginStop("S13", "R-66 - Coubertin", 7)
            )
        )
    )

    val DEFAULT_HOME = PRESETS[0] // Nuevo Cáceres
}
