package com.majeur.psclient.model;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class BattleActionRequest {

    private final int mReqId;
    private Const mGameType;

    private final boolean mTeamPreview;
    private final boolean mShouldWait;
    private boolean[] mForceSwitch;
    private boolean[] mTrapped;
    private boolean[] mCanMegaEvo;
    private boolean[] mCanDynamax;
    private Move[][] mMoves;
    private final List<SidePokemon> mSide;

    public BattleActionRequest(JSONObject jsonObject, Const gameType) throws JSONException {
        mGameType = gameType;
        mShouldWait = jsonObject.optBoolean("wait", false);
        mTeamPreview = jsonObject.optBoolean("teamPreview", false);
        mReqId = jsonObject.getInt("rqid");

        JSONArray forceSwitchJsonArray = jsonObject.optJSONArray("forceSwitch");
        if (forceSwitchJsonArray != null) {
            int N = forceSwitchJsonArray.length();
            mForceSwitch = new boolean[N];
            for (int i = 0; i < N; i++) {
                mForceSwitch[i] = forceSwitchJsonArray.getBoolean(i);
            }
        }

        JSONArray activeJsonArray = jsonObject.optJSONArray("active");
        if (activeJsonArray != null) {
            int N = activeJsonArray.length();
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

                boolean canDynamax = movesContainer.optBoolean("canDynamax", false);
                if (canDynamax) {
                    if (mCanDynamax == null) mCanDynamax = new boolean[N];
                    mCanDynamax[i] = true;
                }

                JSONArray movesJsonArray = movesContainer.getJSONArray("moves");
                JSONArray zMovesJsonArray = movesContainer.optJSONArray("canZMove");
                JSONObject maxMoveJsonObject = movesContainer.optJSONObject("maxMoves");
                JSONArray maxMovesJsonArray = maxMoveJsonObject != null ? maxMoveJsonObject.optJSONArray("maxMoves")
                        : null;

                if (mMoves == null) mMoves = new Move[N][0];

                mMoves[i] = new Move[movesJsonArray.length()];
                for (int j = 0; j < movesJsonArray.length(); j++)
                    mMoves[i][j] = new Move(j, movesJsonArray.getJSONObject(j),
                            zMovesJsonArray != null ? zMovesJsonArray.optJSONObject(j) : null,
                            maxMovesJsonArray != null ? maxMovesJsonArray.optJSONObject(j) : null);
            }
        }

        JSONObject sideJsonObject = jsonObject.getJSONObject("side");
        JSONArray pokemonArray = sideJsonObject.getJSONArray("pokemon");
        int N = pokemonArray.length();
        mSide = new ArrayList<>(N);
        for (int i = 0; i < N; i++)
            mSide.add(SidePokemon.fromJson(pokemonArray.getJSONObject(i), i));
    }

    public Move[] getMoves(int which) {
        if (mMoves == null || which >= mMoves.length)
            return null;
        return mMoves[which];
    }

    public List<SidePokemon> getSide() {
        return mSide;
    }

    public int getId() {
        return mReqId;
    }

    public int getCount() {
        switch (mGameType) {
            case SINGLE:
                return 1;
            case DOUBLE:
                return 2;
            case TRIPLE:
                return 3;
            default:
                return 1;
        }
    }

    public void setGameType(Const gameType) {
        mGameType = gameType;
    }

    public boolean shouldWait() {
        return mShouldWait;
    }

    public boolean teamPreview() {
        return mTeamPreview;
    }

    public boolean shouldPass(int which) {
        return !forceSwitch(which) && getMoves(which) == null;
    }

    public boolean forceSwitch(int which) {
        if (mForceSwitch == null || which >= mForceSwitch.length)
            return false;
        return mForceSwitch[which];
    }

    public boolean trapped(int which) {
        if (mTrapped == null || which >= mTrapped.length)
            return false;
        return mTrapped[which];
    }

    public boolean canMegaEvo(int which) {
        if (mCanMegaEvo == null || which >= mCanMegaEvo.length)
            return false;
        return mCanMegaEvo[which];
    }

    public boolean canDynamax(int which) {
        if (mCanDynamax == null || which >= mCanDynamax.length)
            return false;
        return mCanDynamax[which];
    }

    public boolean isDynamaxed(int which) {
        if (canDynamax(which) || getMoves(which) == null) return false;
        boolean hasMaxMoves = false;
        for (Move move : getMoves(which)) {
            if (move.maxMoveId != null) {
                hasMaxMoves = true;
                break;
            }
        }
        return hasMaxMoves;
    }

    @Override
    public String toString() {
        return "BattleActionRequest{" +
                "mReqId=" + mReqId +
                ", mTeamPreview=" + mTeamPreview +
                ", mShouldWait=" + mShouldWait +
                ", mForceSwitch=" + Arrays.toString(mForceSwitch) +
                ", mTrapped=" + Arrays.toString(mTrapped) +
                ", mCanMegaEvo=" + Arrays.toString(mCanMegaEvo) +
                ", mCanDynamax=" + Arrays.toString(mCanDynamax) +
                ", mMoves=" + Arrays.toString(mMoves) +
                ", side=" + mSide +
                '}';
    }
}
