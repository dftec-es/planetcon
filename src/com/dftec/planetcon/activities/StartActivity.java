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

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.Toast;
import com.dftec.planetcon.R;
import com.dftec.planetcon.data.PlayerPref;
import com.dftec.planetcon.settings.ListPlayersAdapter;
import com.dftec.planetcon.settings.ListPlayersPreference;
import com.dftec.planetcon.settings.NumberPicker;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

public class StartActivity extends Activity {

    private static final String SAVE_PLAYERS_PREFS = "Save_0";
    private static final String SAVE_RADIUS = "Save_1";
    private static final String SAVE_PLANETS = "Save_2";
    private static final String SAVE_TURNS = "Save_3";
    private static final String SAVE_PLAYERS = "Save_4";
    private Spinner mSpinnerRadius;
    private Spinner mSpinnerDensity;
    private Spinner mSpinnerPositions;
    private Spinner mSpinnerFow;
    private Spinner mSpinnerDefense;
    private Spinner mSpinnerUpkeep;
    private Spinner mSpinnerAI;
    private Spinner mSpinnerHostility;
    private NumberPicker mPickerRadius;
    private NumberPicker mPickerPlanets;
    private NumberPicker mPickerTurns;
    private NumberPicker mPickerPlayers;
    private ListView mListPlayers;
    private ListPlayersAdapter mListAdapter;

    private SharedPreferences mPrefs;
    private Random mRandom;

    private ArrayList<PlayerPref> aPlayersPrefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        setContentView(R.layout.activity_start);

        mRandom = new Random();
        // ArrayList to save the current selected preferences for each player
        aPlayersPrefs = new ArrayList<PlayerPref>(SettingsActivity.NUM_PLAYERS_PREFS);
        // Adapter to display in the ListView the data from the ArrayList
        mListAdapter = new ListPlayersAdapter(this, aPlayersPrefs);
        mListPlayers = (ListView) findViewById(R.id.list_players);

        // Old scrollView + listView replaced by listView + header
        View header = getLayoutInflater().inflate(R.layout.activity_start_header, mListPlayers, false);
        mListPlayers.addHeaderView(header, null, false);
        mListPlayers.setAdapter(mListAdapter);

        mPickerRadius = (NumberPicker) findViewById(R.id.picker_radius);
        mPickerPlanets = (NumberPicker) findViewById(R.id.picker_planets);
        mPickerTurns = (NumberPicker) findViewById(R.id.picker_turns);
        mPickerPlayers = (NumberPicker) findViewById(R.id.picker_players);
        mSpinnerRadius = (Spinner) findViewById(R.id.spinner_radius);
        mSpinnerDensity = (Spinner) findViewById(R.id.spinner_density);
        mSpinnerPositions = (Spinner) findViewById(R.id.spinner_positions);
        mSpinnerFow = (Spinner) findViewById(R.id.spinner_fow);
        mSpinnerDefense = (Spinner) findViewById(R.id.spinner_defense);
        mSpinnerUpkeep = (Spinner) findViewById(R.id.spinner_upkeep);
        mSpinnerAI= (Spinner) findViewById(R.id.spinner_global_ai);
        mSpinnerHostility = (Spinner) findViewById(R.id.spinner_global_hostility);

        mPickerRadius.setRange(0, SettingsActivity.MAX_RADIUS);
        mPickerPlanets.setRange(0, SettingsActivity.MAX_PLANETS);
        mPickerTurns.setRange(0, SettingsActivity.MAX_TURNS);
        if (mPrefs.getBoolean(SettingsActivity.KEY_PLAYERS_EXTRA, SettingsActivity.DEF_PLAYERS_EXTRA)) {
            // player count > NUM_PLAYERS_PREFS enables custom ids
            mPickerPlayers.setRange(0, SettingsActivity.MAX_PLAYERS);
        } else {
            mPickerPlayers.setRange(0, SettingsActivity.NUM_PLAYERS_DEF);
        }

        // When number of players changes
        mPickerPlayers.setOnChangeListener(new NumberPicker.OnChangedListener() {

            public void onChanged(NumberPicker picker, int oldVal, int newVal) {
                // Added players take the preferences from the spinner (ai and hostility)
                int hostility, randomHostility;
                String sHostility = getResString(R.array.pref_global_hostility_values, mSpinnerHostility.getSelectedItemPosition());
                try {
                    hostility = Integer.parseInt(sHostility);
                } catch (NumberFormatException nfe) {
                    hostility = SettingsActivity.HOSTILITY_RANDOM;
                }

                for (int p = mListAdapter.getCount(); p < newVal && p < aPlayersPrefs.size(); p++) {
                    aPlayersPrefs.get(p).sAI = getResString(R.array.pref_global_ai_values, mSpinnerAI.getSelectedItemPosition());
                    if (hostility == SettingsActivity.HOSTILITY_RANDOM) {
                        // generate random hostility
                        randomHostility = 2 * mRandom.nextInt(SettingsActivity.HOSTILITY_MAX + 1) - SettingsActivity.HOSTILITY_MAX;
                        aPlayersPrefs.get(p).sHostility = String.valueOf(randomHostility);
                    } else {
                        // use hostility selected in spinner
                        aPlayersPrefs.get(p).sHostility = sHostility;
                    }
                }
                // number of players displayed in the ListView
                mListAdapter.setCount(newVal);
            }
        });

        // When galaxy radius changes
        mSpinnerRadius.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                // Custom radius
                if (getResString(R.array.pref_radius_values, pos).equals("0")) {
                    mPickerRadius.setVisibility(View.VISIBLE);
                    mPickerRadius.requestFocus();
                } else {
                    mPickerRadius.setVisibility(View.GONE);
                }
                mPickerRadius.invalidate();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        // When galaxy density changes
        mSpinnerDensity.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                // Custom density
                if (getResString(R.array.pref_density_values, pos).equals("0")) {
                    mPickerPlanets.setVisibility(View.VISIBLE);
                    mPickerPlanets.requestFocus();
                } else {
                    mPickerPlanets.setVisibility(View.GONE);
                }
                mPickerPlanets.invalidate();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        if (savedInstanceState != null) {
            // Restore values from saved state
            mPickerRadius.setCurrent(savedInstanceState.getInt(SAVE_RADIUS, 0));
            mPickerPlanets.setCurrent(savedInstanceState.getInt(SAVE_PLANETS, 0));
            mPickerTurns.setCurrent(savedInstanceState.getInt(SAVE_TURNS, 0));
            mPickerPlayers.setCurrent(savedInstanceState.getInt(SAVE_PLAYERS, 0));
            aPlayersPrefs = savedInstanceState.getParcelableArrayList(SAVE_PLAYERS_PREFS);
            mListAdapter.setList(aPlayersPrefs);
        } else {
            loadPreferences(mPrefs, aPlayersPrefs);
        }

    }

    @Override
    protected void onResume() {
        super.onResume();
        findViewById(R.id.animated).startAnimation(AnimationUtils.loadAnimation(this, R.anim.slide_in_right));
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        // Save the user's current game state
        savedInstanceState.putParcelableArrayList(SAVE_PLAYERS_PREFS, aPlayersPrefs);
        savedInstanceState.putInt(SAVE_RADIUS, mPickerRadius.getCurrent());
        savedInstanceState.putInt(SAVE_PLANETS, mPickerPlanets.getCurrent());
        savedInstanceState.putInt(SAVE_TURNS, mPickerTurns.getCurrent());
        savedInstanceState.putInt(SAVE_PLAYERS, mPickerPlayers.getCurrent());
    }

////// On click button methods //////

    public void menuStart(View view) {
        savePreferences(mPrefs, aPlayersPrefs);

        Intent gameIntent = new Intent(this, GameActivity.class);
        gameIntent.putExtra(GameActivity.INTENT_ID, GameActivity.ID_NEW);
        startActivity(gameIntent);
    }

    public void menuBack(View view) {
        findViewById(R.id.animated).startAnimation(AnimationUtils.loadAnimation(this, R.anim.slide_out_right));
        finish();
    }

    public void menuRevert(View view) {
        loadPreferences(mPrefs, aPlayersPrefs);
        mListPlayers.invalidateViews();
    }

    public void reassignHostility(View view) {
        String sHostility = getResString(R.array.pref_global_hostility_values, mSpinnerHostility.getSelectedItemPosition());
        int hostility;
        int numPlayers = mListAdapter.getCount();
        int n;

        if (numPlayers > 0 && numPlayers <= aPlayersPrefs.size()) {
            try {
                hostility = Integer.parseInt(sHostility);
            } catch (NumberFormatException nfe) {
                hostility = SettingsActivity.HOSTILITY_RANDOM;
            }

            if (hostility == SettingsActivity.HOSTILITY_RANDOM) {
                // Shuffle the AI level of current players 
                ArrayList<String> shuffledHostility = new ArrayList<String>(numPlayers);
                for (n = 0 ; n < numPlayers; n++) {
                    shuffledHostility.add(aPlayersPrefs.get(n).sHostility);
                }
                Collections.shuffle(shuffledHostility, mRandom);
                for (n = 0 ; n < numPlayers; n++) {
                    aPlayersPrefs.get(n).sHostility = shuffledHostility.get(n);
                }
            } else {
                // Assign the current AI level to every player
                for (n = 0 ; n < numPlayers; n++) {
                    aPlayersPrefs.get(n).sHostility = sHostility;
                }
            }
        }
        mListPlayers.invalidateViews();
    }

    public void showHint(View view) {
        // Use the content description as help to show when textView clicked
        Toast.makeText(this, view.getContentDescription(), Toast.LENGTH_SHORT).show();
    }

////// Preferences methods //////

    private void loadPreferences(SharedPreferences prefs, ArrayList<PlayerPref> playersPrefs) {
        mPickerRadius.setCurrent(getNumericPref(prefs, SettingsActivity.KEY_RADIUS_CUSTOM, SettingsActivity.DEF_RADIUS_CUSTOM, 0, SettingsActivity.MAX_RADIUS));
        mPickerPlanets.setCurrent(getNumericPref(prefs, SettingsActivity.KEY_PLANETS_CUSTOM, SettingsActivity.DEF_PLANETS_CUSTOM, 0, SettingsActivity.MAX_PLANETS));
        mPickerTurns.setCurrent(getNumericPref(prefs, SettingsActivity.KEY_TURNS, SettingsActivity.DEF_TURNS, 0, SettingsActivity.MAX_TURNS));
        mPickerPlayers.setCurrent(getNumericPref(prefs, SettingsActivity.KEY_PLAYERS, SettingsActivity.DEF_PLAYERS, 0, SettingsActivity.MAX_PLAYERS));
        loadPref(prefs, mSpinnerRadius, R.array.pref_radius_values, SettingsActivity.KEY_RADIUS, SettingsActivity.DEF_RADIUS);
        loadPref(prefs, mSpinnerDensity, R.array.pref_density_values, SettingsActivity.KEY_DENSITY, SettingsActivity.DEF_DENSITY);
        loadPref(prefs, mSpinnerPositions, R.array.pref_positions_values, SettingsActivity.KEY_POSITIONS, SettingsActivity.DEF_POSITIONS);
        loadPref(prefs, mSpinnerFow, R.array.pref_fow_values, SettingsActivity.KEY_FOW, SettingsActivity.DEF_FOW);
        loadPref(prefs, mSpinnerDefense, R.array.pref_defense_values, SettingsActivity.KEY_DEFENSE, SettingsActivity.DEF_DEFENSE);
        loadPref(prefs, mSpinnerUpkeep, R.array.pref_upkeep_values, SettingsActivity.KEY_UPKEEP, SettingsActivity.DEF_UPKEEP);
        loadPref(prefs, mSpinnerAI, R.array.pref_global_ai_values, SettingsActivity.KEY_GLOBAL_AI, SettingsActivity.DEF_GLOBAL_AI);
        loadPref(prefs, mSpinnerHostility, R.array.pref_global_hostility_values, SettingsActivity.KEY_GLOBAL_HOSTILITY, SettingsActivity.DEF_GLOBAL_HOSTILITY);

        // Load settings for players with persistent preferences
        ListPlayersPreference.loadPreferences(prefs, playersPrefs);
    }

    private void savePreferences(SharedPreferences prefs, ArrayList<PlayerPref> playersPrefs) {
        SharedPreferences.Editor prefsEditor = prefs.edit();

        prefsEditor.putString(SettingsActivity.KEY_RADIUS_CUSTOM, String.valueOf(mPickerRadius.getCurrent()));
        prefsEditor.putString(SettingsActivity.KEY_PLANETS_CUSTOM, String.valueOf(mPickerPlanets.getCurrent()));
        prefsEditor.putString(SettingsActivity.KEY_TURNS, String.valueOf(mPickerTurns.getCurrent()));
        prefsEditor.putString(SettingsActivity.KEY_PLAYERS, String.valueOf(mPickerPlayers.getCurrent()));
        putPref(prefsEditor, mSpinnerRadius, R.array.pref_radius_values, SettingsActivity.KEY_RADIUS);
        putPref(prefsEditor, mSpinnerDensity, R.array.pref_density_values, SettingsActivity.KEY_DENSITY);
        putPref(prefsEditor, mSpinnerPositions, R.array.pref_positions_values, SettingsActivity.KEY_POSITIONS);
        putPref(prefsEditor, mSpinnerFow, R.array.pref_fow_values, SettingsActivity.KEY_FOW);
        putPref(prefsEditor, mSpinnerDefense, R.array.pref_defense_values, SettingsActivity.KEY_DEFENSE);
        putPref(prefsEditor, mSpinnerUpkeep, R.array.pref_upkeep_values, SettingsActivity.KEY_UPKEEP);
        putPref(prefsEditor, mSpinnerAI, R.array.pref_global_ai_values, SettingsActivity.KEY_GLOBAL_AI);
        putPref(prefsEditor, mSpinnerHostility, R.array.pref_global_hostility_values, SettingsActivity.KEY_GLOBAL_HOSTILITY);

        prefsEditor.commit();

        // Save settings for players with persistent preferences
        ListPlayersPreference.putPreferences(prefsEditor, playersPrefs);

    }

    private void loadPref(SharedPreferences prefs, Spinner spinner, int id, String key, String def) {
        // temporary adapter created to get the position of an item inside the array of values
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, id, android.R.layout.simple_spinner_item);
        String selectedString = prefs.getString(key, def);
        int position = adapter.getPosition(selectedString);
        // Force the spinner to load the stored preference
        spinner.setSelection(position);
    }

    private void putPref(SharedPreferences.Editor prefsEditor, Spinner spinner, int id, String key) {
        int position = spinner.getSelectedItemPosition();
        String selectedString = getResources().obtainTypedArray(id).getString(position);
        // Save to preferences the value selected in the spinner
        prefsEditor.putString(key, selectedString);
    }

    private String getResString(int id, int position) {
        // Get String at position, from string-array resource id
        return getResources().obtainTypedArray(id).getString(position);
    }

    private int getNumericPref(SharedPreferences prefs, String key, String def, int min, int max) {
        int num = SettingsActivity.getNumericPref(prefs, key, def);
        return Math.min(max, Math.max(min, num));
    }

}
