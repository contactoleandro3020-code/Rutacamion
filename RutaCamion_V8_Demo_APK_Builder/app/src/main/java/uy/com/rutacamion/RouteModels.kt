package uy.com.rutacamion

data class GeoPoint(val latitude: Double, val longitude: Double)

data class TruckProfile(
    val weightTons: Double = 20.0,
    val heightMeters: Double = 4.1,
    val widthMeters: Double = 2.6,
    val lengthMeters: Double = 16.5
)

data class RoadRestriction(
    val name: String,
    val maxWeightTons: Double? = null,
    val maxHeightMeters: Double? = null,
    val maxWidthMeters: Double? = null,
    val maxLengthMeters: Double? = null,
    val note: String
)

data class RouteOption(
    val name: String,
    val distanceKm: Int,
    val estimatedMinutes: Int,
    val restrictions: List<RoadRestriction>,
    val geometry: List<GeoPoint>
)

data class RouteAssessment(
    val route: RouteOption,
    val isCompatible: Boolean,
    val warnings: List<String>
)

object DemoRouteEngine {
    private val montevideo = GeoPoint(-34.9011, -56.1645)
    private val florida = GeoPoint(-34.0956, -56.2142)
    private val durazno = GeoPoint(-33.3806, -56.5236)
    private val pasoDeLosToros = GeoPoint(-32.8167, -56.5167)
    private val tacuarembo = GeoPoint(-31.7169, -55.9811)
    private val rivera = GeoPoint(-30.9053, -55.5508)

    private val routes = mapOf(
        "Montevideo → Tacuarembó" to listOf(
            RouteOption(
                name = "Ruta principal por RN5",
                distanceKm = 390,
                estimatedMinutes = 300,
                restrictions = emptyList(),
                geometry = listOf(montevideo, florida, durazno, pasoDeLosToros, tacuarembo)
            ),
            RouteOption(
                name = "Alternativa urbana de demostración",
                distanceKm = 382,
                estimatedMinutes = 325,
                restrictions = listOf(
                    RoadRestriction(
                        name = "Puente de demostración",
                        maxWeightTons = 18.0,
                        maxHeightMeters = 3.9,
                        note = "Dato de prueba; deberá reemplazarse por información oficial."
                    )
                ),
                geometry = listOf(montevideo, florida, durazno, GeoPoint(-32.55, -56.25), tacuarembo)
            )
        ),
        "Montevideo → Rivera" to listOf(
            RouteOption(
                "Ruta principal por RN5",
                500,
                390,
                emptyList(),
                listOf(montevideo, florida, durazno, pasoDeLosToros, tacuarembo, rivera)
            ),
            RouteOption(
                "Desvío local de demostración",
                480,
                420,
                listOf(
                    RoadRestriction(
                        name = "Tramo angosto de demostración",
                        maxWidthMeters = 2.5,
                        note = "Dato de prueba para validar el motor de restricciones."
                    )
                ),
                listOf(montevideo, florida, durazno, GeoPoint(-32.2, -55.7), rivera)
            )
        ),
        "Tacuarembó → Montevideo" to listOf(
            RouteOption(
                "Ruta principal por RN5",
                390,
                300,
                emptyList(),
                listOf(tacuarembo, pasoDeLosToros, durazno, florida, montevideo)
            ),
            RouteOption(
                "Acceso secundario de demostración",
                375,
                335,
                listOf(
                    RoadRestriction(
                        name = "Curva cerrada de demostración",
                        maxLengthMeters = 14.0,
                        note = "Dato de prueba para vehículos largos."
                    )
                ),
                listOf(tacuarembo, GeoPoint(-32.45, -56.05), durazno, florida, montevideo)
            )
        )
    )

    fun destinations(): List<String> = routes.keys.sorted()

    fun endpoints(destination: String): Pair<GeoPoint, GeoPoint>? = when (destination) {
        "Montevideo → Tacuarembó" -> montevideo to tacuarembo
        "Montevideo → Rivera" -> montevideo to rivera
        "Tacuarembó → Montevideo" -> tacuarembo to montevideo
        else -> null
    }

    fun assess(destination: String, truck: TruckProfile): List<RouteAssessment> {
        return routes[destination].orEmpty().map { route ->
            val warnings = route.restrictions.flatMap { restriction ->
                buildList {
                    restriction.maxWeightTons?.let { max ->
                        if (truck.weightTons > max) add("Excede el peso máximo de ${format(max)} t en ${restriction.name}.")
                    }
                    restriction.maxHeightMeters?.let { max ->
                        if (truck.heightMeters > max) add("Excede la altura máxima de ${format(max)} m en ${restriction.name}.")
                    }
                    restriction.maxWidthMeters?.let { max ->
                        if (truck.widthMeters > max) add("Excede el ancho máximo de ${format(max)} m en ${restriction.name}.")
                    }
                    restriction.maxLengthMeters?.let { max ->
                        if (truck.lengthMeters > max) add("Excede el largo máximo de ${format(max)} m en ${restriction.name}.")
                    }
                }
            }
            RouteAssessment(route, warnings.isEmpty(), warnings)
        }.sortedWith(compareByDescending<RouteAssessment> { it.isCompatible }.thenBy { it.route.estimatedMinutes })
    }

    private fun format(value: Double): String =
        if (value % 1.0 == 0.0) value.toInt().toString() else value.toString().replace('.', ',')
}
