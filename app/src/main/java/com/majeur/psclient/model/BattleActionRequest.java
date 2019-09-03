package com.majeur.psclient.model;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class BattleActionRequest {

    private int mReqId;

    private boolean mTeamPreview;
    private boolean mShouldWait;
    private boolean[] mForceSwitch;
    private boolean[] mTrapped;
    private boolean[] mCanMegaEvo;

    private List<Move> mFirstPokemonMoves = null;
    private List<Move> mSecondPokemonMoves = null;
    private List<SidePokemon> side;

    public BattleActionRequest(JSONObject jsonObject) throws JSONException {
        mShouldWait = jsonObject.optBoolean("wait", false);
        mTeamPreview = jsonObject.optBoolean("teamPreview", false);
        mReqId = jsonObject.getInt("rqid");

        JSONArray waitJsonArray = jsonObject.optJSONArray("forceSwitch");
        if (waitJsonArray != null) {
            int N = Math.min(waitJsonArray.length(), 2);
            mForceSwitch = new boolean[N];
            for (int i = 0; i < N; i++) {
                mForceSwitch[i] = waitJsonArray.getBoolean(i);
            }
        }

        JSONArray activeJsonArray = jsonObject.optJSONArray("active");
        if (activeJsonArray != null) {
            int N = Math.min(activeJsonArray.length(), 2);
            for (int i = 0; i < N; i++) {
                JSONObject movesContainer = activeJsonArray.getJSONObject(i);

                boolean trapped = movesContainer.optBoolean("trapped", false);
                if (trapped) {
                    if (mTrapped == null) mTrapped = new boolean[N];
                    mTrapped[i] = true;
                }

                boolean canMegaEvo = movesContainer.optBoolean("canMegaEvo", false);
                if (canMegaEvo) {
                    if (mCanMegaEvo == null) mCanMegaEvo = new boolean[N];
                    mCanMegaEvo[i] = true;
                }

                JSONArray movesJsonArray = movesContainer.getJSONArray("moves");
                JSONArray zMovesJsonArray = movesContainer.optJSONArray("canZMove");
                List<Move> moveList = new LinkedList<>();

                for (int j = 0; j < movesJsonArray.length(); j++)
                    moveList.add(new Move(j, movesJsonArray.getJSONObject(j),
                            zMovesJsonArray != null ? zMovesJsonArray.optJSONObject(j) : null));

                if (i == 0)
                    mFirstPokemonMoves = moveList;
                else
                    mSecondPokemonMoves = moveList;
            }
        }

        JSONObject sideJsonObject = jsonObject.getJSONObject("side");
        JSONArray pokemonArray = sideJsonObject.getJSONArray("pokemon");
        int N = pokemonArray.length();
        side = new ArrayList<>(N);
        for (int i = 0; i < N; i++)
            side.add(new SidePokemon(i, pokemonArray.getJSONObject(i)));
    }

    public List<Move> getMoves() {
        return getMoves(0);
    }

    public List<Move> getMoves(int which) {
        if (which == 0)
            return mFirstPokemonMoves;
        else
            return mSecondPokemonMoves;
    }

    public List<SidePokemon> getSide() {
        return side;
    }

    public int getId() {
        return mReqId;
    }

    public boolean teamPreview() {
        return mTeamPreview;
    }

    public boolean shouldWait() {
        return mShouldWait;
    }

    public boolean forceSwitch() {
        return forceSwitch(0);
    }

    public boolean trapped() {
        return trapped(0);
    }

    public boolean canMegaEvo() {
        return canMegaEvo(0);
    }

    public boolean forceSwitch(int which) {
        if (mForceSwitch == null)
            return false;

        if (which == 0)
            return mForceSwitch[0];
        else
            return mForceSwitch[1];
    }

    public boolean trapped(int which) {
        if (mTrapped == null)
            return false;

        if (which == 0)
            return mTrapped[0];
        else
            return mTrapped[1];
    }

    public boolean canMegaEvo(int which) {
        if (mCanMegaEvo == null)
            return false;

        if (which == 0)
            return mCanMegaEvo[0];
        else
            return mCanMegaEvo[1];
    }

    @Override
    public String toString() {
        return "BattleActionRequest{" +
                "mReqId=" + mReqId +
                ", mShouldWait=" + mShouldWait +
                ", mForceSwitch=" + Arrays.toString(mForceSwitch) +
                ", mTrapped=" + Arrays.toString(mTrapped) +
                ", mFirstPokemonMoves=" + mFirstPokemonMoves +
                ", mSecondPokemonMoves=" + mSecondPokemonMoves +
                ", side=" + side +
                '}';
    }
}
