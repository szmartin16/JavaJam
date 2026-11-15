package com.javaJam.service;

import java.util.prefs.Preferences;

public final class Globals {
    private Globals() {}

    public static final String VERSION_NUMBER = "0.3.1";
    public static final int DEFAULT_CUTOFF_FRONT = 0;
    public static final int DEFAULT_CUTOFF_BACK = 10;
    public static final String DEFAULT_QUALITY = "1080p";
    public static final String DEFAULT_USER = "Jhone Doe";




    public static final Preferences PREFS = Preferences.userNodeForPackage(Globals.class);

    public static int getCutoffBack() {
        return PREFS.getInt("CUTOFF_BACK", DEFAULT_CUTOFF_BACK);
    }

    public static int getCutoffFront() {
        return PREFS.getInt("CUTOFF_FRONT", DEFAULT_CUTOFF_FRONT);
    }

    public static String getQuality() {
        return PREFS.get("QUALITY", DEFAULT_QUALITY);
    }


}
