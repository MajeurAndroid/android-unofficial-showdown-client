package com.majeur.psclient.model;

import android.text.TextUtils;

import java.io.Serializable;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import static com.majeur.psclient.model.Id.toId;
import static com.majeur.psclient.model.Id.toIdSafe;
import static com.majeur.psclient.util.Utils.parseWithDefault;

public class Team implements Serializable {

    private static int sUniqueIdInc = 1;

    private static String copiedTeamLabel(String label) {
        return label + " (Copy)";
//        TODO: Take in account other labels of the same group that could match
//        String newLabel = label;
//        Pattern pattern = Pattern.compile("\\((Copy)(\\d*)\\)$");
//        Matcher matcher = pattern.matcher(newLabel);
//        if (matcher.find()) {
//            String group2 = matcher.group(2);
//            if (TextUtils.isEmpty(group2)) {
//                newLabel = newLabel.substring(0, newLabel.length() - 1) + "2)";
//            } else {
//                int n = Integer.parseInt(group2);
//                newLabel = newLabel.substring(0, newLabel.lastIndexOf(str(n))) + ++n + ")";
//            }
//        } else {
//            newLabel = newLabel + " (Copy)";
//        }
//        return newLabel;
    }

    public static class Group {

        public final String format;
        public final List<Team> teams;

        public Group(String format) {
            this.format = format;
            this.teams = new LinkedList<>();
        }

        public void sort() {
            Collections.sort(teams, new Comparator<Team>() {
                @Override
                public int compare(Team t1, Team t2) {
                    return t1.label.toLowerCase().compareTo(t2.label.toLowerCase());
                }
            });
        }
    }

    public static Team dummyTeam(String label) {
        List<TeamPokemon> pokemons = new LinkedList<>();
        for (int i = 0; i < 6; i++) pokemons.add(TeamPokemon.dummyPokemon());
        return new Team(label, pokemons, null);
    }

    public final int uniqueId;
    public String label;
    public String format;
    public final List<TeamPokemon> pokemons;

    private Team(int uniqueId, String label, List<TeamPokemon> pokemons, String format) {
        this.uniqueId = uniqueId;
        this.label = label;
        this.pokemons = pokemons;
        this.format = format;
    }

    public Team(String label, List<TeamPokemon> pokemons, String format) {
        this(sUniqueIdInc++, label, pokemons, format);
    }

    public Team(Team source) {
        this(copiedTeamLabel(source.label), source.pokemons, source.format);
    }

    public boolean isEmpty() {
        return pokemons.isEmpty();
    }

    public String pack() {
        StringBuilder buf = new StringBuilder();

        for (TeamPokemon set : pokemons) {
            if (buf.length() > 0) buf.append("]");

            // name
            buf.append(set.name != null ? set.name : set.species);

            // species
            String id = toId(set.species);
            boolean blank = set.name == null || toId(set.name).equals(id);
            buf.append("|").append(blank ? "" : set.species);

            // item
            buf.append("|").append(toIdSafe(set.item));

            // ability
            buf.append("|").append(toIdSafe(set.ability));

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

    public static Team unpack(final String label, String format, String buf) {
        if (buf == null || TextUtils.isEmpty(buf))
            return new Team(label, new LinkedList<TeamPokemon>(), format);

        if (buf.charAt(0) == '[' && buf.charAt(buf.length() - 1) == ']') {
            // TODO buf = this.packTeam(JSON.parse(buf));
        }

        List<TeamPokemon> team = new LinkedList<>();
        int i = 0, j = 0;

        // limit to 24
        for (int count = 0; count < 24; count++) {

            // name
            j = buf.indexOf('|', i);
            if (j < 0) return null;
            String name = buf.substring(i, j);
            i = j + 1;

            // species
            j = buf.indexOf('|', i);
            if (j < 0) return null;
            String species = buf.substring(i, j);
            if (species.equals(""))
                species = name;
            TeamPokemon pokemon = new TeamPokemon(species);
            team.add(pokemon);
            i = j + 1;

            // item
            j = buf.indexOf('|', i);
            if (j < 0) return null;
            pokemon.item = buf.substring(i, j);
            i = j + 1;

            // ability
            j = buf.indexOf('|', i);
            if (j < 0) return null;
            pokemon.ability = buf.substring(i, j);
            i = j + 1;

            // moves
            j = buf.indexOf('|', i);
            if (j < 0) return null;
            pokemon.moves = buf.substring(i, j).split(",", 24);//.filter(x => x);
            i = j + 1;

            // nature
            j = buf.indexOf('|', i);
            if (j < 0) return null;
            pokemon.nature = buf.substring(i, j);
            i = j + 1;

            // evs
            j = buf.indexOf('|', i);
            if (j < 0) return null;
            if (j != i) {
                String[] evs = buf.substring(i, j).split(",", 6);
                pokemon.evs.hp = parseWithDefault(evs[0], 0);
                pokemon.evs.atk = parseWithDefault(evs[1], 0);
                pokemon.evs.def = parseWithDefault(evs[2], 0);
                pokemon.evs.spa = parseWithDefault(evs[3], 0);
                pokemon.evs.spd = parseWithDefault(evs[4], 0);
                pokemon.evs.spe = parseWithDefault(evs[5], 0);
            }
            i = j + 1;

            // gender
            j = buf.indexOf('|', i);
            if (j < 0) return null;
            if (i != j) pokemon.gender = buf.substring(i, j);
            i = j + 1;

            // ivs
            j = buf.indexOf('|', i);
            if (j < 0) return null;
            if (j != i) {
                String[] ivs = buf.substring(i, j).split(",", 6);
                pokemon.ivs.hp = parseWithDefault(ivs[0], 31);
                pokemon.ivs.atk = parseWithDefault(ivs[1], 31);
                pokemon.ivs.def = parseWithDefault(ivs[2], 31);
                pokemon.ivs.spa = parseWithDefault(ivs[3], 31);
                pokemon.ivs.spd = parseWithDefault(ivs[4], 31);
                pokemon.ivs.spe = parseWithDefault(ivs[5], 31);
            }
            i = j + 1;

            // shiny
            j = buf.indexOf('|', i);
            if (j < 0) return null;
            if (i != j) pokemon.shiny = true;
            i = j + 1;

            // level
            j = buf.indexOf('|', i);
            if (j < 0) return null;
            if (i != j) pokemon.level = Integer.parseInt(buf.substring(i, j));
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
                pokemon.happiness = parseWithDefault(misc[0], 255);
//                poke.hpType = misc[1];
//                poke.pokeball = misc[2];TODO
            }
            if (j < 0) break;
            i = j + 1;
        }

        return new Team(label, team, format);
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
