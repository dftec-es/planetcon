/*
 * Copyright 2014 David Fernandez <dftec.es@gmail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.dftec.planetcon.activities;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceGroup;
import com.dftec.planetcon.R;


public class SettingsActivity extends PreferenceActivity implements
        OnSharedPreferenceChangeListener {

    public static final int INTENT_CODE_SETTINGS = 1;
    public static final int INTENT_CODE_HELP = 2;
    public static final String INTENT_NUM_PLAYERS = "num_players";
    // Must match preferences.xml keys
    public static final String KEY_PLAYERS = "players";
    public static final String DEF_PLAYERS = "4";
    public static final String KEY_RADIUS = "radius";
    public static final String DEF_RADIUS = "7";
    public static final String KEY_DENSITY = "density";
    public static final String DEF_DENSITY = "4";
    public static final String KEY_POSITIONS = "positions";
    public static final String DEF_POSITIONS = "0";
    public static final String KEY_RADIUS_CUSTOM = "radius_custom";
    public static final String DEF_RADIUS_CUSTOM = "9";
    public static final String KEY_PLANETS_CUSTOM = "planets_custom";
    public static final String DEF_PLANETS_CUSTOM = "26";

    public static final String KEY_PLAYER0_ID = "player#_id";
    public static final String DEF_PLAYER0_ID = "#";
    public static final String KEY_PLAYER0_NAME = "player#_name";
    public static final String DEF_PLAYER0_NAME = "P#";
    public static final String KEY_PLAYER0_AI = "player#_ai";
    public static final String DEF_PLAYER_AI = "1";
    public static final String KEY_PLAYER0_HOSTILITY = "player#_hostility";
    public static final String DEF_PLAYER_HOSTILITY = "0";
    public static final String KEY_PLAYER0_HEXCOLOR = "player#_hexcolor";
    public static final String DEF_PLAYER_HEXCOLOR = "red";

    public static final String KEY_GLOBAL_AI = "ai_control";
    public static final String DEF_GLOBAL_AI = "1";
    public static final String KEY_GLOBAL_HOSTILITY = "ai_hostility";
    public static final String DEF_GLOBAL_HOSTILITY = "0";

    public static final String KEY_FOW = "fow";
    public static final String DEF_FOW = "1";
    public static final String KEY_DEFENSE = "defense";
    public static final String DEF_DEFENSE = "0";
    public static final String KEY_UPKEEP = "upkeep";
    public static final String DEF_UPKEEP = "0";
    public static final String KEY_PRODUCTION = "production";
    public static final String DEF_PRODUCTION = "0";

    public static final String KEY_AUTOSAVE = "autosave";
    public static final boolean DEF_AUTOSAVE = true;
    public static final String KEY_TIPS = "tips";
    public static final boolean DEF_TIPS = true;
    public static final String KEY_TIPS_NUM = "tips_num";
    public static final int DEF_TIPS_NUM = 0;

    public static final String KEY_SCREEN = "screen";
    public static final boolean DEF_SCREEN = false;
    public static final String KEY_BACKGROUND = "background";
    public static final boolean DEF_BACKGROUND = true;
    public static final String KEY_SCROLL = "scroll";
    public static final boolean DEF_SCROLL = true;
    public static final String KEY_ZOOM = "zoom_int";
    public static final int DEF_ZOOM = 2;
    public static final String KEY_GRID_ALPHA = "grid_alpha_int";
    public static final int DEF_GRID_ALPHA = 50;
    public static final String KEY_TEXT_ALPHA = "text_alpha_int";
    public static final int DEF_TEXT_ALPHA = 50;
    public static final String KEY_TEXT_SIZE = "text_size";
    public static final String DEF_TEXT_SIZE = "0";

    public static final String KEY_PLAYERS_EXTRA = "players_extra";
    public static final boolean DEF_PLAYERS_EXTRA = false;
    public static final String KEY_PLAYERS_LIST = "players_list";
    public static final int DEF_PLAYERS_LIST = 0;
    public static final String KEY_TURNS = "turns";
    public static final String DEF_TURNS = "20";

    public static final int NUM_PLAYERS_DEF = 9;
    public static final int NUM_PLAYERS_PREFS = 36;
    public static final int MAX_PLAYERS = 36;
    public static final int MAX_RADIUS = 36;
    public static final int MAX_DENSITY = 10;
    public static final int MAX_PLANETS = 99;
    public static final int MAX_TURNS = 99;
    public static final int MAX_POSITIONS = 10;
    public static final int ZOOM_MAX = 10;
    public static final int AI_HUMAN = 0;
    public static final int AI_HIDE = 1;
    public static final int AI_SHOW = 2;
    public static final int AI_IDLE = 3;
    public static final int HOSTILITY_MAX = 4;
    public static final int HOSTILITY_RANDOM = -10;
    public static final int HOSTILITY_CUSTOM = 10;
    public static final int DEFENSE_NONE = 0;
    public static final int DEFENSE_PLAYER = 1;
    public static final int DEFENSE_PLANET = 2;
    public static final int FOW_UNKNOWN = 0;
    public static final int FOW_THREATS = 1;
    public static final int FOW_KNOWN = 2;

    /** Preference keys used in previous versions
    * (String: int) "zoom"                  // Replaced by (int) "zoom_int"
    * (String: int) "grid_alpha"            // Replaced by (int) "grid_alpha_int"
    * (String: int) "text_alpha"            // Replaced by (int) "text_alpha_int"
    * (String: hue) "player#_color"         // Replaced by (String: hex format) "player#_hexcolor"
    * (boolean) "custom_ids"                // Renamed to (boolean) "players_extra"
    * (String: int) "players_opponents"     // Renamed to (String: int) "ai_control"
    * (String: int) "players_custom"        // Removed
    * (String: hue) "players_hue";          // Removed
    */


//    @SuppressWarnings("deprecation")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.layout.activity_settings);
        initSummary(getPreferenceScreen());
        initPrefsEnabled(getPreferenceScreen().getSharedPreferences());
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        updatePrefSummary(findPreference(key));
    }

    @SuppressWarnings("deprecation")
    @Override
    protected void onResume() {
        super.onResume();
        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
    }

    @SuppressWarnings("deprecation")
    @Override
    protected void onPause() {
        super.onPause();
        getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
    }

    @SuppressWarnings("deprecation")
    private void initPrefsEnabled (SharedPreferences sharedPreferences) {
        int numPlayers = 0;
        if (getIntent().getExtras() != null) {
            numPlayers = getIntent().getIntExtra(INTENT_NUM_PLAYERS, 0);
        }
        // Disable player options depending on number of players
        if (numPlayers == 0) {
            findPreference(KEY_TURNS).setEnabled(false);
            findPreference(KEY_PLAYERS_LIST).setEnabled(false);
        } else {
            findPreference(KEY_TURNS).setEnabled(true);
            findPreference(KEY_PLAYERS_LIST).setEnabled(true);
            // This preference stores the number of players shown when this preference dialog is opened
            sharedPreferences.edit().putInt(KEY_PLAYERS_LIST, numPlayers).commit();
        }
    }

    private void initSummary(Preference pref) {
        if (pref instanceof PreferenceGroup) {
            PreferenceGroup prefGrp = (PreferenceGroup) pref;
            for (int i = 0; i < prefGrp.getPreferenceCount(); i++) {
                initSummary(prefGrp.getPreference(i));
            }
        } else {
            updatePrefSummary(pref);
        }
    }

    private void updatePrefSummary(Preference pref) {
        if (pref instanceof ListPreference) {
            ListPreference listPref = (ListPreference) pref;
            pref.setSummary(newSummary(listPref.getTitle(), listPref.getSummary(), listPref.getEntry()));
        } else
        if (pref instanceof EditTextPreference) {
            EditTextPreference editTextPref = (EditTextPreference) pref;
            pref.setSummary(newSummary(editTextPref.getTitle(), editTextPref.getSummary(), editTextPref.getText()));
        }
    }

    private CharSequence newSummary(CharSequence title, CharSequence oldSummary, CharSequence newValue) {
        String newSummary = "";

        if (newValue != null) {
            // If there was a summary, add the value at the end
            if (oldSummary != null) {
                newSummary = oldSummary.toString();
                int index = newSummary.indexOf(": ");
                if (index >= 0) {
                    newSummary = newSummary.substring(0, index);
                }
                newSummary = newSummary.concat(": ").concat(newValue.toString());
            }
            // If there was no summary, use the title
            else if (title != null) {
                newSummary = title.toString();
                newSummary = newSummary.concat(": ").concat(newValue.toString());
            }
        }
        return newSummary;
    }

    public void restart() {
        finish();
        // Both enter and exit animations are set to zero, so no transition animation is applied
        overridePendingTransition(0, 0);
        startActivity(getIntent());
        overridePendingTransition(0, 0);
    }

    public static int getNumericPref(SharedPreferences sharedPreferences, String key, String def) {
        String sNum = sharedPreferences.getString(key, def);
        try {
            // value stored in preferences could have wrong format in runtime
            return Integer.parseInt(sNum);
        } catch (NumberFormatException nfe) {
            // default value set in this code, does not change in runtime
            return Integer.parseInt(def);
        }
    }
}
