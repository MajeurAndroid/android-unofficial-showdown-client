package com.majeur.psclient.model;

import java.io.Serializable;

import androidx.annotation.NonNull;

public class Item implements Serializable {

    public String id;
    public String name;

    public String desc;
    public int spriteNum;

    @NonNull
    @Override
    public String toString() {
        return name; // Used for native ArrayAdapters
    }
}
