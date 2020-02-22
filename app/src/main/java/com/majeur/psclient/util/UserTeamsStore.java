package com.majeur.psclient.util;

import android.content.Context;
import android.os.AsyncTask;

import com.majeur.psclient.model.Team;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class UserTeamsStore {

    private static final String FILE_NAME = "user_teams.json";
    private static final String JSON_KEY_FORMAT = "label";
    private static final String JSON_KEY_TEAMS = "teams";
    private static final String JSON_KEY_TEAM_LABEL = "label";
    private static final String JSON_KEY_TEAM_DATA = "data";

    private File mJsonFile;
    private JSONArray mPersistedJson;

    public UserTeamsStore(Context context) {
        mJsonFile = new File(context.getFilesDir(), FILE_NAME);
        try {
            FileInputStream fis = new FileInputStream(mJsonFile);
            //Log.e(getClass().getSimpleName(), Utils.convertStreamToString(fis));
            fis.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void read(final Callback<List<Team.Group>> callback) {
        new ReadJsonFileTask(new Callback<JSONArray>() {
            @Override
            public void callback(JSONArray array) {
                mPersistedJson = array;

                if (mPersistedJson == null) {
                    callback.callback(Collections.<Team.Group>emptyList());
                    return;
                }

                try {
                    callback.callback(parseJson());
                } catch (JSONException e) {
                    e.printStackTrace();
                    callback.callback(Collections.<Team.Group>emptyList());
                }
            }
        }).execute(mJsonFile);
    }

    private List<Team.Group> parseJson() throws JSONException {
        List<Team.Group> teamGroups = new ArrayList<>();

        for (int i = 0; i < mPersistedJson.length(); i++) {
            JSONObject teamGroupJson = mPersistedJson.getJSONObject(i);

            String format = teamGroupJson.getString(JSON_KEY_FORMAT);
            Team.Group teamGroup = new Team.Group(format);

            JSONArray teamsJsonArray = teamGroupJson.getJSONArray(JSON_KEY_TEAMS);
            for (int j = 0; j < teamsJsonArray.length(); j++) {
                JSONObject teamJsonObject = teamsJsonArray.getJSONObject(j);
                String label = teamJsonObject.optString(JSON_KEY_TEAM_LABEL);
                String data = teamJsonObject.getString(JSON_KEY_TEAM_DATA);
                teamGroup.teams.add(Team.unpack(label, format, data));
            }
            teamGroups.add(teamGroup);
        }

        return teamGroups;
    }

    public void write(List<Team.Group> teamGroups, final Callback<Boolean> callback) {
        try {
            mPersistedJson = makeJson(teamGroups);

            new WriteJsonFileTask(new Callback<Boolean>() {
                @Override
                public void callback(Boolean aBoolean) {
                    if (callback != null)
                        callback.callback(aBoolean);
                }
            }).execute(mJsonFile, mPersistedJson);

        } catch (JSONException e) {
            e.printStackTrace();
            if (callback != null)
                callback.callback(Boolean.FALSE);
        }
    }

    private JSONArray makeJson(List<Team.Group> teamGroups) throws JSONException {
        JSONArray jsonArray = new JSONArray();

        for (Team.Group teamGroup : teamGroups) {
            JSONObject teamGroupJsonObject = new JSONObject();
            teamGroupJsonObject.put(JSON_KEY_FORMAT, teamGroup.format);

            JSONArray teamsJsonArray = new JSONArray();
            for (Team team : teamGroup.teams) {
                JSONObject teamJsonObject = new JSONObject();
                teamJsonObject.put(JSON_KEY_TEAM_LABEL, team.label == null ? "Unnamed team" : team.label);
                teamJsonObject.put(JSON_KEY_TEAM_DATA, team.pack());
                teamsJsonArray.put(teamJsonObject);
            }

            teamGroupJsonObject.put(JSON_KEY_TEAMS, teamsJsonArray);

            jsonArray.put(teamGroupJsonObject);
        }

        return jsonArray;
    }

    private static class ReadJsonFileTask extends AsyncTask<File, Void, JSONArray> {

        private Callback<JSONArray> mCallback;

        ReadJsonFileTask(Callback<JSONArray> callback) {
            mCallback = callback;
        }

        @Override
        protected void onPostExecute(JSONArray array) {
            super.onPostExecute(array);
            if (mCallback != null)
                mCallback.callback(array);
        }

        @Override
        protected JSONArray doInBackground(File... files) {
            try {
                File jsonFile = files[0];
                InputStream inputStream = new FileInputStream(jsonFile);
                String rawJson = Utils.convertStreamToString(inputStream);
                inputStream.close();
                return new JSONArray(rawJson);
            } catch (JSONException | IOException e) {
                e.printStackTrace();
                return null;
            }
        }
    }

    private static class WriteJsonFileTask extends AsyncTask<Object, Void, Boolean> {

        private Callback<Boolean> mCallback;

        WriteJsonFileTask(Callback<Boolean> callback) {
            mCallback = callback;
        }

        @Override
        protected void onPostExecute(Boolean success) {
            super.onPostExecute(success);
            if (mCallback != null)
                mCallback.callback(success);
        }

        @Override
        protected Boolean doInBackground(Object... objects) {
            try {
                File jsonFile = (File) objects[0];
                JSONArray jsonArray = (JSONArray) objects[1];
                OutputStream outputStream = new FileOutputStream(jsonFile);
                outputStream.write(jsonArray.toString().getBytes());
                outputStream.close();
                return Boolean.TRUE;
            } catch (IOException e) {
                e.printStackTrace();
                return Boolean.FALSE;
            }
        }
    }
}
