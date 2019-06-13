package com.majeur.psclient.service;

import com.majeur.psclient.model.BattleFormat;

import java.util.List;

public class TeamsMessageObserver extends MessageObserver {

    private List<BattleFormat.Category> mBattleFormatCategories;


    @Override
    protected boolean onMessage(ServerMessage message) {
        switch (message.command) {
            case "formats":
                mBattleFormatCategories = getService().getSharedData("formats");
                break;
        }
        return true;
    }

    public List<BattleFormat.Category> getBattleFormatCategories() {
        return mBattleFormatCategories;
    }
}
