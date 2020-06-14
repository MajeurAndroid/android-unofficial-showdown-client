package com.majeur.psclient.model.battle

import com.majeur.psclient.R
import com.majeur.psclient.util.toId


object FieldEffects {

    fun getDrawableResourceId(name: String?): Int {
        return when (name?.toId()) {
            "electricterrain" -> R.drawable.weather_electricterrain
            "grassyterrain" -> R.drawable.weather_grassyterrain
            "hail" -> R.drawable.weather_hail
            "mistyterrain" -> R.drawable.weather_mistyterrain
            "psychicterrain" -> R.drawable.weather_psychicterrain
            "primordialsea", "raindance" -> R.drawable.weather_raindance
            "sandstorm" -> R.drawable.weather_sandstorm
            "strongwind" -> R.drawable.weather_strongwind
            "desolateland", "sunnyday" -> R.drawable.weather_sunnyday
            "trickroom" -> R.drawable.weather_trickroom
            else -> -1
        }
    }
}