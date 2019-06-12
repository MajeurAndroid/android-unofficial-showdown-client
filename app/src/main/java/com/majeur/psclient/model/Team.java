package com.majeur.psclient.model;

import android.text.TextUtils;
import android.util.Log;

import java.util.LinkedList;
import java.util.List;

import static com.majeur.psclient.model.Id.toId;
import static com.majeur.psclient.util.Utils.parseWithDefault;

public class Team {

    public static class Group {

        public Group(String format) {
            this.format = format;
            this.teams = new LinkedList<>();
        }

        public final String format;
        public final List<Team> teams;
    }

    public static Team dummyTeam(String label) {
        List<Poke> pokemons = new LinkedList<>();
        for (int i = 0; i < 6; i++) pokemons.add(Poke.dummyPokemon());
        return new Team(label, pokemons, null);
    }

    public final String label;
    public final String format;
    public final List<Poke> pokemons;

    public Team(String label, List<Poke> pokemons, String format) {
        this.label = label;
        this.pokemons = pokemons;
        this.format = format;
    }

    public String pack() {
        StringBuilder buf = new StringBuilder();

        for (Poke set : pokemons) {
            if (buf.length() > 0) buf.append("]");

            // name
            buf.append(set.name != null ? set.name : set.species);

            // species
            String id = toId(set.species);
            boolean blank = set.name == null || toId(set.name).equals(id);
            buf.append("|").append(blank ? "" : set.species);

            // item
            buf.append("|").append(toId(set.item));

            // ability
                if (set.abilitySlot == null || set.abilitySlot.equals("0")) {
                    buf.append("|");
                } else if (set.abilitySlot.equals("1")) {
                    buf.append("|1");
                } else if (set.abilitySlot.equals("H")) {
                    buf.append("|H");
                } else {
                    buf.append("|").append(id);
                }

            // moves
            buf.append("|").append(TextUtils.join(",", set.moves));

            // nature
            buf.append("|").append(set.nature);

            // evs
            buf.append("|");
            if (set.evs.hp != 0)
                buf.append(set.evs.hp);
            buf.append(",");
            if (set.evs.atk != 0)
                buf.append(set.evs.atk);
            buf.append(",");
            if (set.evs.def != 0)
                buf.append(set.evs.def);
            buf.append(",");
            if (set.evs.spa != 0)
                buf.append(set.evs.spa);
            buf.append(",");
            if (set.evs.spd != 0)
                buf.append(set.evs.spd);
            buf.append(",");
            if (set.evs.spe != 0)
                buf.append(set.evs.spe);

            // gender
            if (set.gender != null) {
                buf.append("|").append(set.gender);
            } else {
                buf.append("|");
            }

            // ivs
            buf.append("|");
            if (set.ivs.hp != 31)
                buf.append(set.ivs.hp);
            buf.append(",");
            if (set.ivs.atk != 31)
                buf.append(set.ivs.atk);
            buf.append(",");
            if (set.ivs.def != 31)
                buf.append(set.ivs.def);
            buf.append(",");
            if (set.ivs.spa != 31)
                buf.append(set.ivs.spa);
            buf.append(",");
            if (set.ivs.spd != 31)
                buf.append(set.ivs.spd);
            buf.append(",");
            if (set.ivs.spe != 31)
                buf.append(set.ivs.spe);

            // shiny
            if (set.shiny) {
                buf.append("|S");
            } else {
                buf.append("|");
            }

            // level
            if (set.level != 100) {
                buf.append("|").append(set.level);
            } else {
                buf.append("|");
            }

            // happiness
            if (set.happiness != 255) {
                buf.append("|").append(set.happiness);
            } else {
                buf.append("|");
            }

            if (set.hpType != null)
                buf.append(",").append(set.hpType);

            if (set.pokeball != null && !set.pokeball.equals("pokeball"))
                buf.append(",").append(toId(set.pokeball));
        }

        return buf.toString();
    }

    public static Team unpack(final String label, String buf) {
        if (buf == null) return null;

        if (buf.charAt(0) == '[' && buf.charAt(buf.length() - 1) == ']') {
            // TODO buf = this.packTeam(JSON.parse(buf));
        }

        List<Poke> team = new LinkedList<>();
        int i = 0, j = 0;

        // limit to 24
        for (int count = 0; count < 24; count++) {
            Poke poke = new Poke();
            team.add(poke);

            // name
            j = buf.indexOf('|', i);
            if (j < 0) return null;
            poke.name = buf.substring(i, j);
            i = j + 1;

            // species
            j = buf.indexOf('|', i);
            if (j < 0) return null;
            poke.species = buf.substring(i, j);
            if (poke.species.equals(""))
                poke.species = poke.name;
            i = j + 1;

            // item
            j = buf.indexOf('|', i);
            if (j < 0) return null;
            poke.item = buf.substring(i, j);
            i = j + 1;

            // ability
            j = buf.indexOf('|', i);
            if (j < 0) return null;
            poke.ability = buf.substring(i, j);
            i = j + 1;

            // moves
            j = buf.indexOf('|', i);
            if (j < 0) return null;
            poke.moves = buf.substring(i, j).split(",", 24);//.filter(x => x);
            i = j + 1;

            // nature
            j = buf.indexOf('|', i);
            if (j < 0) return null;
            poke.nature = buf.substring(i, j);
            i = j + 1;

            // evs
            j = buf.indexOf('|', i);
            if (j < 0) return null;
            if (j != i) {
                String[] evs = buf.substring(i, j).split(",", 6);
                poke.evs.hp = parseWithDefault(evs[0], 0);
                poke.evs.atk = parseWithDefault(evs[1], 0);
                poke.evs.def = parseWithDefault(evs[2], 0);
                poke.evs.spa = parseWithDefault(evs[3], 0);
                poke.evs.spd = parseWithDefault(evs[4], 0);
                poke.evs.spe = parseWithDefault(evs[5], 0);
            }
            i = j + 1;

            // gender
            j = buf.indexOf('|', i);
            if (j < 0) return null;
            if (i != j) poke.gender = buf.substring(i, j);
            i = j + 1;

            // ivs
            j = buf.indexOf('|', i);
            if (j < 0) return null;
            if (j != i) {
                String[] ivs = buf.substring(i, j).split(",", 6);
                poke.ivs.hp  = parseWithDefault(ivs[0], 31);
                poke.ivs.atk = parseWithDefault(ivs[1], 31);
                poke.ivs.def = parseWithDefault(ivs[2], 31);
                poke.ivs.spa = parseWithDefault(ivs[3], 31);
                poke.ivs.spd = parseWithDefault(ivs[4], 31);
                poke.ivs.spe = parseWithDefault(ivs[5], 31);
            }
            i = j + 1;

            // shiny
            j = buf.indexOf('|', i);
            if (j < 0) return null;
            if (i != j) poke.shiny = true;
            i = j + 1;

            // level
            j = buf.indexOf('|', i);
            if (j < 0) return null;
            if (i != j) poke.level = Integer.parseInt(buf.substring(i, j));
            i = j + 1;

            // happiness
            j = buf.indexOf(']', i);
            String[] misc = null;
            if (j < 0) {
                if (i < buf.length()) misc = buf.substring(i).split(",", 3);
            } else {
                if (i != j) misc = buf.substring(i, j).split(",", 3);
            }
            if (misc != null) {
                poke.happiness = parseWithDefault(misc[0], 255);
//                poke.hpType = misc[1];
//                poke.pokeball = misc[2];TODO
            }
            if (j < 0) break;
            i = j + 1;
        }

        return new Team(label, team, null);
    }

    @Override
    public String toString() {
        return "Team{" +
                "label='" + label + '\'' +
                ", format='" + format + '\'' +
                ", pokemons=" + pokemons +
                '}';
    }
}
