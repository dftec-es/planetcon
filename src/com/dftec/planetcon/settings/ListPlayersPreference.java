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
package com.dftec.planetcon.settings;

import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Parcelable;
import android.preference.DialogPreference;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ListView;
import android.widget.LinearLayout;
import com.dftec.planetcon.activities.SettingsActivity;
import com.dftec.planetcon.data.PlayerData;
import com.dftec.planetcon.data.PlayerPref;
import java.util.ArrayList;

public class ListPlayersPreference extends DialogPreference {

    private final Context mContext;
    private final SharedPreferences mPrefs;
    // ArrayList to save the current selected preferences for each player (from 0 to NUM_PLAYERS_PREFS-1)
    private ArrayList<PlayerPref> aPlayersPrefs;
    // Adapter to display in the ListView the data from the ArrayList
    private final ListPlayersAdapter mListAdapter;

    public ListPlayersPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        mPrefs = PreferenceManager.getDefaultSharedPreferences(context);

        aPlayersPrefs = new ArrayList<PlayerPref>(SettingsActivity.NUM_PLAYERS_PREFS);
        loadPreferences(mPrefs, aPlayersPrefs);

        mListAdapter = new ListPlayersAdapter(context, aPlayersPrefs);
    }

    @Override
    protected View onCreateDialogView() {
        int numPlayers = mPrefs.getInt(SettingsActivity.KEY_PLAYERS_LIST, SettingsActivity.DEF_PLAYERS_LIST);
        ListView listPlayers = new ListView(mContext);
        listPlayers.setAdapter(mListAdapter);
        mListAdapter.setCount(numPlayers);

        // Needed to show the keyboard when a editText inside the listView is focused
        listPlayers.post(new Runnable() {
            @Override
            public void run() {
                Window window = getDialog().getWindow();
                if (window != null) {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE|WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);
                }
            }
        });

        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
        LinearLayout linearLayout = new LinearLayout(mContext);
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        linearLayout.setGravity(Gravity.CENTER);
        // Needed to avoid autofocus of editText views
        linearLayout.setFocusableInTouchMode(true);
        linearLayout.addView(listPlayers, layoutParams);

        return linearLayout;
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        super.onClick(dialog, which);

        if (which == DialogInterface.BUTTON_POSITIVE) {
            putPreferences(mPrefs.edit(), aPlayersPrefs);
        }
        else if (which == DialogInterface.BUTTON_NEGATIVE) {
            // Needed to revert the changes when the dialog is closed
            // (else the changes are restored by onRestoreInstanceState when the dialog is opened again)
            loadPreferences(mPrefs, aPlayersPrefs);
        }
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        Bundle state = new Bundle();
        state.putParcelableArrayList("players", aPlayersPrefs);
        state.putParcelable("super", super.onSaveInstanceState());
        return state;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        if (state instanceof Bundle) {
            Bundle bundle = (Bundle) state;
            aPlayersPrefs = bundle.getParcelableArrayList("players");
            mListAdapter.setList(aPlayersPrefs);
            super.onRestoreInstanceState(bundle.getParcelable("super"));
        } else {
            super.onRestoreInstanceState(state);
        }
        notifyChanged();
    }

    private static String getPlayerKey(String key, int p) {
        // For player in position 0, it converts the key player# to player1
        int nPreference = p + 1;
        return key.replace("#", String.valueOf(nPreference));
    }

    private static String getDefaultColor(int p) {
        // For player in position 0, it returns the default color of player1
        int nPlayer = p + 1;
        return PlayerData.defaultHexColor(nPlayer);
    }

    public static void resetPreferences(SharedPreferences.Editor prefsEditor) {
        String key, def;

        // Default settings for all players with persistent preferences
        for (int p = 0; p < SettingsActivity.NUM_PLAYERS_PREFS; p++) {
            key = getPlayerKey(SettingsActivity.KEY_PLAYER0_HEXCOLOR, p);
            def = getDefaultColor(p);
            prefsEditor.putString(key, def);
            key = getPlayerKey(SettingsActivity.KEY_PLAYER0_ID, p);
            def = getPlayerKey(SettingsActivity.DEF_PLAYER0_ID, p);
            prefsEditor.putString(key, def);
            // TODO: use default name from string resources
            key = getPlayerKey(SettingsActivity.KEY_PLAYER0_NAME, p);
            def = getPlayerKey(SettingsActivity.DEF_PLAYER0_NAME, p);
            prefsEditor.putString(key, def);
            key = getPlayerKey(SettingsActivity.KEY_PLAYER0_AI, p);
            def = p == 0 ? String.valueOf(SettingsActivity.AI_HUMAN) : SettingsActivity.DEF_PLAYER_AI;
            prefsEditor.putString(key, def);
            key = getPlayerKey(SettingsActivity.KEY_PLAYER0_HOSTILITY, p);
            def = SettingsActivity.DEF_PLAYER_HOSTILITY;
            prefsEditor.putString(key, def);
        }
        prefsEditor.commit();
    }

    public static void loadPreferences(SharedPreferences prefs, ArrayList<PlayerPref> playersPrefs) {
        String key, def;

        playersPrefs.clear();
        // Load the settings from preferences to the array list
        for (int p = 0; p < SettingsActivity.NUM_PLAYERS_PREFS; p++) {
            PlayerPref player = new PlayerPref();
            // personalize default colors
            key = getPlayerKey(SettingsActivity.KEY_PLAYER0_HEXCOLOR, p);
            def = getDefaultColor(p);
            player.sColor = prefs.getString(key, def);
            // personalize default id #
            key = getPlayerKey(SettingsActivity.KEY_PLAYER0_ID, p);
            def = getPlayerKey(SettingsActivity.DEF_PLAYER0_ID, p);
            player.sId = prefs.getString(key, def);
            // personalize default name P#
            key = getPlayerKey(SettingsActivity.KEY_PLAYER0_NAME, p);
            def = getPlayerKey(SettingsActivity.DEF_PLAYER0_NAME, p);
            player.sName = prefs.getString(key, def);
            // load ai control
            key = getPlayerKey(SettingsActivity.KEY_PLAYER0_AI, p);
            if (p == 0) {
                def = String.valueOf(SettingsActivity.AI_HUMAN);
            } else {
                def = SettingsActivity.DEF_PLAYER_AI;
            }
            player.sAI = prefs.getString(key, def);
            // load ai hostility
            key = getPlayerKey(SettingsActivity.KEY_PLAYER0_HOSTILITY, p);
            player.sHostility = prefs.getString(key, SettingsActivity.DEF_PLAYER_HOSTILITY);
            // fill the array of players
            playersPrefs.add(player);
        }

    }

    public static void putPreferences(SharedPreferences.Editor prefsEditor, ArrayList<PlayerPref> playersPrefs) {
        String key;
        // Put the settings from the array list to the preferences editor
        for (int p = 0; (p < playersPrefs.size()) && (p < SettingsActivity.NUM_PLAYERS_PREFS); p++) {
            key = getPlayerKey(SettingsActivity.KEY_PLAYER0_HEXCOLOR, p);
            prefsEditor.putString(key, playersPrefs.get(p).sColor);
            key = getPlayerKey(SettingsActivity.KEY_PLAYER0_ID, p);
            prefsEditor.putString(key, playersPrefs.get(p).sId);
            key = getPlayerKey(SettingsActivity.KEY_PLAYER0_NAME, p);
            prefsEditor.putString(key, playersPrefs.get(p).sName);
            key = getPlayerKey(SettingsActivity.KEY_PLAYER0_AI, p);
            prefsEditor.putString(key, playersPrefs.get(p).sAI);
            key = getPlayerKey(SettingsActivity.KEY_PLAYER0_HOSTILITY, p);
            prefsEditor.putString(key, playersPrefs.get(p).sHostility);
        }
        prefsEditor.commit();
    }

}
