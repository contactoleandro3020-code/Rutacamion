package uy.com.rutacamion

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

object ValhallaClient {
    // Endpoint público solo para demostración. Para producción debe usarse una instancia propia.
    private const val BASE_URL = "https://valhalla1.openstreetmap.de/route"

    suspend fun route(start: GeoPoint, end: GeoPoint, truck: TruckProfile): Result<RouteOption> = withContext(Dispatchers.IO) {
        runCatching {
            val payload = JSONObject().apply {
                put("locations", org.json.JSONArray().apply {
                    put(JSONObject().put("lat", start.latitude).put("lon", start.longitude))
                    put(JSONObject().put("lat", end.latitude).put("lon", end.longitude))
                })
                put("costing", "truck")
                put("costing_options", JSONObject().put("truck", JSONObject().apply {
                    put("height", truck.heightMeters)
                    put("width", truck.widthMeters)
                    put("length", truck.lengthMeters)
                    put("weight", truck.weightTons)
                }))
                put("units", "kilometers")
                put("language", "es-ES")
            }
            val url = URL("$BASE_URL?json=${URLEncoder.encode(payload.toString(), "UTF-8")}")
            val connection = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 12000
                readTimeout = 20000
                requestMethod = "GET"
                setRequestProperty("User-Agent", "RutaCamion-Demo/0.2")
            }
            val body = (if (connection.responseCode in 200..299) connection.inputStream else connection.errorStream)
                .bufferedReader().use { it.readText() }
            if (connection.responseCode !in 200..299) error("Servidor de rutas: ${connection.responseCode}")
            val root = JSONObject(body)
            val trip = root.getJSONObject("trip")
            val summary = trip.getJSONObject("summary")
            val legs = trip.getJSONArray("legs")
            val points = mutableListOf<GeoPoint>()
            for (i in 0 until legs.length()) {
                points += decodePolyline6(legs.getJSONObject(i).getString("shape"))
            }
            RouteOption(
                name = "Ruta para camión",
                distanceKm = summary.getDouble("length").toInt(),
                estimatedMinutes = (summary.getDouble("time") / 60.0).toInt(),
                restrictions = emptyList(),
                geometry = points.distinct()
            )
        }
    }

    private fun decodePolyline6(encoded: String): List<GeoPoint> {
        val result = mutableListOf<GeoPoint>()
        var index = 0; var lat = 0; var lon = 0
        while (index < encoded.length) {
            var shift = 0; var value = 0; var b: Int
            do { b = encoded[index++].code - 63; value = value or ((b and 0x1f) shl shift); shift += 5 } while (b >= 0x20)
            lat += if (value and 1 != 0) (value shr 1).inv() else value shr 1
            shift = 0; value = 0
            do { b = encoded[index++].code - 63; value = value or ((b and 0x1f) shl shift); shift += 5 } while (b >= 0x20)
            lon += if (value and 1 != 0) (value shr 1).inv() else value shr 1
            result += GeoPoint(lat / 1e6, lon / 1e6)
        }
        return result
    }
}
