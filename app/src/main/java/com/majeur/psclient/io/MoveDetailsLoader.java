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

import static com.majeur.psclient.model.Id.toId;

public class MoveDetailsLoader extends AbsDataLoader<String, Move.Details> {

    private Resources mResources;

    public MoveDetailsLoader(Context context) {
        mResources = context.getResources();
    }

    @Override
    protected Move.Details[] onCreateResultArray(int length) {
        return new Move.Details[length];
    }

    @Override
    protected void onInterceptQuery(String[] queries) {
        for (int i = 0; i < queries.length; i++) {
            if (queries[i] == null) continue;
            if (queries[i].toLowerCase().startsWith("z-")) queries[i] = queries[i].substring(2);
            queries[i] = toId(queries[i]);
        }
    }

    @Override
    protected LoadInterface<String, Move.Details> onCreateLoadInterface() {
        return new LoadInterfaceImpl();
    }

    private class LoadInterfaceImpl implements LoadInterface<String, Move.Details> {

        private JsonReader mJsonReader;

        @Override
        public void onPreLoad() {
            InputStream inputStream = mResources.openRawResource(R.raw.moves);
            mJsonReader = new JsonReader(new InputStreamReader(inputStream));
        }

        @Override
        public void onLoadData(String[] queries, Move.Details[] results) {
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

        private void parseJson(String[] queries, Move.Details[] results) throws IOException {
            List<String> desiredKeys = new ArrayList<>();
            for (int i = 0; i < queries.length; i++) {
                if (queries[i] != null && results[i] == null)
                    desiredKeys.add(queries[i]);
                else
                    desiredKeys.add("do_not_match");
            }

            mJsonReader.beginObject();
            while (mJsonReader.hasNext()) {
                String name = mJsonReader.nextName();
                if (desiredKeys.contains(name)) {
                    Move.Details move = parseMove();
                    for (int i = 0; i < results.length; i++) {
                        if (desiredKeys.get(i).equals(name))
                            results[i] = move;
                    }
                } else {
                    mJsonReader.skipValue();
                }
            }
            mJsonReader.endObject();
        }

        private Move.Details parseMove() throws IOException {
            int accuracy = 0;
            int basePower = 0;
            int zPower = 0;
            int priority = 0;
            int pp = 0;
            String category = null;
            String desc = null, shortDesc = null;
            String zEffect = null;
            String type = null;
            String moveName = null;
            String target = null;
            int maxPower = 0;
            mJsonReader.beginObject();
            while (mJsonReader.hasNext()) {
                String name = mJsonReader.nextName();
                switch (name) {
                    case "accuracy":
                        JsonToken token = mJsonReader.peek();
                        if (token == JsonToken.BOOLEAN)
                            accuracy = mJsonReader.nextBoolean() ? -1 : 0;
                        else
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
                    case "shortDesc":
                        shortDesc = mJsonReader.nextString();
                        break;
                    case "type":
                        type = mJsonReader.nextString();
                        break;
                    case "priority":
                        priority = mJsonReader.nextInt();
                        break;
                    case "name":
                        moveName = mJsonReader.nextString();
                        break;
                    case "pp":
                        pp = mJsonReader.nextInt();
                        break;
                    case "zMovePower":
                        zPower = mJsonReader.nextInt();
                        break;
                    case "target":
                        target = mJsonReader.nextString();
                        break;
                    case "zMoveEffect":
                        zEffect = zMoveEffects(mJsonReader.nextString());
                        break;
                    case "gmaxPower":
                        maxPower = mJsonReader.nextInt();
                        break;
                    default:
                        mJsonReader.skipValue();
                        break;
                }
            }
            mJsonReader.endObject();
            return new Move.Details(moveName, accuracy, priority, basePower, zPower, category,
                    desc != null ? desc : shortDesc, type, pp, target, zEffect, maxPower);
        }

        private String zMoveEffects(String effect) {
            if (effect == null) return null;
            switch (effect) {
                case "clearnegativeboost": return "Restores negative stat stages to 0";
                case "crit2": return "Crit ratio +2";
                case "heal": return "Restores HP 100%";
                case "curse": return "Restores HP 100% if user is Ghost type, otherwise Attack +1";
                case "redirect": return "Redirects opposing attacks to user";
                case "healreplacement": return "Restores replacement's HP 100%";
                default: return null;
            }
        }
    }
}
