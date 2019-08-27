package com.majeur.psclient.io;

import android.content.Context;
import android.content.res.Resources;
import android.util.JsonReader;

import com.majeur.psclient.R;
import com.majeur.psclient.model.Species;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import static com.majeur.psclient.util.Utils.addNullSafe;

public class AllSpeciesLoader extends DataLoader<String, List> {

    private Resources mResources;

    public AllSpeciesLoader(Context context) {
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
            InputStream inputStream = mResources.openRawResource(R.raw.dex);
            mJsonReader = new JsonReader(new InputStreamReader(inputStream));
        }

        @Override
        public void onLoadData(String[] queries, List[] results) {
            if (queries.length != 1)
                throw new InvalidQueryException("For SpeciesLoader, query must contains only one string (constraint for species search).");
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
            List<Species> species = new ArrayList<>();
            mJsonReader.beginObject();
            while (mJsonReader.hasNext()) {
                String name = mJsonReader.nextName();
                if (name.contains(constraint))
                    addNullSafe(species, parseSpecies(name));
                else
                    mJsonReader.skipValue();
            }
            mJsonReader.endObject();

            results[0] = species;
        }

        private Species parseSpecies(String id) throws IOException {
            Species species = new Species();
            species.id = id;

            mJsonReader.beginObject();
            while (mJsonReader.hasNext()) {
                final String name = mJsonReader.nextName();
                switch (name) {
                    case "species":
                        species.name = mJsonReader.nextString();
                        break;
                    case "forme":
                        String forme = mJsonReader.nextString();
                        //if (forme.contains("Mega") || forme.contains("Primal"))
                        //    skip = true;
                        break;
                    default:
                        mJsonReader.skipValue();
                        break;
                }
            }
            mJsonReader.endObject();
            return species;
        }
    }
}
