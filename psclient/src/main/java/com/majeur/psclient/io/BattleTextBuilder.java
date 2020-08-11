package com.majeur.psclient.io;

import android.content.Context;
import android.os.AsyncTask;
import android.text.TextUtils;
import android.util.Log;
import androidx.annotation.Nullable;
import com.majeur.psclient.R;
import com.majeur.psclient.model.battle.Player;
import com.majeur.psclient.model.battle.PokemonId;
import com.majeur.psclient.model.pokemon.BattlingPokemon;
import com.majeur.psclient.util.Utils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.InputStream;
import java.util.concurrent.ExecutionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static android.text.TextUtils.isEmpty;
import static com.majeur.psclient.util.Utils.contains;
import static com.majeur.psclient.util.Utils.firstCharUpperCase;
import static com.majeur.psclient.util.Utils.parseBoldTags;


/**
 * This class is a java replica of https://github.com/smogon/pokemon-showdown-client/blob/master/src/battle-text-parser.ts
 * Last updated on 8 may of 2020
 * More and less corresponding to this commit:
 * https://github.com/smogon/pokemon-showdown-client/commit/0c19bb3c82d39c9b01cdf44723af945b7622fc4a#diff-73c45fc8dbf03d7d5353d2ad39123f96
 */
public final class BattleTextBuilder {

    /* QUICK FIXES BEFORE KOTLIN */

    private static String toId(String v) {
        return com.majeur.psclient.util.ExtensionsKt.toId(v);
    }

    private static String toId(String v, String fallback) {
        return v != null ? toId(v) : fallback;
    }

    private static String toIdSafe(String v) {
        return toId(v, "");
    }

    public static String str(int number) {
        return com.majeur.psclient.util.ExtensionsKt.toSignedString(number);
    }

    /* QUICK FIXES BEFORE KOTLIN */

    private static final String TAG = BattleTextBuilder.class.getSimpleName();

    private static final String PH_TRAINER = "[TRAINER]";
    private static final String PH_NICKNAME = "[NICKNAME]";
    private static final String PH_NUMBER = "[NUMBER]";
    private static final String PH_FULLNAME = "[FULLNAME]";
    private static final String PH_POKEMON = "[POKEMON]";
    private static final String PH_TARGET = "[TARGET]";
    private static final String PH_MOVE = "[MOVE]";
    private static final String PH_ABILITY = "[ABILITY]";
    private static final String PH_ITEM = "[ITEM]";
    private static final String PH_SPECIES = "[SPECIES]";
    private static final String PH_TYPE = "[TYPE]";
    private static final String PH_EFFECT = "[EFFECT]";
    private static final String PH_TEAM = "[TEAM]";
    private static final String PH_SOURCE = "[SOURCE]";
    private static final String PH_PERCENTAGE = "[PERCENTAGE]";
    private static final String PH_STAT = "[STAT]";
    private static final String PH_PARTY = "[PARTY]";
    private static final String PH_NAME = "[NAME]";

    private JSONObject mJSONObject;
    private JsonReadTask mJsonReadTask;
    private PokemonIdFactory mPokemonIdFactory;

    public BattleTextBuilder(Context context) {
        InputStream inputStream = context.getResources().openRawResource(R.raw.battle_texts);
        mJsonReadTask = new JsonReadTask(jsonObject -> {
            mJSONObject = jsonObject;
            mJsonReadTask = null;
        });
        mJsonReadTask.execute(inputStream);
    }

    public void setPokemonIdFactory(PokemonIdFactory pokemonIdFactory) {
        mPokemonIdFactory = pokemonIdFactory;
    }

    // If IO is like REALLY low on some devices, ensure we would be ready.
    private void checkReady() {
        if (mJSONObject == null) {
            try {
                mJSONObject = mJsonReadTask.get();
            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private String resolve(String objectKey, String key, boolean useDefault) {
        if (key == null) return null;
        if (objectKey == null) {
            if (!useDefault) return null;
            objectKey = "default";
        }
        objectKey = toId(effect(objectKey));
        JSONObject object = mJSONObject.optJSONObject(objectKey);
        if (object == null) {
            if (!useDefault) return null;
            objectKey = "default";
            object = mJSONObject.optJSONObject(objectKey);
        }
        String template = object.optString(key);
        if (isEmpty(template)) {
            if (!useDefault || objectKey.equals("default")) return null;
            template = mJSONObject.optJSONObject("default").optString(key);
            if (isEmpty(template)) return null;
        }
        if (template.charAt(0) == '#')
            return resolve(template.substring(1), key);
        return template;
    }

    private String resolve(String objectKey, String key) {
        return resolve(objectKey, key, true);
    }

    private String resolve(String key) {
        return resolve("default", key);
    }

    private String resolveOwn(String objectKey, String key, boolean own) {
        return resolve(objectKey, key + (own ? "Own" : ""));
    }

    private String resolveOwn(String key, boolean own) {
        return resolveOwn("default", key, own);
    }

    private String formatPlaceHolders(String template, String... formats) {
        if (template == null) return null;
        if (formats.length % 2 != 0)
            throw new IllegalArgumentException("Args formats aren't well mapped (Must be filled with 'ph/value' pairs)");
        for (int i = 0; i < formats.length - 1; ) {
            String placeHolder = formats[i++];
            String value = formats[i++];
            if (placeHolder == null || value == null) continue;
            template = template.replaceFirst(Pattern.quote(placeHolder),
                    Matcher.quoteReplacement(value));
        }
        return template.trim();
    }

    private CharSequence line(String template, String... formats) {
        return line(formatPlaceHolders(template, formats));
    }

    private CharSequence line(String lineContent) {
        if (lineContent == null || lineContent.trim().equals("null")) return null;
        return parseBoldTags(firstCharUpperCase(lineContent.trim()));
    }

    private CharSequence lines(CharSequence line1, CharSequence line2) {
        if (line1 == null && line2 != null) return line2;
        if (line1 != null && line2 == null) return line1;
        else return TextUtils.concat(line1, "\n", line2);
    }

    private CharSequence lines(CharSequence line1, CharSequence line2, CharSequence line3) {
        if (line1 == null && line2 == null && line3 != null) return line3;
        if (line1 == null && line2 != null && line3 == null) return line2;
        if (line1 != null && line2 == null && line3 == null) return line1;
        if (line1 != null && line2 != null && line3 == null) return lines(line1, line2);
        if (line1 != null && line2 == null && line3 != null) return lines(line1, line3);
        if (line1 == null && line2 != null && line3 != null) return lines(line2, line3);
        else return TextUtils.concat(line1, "\n", line2, "\n", line3);
    }

    private String pokemon(PokemonId pokemonId) {
        if (pokemonId == null) return null;
        if (pokemonId.getFoe())
            return formatPlaceHolders(resolve("opposingPokemon"), PH_NICKNAME, pokemonId.getName());
        else
            return formatPlaceHolders(resolve("pokemon"), PH_NICKNAME, pokemonId.getName());
    }

    private PokemonId getPokemonId(String rawPoke) {
        if (rawPoke == null) return null;
        if (mPokemonIdFactory == null)
            throw new IllegalStateException("PokemonIdFactory isn't set");
        return mPokemonIdFactory.getPokemonId(rawPoke);
    }

    private String pokemon(String rawPoke) {
        if (rawPoke == null) return null;
        PokemonId id = getPokemonId(rawPoke);
        if (id == null) return "???poke:" + rawPoke + "???";
        return pokemon(id);
    }

    private String pokemonFull(BattlingPokemon pokemon) {
		String nickname = pokemon.getName();
		String species = pokemon.getSpecies();
        if (species.equalsIgnoreCase(nickname) || nickname == null)
            return "**" + species + "**";
        return nickname + " (**" + species + "**)";
    }

    private String team(PokemonId pokemonId) {
        if (pokemonId == null) return null;
        return resolve(pokemonId.getFoe() ? "opposingTeam" : "team");
    }

    private String team(Player player) {
        if (player == null) return null;
        return resolve(player == Player.FOE ? "opposingTeam" : "team");
    }

    private String party(Player player) {
        return resolve(player == Player.FOE ? "opposingParty" : "party");
    }

    private String trainer(PokemonId pokemonId) {
        if (pokemonId == null) return null;
        return resolve(pokemonId.getFoe() ? "opposingTeam" : "team");
    }

    public String effect(String effect) {
        if (effect == null) return null;
        if (effect.startsWith("item:") || effect.startsWith("move:"))
            effect = effect.substring(5);
        else if (effect.startsWith("ability:"))
            effect = effect.substring(8);
        return effect.trim();
    }

    private String ability(String name, PokemonId holder) {
        if (name == null) return null;
        return formatPlaceHolders(resolve("abilityActivation"), PH_POKEMON, pokemon(holder), PH_ABILITY, effect(name));
    }

    private String maybeAbility(String effect, PokemonId holder) {
        if (effect == null || !effect.startsWith("ability:")) return null;
        return ability(effect.substring(8).trim(), holder);
    }

    private String maybeAbility(String effect, String rawPoke) {
        if (rawPoke == null) return null;
        PokemonId id = getPokemonId(rawPoke);
        if (id == null) return "???poke:" + rawPoke + "???";
        return maybeAbility(effect, id);
    }

    private String stat(String stat) {
        String name = resolve(stat != null ? stat : "stats", "statName");
        if (name == null) return "???stat:" + stat + "???";
        return name;
    }

    public CharSequence turn(int number) {
        return line(resolve("turn"), PH_NUMBER, str(number));
    }

    public CharSequence start(String username1, String username2) {
        checkReady();
        return line(resolve("startBattle"), PH_TRAINER, username1, PH_TRAINER, username2);
    }


    public CharSequence win(String username) {
        return line(resolve("winBattle"), PH_TRAINER, username);
    }

    public CharSequence tie(String username1, String username2) {
        return line(resolve("tieBattle"),
                PH_TRAINER, username1,
                PH_TRAINER, username2);
    }

    public CharSequence switchIn(BattlingPokemon pokemon, String username) {
        return line(resolveOwn("switchIn", !pokemon.getFoe()),
                PH_FULLNAME, pokemonFull(pokemon), PH_TRAINER, username);
    }

    public CharSequence drag(BattlingPokemon pokemon) {
        return line(resolve("drag"), PH_FULLNAME, pokemonFull(pokemon));
    }

    public CharSequence switchOut(BattlingPokemon pokemon, String username, String from) {
        if (pokemon == null) return null;
        PokemonId pkmnId = pokemon.getId();
        String pokemonName = pokemon(pkmnId);
        String template = resolveOwn(from, "switchOut", !pkmnId.getFoe());
        return line(template, PH_TRAINER, username, PH_POKEMON, pokemonName,
                PH_NICKNAME, pkmnId.getName());
    }

    public CharSequence pokemonChange(String cmd, PokemonId pkmnId, String arg2, String arg3, String of, String from) {
        String pokemon = pkmnId.getName();
        String newSpecies = null;
        switch (cmd) {
            case "detailschange":
                newSpecies = arg2.split(",")[0].trim();
                break;
            case "-transform":
                newSpecies = arg3;
                break;
            case "-formechange":
                newSpecies = arg2;
                break;
        }
        String newSpeciesId = toIdSafe(newSpecies);
        String id = null;
        String templateName = "transform";
        if (!cmd.equals("-transform")) {
            switch (newSpeciesId) {
                case "greninjaash":
                    id = "battlebond";
                    break;
                case "mimikyubusted":
                    id = "disguise";
                    break;
                case "zygardecomplete":
                    id = "powerconstruct";
                    break;
                case "necrozmaultra":
                    id = "ultranecroziumz";
                    break;
                case "darmanitanzen":
                    id = "zenmode";
                    break;
                case "darmanitan":
                    id = "zenmode";
                    templateName = "transformEnd";
                    break;
                case "darmanitangalarzen":
                    id = "zenmode";
                    break;
                case "darmanitangalar":
                    id = "zenmode";
                    templateName = "transformEnd";
                    break;
                case "aegislashblade":
                    id = "stancechange";
                    break;
                case "aegislash":
                    id = "stancechange";
                    templateName = "transformEnd";
                    break;
                case "wishiwashischool":
                    id = "schooling";
                    break;
                case "wishiwashi":
                    id = "schooling";
                    templateName = "transformEnd";
                    break;
                case "miniormeteor":
                    id = "shieldsdown";
                    break;
                case "minior":
                    id = "shieldsdown";
                    templateName = "transformEnd";
                    break;
                case "eiscuenoice":
                    id = "iceface";
                    break;
                case "eiscue":
                    id = "iceface";
                    templateName = "transformEnd";
                    break;
            }
        } else if (newSpecies != null) {
            id = "transform";
        }
        String template = resolve(id, templateName);
        CharSequence line1 = line(of != null ? maybeAbility(from, of) : maybeAbility(from, pkmnId));
        CharSequence line2 = line(template, PH_POKEMON, pokemon(pkmnId), PH_SPECIES, newSpecies);
        return lines(line1, line2);
    }

    public CharSequence faint(PokemonId pokemonId) {
        return line(resolve("faint"), PH_POKEMON, pokemon(pokemonId));
    }

    public CharSequence swap(PokemonId pokemonId, @Nullable PokemonId target) {
        if (target == null)
            return line(resolve("swapCenter"), PH_POKEMON, pokemon(pokemonId));
        return line(resolve("swap"), PH_POKEMON, pokemon(pokemonId),
                PH_TARGET, pokemon(target));
    }

    // |move|p2a: salamencemega|Outrage|p1a: Wobbuffet|[from]lockedmove
    public CharSequence move(PokemonId pkmnId, String move, String from, String of, String zMove) {
        String pokemon = pokemon(pkmnId);
        Log.w(TAG, "poke=" + pokemon + " move=" + move + " from=" + from);

        CharSequence line1 = line(of != null ? maybeAbility(from, of) : maybeAbility(from, pkmnId));
        Log.w(TAG, "line1=" + line1  + " null=" + (line1 == null));

        if (zMove != null)
            line1 = line(resolve("zEffect"), PH_POKEMON, pokemon);

        CharSequence line2 = line(resolve(from, "move"), PH_POKEMON, pokemon,
                PH_MOVE, move);
        Log.w(TAG, "line2=" + line2 + " null=" + (line2 == null));

        return lines(line1, line2);
    }

    public CharSequence cant(PokemonId pkmnId, String effect, String move, String of) {
        String template = resolve(effect, "cant", false);
        if (template == null) template = resolve(move == null ? "cantNoMove" : "cant");
        CharSequence line1 = line(of != null ? maybeAbility(effect, of) : maybeAbility(effect, pkmnId));
        CharSequence line2 = line(template, PH_POKEMON, pokemon(pkmnId),
                PH_MOVE, move);
        return lines(line1, line2);
    }

    public CharSequence start(PokemonId pkmnId, String effect, String arg3, String from, String of,
                              String already, String fatigue, String zeffect, String damage, String block,
                              String upkeep) {
        CharSequence line1 = line(maybeAbility(effect, pkmnId));
        if (line1 == null)
            line1 = line(of != null ? maybeAbility(from, of) : maybeAbility(effect, pkmnId));
        CharSequence line2;
        String effectId = toId(effect(effect));
        if (effectId.equals("typechange")) {
            String template = resolve(from, "typeChange");
            line2 = line(template, PH_POKEMON, pokemon(pkmnId), PH_TYPE, arg3, PH_SOURCE, pokemon(of));
            return lines(line1, line2);
        }
        if (effectId.equals("typeadd")) {
            String template = resolve(from, "typeAdd");
            line2 = line(template, PH_POKEMON, pokemon(pkmnId), PH_TYPE, arg3);
            return lines(line1, line2);
        }
        if (effectId.startsWith("stockpile")) {
            String num = effectId.substring(9);
            String template = resolve("stockpile", "start");
            line2 = line(template, PH_POKEMON, pokemon(pkmnId), PH_NUMBER, num);
            return lines(line1, line2);
        }
        if (effectId.startsWith("perish")) {
            String num = effectId.substring(6);
            String template = resolve("perishsong", "activate");
            line2 = line(template, PH_POKEMON, pokemon(pkmnId), PH_NUMBER, num);
            return lines(line1, line2);
        }
        String templateId = "start";
        if (already != null) templateId = "alreadyStarted";
        if (fatigue != null) templateId = "startFromFatigue";
        if (zeffect != null) templateId = "startFromZEffect";
        if (damage != null) templateId = "activate";
        if (block != null) templateId = "block";
        if (upkeep != null) templateId = "upkeep";
        if (effectId.equals("reflect") || effectId.equals("lightscreen")) templateId = "startGen1";
        if (templateId.equals("start") && from != null && from.startsWith("item:"))
            templateId += "FromItem";
        String template = resolve(effect, templateId);
        line2 = line(template, PH_POKEMON, pokemon(pkmnId), PH_EFFECT, effect(effect), PH_MOVE, arg3, PH_SOURCE, pokemon(of), PH_ITEM, effect(from));
        return lines(line1, line2);
    }

    public CharSequence end(PokemonId pkmnId, String effect, String from, String of) {
        CharSequence line1 = line(maybeAbility(effect, pkmnId));
        if (line1 == null)
            line1 = line(of != null ? maybeAbility(from, of) : maybeAbility(from, pkmnId));
        CharSequence line2;
        String id = toId(effect(effect));
        if (id.equals("doomdesire") || id.equals("futuresight")) {
            String template = resolve(effect, "activate");
            line2 = line(template, PH_TARGET, pokemon(pkmnId));
            return lines(line1, line2);
        }
        String templateId = "end";
        String template = null;
        if (from != null && from.startsWith("item:")) template = resolve(effect, "endFromItem");
        if (template == null) template = resolve(effect, templateId);
        line2 = line(template, PH_POKEMON, pokemon(pkmnId), PH_EFFECT, effect(effect), PH_SOURCE, pokemon(of));
        return lines(line1, line2);
    }

    public CharSequence ability(PokemonId pkmnId, String ability, String oldAbility, String arg4,
                                String from, String of, String fail) {
        CharSequence line1 = null;
        CharSequence line2;
        if (oldAbility != null && (oldAbility.startsWith("p1") || oldAbility.startsWith("p2") || oldAbility.equals("boost"))) {
            arg4 = oldAbility;
            oldAbility = null;
        }
        if (oldAbility != null) line1 = line(ability(oldAbility, pkmnId));
        line1 = lines(line1, line(ability(ability, pkmnId)));
        if (fail != null) {
            String template = resolve(from, "block");
            line2 = line(template);
            return lines(line1, line2);
        }
        if (from != null) {
            line1 = lines(line(maybeAbility(from, pkmnId)), line1);
            String template = resolve(from, "changeAbility");
            line2 = line(template, PH_POKEMON, pokemon(pkmnId), PH_ABILITY, effect(ability), PH_SOURCE, pokemon(of));
            return lines(line1, line2);
        }
        String id = toId(effect(ability));
        if (id.equals("unnerve")) {
            String template = resolve(ability, "start");
            line2 = line(template, PH_TEAM, "TEAM");// team(arg4)); TODO
            return lines(line1, line2);
        }
        String templateId = "start";
        if (id.equals("anticipation") || id.equals("sturdy")) templateId = "activate";
        String template = resolve(ability, templateId, false);
        line2 = line(template, PH_POKEMON, pokemon(pkmnId));
        return lines(line1, line2);
    }

    public CharSequence endability(PokemonId pkmnId, String ability, String from, String of) {
        if (ability != null) return line(ability(ability, pkmnId));
        CharSequence line1 = line(of != null ? maybeAbility(from, of) : maybeAbility(from, pkmnId));
        String template = resolve("Gastro Acid", "start");
        CharSequence line2 = line(template, PH_POKEMON, pokemon(pkmnId));
        return lines(line1, line2);
    }

    public CharSequence item(PokemonId pkmnId, String item, String from, String of) {
        String id = toId(effect(from), null);
        String target = null;
        if (id != null && (id.equals("magician") || id.equals("pickpocket"))) {
            target = of;
            of = null;
        }
        CharSequence line1 = line(of != null ? maybeAbility(from, of) : maybeAbility(from, pkmnId));
        CharSequence line2;
        String[] excludes = {"thief", "covet", "bestow", "magician", "pickpocket"};
        if (contains(id, excludes)) {
            String template = resolve(from, "takeItem");
            line2 = line(template, PH_POKEMON, pokemon(pkmnId), PH_ITEM, effect(item),
                    PH_SOURCE, pokemon(target != null ? target : of));
            return lines(line1, line2);
        }
        if ("frisk".equals(id)) {
            boolean hasTarget = of != null && pkmnId != null && !pokemon(of).equals(pokemon(pkmnId));
            String template = resolve("Frisk", hasTarget ? "activate" : "activateNoTarget");
            line2 = line(template, PH_POKEMON, pokemon(of), PH_ITEM, effect(item), PH_TARGET, pokemon(pkmnId));
            return lines(line1, line2);
        }
        if (from != null) {
            String template = resolve(from, "addItem");
            line2 = line(template, PH_POKEMON, pokemon(pkmnId), PH_ITEM, effect(item));
            return lines(line1, line2);
        }
        String template = resolve(item, "start", false);
        line2 = line(template, PH_POKEMON, pokemon(pkmnId));
        return lines(line1, line2);
    }

    public CharSequence enditem(PokemonId pkmnId, String item, String from, String of, String eat,
                                String move, String weaken) {
        CharSequence line1 = line(of != null ? maybeAbility(from, of) : maybeAbility(from, pkmnId));
        CharSequence line2;
        if (eat != null) {
            String template = resolve(from, "eatItem");
            line2 = line(template, PH_POKEMON, pokemon(pkmnId), PH_ITEM, effect(item));
            return lines(line1, line2);
        }
        String id = toId(effect(from), null);
        if ("gem".equals(id)) {
            String template = resolve(item, "useGem");
            line2 = line(template, PH_POKEMON, pokemon(pkmnId), PH_ITEM, effect(item), PH_MOVE, move);
            return lines(line1, line2);
        }
        if ("stealeat".equals(id)) {
            String template = resolve("Bug Bite", "removeItem");
            line2 = line(template, PH_SOURCE, pokemon(of), PH_ITEM, effect(item));
            return lines(line1, line2);
        }
        if (from != null) {
            String template = resolve(from, "removeItem");
            line2 = line(template, PH_POKEMON, pokemon(pkmnId), PH_ITEM, effect(item), PH_SOURCE, pokemon(of));
            return lines(line1, line2);
        }
        if (weaken != null) {
            String template = resolve("activateWeaken");
            line2 = line(template, PH_POKEMON, pokemon(pkmnId), PH_ITEM, effect(item));
            return lines(line1, line2);
        }
        String template = resolve(item, "end", false);
        if (template == null)
            template = formatPlaceHolders(resolve("activateItem"), PH_ITEM, effect(item));
        line2 = line(template, PH_POKEMON, pokemon(pkmnId), PH_TARGET, pokemon(of));
        return lines(line1, line2);
    }

    public CharSequence status(PokemonId pkmnId, String status, String from, String of) {
        CharSequence line1 = line(of != null ? maybeAbility(from, of) : maybeAbility(from, pkmnId));
        CharSequence line2;
        String id = toId(effect(from), null);
        if ("rest".equals(id)) {
            String template = resolve(status, "startFromRest");
            line2 = line(template, PH_POKEMON, pokemon(pkmnId));
            return lines(line1, line2);
        }
        String template = resolve(status, "start");
        line2 = line(template, PH_POKEMON, pokemon(pkmnId));
        return lines(line1, line2);
    }

    public CharSequence curestatus(PokemonId pkmnId, String status, String from, String of,
                                   String thaw) {
        String id = toId(effect(from), null);
        if ("naturalcure".equals(id)) {
            String template = resolve(from, "activate");
            return line(template, PH_POKEMON, pokemon(pkmnId));
        }
        CharSequence line1 = line(of != null ? maybeAbility(from, of) : maybeAbility(from, pkmnId));
        CharSequence line2;
        if (from != null && from.startsWith("item:")) {
            String template = resolve(status, "endFromItem");
            line2 = line(template, PH_POKEMON, pokemon(pkmnId), PH_ITEM, effect(from));
            return lines(line1, line2);
        }
        if (thaw != null) {
            String template = resolve(status, "endFromMove");
            line2 = line(template, PH_POKEMON, pokemon(pkmnId), PH_MOVE, effect(from));
            return lines(line1, line2);
        }
        String template = resolve(status, "end", false);
        if (template == null) template = formatPlaceHolders(resolve("end"), PH_EFFECT, status);
        line2 = line(template, PH_POKEMON, pokemon(pkmnId));
        return lines(line1, line2);
    }

    public CharSequence cureTeam(String from) {
        return line(resolve(from, "activate"));
    }

    public CharSequence single(PokemonId pkmnId, String effect, String from, String of) {
        CharSequence line1 = line(of != null ? maybeAbility(effect, of) : maybeAbility(effect, pkmnId));
        CharSequence line2;
        if (line1 == null) line1 = line(of != null ? maybeAbility(from, of) : maybeAbility(from, pkmnId));
        String id = toId(effect(effect));
        if (id.equals("instruct")) {
            String template = resolve(effect, "activate");
            line2 = line(template, PH_POKEMON, pokemon(of), PH_TARGET, pokemon(pkmnId));
            return lines(line1, line2);
        }
        String template = resolve(effect, "start", false);
        if (template == null)
            template = formatPlaceHolders(resolve("start"), PH_EFFECT, effect(effect));
        line2 = line(template, PH_POKEMON, pokemon(pkmnId), PH_SOURCE, pokemon(of), PH_TEAM, team(pkmnId));
        return lines(line1, line2);
    }

    public CharSequence sidestart(Player player, String effect) {
        String template = resolve(effect, "start", false);
        if (template == null)
            template = formatPlaceHolders(resolve("startTeamEffect"), PH_EFFECT, effect(effect));
        return line(template, PH_TEAM, team(player), PH_PARTY, party(player));
    }

    public CharSequence sideend(Player player, String effect) {
        String template = resolve(effect, "end", false);
        if (template == null)
            template = formatPlaceHolders(resolve("endTeamEffect"), PH_EFFECT, effect(effect));
        return line(template, PH_TEAM, team(player), PH_PARTY, party(player));
    }

    public CharSequence weather(String weather, String previousWeather, String from, String of, String upkeep) {
        if (weather == null || weather.equals("none")) {
            String template = resolve(from, "end", false);
            if (template == null) return line(resolve("endFieldEffect"), PH_EFFECT, effect(previousWeather));
            return line(template);
        }
        if (upkeep != null) {
            return line(resolve(weather, "upkeep", false));
        }
        CharSequence line1 = line(maybeAbility(from, of));
        String template = resolve(weather, "start", false);
        if (template == null) template = formatPlaceHolders(resolve("startFieldEffect"), PH_EFFECT, effect(weather));
        return lines(line1, line(template));
    }

    // case '-fieldstart': case '-fieldactivate': {
    public CharSequence field(String cmd, String effect, String from, String of) {
        CharSequence line1 = line(maybeAbility(from, of));
        String templateId = cmd.substring(6);
        String id = toId(effect(effect));
        if ("perishsong".equals(id)) templateId = "start";
        String template = resolve(effect, templateId, false);
        if (template == null)
            template = formatPlaceHolders(resolve("startFieldEffect"), PH_EFFECT, effect(effect));
        CharSequence line2 = line(template, PH_POKEMON, pokemon(of));
        return lines(line1, line2);
    }

    public CharSequence fieldend(String effect) {
        String template = resolve(effect, "end", false);
        if (template == null)
            template = formatPlaceHolders(resolve("endFieldEffect"), PH_EFFECT, effect(effect));
        return line(template);
    }

    public CharSequence sethp(String from) {
        return line(resolve(from, "activate"));
    }

    public CharSequence activate(PokemonId pkmnId, String effect, String target, String of,
                                 String ability, String ability2, String move, String number,
                                 String item, String name) {
        String id = toIdSafe(effect(effect));
        PokemonId targetId = getPokemonId(target);
        if ("celebrate".equals(id)) {
            return line(resolve("celebrate", "activate"), PH_TRAINER, trainer(pkmnId));
        }
        String[] includes = {"hyperspacefury", "hyperspacehole", "phantomforce", "shadowforce", "feint"};
        if (targetId != null && contains(id, includes)) {
            pkmnId = getPokemonId(of);
            targetId = pkmnId;
            if (pkmnId == null) pkmnId = targetId; //TODO
        }
        if (targetId != null) targetId = of != null ? getPokemonId(of) : pkmnId;
        CharSequence line1 = line(maybeAbility(effect, pkmnId));
        CharSequence line2;
        if ("lockon".equals(id) || "mindreader".equals(id)) {
            String template = resolve(effect, "start");
            line2 = line(template, PH_POKEMON, pokemon(of), PH_SOURCE, pokemon(pkmnId));
            return lines(line1, line2);
        }
        if ("mummy".equals(id)) {
            line1 = lines(line1, line(ability(ability, targetId)));
            line1 = lines(line1, ability("Mummy", targetId));
            String template = resolve("mummy", "changeAbility");
            line2 = line(template, PH_TARGET, pokemon(targetId));
            return lines(line1, line2);
        }
        String templateId = "activate";
        if ("forewarn".equals(id) && pkmnId == targetId) {
            templateId = "activateNoTarget";
        }
        String template = resolve(effect, templateId, false);
        if (template == null) {
            if (line1 != null) return line1; // Abilities don"t have a default template
            template = resolve("activate");
            line2 = line(template, PH_EFFECT, effect(effect));
            return lines(line1, line2);
        }
        if ("brickbreak".equals(id)) {
            template = formatPlaceHolders(template, PH_TEAM, team(targetId != null ? targetId.getPlayer() : null));
        }
        if (ability != null) {
            line1 = lines(line1, ability(ability, pkmnId));
        }
        if (ability2 != null) {
            line1 = lines(line1, ability(ability2, targetId));
        }

        if (move != null || number != null || item != null || name != null) {
            template = formatPlaceHolders(PH_MOVE, move, PH_NUMBER, number, PH_ITEM, item, PH_NAME, name);
        }
        line2 = line(template, PH_POKEMON, pokemon(pkmnId), PH_TARGET, pokemon(targetId),
                PH_SOURCE, pokemon(of));
        return lines(line1, line2);
    }

    public CharSequence prepare(PokemonId pkmnId, String effect, String target) {
        String template = resolve(effect, "prepare");
        return line(template, PH_POKEMON, pokemon(pkmnId), PH_TARGET, pokemon(target));
    }

    public CharSequence damage(PokemonId pkmnId, String percentage, String from, String of,
                               String partiallytrapped) {
        String template = resolve(from, "damage", false);
        CharSequence line1 = line(of != null ? maybeAbility(from, of) : maybeAbility(from, pkmnId));
        CharSequence line2;
        String id = toId(effect(from), null);
        if (template != null) {
            line2 = line(template, PH_POKEMON, pokemon(pkmnId));
            return lines(line1, line2);
        }

        if (isEmpty(from)) {
            template = resolve(percentage != null ? "damagePercentage" : "damage");
            line2 = line(template, PH_POKEMON, pokemon(pkmnId), PH_PERCENTAGE, percentage);
            return lines(line1, line2);
        }
        if (from != null && from.startsWith("item:")) {
            template = resolve(of != null ? "damageFromPokemon" : "damageFromItem");
            line2 = line(template, PH_POKEMON, pokemon(pkmnId), PH_ITEM, effect(from), PH_SOURCE, pokemon(of));
            return lines(line1, line2);
        }
        if (partiallytrapped != null || id.equals("bind") || id.equals("wrap")) {
            template = resolve("damageFromPartialTrapping");
            line2 = line(template, PH_POKEMON, pokemon(pkmnId), PH_MOVE, effect(from));
            return lines(line1, line2);
        }

        template = resolve("damage");
        line2 = line(template, PH_POKEMON, pokemon(pkmnId));
        return lines(line1, line2);
    }

    public CharSequence heal(PokemonId pkmnId, String from, String of, String wisher) {
        String template = resolve(from, "heal", false);
        CharSequence line1 = line(maybeAbility(from, pkmnId));
        CharSequence line2;
        if (template != null) {
            line2 = line(template, PH_POKEMON, pokemon(pkmnId), PH_SOURCE, pokemon(of), PH_NICKNAME, pokemon(wisher));
            return lines(line1, line2);
        }

        if (from != null && !from.startsWith("ability:")) {
            template = resolve("healFromEffect");
            line2 = line(template, PH_POKEMON, pokemon(pkmnId), PH_EFFECT, effect(from));
            return lines(line1, line2);
        }

        template = resolve("heal");
        line2 = line(template, PH_POKEMON, pokemon(pkmnId));
        return lines(line1, line2);
    }

    public CharSequence boost(String cmd, PokemonId pkmnId, String stat, String num, String from, String of,
                              String multiple, String zeffect) {
        CharSequence line1 = line(of != null ? maybeAbility(from, of) : maybeAbility(from, pkmnId));
        CharSequence line2;
        int amount = Utils.parseWithDefault(num, -1);
        String templateId = cmd.substring(1);
        if (amount >= 3) templateId += "3";
        else if (amount >= 2) templateId += "2";
        else if (amount == 0) templateId += "0";
        if (amount != -1 && zeffect != null) {
            templateId += (multiple != null ? "MultipleFromZEffect" : "FromZEffect");
        } else if (amount != -1 && from != null && from.startsWith("item:")) {
            String template = resolve(from, templateId + "FromItem");
            return lines(line1, line(template, PH_POKEMON, pokemon(pkmnId), PH_STAT, stat(stat), PH_ITEM, effect(from)));
        }
		String template = resolve(from, templateId);
        line2 = line(template, PH_POKEMON, pokemon(pkmnId), PH_STAT, stat(stat));
        return lines(line1, line2);
    }

    public CharSequence setboost(PokemonId pkmnId, String from, String of) {
        CharSequence line1 = line(of != null ? maybeAbility(from, of) : maybeAbility(from, pkmnId));
        String template = resolve(from, "boost");
        CharSequence line2 = line(template, PH_POKEMON, pokemon(pkmnId));
        return lines(line1, line2);
    }

    /* TODO
    case '-swapboost': {
        const [, pokemon, target] = args;
        const line1 = this.maybeAbility(kwArgs.from, kwArgs.of || pokemon);
        const id = BattleTextParser.effectId(kwArgs.from);
        let templateId = 'swapBoost';
        if (id === 'guardswap') templateId = 'swapDefensiveBoost';
        if (id === 'powerswap') templateId = 'swapOffensiveBoost';
        const template = this.template(templateId, kwArgs.from);
        return line1 + template.replace('[POKEMON]', this.pokemon(pokemon)).replace('[TARGET]', this.pokemon(target));
    }

    case '-copyboost': {
        const [, pokemon, target] = args;
        const line1 = this.maybeAbility(kwArgs.from, kwArgs.of || pokemon);
        const template = this.template('copyBoost', kwArgs.from);
        return line1 + template.replace('[POKEMON]', this.pokemon(pokemon)).replace('[TARGET]', this.pokemon(target));
    }
    */

    public CharSequence clearBoost(PokemonId pkmnId, String source, String from, String of, String zEffect) {
        CharSequence line1 = line(of != null ? maybeAbility(from, of) : maybeAbility(from, pkmnId));
        String templateId = "clearBoost";
        if (zEffect != null) templateId = "clearBoostFromZEffect";
        String template = resolve(from, templateId);
        CharSequence line2 = line(template, PH_POKEMON, pokemon(pkmnId), PH_SOURCE, pokemon(source));
        return lines(line1, line2);
    }

    public CharSequence invertBoost(PokemonId pkmnId, String from, String of) {
        CharSequence line1 = line(of != null ? maybeAbility(from, of) : maybeAbility(from, pkmnId));
        String template = resolve(from, "invertBoost");
        CharSequence line2 = line(template, PH_POKEMON, pokemon(pkmnId));
        return lines(line1, line2);
    }

    public CharSequence clearAllBoost(String from) {
        return line(resolve("clearAllBoost"));
    }

    public CharSequence moveeffect(String cmd, PokemonId pkmnId, String spread) {
        String templateId = cmd.substring(1);
        if ("supereffective".equals(templateId)) templateId = "superEffective";
        if (spread != null) templateId += "Spread";
		String template = resolve(templateId);
        return line(template, PH_POKEMON, pokemon(pkmnId));
    }

    public CharSequence block(PokemonId pkmnId, String effect, String move, String attacker, String from, String of) {
        CharSequence line1 = line(of != null ? maybeAbility(from, of) : maybeAbility(from, pkmnId));
        String template = resolve(effect, "block");
        CharSequence line2 = line(template, PH_POKEMON, pokemon(pkmnId), PH_SOURCE, pokemon(attacker != null ? attacker : of),
                PH_MOVE, move);
        return lines(line1, line2);
    }

    public CharSequence fail(PokemonId pkmnId, String effect, String stat, String from, String of,
                             String msg, String heavy, String weak, String forme) {
        String id = toId(effect(effect), null);
        String blocker = toId(effect(from), null);
        CharSequence line1 = line(of != null ? maybeAbility(from, of) : maybeAbility(from, pkmnId));
        CharSequence line2;
        String templateId = "block";
        String[] includes = {"desolateland", "primordialsea"};
        String[] excludes = {"sunnyday", "raindance", "sandstorm", "hail"};
        if (contains(blocker, includes) && !contains(id, excludes)) {
            templateId = "blockMove";
        } else if ("uproar".equals(blocker) && msg != null) {
            templateId = "blockSelf";
        }
        String template = resolve(from, templateId);
        if (template != null) {
            line2 = line(template, PH_POKEMON, pokemon(pkmnId));
            return lines(line1, line2);
        }

        if ("unboost".equals(id)) {
            template = resolve("unboost", stat != null ? "failSingular" : "fail");
            if ("flowerveil".equals(toId(effect(effect), null))) {
                template = resolve(from, "block");
                pkmnId = getPokemonId(of);
            }
            line2 = line(template, PH_POKEMON, pokemon(pkmnId), PH_STAT, stat);
            return lines(line1, line2);
        }

        templateId = "fail";
        includes = new String[]{"brn", "frz", "par", "psn", "slp", "substitute"};
        if (contains(id, includes)) {
            templateId = "alreadyStarted";
        }
        if (heavy != null) templateId = "failTooHeavy";
        if (weak != null) templateId = "fail";
        if (forme != null) templateId = "failWrongForme";
        template = resolve(id, templateId);
        line2 = line(template, PH_POKEMON, pokemon(pkmnId));
        return lines(line1, line2);
    }

    public CharSequence immune(PokemonId pkmnId, String from, String of, String ohko) {
        CharSequence line1 = line(of != null ? maybeAbility(from, of) : maybeAbility(from, pkmnId));
        CharSequence line2;
        String template = resolve(from, "block");
        if (template == null) {
			String templateId = ohko != null ? "immuneOHKO" : "immune";
            template = resolve(from, pkmnId != null ? templateId : "immuneNoPokemon");
        }
        line2 = line(template, PH_POKEMON, pokemon(pkmnId));
        return lines(line1, line2);
    }

    public CharSequence miss(PokemonId sourceId, PokemonId targetId, String from, String of) {
        CharSequence line1 = line(of != null ? maybeAbility(from, of) : maybeAbility(from, targetId));
        CharSequence line2;
        if (targetId == null) {
            String template = resolve("missNoPokemon");
            line2 = line(template, PH_SOURCE, pokemon(sourceId));
            return lines(line1, line2);
        }
        String template = resolve("miss");
        line2 = line(template, PH_POKEMON, pokemon(targetId));
        return lines(line1, line2);
    }

    public CharSequence center() {
        return line(resolve("center"));
    }

    public CharSequence ohko() {
        return line(resolve("ohko"));
    }

    public CharSequence combine() {
        return line(resolve("combine"));
    }

    public CharSequence notarget() {
        return line(resolve("noTarget"));
    }

    public CharSequence mega(PokemonId pkmnId, String species, String item, boolean primal) {
        String id = null;
        String templateId = primal ? "primal" : "mega";
        if ("Rayquaza".equals(species)) {
            id = "dragonascent";
            templateId = "megaNoItem";
        }
        if (item == null && !primal) templateId = "megaNoItem";
        String template = resolve(id, templateId);
        String trainer = trainer(pkmnId);
        String pokemonName = pokemon(pkmnId);
        CharSequence line1 = line(template, PH_POKEMON, pokemonName, PH_ITEM, item, PH_TRAINER, trainer);
        CharSequence line2 = null;
        if (!primal) {
            String template2 = resolve("transformMega");
            line2 = line(template2, PH_POKEMON, pokemonName, PH_SPECIES, species);
        }
        return lines(line1, line2);
    }

    public CharSequence zpower(PokemonId pkmnId) {
        String template = resolve("zPower");
        return line(template, PH_POKEMON, pokemon(pkmnId));
    }

    public CharSequence burst(PokemonId pkmnId) {
        String template = resolve("Ultranecrozium Z", "activate");
        return line(template, PH_POKEMON, pokemon(pkmnId));
    }

    public CharSequence zbroken(PokemonId pkmnId) {
        String template = resolve("zBroken");
        return line(template, PH_POKEMON, pokemon(pkmnId));
    }

    public CharSequence hitcount(String num) {
        if ("1".equals(num))
            return line(resolve("hitCountSingular"));
        return line (resolve("hitCount"), PH_NUMBER, num);
    }

    public CharSequence waiting(PokemonId pkmnId, String target) {
        String template = resolve("Water Pledge", "activate");
        return line(template, PH_POKEMON, pokemon(pkmnId), PH_TARGET, pokemon(target));
    }

    private static class JsonReadTask extends AsyncTask<InputStream, Void, JSONObject> {

        interface Callback {
            void onFileRead(JSONObject jsonObject);
        }

        private Callback mCallback;

        public JsonReadTask(Callback callback) {
            mCallback = callback;
        }

        @Override
        protected void onPostExecute(JSONObject s) {
            super.onPostExecute(s);
            mCallback.onFileRead(s);
        }

        @Override
        protected JSONObject doInBackground(InputStream... inputStreams) {
            String fileContent = Utils.convertStreamToString(inputStreams[0]);

            if (fileContent == null)
                return null;

            try {
                return new JSONObject(fileContent);
            } catch (JSONException e) {
                e.printStackTrace();
                return null;
            }
        }
    }

    public interface PokemonIdFactory {
        public PokemonId getPokemonId(String rawString);
    }
}
