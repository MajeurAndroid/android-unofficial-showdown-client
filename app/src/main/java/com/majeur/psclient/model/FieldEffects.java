package com.majeur.psclient.model;

import com.majeur.psclient.R;

import static com.majeur.psclient.model.Id.toIdSafe;

public class FieldEffects {

    public static int getDrawableResourceId(String name) {
        String id = toIdSafe(name);
        switch (id) {
            case "electricterrain": return R.drawable.weather_electricterrain;
            case "grassyterrain": return R.drawable.weather_grassyterrain;
            case "hail": return R.drawable.weather_hail;
            case "mistyterrain": return R.drawable.weather_mistyterrain;
            case "psychicterrain": return R.drawable.weather_psychicterrain;
            case "primordialsea":
            case "raindance": return R.drawable.weather_raindance;
            case "sandstorm": return R.drawable.weather_sandstorm;
            case "strongwind": return R.drawable.weather_strongwind;
            case "desolateland":
            case "sunnyday": return R.drawable.weather_sunnyday;
            case "trickroom": return R.drawable.weather_trickroom;
            default: return -1;
        }
    }
}
