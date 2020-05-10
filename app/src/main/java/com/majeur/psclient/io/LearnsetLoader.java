package com.majeur.psclient.io;

import android.content.Context;
import android.content.res.Resources;
import android.util.JsonReader;
import androidx.collection.ArraySet;
import com.majeur.psclient.R;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

@SuppressWarnings({"unchecked", "rawtypes"})
public class LearnsetLoader extends AbsDataLoader<String, Set> {

    private final Resources mResources;

    public LearnsetLoader(Context context) {
        mResources = context.getResources();
    }

    @Override
    protected Set[] onCreateResultArray(int length) {
        return new Set[length];
    }

    @Override
    protected LoadInterface<String, Set> onCreateLoadInterface() {
        return new LoadInterfaceImpl();
    }

    private class LoadInterfaceImpl implements LoadInterface<String, Set> {

        private JsonReader mJsonReader;

        @Override
        public void onPreLoad() {
            InputStream inputStream = mResources.openRawResource(R.raw.learnsets);
            mJsonReader = new JsonReader(new InputStreamReader(inputStream));
        }

        @Override
        public void onLoadData(String[] queries, Set[] results) {
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

        private void parseJson(String[] queries, Set[] results) throws IOException {
            List<String> desiredKeys = new ArrayList<>();
            for (int i = 0; i < queries.length; i++) {
                if (results[i] == null)
                    desiredKeys.add(queries[i]);
                else
                    desiredKeys.add("will_not_match");
            }
            mJsonReader.beginObject();
            while (mJsonReader.hasNext()) {
                String species = mJsonReader.nextName();
                mJsonReader.beginObject();
                boolean mergeMoves = desiredKeys.contains(species);
                if (mJsonReader.nextName().equals("also")) {
                    if (!mergeMoves) {
                        String match = checkSpecies(desiredKeys);
                        if (match != null) {
                            mergeMoves = true;
                            species = match;
                        }
                    } else {
                        mJsonReader.skipValue();
                    }
                    mJsonReader.nextName(); // Make sure we skip the 'moves' key name
                }
                if (mergeMoves) {
                    int resultIndex = desiredKeys.indexOf(species);
                    if (results[resultIndex] == null) results[resultIndex] = new ArraySet();
                    results[resultIndex].addAll(parseMoves());
                } else {
                    mJsonReader.skipValue();
                }
                mJsonReader.endObject();
            }
            mJsonReader.endObject();
        }

        private String checkSpecies(List<String> desiredSpecies) throws IOException {
            String result = null;
            mJsonReader.beginArray();
            while (mJsonReader.hasNext()) {
                String species = mJsonReader.nextString();
                if (desiredSpecies.contains(species))
                    result = species;
            }
            mJsonReader.endArray();
            return result;
        }

        private List<String> parseMoves() throws IOException {
            List<String> moves = new LinkedList<>();
            mJsonReader.beginArray();
            while (mJsonReader.hasNext())
                moves.add(mJsonReader.nextString());
            mJsonReader.endArray();
            return moves;
        }
    }
}
