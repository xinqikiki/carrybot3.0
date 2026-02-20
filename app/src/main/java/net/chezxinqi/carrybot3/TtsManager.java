package net.chezxinqi.carrybot3;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.util.Log;

import java.util.Locale;

public final class TtsManager {

    private static final String TAG = "CarryBotTTS";
    private static TextToSpeech tts;
    private static boolean ready = false;
    private static String pendingText;
    private static boolean retryRequested = false;

    private TtsManager() {
    }

    public static void speak(Context context, String text) {
        if (context == null || text == null || text.trim().isEmpty()) {
            return;
        }
        if (!UiPrefs.isTtsEnabled(context)) {
            return;
        }
        ensureInit(context.getApplicationContext());
        if (!ready) {
            pendingText = text;
            return;
        }
        int result = speakInternal(context.getApplicationContext(), text);
        if (result == TextToSpeech.ERROR && !retryRequested) {
            retryRequested = true;
            pendingText = text;
            resetEngine();
            ensureInit(context.getApplicationContext());
        }
    }

    public static void warmUp(Context context) {
        if (context == null) {
            return;
        }
        ensureInit(context.getApplicationContext());
    }

    public static void applyLanguage(Context context) {
        if (tts == null || context == null) {
            return;
        }
        Locale locale = UiStrings.getLocale(context);
        int result = tts.setLanguage(locale);
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            tts.setLanguage(Locale.ENGLISH);
        }
    }

    private static void ensureInit(Context context) {
        if (tts != null) {
            return;
        }
        tts = new TextToSpeech(context, status -> {
            if (status == TextToSpeech.SUCCESS) {
                ready = true;
                retryRequested = false;
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                    tts.setAudioAttributes(
                            new AudioAttributes.Builder()
                                    .setUsage(AudioAttributes.USAGE_MEDIA)
                                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                                    .build()
                    );
                }
                applyLanguage(context);
                if (pendingText != null) {
                    String toSpeak = pendingText;
                    pendingText = null;
                    speakInternal(context, toSpeak);
                }
            } else {
                Log.e(TAG, "TTS init failed, status=" + status);
                ready = false;
                resetEngine();
            }
        });
    }

    private static void prepareAudio(Context context) {
        try {
            AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
            if (audioManager == null) {
                return;
            }
            audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_UNMUTE, 0);
            int current = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
            int max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
            int target = Math.max(4, max / 2);
            if (current < target) {
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, target, 0);
            }
        } catch (Exception ignored) {
        }
    }

    private static int speakInternal(Context context, String text) {
        prepareAudio(context);
        Bundle params = new Bundle();
        params.putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1.0f);
        params.putInt(TextToSpeech.Engine.KEY_PARAM_STREAM, AudioManager.STREAM_MUSIC);
        return tts.speak(text, TextToSpeech.QUEUE_FLUSH, params, "carrybot_tts");
    }

    private static void resetEngine() {
        if (tts != null) {
            try {
                tts.stop();
                tts.shutdown();
            } catch (Exception ignored) {
            }
        }
        tts = null;
        ready = false;
    }
}
