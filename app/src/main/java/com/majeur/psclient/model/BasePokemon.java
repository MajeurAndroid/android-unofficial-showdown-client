package com.majeur.psclient.model;

import java.io.Serializable;

import static com.majeur.psclient.model.Id.toId;
import static com.majeur.psclient.util.Utils.contains;
import static com.majeur.psclient.util.Utils.substring;

public class BasePokemon implements Serializable {

    public String species;
    public String baseSpecies;
    public String forme;
    public String spriteId;

    public BasePokemon(String rawSpecies) {
        String id = toId(rawSpecies);
        species = rawSpecies;

        String[] excluded = {"hooh", "hakamoo", "jangmoo", "kommoo", "porygonz"};
        if (!contains(id, excluded)) {
            int dashIndex = rawSpecies.indexOf('-');
            if (id.equals("kommoototem")) {
                baseSpecies = "Kommo-o";
                forme = "Totem";
            } else if (dashIndex > 0) {
                baseSpecies = rawSpecies.substring(0, dashIndex);
                forme = rawSpecies.substring(dashIndex + 1);
            }
        }

        if (!id.equals("yanmega") && "mega".equals(substring(id, -4))) {
            baseSpecies = substring(id, 0, -4);
            forme = substring(id, -4);
        } else if ("primal".equals(substring(id, -6))) {
            baseSpecies = substring(id, 0, -6);
            forme = substring(id, -6);
        } else if ("alola".equals(substring(id, -5))) {
            baseSpecies = substring(id, 0, -5);
            forme = substring(id, -5);
        }

        if (baseSpecies == null) baseSpecies = species;
        String formeId = forme != null ? toId(forme) : "";
        spriteId = toId(baseSpecies) + "-" + formeId;
        if ("totem".equals(substring(spriteId, -5))) spriteId = substring(spriteId, 0, -5);
        if ("-".equals(substring(spriteId, -1))) spriteId = substring(spriteId, 0, -1);
    }
}
