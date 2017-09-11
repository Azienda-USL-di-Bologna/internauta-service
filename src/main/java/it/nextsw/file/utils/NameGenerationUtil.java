package it.nextsw.file.utils;

import java.util.Random;

/**
 * Created by f.longhitano on 27/06/2017.
 */
public class NameGenerationUtil{

    private static final int DEFAULT_LENGTH=10;
    private static final char[] DEFAULT_CHARACTER="abcdefghijklmnopqrstuvwxyz0123456789_-".toCharArray();

    public static String generateName() {
        return NameGenerationUtil.generateName(DEFAULT_LENGTH, DEFAULT_CHARACTER);
    }

    public static String generateName(int legth, char[] characters) {
        StringBuilder sb = new StringBuilder();
        Random random = new Random();
        for (int i = 0; i < legth; i++) {
            char c = characters[random.nextInt(characters.length)];
            sb.append(c);
        }
        return sb.toString();
    }
}
