package com.majeur.psclient.io;

import android.content.Context;
import android.content.res.Resources;
import android.util.JsonReader;
import com.majeur.psclient.R;
import com.majeur.psclient.model.Item;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class ItemLoader extends AbsDataLoader<String, Item> {

    private Resources mResources;

    public ItemLoader(Context context) {
        mResources = context.getResources();
    }

    @Override
    protected Item[] onCreateResultArray(int length) {
        return new Item[length];
    }

    @Override
    protected LoadInterface<String, Item> onCreateLoadInterface() {
        return new LoadInterfaceImpl();
    }

    private class LoadInterfaceImpl implements LoadInterface<String, Item> {

        private JsonReader mJsonReader;

        @Override
        public void onPreLoad() {
            InputStream inputStream = mResources.openRawResource(R.raw.items);
            mJsonReader = new JsonReader(new InputStreamReader(inputStream));
        }

        @Override
        public void onLoadData(String[] queries, Item[] results) {
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

        private void parseJson(String[] queries, Item[] results) throws IOException {
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
                if (desiredKeys.contains(name))
                    results[desiredKeys.indexOf(name)] = parseItem();
                else
                    mJsonReader.skipValue();
            }
            mJsonReader.endObject();
        }

        private Item parseItem() throws IOException {
            Item item = new Item();
            mJsonReader.beginObject();
            while (mJsonReader.hasNext()) {
                final String name = mJsonReader.nextName();
                switch (name) {
                    case "name":
                        item.name = mJsonReader.nextString();
                        break;
                    case "id":
                        item.id = mJsonReader.nextString();
                        break;
                    case "desc":
                        item.desc = mJsonReader.nextString();
                        break;
                    case "spritenum":
                        item.spriteNum = mJsonReader.nextInt();
                        break;
                    default:
                        mJsonReader.skipValue();
                        break;
                }
            }
            mJsonReader.endObject();
            return item;
        }
    }
}
