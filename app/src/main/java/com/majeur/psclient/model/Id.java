package com.majeur.psclient.model;

public class Id {
    public static String toId(String string) {
        return string.toLowerCase().replaceAll("[^a-z0-9]", "").trim();
    }

    public static void toId(String[] strings) {
        for (int i = 0; i < strings.length; i++)
            strings[i] = toId(strings[i]);
    }
}

