package com.majeur.psclient.model;

public class User {

    public static char getGroup(String username) {
        char c = username.charAt(0);
        if (!Character.isAlphabetic(c) && !Character.isDigit(c))
            return c;
        else return 0;
    }
}
