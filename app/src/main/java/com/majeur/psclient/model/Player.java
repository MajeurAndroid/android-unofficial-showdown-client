package com.majeur.psclient.model;

public enum  Player {

    FOE,
    TRAINER;

    public static Player get(String playerId, String username1, String username2, String myUsername) {
        playerId = playerId.substring(0, "pX".length());
        boolean idIsOne = playerId.contains("1");
        boolean watching = !username1.equals(myUsername) && !username2.equals(myUsername);
        if (watching)
            return idIsOne ? TRAINER : FOE;

        if (idIsOne)
            return username1.equals(myUsername) ? TRAINER : FOE;
        else
            return username2.equals(myUsername) ? TRAINER : FOE;
    }

    public String username(String username1, String username2, String myUsername) {
        if (username1.equals(myUsername)) {
            return this == TRAINER ? username1 : username2;
        } else if (username2.equals(myUsername)) {
            return this == TRAINER ? username2 : username1;
        } else {
            return this == TRAINER ? username1 : username2;
        }
    }

}
