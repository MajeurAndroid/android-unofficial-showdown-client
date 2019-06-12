package com.majeur.psclient.service;

import com.majeur.psclient.model.BattleFormat;

import java.util.List;

public class TeamsMessageHandler extends ShowdownMessageHandler {

    private List<BattleFormat.Category> mBattleFormatCategories;

    @Override
    protected int getPriority() {
        return -1;
    }

    @Override
    boolean shouldHandleMessages(String messages) {
        return messages.charAt(0) != '>';
    }

    @Override
    protected void onHandleMessage(MessageIterator message) {
        String command = message.next();
        switch (command) {
            case "formats":
                mBattleFormatCategories = getShowdownService().getSharedData("formats");
                break;
        }
    }

    public List<BattleFormat.Category> getBattleFormatCategories() {
        return mBattleFormatCategories;
    }
}
