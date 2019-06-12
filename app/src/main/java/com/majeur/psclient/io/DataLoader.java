package com.majeur.psclient.io;

import android.annotation.SuppressLint;
import android.os.AsyncTask;
import android.util.Log;

import java.util.HashMap;
import java.util.Map;

public abstract class DataLoader<Q, R> {

    public interface Callback<R> {
        void onLoaded(R[] results);
    }

    protected interface LoadInterface<Q, R> {

        void onPreLoad();

        void onLoadData(Q[] queries, R[] results);
    }

    private Map<Q, R> mCache = new HashMap<>();

    @SuppressLint("StaticFieldLeak")
    @SuppressWarnings("Unchecked")
    public void load(final Q[] queries, final Callback<R> callback) {
        onInterceptQuery(queries);

        final R[] resultArray = onCreateResultArray(queries.length);

        for (int i = 0; i < queries.length; i++) {
            Q query = queries[i];
            if (mCache.containsKey(query))
                resultArray[i] = mCache.get(query);
        }

        if (allNonNull(resultArray)) {
            onInterceptResult(resultArray);
            callback.onLoaded(resultArray);
        } else {
            final LoadInterface<Q, R> loadInterface = onCreateLoadInterface();
            AsyncTask<Q, Void, R[]> asyncTask = new AsyncTask<Q, Void, R[]>() {
                @Override
                protected R[] doInBackground(Q[] queries) {
                    loadInterface.onLoadData(queries, resultArray);
                    return resultArray;
                }

                @Override
                protected void onPostExecute(R[] results) {
                    for (int i = 0; i < results.length; i++) {
                        Q query = queries[i];
                        if (!mCache.containsKey(query))
                            mCache.put(query, results[i]);
                    }
                    onInterceptResult(results);
                    callback.onLoaded(results);
                }
            };
            loadInterface.onPreLoad();
            asyncTask.execute(queries);
        }
    }

    @SuppressLint("StaticFieldLeak")
    @SuppressWarnings("Unchecked")
    public R[] load(final Q[] queries) {
        onInterceptQuery(queries);

        final R[] resultArray = onCreateResultArray(queries.length);

        for (int i = 0; i < queries.length; i++) {
            Q query = queries[i];
            if (mCache.containsKey(query))
                resultArray[i] = mCache.get(query);
        }

        if (allNonNull(resultArray)) {
            return resultArray;
        } else {
            final LoadInterface<Q, R> loadInterface = onCreateLoadInterface();
            loadInterface.onPreLoad();
            loadInterface.onLoadData(queries, resultArray);
            for (int i = 0; i < resultArray.length; i++) {
                Q query = queries[i];
                if (!mCache.containsKey(query))
                    mCache.put(query, resultArray[i]);
            }
            return resultArray;
        }
    }

    private boolean allNonNull(R[] results) {
        for (R result : results)
            if (result == null)
                return false;
        return true;
    }

    protected void onInterceptQuery(Q[] queries) {
    }

    protected void onInterceptResult(R[] results) {
    }

    protected abstract R[] onCreateResultArray(int length);

    protected abstract LoadInterface<Q, R> onCreateLoadInterface();
}
