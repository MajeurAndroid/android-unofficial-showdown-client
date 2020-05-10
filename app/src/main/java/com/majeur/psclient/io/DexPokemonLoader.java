package com.majeur.psclient.io;

import android.content.Context;
import android.content.res.Resources;
import android.util.JsonReader;
import androidx.core.util.Pair;
import com.majeur.psclient.R;
import com.majeur.psclient.model.DexPokemon;
import com.majeur.psclient.model.Stats;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class DexPokemonLoader extends AbsDataLoader<String, DexPokemon> {

    private Resources mResources;

    public DexPokemonLoader(Context context) {
        mResources = context.getResources();
    }

    @Override
    protected DexPokemon[] onCreateResultArray(int length) {
        return new DexPokemon[length];
    }

    @Override
    protected LoadInterface<String, DexPokemon> onCreateLoadInterface() {
        return new LoadInterfaceImpl();
    }

    private class LoadInterfaceImpl implements LoadInterface<String, DexPokemon> {

        private JsonReader mJsonReader;

        @Override
        public void onPreLoad() {
            InputStream inputStream = mResources.openRawResource(R.raw.dex);
            mJsonReader = new JsonReader(new InputStreamReader(inputStream));
        }

        @Override
        public void onLoadData(String[] queries, DexPokemon[] results) {
            try {
                parseJson(queries, results);
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    mJsonReader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        private void parseJson(String[] queries, DexPokemon[] results) throws IOException {
            List<String> desiredKeys = new ArrayList<>();
            for (int i = 0; i < queries.length; i++) {
                if (results[i] == null)
                    desiredKeys.add(queries[i]);
                else
                    desiredKeys.add("do_not_match");
            }

            mJsonReader.beginObject();
            while (mJsonReader.hasNext()) {
                String name = mJsonReader.nextName();
                if (desiredKeys.contains(name)) {
                    results[desiredKeys.indexOf(name)] = parsePokemon();
                } else {
                    mJsonReader.skipValue();
                }
            }
            mJsonReader.endObject();
        }

        private DexPokemon parsePokemon() throws IOException {
            String species = null;
            int num = 0;
            String firstType = null;
            String secondType = null;
            Stats baseStats = null;
            List<String> abilities = Collections.emptyList();
            String hiddenAbility = null;
            float height = 0f;
            float weight = 0f;
            String color = null;
            String gender = null;
            String tier = null;

            mJsonReader.beginObject();
            while (mJsonReader.hasNext()) {
                String name = mJsonReader.nextName();
                switch (name) {
                    case "num":
                        num = mJsonReader.nextInt();
                        break;
                    case "name":
                        species = mJsonReader.nextString();
                        break;
                    case "types":
                        String[] types = parseTypes();
                        firstType = types[0];
                        secondType = types[1];
                        break;
                    case "genderRatio":
                          // TODO
                        mJsonReader.skipValue();
                        break;
                    case "baseStats":
                        baseStats = parseStats();
                        break;
                    case "abilities":
                        Pair<String, List<String>> p = parseAbilities();
                        hiddenAbility = p.first;
                        abilities = p.second;
                        break;
                    case "heightm":
                        height = (float) mJsonReader.nextDouble();
                        break;
                    case "weightkg":
                        weight = (float) mJsonReader.nextDouble();
                        break;
                    case "color":
                        color = mJsonReader.nextString();
                        break;
                    case "gender":
                        gender = mJsonReader.nextString();
                        break;
                    case "evos":
                        // TODO
                        mJsonReader.skipValue();
                        break;
                    case "LC":
                        tier = mJsonReader.nextString();
                        break;
                    default:
                        mJsonReader.skipValue();
                        break;
                }
            }
            mJsonReader.endObject();

            return new DexPokemon(species, num, firstType, secondType, baseStats, abilities,
                    hiddenAbility, height, weight, color, gender, tier);
        }

        private String[] parseTypes() throws IOException {
            String[] types = new String[2];
            mJsonReader.beginArray();
            while (mJsonReader.hasNext()) {
                if (types[0] == null)
                    types[0] = mJsonReader.nextString();
                else
                    types[1] = mJsonReader.nextString();
            }
            mJsonReader.endArray();
            return types;
        }

        private Pair<String, List<String>> parseAbilities() throws IOException {
            String hiddenAbility = null;
            List<String> abilities = new LinkedList<>();
            mJsonReader.beginObject();
            while (mJsonReader.hasNext()) {
                if (mJsonReader.nextName().equals("H"))
                    hiddenAbility = mJsonReader.nextString();
                else
                    abilities.add(mJsonReader.nextString());
            }
            mJsonReader.endObject();
            return new Pair<>(hiddenAbility, abilities);
        }

        private Stats parseStats() throws IOException {
            int hp = 0;
            int atk = 0;
            int def = 0;
            int spa = 0;
            int spd = 0;
            int spe = 0;

            mJsonReader.beginObject();
            while (mJsonReader.hasNext()) {
                String name = mJsonReader.nextName();
                switch (name) {
                    case "hp":
                        hp = mJsonReader.nextInt();
                        break;
                    case "atk":
                        atk = mJsonReader.nextInt();
                        break;
                    case "def":
                        def = mJsonReader.nextInt();
                        break;
                    case "spa":
                        spa = mJsonReader.nextInt();
                        break;
                    case "spd":
                        spd = mJsonReader.nextInt();
                        break;
                    case "spe":
                        spe = mJsonReader.nextInt();
                        break;
                }
            }
            mJsonReader.endObject();
            return new Stats(hp, atk, def, spa, spd, spe);
        }
    }
}
