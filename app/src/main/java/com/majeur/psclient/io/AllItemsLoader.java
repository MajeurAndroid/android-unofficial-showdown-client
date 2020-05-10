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

import static com.majeur.psclient.util.Utils.addNullSafe;

public class AllItemsLoader extends AbsDataLoader<String, List> {

    private Resources mResources;

    public AllItemsLoader(Context context) {
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
            InputStream inputStream = mResources.openRawResource(R.raw.items);
            mJsonReader = new JsonReader(new InputStreamReader(inputStream));
        }

        @Override
        public void onLoadData(String[] queries, List[] results) {
            if (queries.length != 1)
                throw new InvalidQueryException("For ItemsLoader, query must contains only one string (constraint for species search).");
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
            String constraint = queries[0];
            List<Item> species = new ArrayList<>();
            mJsonReader.beginObject();
            while (mJsonReader.hasNext()) {
                String name = mJsonReader.nextName();
                if (name.contains(constraint))
                    addNullSafe(species, parseSpecies());
                else
                    mJsonReader.skipValue();
            }
            mJsonReader.endObject();

            results[0] = species;
        }

        private Item parseSpecies() throws IOException {
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
