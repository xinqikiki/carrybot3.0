package net.chezxinqi.carrybot3;

import android.content.Context;
import android.content.SharedPreferences;

public final class UiPrefs {

    private static final String PREFS = "carrybot_prefs";
    private static final String KEY_LANG = "lang";
    private static final String KEY_TTS = "tts";
    private static final String KEY_CONTRAST = "contrast";

    private UiPrefs() {
    }

    public static String getLang(Context context) {
        return prefs(context).getString(KEY_LANG, UiStrings.LANG_FR);
    }

    public static void setLang(Context context, String lang) {
        prefs(context).edit().putString(KEY_LANG, lang).apply();
    }

    public static boolean isTtsEnabled(Context context) {
        return prefs(context).getBoolean(KEY_TTS, false);
    }

    public static void setTtsEnabled(Context context, boolean enabled) {
        prefs(context).edit().putBoolean(KEY_TTS, enabled).apply();
    }

    public static boolean isContrastEnabled(Context context) {
        return prefs(context).getBoolean(KEY_CONTRAST, false);
    }

    public static void setContrastEnabled(Context context, boolean enabled) {
        prefs(context).edit().putBoolean(KEY_CONTRAST, enabled).apply();
    }

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
}
