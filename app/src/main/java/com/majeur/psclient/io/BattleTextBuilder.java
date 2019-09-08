package com.majeur.psclient.io;

import android.content.Context;
import android.os.AsyncTask;
import android.text.Html;
import android.text.Spanned;
import android.text.SpannedString;
import android.text.TextUtils;
import android.util.Log;

import com.majeur.psclient.R;
import com.majeur.psclient.model.Condition;
import com.majeur.psclient.model.Player;
import com.majeur.psclient.model.PokemonId;
import com.majeur.psclient.util.Utils;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.InputStream;
import java.util.concurrent.ExecutionException;

import static com.majeur.psclient.model.Id.toId;
import static com.majeur.psclient.util.Utils.array;
import static com.majeur.psclient.util.Utils.italicText;
import static java.lang.String.format;


@SuppressWarnings("DefaultLocale")
public final class BattleTextBuilder {

    private static final String TAG = BattleTextBuilder.class.getSimpleName();

    private JSONObject mJSONObject;
    private JsonReadTask mJsonReadTask;

    public BattleTextBuilder(Context context) {
        InputStream inputStream = context.getResources().openRawResource(R.raw.battle_texts);
        mJsonReadTask = new JsonReadTask(new JsonReadTask.Callback() {
            @Override
            public void onFileRead(JSONObject jsonObject) {
                mJSONObject = jsonObject;
                mJsonReadTask = null;
            }
        });
        mJsonReadTask.execute(inputStream);
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

    private JSONObject object(String key) {
        return mJSONObject.optJSONObject(key);
    }

    private JSONObject global() {
        return object("default");
    }

    private String resolve(String objectKey, String key) {
        //Log.d(getClass().getSimpleName(), "Resolving: " + objectKey + ":{" + key + "}");
        JSONObject object = object(objectKey);
        if (object == null) return null;
        String template = object.optString(key);
        if (template == null || TextUtils.isEmpty(template)) return null;
        if (template.charAt(0) == '#')
            return resolve(template.substring(1), key);
        return template;
    }

    private Spanned line(String s) {
        if (s == null) {
            Log.w(TAG, "Cannot build battle text");
            return italicText("Oops, cannot build battle text for this action");
        }
        return Html.fromHtml(Utils.firstCharUpperCase(s.trim()));
    }

    private Spanned linef(String s, Object... os) {
        if (s == null) {
            Log.w(TAG, "Cannot build battle text for this action");
            return italicText("Oops, cannot build battle text for this action");
        }
        return line(format(s, os));
    }

    private String pokemon(PokemonId pokemonId) {
        if (pokemonId.foe)
            return format(global().optString("opposingPokemon"), pokemonId.name);
        else
            return format(global().optString("pokemon"), pokemonId.name);
    }

    public Spanned startBattle(String username1, String username2) {
        checkReady();
        return linef(global().optString("startBattle"), username1, username2);
    }

    public Spanned statModifier(PokemonId pokemonId, String stat, int amount) {
        String pokemonName = pokemon(pokemonId);
        stat = Utils.firstCharUpperCase(stat);
        switch (amount) {
            case 1:
                return linef(global().optString("boost"), pokemonName, stat);
            case 2:
                return linef(global().optString("boost2"), pokemonName, stat);
            case 3:
                return linef(global().optString("boost3"), pokemonName, stat);
            case -1:
                return linef(global().optString("unboost"), pokemonName, stat);
            case -2:
                return linef(global().optString("unboost2"), pokemonName, stat);
            case -3:
                return linef(global().optString("unboost3"), pokemonName, stat);
            default:
                return line(global().optString("boost0"));
        }
    }

    public Spanned faint(PokemonId pokemonId) {
        String pokemonName = pokemon(pokemonId);
        return linef(global().optString("faint"), pokemonName);
    }

    //|-setboost|p1a: Slurpuff|atk|6|[from] move: Belly Drum
    public Spanned statModifierSet(PokemonId pokemonId, String stat, int value, String from) {
        String pokemonName = pokemon(pokemonId);
        stat = Utils.firstCharUpperCase(stat);

        if (from != null) {
            from = from.substring("[from] ".length());
            if (from.contains(":"))
                from = from.substring(from.indexOf(':') + 1);
            String template = resolve(toId(from), "boost");
            if (template != null)
                return linef(template, pokemonName);
        }

        return linef("%s's %s was boosted to %d", pokemonName, stat, value);
    }

    public Spanned[] moveEffect(String type, PokemonId pokemonId) {
        final String printText;
        final String toastText;
        String pokemonName = pokemon(pokemonId);
        switch (type) {
            case "crit":
                printText = global().optString("crit");
                toastText = "Critical";
                break;
            case "resisted":
                printText = global().optString("resisted");
                toastText = "Resisted";
                break;
            case "supereffective":
                printText = global().optString("superEffective");
                toastText = "Super Effective";
                break;
            case "immune":
                printText = format(global().optString("immune"), pokemonName);
                toastText = "Immune";
                break;
            default:
                toastText = printText = "yolo";
        }
        return new Spanned[]{line(printText), new SpannedString(toastText)};
    }


    //|-status|p1a: Regirock|slp|[from] move: Rest
    public Spanned status(PokemonId pokemonId, String status, boolean start, String from) {
        String pokemonName = pokemon(pokemonId);
        if (from != null) {
            if (from.contains("move"))
                return linef(object("slp").optString("startFromRest"), pokemonName);

            if (from.contains("item")) {
                String item = from.substring(from.indexOf(':') + 1);
                String template = resolve(toId(status), start ? "startFromItem" : "endFromItem");
                if (template != null)
                    return linef(template, pokemonName, item);
            }
        }
        String template = resolve(toId(status), start ? "start" : "end");
        return linef(template, pokemonName);
    }

    // |-ability|p2a: Deoxys|Pressure
    // |-ability|p1a: Mawile|Intimidate|boost
    // |-ability|p1a: Pyroar|Unnerve|p2: qmqmqm
    public Spanned[] ability(PokemonId pokemonId, String ability, String action, Player player, boolean start) {
        String pokemonName = pokemon(pokemonId);
        String mainKey = start ? "start" : "end";
        Spanned msg = null;
        if (start)
            msg = linef(global().optString("abilityActivation"), pokemonId.name, ability);

        if (ability == null) return new Spanned[]{msg};
        String template = resolve(toId(ability), mainKey);
        if (template == null) return new Spanned[]{msg};

        if (ability.equalsIgnoreCase("unnerve"))
            pokemonName = global().optString(player == Player.FOE ? "opposingTeam" : "team");

        Spanned msg2 = linef(template, pokemonName);
        if (msg == null)
            return new Spanned[]{msg2};
        return new Spanned[]{msg, msg2};
    }
//
//    Neee7 withdrew Hypno!
//    Neee7 sent out Rotom (Rotom-Frost)!
//    Pointed stones dug into the opposing Rotom!
//
//    Skarmory used Spikes!
//    Spikes were scattered on the ground all around the opposing team!
//
//    Hypno's wish came true!

//    |-heal|p1a: Rotom|100/100|[from] move: Wish|[wisher] Hypno

    //  |-heal|p2a: Stunfisk|328/328 slp|[silent]
    //  |-heal|p1a: Pidgeot|100/100
    //  |-heal|p2a: GogoatX|278/344|[from] drain|[of] p1a: Groudon
    //  |-heal|p2a: SwoobatX|135/250|[from] item: Leftovers
    //  |-damage|p1a: Pidgeot|29/100
    //  |-damage|p2a: Mghtya|240/255|[from] Sandstorm
    //  |-damage|p2a: Thu|22/100 brn|[from] brn
    //  |-damage|p1a: Raticate|69/100|[from] Recoil
    //  |-damage|p2a: Pheromosa|196/224|[from] Stealth Rock
    //  |-damage|p1a: Raticate|59/100|[from] item: Life Orb
    //  |-damage|p1a: Marshad|75/100|[from] ability: Aftermath|[of] p2a: Electrode
    //  |-heal|p1a: Jellicent|28/100|[from] ability: Water Absorb|[of] p2a: Relicanth
    public Spanned[] healthChange(String mainKey, PokemonId pokemonId, Condition condition,
                                  String effect, PokemonId of) {
        String pokemonName = pokemon(pokemonId);
        if (effect == null) {
            if (mainKey.equals("heal") || condition == null)
                return array(linef(global().optString(mainKey), pokemonName));
            String percentage = format("%d", (int) condition.health);
            return array(linef(global().optString("damagePercentage"), pokemonName, percentage));
        }

        if (effect.contains("silent"))
            return array();

        effect = effect.substring(7); // "[from]".length()

        //  |-heal|p1a: Gliscor|57/100 tox|[from] ability: Poison Heal
        //  |-damage|p1a: Marshad|75/100|[from] ability: Aftermath|[of] p2a: Electrode
        //  |-heal|p1a: Jellicent|28/100|[from] ability: Water Absorb|[of] p2a: Relicanth
        if (effect.contains("ability")) {
            String ofPokemonName = of != null ? pokemon(of) : null;
            String ability = effect.substring(effect.indexOf(':') + 1);
            String concernedPokemon = mainKey.equals("heal") || ofPokemonName == null ? pokemonName : ofPokemonName;
            Spanned msg1 = linef(global().optString("abilityActivation"), concernedPokemon, ability);

            String template = resolve(toId(ability), mainKey);
            Spanned msg2;
            if (template != null)
                msg2 = linef(template, ofPokemonName);
            else
                msg2 = linef(global().optString(mainKey), pokemonName);
            return array(msg1, msg2);
        }

        if (effect.contains("item")) {
            String item = effect.substring(effect.indexOf(':') + 1);
            String template = resolve(toId(item), mainKey);

            if (template == null) // Only append with damage
                return array(linef(global().optString("damageFromItem"), pokemonName, item));

            return array(linef(template, pokemonName, item));
        }

        if (effect.contains("move")) {
            String move = effect.substring(effect.indexOf(':') + 1);
            String template = resolve(toId(move), mainKey);
            // Let throws if template is null

            return array(linef(template, of.name, move));
        }

        // Search if effect has an entry for damage or heal
        String template = resolve(toId(effect), mainKey);
        if (template != null)
            return array(linef(template, of == null ? pokemonName : pokemon(of)));

        if ("heal".equals(mainKey))
            return array(linef(global().optString("healFromEffect"), pokemonName, effect));
        else
            return array(linef(global().optString("damage"), pokemonName));
    }

    //    switchIn: "[TRAINER] sent out [FULLNAME]!",
//    switchInOwn: "Go! [FULLNAME]!",
//    switchOut: "[TRAINER] withdrew [NICKNAME]!",
//    switchOutOwn: "[NICKNAME], come back!",
    public Spanned switchIn(Player player, String username, String pokemonName) {
        if (player == Player.FOE)
            return linef(global().optString("switchIn"), username, pokemonName);
        else
            return linef(global().optString("switchInOwn"), pokemonName);
    }

    public Spanned switcOut(Player player, String username, String pokemonName) {
        if (player == Player.FOE)
            return linef(global().optString("switchOut"), username, pokemonName);
        else
            return linef(global().optString("switchOutOwn"), pokemonName);
    }

    public Spanned drag(String pokemonName) {
        return linef(global().optString("drag"), pokemonName);
    }


    public Spanned fail(PokemonId pokemonId, String action) {
        if (action == null)
            return line(global().optString("fail"));

        String pokemonName = pokemon(pokemonId);

        if (action.contains("move")) {
            String move = action.substring(action.indexOf(':') + 1);
            String template = resolve(toId(move), "fail");
            if (template != null)
                return linef(template, pokemonName);
        }

        return line(global().optString("fail"));
    }


    //|-miss|p2a: Pyroar|p1a: Swanna
    public Spanned miss(PokemonId source, PokemonId target) {
        if (target == null)
            return linef(global().optString("missNoPokemon"), pokemon(source));

        return linef(global().optString("miss"), pokemon(target));
    }

    public Spanned move(PokemonId pokemonId, String moveName) {
        String pokemonName = pokemon(pokemonId);
        return linef(global().optString("move"), pokemonName, moveName);
    }

    //|-start|POKEMON|EFFECT
//    |-start|p2a: Blissey|move: Yawn|[of] p1a: Meowstic
    // -end|p1: Regigigas|Slow Start|[silent]
    public Spanned volatileStatus(PokemonId pokemonId, String effect, String of, boolean start) {
        if (of != null && of.contains("silent"))
            return null;

        String pokemonName = pokemon(pokemonId);
        if (effect.contains(":"))
            effect = effect.substring(effect.indexOf(':') + 1);

        String key = start ? "start" : "end";
        return linef(resolve(toId(effect), key), pokemonName);
    }

//    //|-weather|SunnyDay|[upkeep]
//    start: "  The sunlight turned harsh!",
//    end: "  The sunlight faded.",
//    upkeep: "  (The sunlight is strong!)",
    //|-weather|Sandstorm|[from] ability: Sand Stream|[of] p2a: Gigalith
//    |-weather|Hail|[from] ability: Snow Warning|[of] p2a: Vanilluxe
//    |-weather|DesolateLand|[from] ability: Desolate Land|[of] p1a: Groudon

    private String mLastWeather;

    public Spanned weather(String weather, String action) {
        if (weather.equals("none")) {
            if (mLastWeather == null) return null;
            String lastWeather = toId(mLastWeather);
            mLastWeather = null;
            return line(resolve(lastWeather, "end"));
        }
        mLastWeather = weather;

        if ("upkeep".contains(action)) {
            String template = resolve(toId(weather), "upkeep");
            return template == null ? null : line(template);
        }
        return line(resolve(toId(weather), "start"));
    }

    // |-sidestart|p1: Neee7|move: Stealth Rock
    // |-sidestart|p1: Neee7|Spikes
    public Spanned side(Player player, String side, boolean start) {
        String teamName = global().optString(player == Player.FOE ? "opposingTeam" : "team");
        String key = start ? "start" : "end";
        if (side.contains("move"))
            side = side.substring(side.indexOf(':') + 1);
        String template = resolve(toId(side), key);
        return linef(template, teamName);
    }

    public Spanned mega(PokemonId pokemonId, String item, String username) {
        String pokemonName = pokemon(pokemonId);
        if (item == null)
            return linef(global().optString("megaNoItem"), pokemonName, username);
        return linef(global().optString("mega"), pokemonName, item);
    }


    // typeChange: "  [POKEMON] transformed into the [TYPE] type!",
    //		typeChangeFromEffect: "  [POKEMON]'s [EFFECT] made it the [TYPE] type!",
    public Spanned typeChange(PokemonId pokemonId, String type, String from) {
        String pokemonName = pokemon(pokemonId);
        if (from == null) {
            return linef(global().optString("typeChange"), pokemonName, type);
        } else {
            if (from.contains(":")) from = from.substring(from.indexOf(':') + 1);
            return linef(global().optString("typeChangeFromEffect"), pokemonName, from, type);
        }
    }

    public Spanned win(String username) {
        return linef(global().optString("winBattle"), username);
    }

    public Spanned tie(String username1, String username2) {
        return linef(global().optString("tieBattle"), username1, username2);
    }


    // |cant|p2a: Steelix|slp
    //
    public Spanned cant(PokemonId pokemonId, String reason, String move) {
        String pokemonName = pokemon(pokemonId);
        if (reason.contains("move")) {
            String reasonMove = reason.substring(reason.indexOf(':') + 1);
            String template = resolve(toId(reasonMove), "cant");
            if (template != null)
                return linef(template, pokemonName, move);
        } else if (reason.contains("ability")) {
            String reasonAbility = reason.substring(reason.indexOf(':') + 1);
            String template = resolve(toId(reasonAbility), "cant");
            if (template != null)
                return linef(template, pokemonName);
        }

        if (move == null)
            return linef(global().optString("cantNoMove"), pokemonName);
        else
            return linef(global().optString("cant"), pokemonName, move);
    }

    //
//    |-enditem|p1a: Regirock|Chesto Berry|[eat]

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
}
