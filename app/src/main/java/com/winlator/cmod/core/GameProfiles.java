package com.winlator.cmod.core;

import android.content.Context;
import android.util.Log;

import com.winlator.cmod.container.Shortcut;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.util.Iterator;
import java.util.Locale;

/**
 * WinLab: ready-made per-game settings (resolution, FPS cap, DX wrapper, Box64 preset).
 * Applied once per shortcut, the first time it shows up in the list.
 * The user can still change everything afterwards in the shortcut settings.
 */
public abstract class GameProfiles {
    private static final String APPLIED_KEY = "winlabProfile";
    private static JSONArray profiles;

    private static JSONArray load(Context context) {
        if (profiles == null) {
            try (InputStream is = context.getAssets().open("game_profiles.json")) {
                byte[] buffer = new byte[is.available()];
                int count = is.read(buffer);
                profiles = new JSONArray(new String(buffer, 0, Math.max(count, 0)));
            }
            catch (Exception e) {
                Log.w("GameProfiles", "Unable to load profiles", e);
                profiles = new JSONArray();
            }
        }
        return profiles;
    }

    private static String execName(Shortcut shortcut) {
        String path = shortcut.path != null ? shortcut.path : "";
        int index = Math.max(path.lastIndexOf('\\'), path.lastIndexOf('/'));
        return path.substring(index + 1).toLowerCase(Locale.ENGLISH);
    }

    public static void applyIfNeeded(Context context, Shortcut shortcut) {
        if (!shortcut.getExtra(APPLIED_KEY).isEmpty()) return;
        String exec = execName(shortcut);
        if (exec.isEmpty()) return;

        JSONArray profiles = load(context);
        for (int i = 0; i < profiles.length(); i++) {
            try {
                JSONObject profile = profiles.getJSONObject(i);
                JSONArray names = profile.getJSONArray("names");
                boolean matches = false;
                for (int j = 0; j < names.length() && !matches; j++)
                    matches = exec.equals(names.getString(j).toLowerCase(Locale.ENGLISH));
                if (!matches) continue;

                JSONObject extras = profile.getJSONObject("extras");
                Iterator<String> keys = extras.keys();
                while (keys.hasNext()) {
                    String key = keys.next();
                    shortcut.putExtra(key, extras.getString(key));
                }
                shortcut.putExtra(APPLIED_KEY, profile.getString("id"));
                shortcut.saveData();
                Log.i("GameProfiles", "Applied profile " + profile.getString("id") + " to " + exec);
                return;
            }
            catch (Exception e) {
                Log.w("GameProfiles", "Bad profile entry", e);
            }
        }
        shortcut.putExtra(APPLIED_KEY, "none");
        shortcut.saveData();
    }
}
