package com.majeur.psclient.io;

import android.content.Context;
import android.content.res.Resources;
import android.util.JsonReader;
import android.util.JsonToken;

import com.majeur.psclient.R;
import com.majeur.psclient.model.Move;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class MoveDetailsLoader extends DataLoader<String, Move.ExtraInfo> {

    private Resources mResources;

    public MoveDetailsLoader(Context context) {
        mResources = context.getResources();
    }

    @Override
    protected Move.ExtraInfo[] onCreateResultArray(int length) {
        return new Move.ExtraInfo[length];
    }

    @Override
    protected LoadInterface<String, Move.ExtraInfo> onCreateLoadInterface() {
        return new LoadInterfaceImpl();
    }

    private class LoadInterfaceImpl implements LoadInterface<String, Move.ExtraInfo> {

        private JsonReader mJsonReader;

        @Override
        public void onPreLoad() {
            InputStream inputStream = mResources.openRawResource(R.raw.moves);
            mJsonReader = new JsonReader(new InputStreamReader(inputStream));
        }

        @Override
        public void onLoadData(String[] queries, Move.ExtraInfo[] results) {
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

        private void parseJson(String[] queries, Move.ExtraInfo[] results) throws IOException {
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
                    results[desiredKeys.indexOf(name)] = parseMove();
                } else {
                    mJsonReader.skipValue();
                }
            }
            mJsonReader.endObject();
        }

        private Move.ExtraInfo parseMove() throws IOException {
            int accuracy = 0;
            int basePower = 0;
            int priority = 0;
            String category = null;
            String desc = null;
            String type = null;
            mJsonReader.beginObject();
            while (mJsonReader.hasNext()) {
                String name = mJsonReader.nextName();
                switch (name) {
                    case "accuracy":
                        JsonToken token = mJsonReader.peek();
                        if (token == JsonToken.BOOLEAN) {
                            mJsonReader.nextBoolean();
                            accuracy = -1;
                        } else
                            accuracy = mJsonReader.nextInt();
                        break;
                    case "basePower":
                        basePower = mJsonReader.nextInt();
                        break;
                    case "category":
                        category = mJsonReader.nextString();
                        break;
                    case "desc":
                        desc = mJsonReader.nextString();
                        break;
                    case "type":
                        type = mJsonReader.nextString();
                        break;
                    case "priority":
                        priority = mJsonReader.nextInt();
                        break;
                    default:
                        mJsonReader.skipValue();
                        break;
                }
            }
            mJsonReader.endObject();
            return new Move.ExtraInfo(accuracy, priority, basePower, category, desc, type);
        }
    }
}
