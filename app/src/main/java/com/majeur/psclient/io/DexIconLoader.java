package com.majeur.psclient.io;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapRegionDecoder;
import android.graphics.Rect;
import android.util.JsonReader;

import com.majeur.psclient.R;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;

public class DexIconLoader extends DataLoader<String, Bitmap> {

    private static final int SHEET_WIDTH = 480;
    private static final int SHEET_HEIGHT = 3030;
    private static final int ELEMENT_WIDTH = 40;
    private static final int ELEMENT_HEIGHT = 30;

    private Resources mResources;

    public DexIconLoader(Context resources) {
        mResources = resources.getResources();
    }

    @Override
    protected Bitmap[] onCreateResultArray(int length) {
        return new Bitmap[length];
    }

    @Override
    protected LoadInterface<String, Bitmap> onCreateLoadInterface() {
        return new LoadInterfaceImpl();
    }

    @Override
    protected void onInterceptQuery(String[] queries) {
        for (int i = 0; i < queries.length; i++)
            if (queries[i].contains("arceus")) queries[i] = "arceus";
    }

    @Override
    protected void onInterceptResult(Bitmap[] results) {
//        for (int i = 0; i < results.length; i++)
//            results[i] = results[i].mutate();
    }

    private class LoadInterfaceImpl implements LoadInterface<String, Bitmap> {

        private JsonReader mJsonReader;
        private InputStream mInputStream;
        private BitmapRegionDecoder mDecoder;
        private Rect mTempRect;

        @Override
        public void onPreLoad() {
            mTempRect = new Rect();
            InputStream inputStream = mResources.openRawResource(R.raw.dex_icon_indexes);
            InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
            mJsonReader = new JsonReader(inputStreamReader);
            mInputStream = mResources.openRawResource(R.raw.dex_icon_sheet);
        }

        @Override
        public void onLoadData(String[] queries, Bitmap[] results) {
            int[] speciesIconIndexes;
            try {
                speciesIconIndexes = parseJson(queries);
            } catch (IOException e) {
                e.printStackTrace();
                return;
            } finally {
                try {
                    mJsonReader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            try {
                mDecoder = BitmapRegionDecoder.newInstance(mInputStream, true);
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }

            for (int i = 0; i < results.length; i++) {
                if (results[i] != null) continue;
                results[i] = loadSpecieBitmap(speciesIconIndexes[i]);
            }

            try {
                mInputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private int[] parseJson(String... species) throws IOException {
            List<String> desiredSpecies = Arrays.asList(species);
            int[] matches = new int[species.length];

            mJsonReader.beginObject();
            while (mJsonReader.hasNext()) {
                String specie = mJsonReader.nextName();
                if (desiredSpecies.contains(specie)) {
                    matches[desiredSpecies.indexOf(specie)] = mJsonReader.nextInt();
                } else {
                    mJsonReader.skipValue();
                }
            }
            mJsonReader.endObject();

            return matches;
        }

        private Bitmap loadSpecieBitmap(int index) {
            int xDim = SHEET_WIDTH / ELEMENT_WIDTH;
            int x = index % xDim;
            int y = index / xDim;
            mTempRect.set(x * ELEMENT_WIDTH, y * ELEMENT_HEIGHT, (x + 1) * ELEMENT_WIDTH, (y + 1) * ELEMENT_HEIGHT);
            return mDecoder.decodeRegion(mTempRect, null);
        }
    }
}

