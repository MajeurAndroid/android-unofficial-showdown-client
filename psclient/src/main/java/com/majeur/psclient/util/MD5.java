package com.majeur.psclient.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class MD5 {

    private static MessageDigest sMessageDigest;
    private static StringBuilder sBuilder;

    static {
        sBuilder = new StringBuilder();
    }

    public static String hash(String in) {
        if (!checkAlgorithm()) return null;
        sMessageDigest.update(in.getBytes());
        byte[] hash = sMessageDigest.digest();
        sBuilder.delete(0, sBuilder.length());
        for (byte b : hash) {
            if ((0xff & b) < 0x10) {
                sBuilder.append("0");
                sBuilder.append(Integer.toHexString((0xFF & b)));
            } else {
                sBuilder.append(Integer.toHexString(0xFF & b));
            }
        }
        return sBuilder.toString();
    }

    private static boolean checkAlgorithm() {
        if (sMessageDigest != null) return true;
        try {
            sMessageDigest = MessageDigest.getInstance("MD5");
            return true;
        } catch (NoSuchAlgorithmException e) {
            return false;
        }
    }

}
