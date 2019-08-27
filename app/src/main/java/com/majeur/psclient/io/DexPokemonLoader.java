package com.majeur.psclient.io;

import android.content.Context;
import android.content.res.Resources;
import android.util.JsonReader;

import com.majeur.psclient.R;
import com.majeur.psclient.model.DexPokemon;
import com.majeur.psclient.model.Stats;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class DexPokemonLoader extends DataLoader<String, DexPokemon> {

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
            DexPokemon pokemon = new DexPokemon();
            mJsonReader.beginObject();
            while (mJsonReader.hasNext()) {
                String name = mJsonReader.nextName();
                switch (name) {
                    case "num":
                        pokemon.num = mJsonReader.nextInt();
                        break;
                    case "species":
                        pokemon.species = mJsonReader.nextString();
                        break;
                    case "types":
                        parseTypes(pokemon);
                        break;
//                    case "genderRatio":
//                        // TODO
//                        break;
                    case "baseStats":
                        pokemon.baseStats = parseStats();
                        break;
                    case "abilities":
                        parseAbilities(pokemon);
                        break;
                    case "heightm":
                        pokemon.heightm = (float) mJsonReader.nextDouble();
                        break;
                    case "weightkg":
                        pokemon.weightkg = (float) mJsonReader.nextDouble();
                        break;
                    case "color":
                        pokemon.color = mJsonReader.nextString();
                        break;
                    case "gender":
                        pokemon.gender = mJsonReader.nextString();
                        break;
                    //case "evos":
//                            [
//                    "ivysaur"
//  ],
//                    "eggGroups": [
//                    "Monster",
//                            "Grass"
//  ],
                    case "LC":
                        pokemon.tier = mJsonReader.nextString();
                        break;
                    default:
                        mJsonReader.skipValue();
                        break;
                }
            }
            mJsonReader.endObject();
            return pokemon;
        }

        private void parseTypes(DexPokemon pokemon) throws IOException {
            mJsonReader.beginArray();
            while (mJsonReader.hasNext()) {
                if (pokemon.firstType == null)
                    pokemon.firstType = mJsonReader.nextString();
                else
                    pokemon.secondType = mJsonReader.nextString();
            }
            mJsonReader.endArray();
        }

        private void parseAbilities(DexPokemon pokemon) throws IOException {
            pokemon.abilities = new LinkedList<>();
            mJsonReader.beginObject();
            while (mJsonReader.hasNext()) {
                if (mJsonReader.nextName().equals("H"))
                    pokemon.hiddenAbility = mJsonReader.nextString();
                else
                    pokemon.abilities.add(mJsonReader.nextString());
            }
            mJsonReader.endObject();
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
