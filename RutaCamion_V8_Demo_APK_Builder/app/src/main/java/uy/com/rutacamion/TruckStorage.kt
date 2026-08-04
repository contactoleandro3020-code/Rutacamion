package uy.com.rutacamion

import android.content.Context

class TruckStorage(context: Context) {
    private val preferences = context.getSharedPreferences("truck_profile", Context.MODE_PRIVATE)

    fun load(): TruckProfile = TruckProfile(
        weightTons = preferences.getFloat("weight", 20f).toDouble(),
        heightMeters = preferences.getFloat("height", 4.1f).toDouble(),
        widthMeters = preferences.getFloat("width", 2.6f).toDouble(),
        lengthMeters = preferences.getFloat("length", 16.5f).toDouble()
    )

    fun save(profile: TruckProfile) {
        preferences.edit()
            .putFloat("weight", profile.weightTons.toFloat())
            .putFloat("height", profile.heightMeters.toFloat())
            .putFloat("width", profile.widthMeters.toFloat())
            .putFloat("length", profile.lengthMeters.toFloat())
            .apply()
    }
}
