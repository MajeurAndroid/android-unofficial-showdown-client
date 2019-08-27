package com.majeur.psclient.io;

import android.content.Context;
import android.content.res.Resources;
import android.util.JsonReader;

import com.majeur.psclient.R;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class LearnsetLoader extends DataLoader<String, List> {

    private Resources mResources;

    public LearnsetLoader(Context context) {
        mResources = context.getResources();
    }

    @Override
    protected List[] onCreateResultArray(int length) {
        return new List[length];
    }

    @Override
    protected LoadInterface<String, List> onCreateLoadInterface() {
        return new LoadInterfaceImpl();
    }

    private class LoadInterfaceImpl implements LoadInterface<String, List> {

        private JsonReader mJsonReader;

        @Override
        public void onPreLoad() {
            InputStream inputStream = mResources.openRawResource(R.raw.learnsets);
            mJsonReader = new JsonReader(new InputStreamReader(inputStream));
        }

        @Override
        public void onLoadData(String[] queries, List[] results) {
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

        private void parseJson(String[] queries, List[] results) throws IOException {
            List<String> desiredKeys = new ArrayList<>();
            for (int i = 0; i < queries.length; i++) {
                if (results[i] == null)
                    desiredKeys.add(queries[i]);
                else
                    desiredKeys.add("will_not_match");
            }
            mJsonReader.beginObject();
            while (mJsonReader.hasNext()) {
                String name = mJsonReader.nextName();
                if (desiredKeys.contains(name)) {
                    results[desiredKeys.indexOf(name)] = parseLearnset();
                } else {
                    mJsonReader.skipValue();
                }
            }
            mJsonReader.endObject();
        }

        private List<String> parseLearnset() throws IOException {
            List<String> learnset = null;
            mJsonReader.beginObject();
            while (mJsonReader.hasNext()) {
                final String name = mJsonReader.nextName();
                switch (name) {
                    case "learnset":
                        learnset = parseMoves();
                        break;
                    default:
                        mJsonReader.skipValue();
                        break;
                }
            }
            mJsonReader.endObject();
            return learnset;
        }

        private List<String> parseMoves() throws IOException {
            List<String> moves = new LinkedList<>();
            mJsonReader.beginObject();
            while (mJsonReader.hasNext()) {
                final String name = mJsonReader.nextName();
                moves.add(name);
                mJsonReader.skipValue();
            }
            mJsonReader.endObject();
            return moves;
        }
    }
}
