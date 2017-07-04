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
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import com.dftec.planetcon.R;
import com.dftec.planetcon.activities.SettingsActivity;
import com.dftec.planetcon.data.PlayerData;
import com.dftec.planetcon.data.PlayerPref;
import com.dftec.planetcon.settings.ColorPickerDialog.OnColorSelectedListener;
import java.util.ArrayList;

public class ListPlayersAdapter extends BaseAdapter {

    private final Context mContext;
    private ArrayList<PlayerPref> aPlayersPrefs;
    private int mCount;

    private class ViewHolder{
        public Button colorView;
        public EditText idView;
        public EditText nameView;
        public CatchingSpinner aiView;
        public CatchingSpinner hostilityView;
    }

    public ListPlayersAdapter(Context context, ArrayList<PlayerPref> players) {
        super();
        mContext = context;
        aPlayersPrefs = players;
        mCount = 0;
    }

    public void setList(ArrayList<PlayerPref> players) {
        // Change the list containing the preferences of the players
        aPlayersPrefs = players;
        notifyDataSetChanged();
    }

    public void setCount(int count) {
        // Set number of players to display in the ListView
        if (count < 0) {
            mCount = 0;
        } else if (count > SettingsActivity.MAX_PLAYERS) {
            mCount = SettingsActivity.MAX_PLAYERS;
        } else {
            mCount = count;
        }
        notifyDataSetChanged();
    }

    @Override
    public boolean isEnabled(int position) {
        // Non selectable items
        return false;
    }

    @Override
    public int getCount() {
        // Get number of players actually displayed in the ListView
        return Math.min(mCount, aPlayersPrefs.size());
    }

    @Override
    public Object getItem(int position) {
        return aPlayersPrefs.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final int player = position;
        final int nPlayer = player + 1;
        final ViewHolder holder;
        View view = convertView;

        if (convertView == null) {
            LayoutInflater inflater = LayoutInflater.from(mContext);
            // Add new view to the ListView
            view = inflater.inflate(R.layout.layout_player, null);
            holder = new ViewHolder();
            holder.colorView = (Button) view.findViewById(R.id.picker_players_color);
            holder.idView = (EditText) view.findViewById(R.id.edit_players_id);
            holder.nameView = (EditText) view.findViewById(R.id.edit_players_name);
            holder.aiView = (CatchingSpinner) view.findViewById(R.id.spinner_players_ai);
            holder.hostilityView = (CatchingSpinner) view.findViewById(R.id.spinner_players_hostility);
            view.setTag(holder);
        } else {
            // Recover cached view
            holder=(ViewHolder)view.getTag();
        }

        holder.colorView.setBackgroundColor(aPlayersPrefs.get(player).getPlayerColor());
        if (mCount > SettingsActivity.NUM_PLAYERS_PREFS) {
            // If more players than persistent preferences, enable custom ids
            holder.idView.setEnabled(true);
        } else {
            holder.idView.setEnabled(false);
            aPlayersPrefs.get(player).sId = String.valueOf(nPlayer);
        }
        holder.idView.setText(aPlayersPrefs.get(player).sId);
        holder.nameView.setText(aPlayersPrefs.get(player).sName);
        // The spinner shows the string-array "_entries", but the preferences stores the "_values"
        loadPrefValue(holder.aiView, R.array.pref_players_ai_values, aPlayersPrefs.get(player).sAI);
        loadPrefValue(holder.hostilityView, R.array.pref_players_hostility_values,  aPlayersPrefs.get(player).sHostility);

        // Save selections to array list aPlayersPrefs when changed
        holder.colorView.setOnClickListener(new View.OnClickListener() {
            // Open the color picker dialog when view clicked
            public void onClick(View view) {
                pickColor(view, player);
            }
        });
        holder.idView.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            // Store the value when the EditText view loses the focus
            public void onFocusChange(View view, boolean hasFocus) {
                String sId = ((EditText) view).getText().toString();
                if (isValidId(sId)) {
                    aPlayersPrefs.get(player).sId = sId;
                } else {
                    ((EditText)view).setText(aPlayersPrefs.get(player).sId);
                }
            }
        });
        holder.nameView.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            // Store the value when the EditText view loses the focus
            public void onFocusChange(View view, boolean hasFocus) {
                String sName = ((EditText) view).getText().toString();
                if (sName.equals("")) {
                    sName = mContext.getResources().getString(R.string.player) + String.valueOf(aPlayersPrefs.get(player).sId);
                    ((EditText)view).setText(sName);
                }
                aPlayersPrefs.get(player).sName = sName;
            }
        });
        holder.aiView.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                // Store the value (resource _values) in the same position than the selected entry (resource _entries)
                aPlayersPrefs.get(player).sAI = getResString(R.array.pref_players_ai_values, pos);
                // If human player, hide the AI level
                if (aPlayersPrefs.get(player).sAI.equals("0")) {
                    holder.hostilityView.setVisibility(View.GONE);
                } else {
                    holder.hostilityView.setVisibility(View.VISIBLE);
                }
                holder.hostilityView.invalidate();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        holder.hostilityView.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                // Store the value (resource _values) in the same position than the selected entry (resource _entries)
                aPlayersPrefs.get(player).sHostility = getResString(R.array.pref_players_hostility_values, pos);
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        return view;
    }

    private void loadPrefValue(CatchingSpinner spinner, int id, String item) {
        //Get the position of an item inside the array of values (resource id), and set as selected in the spinner
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(mContext, id, android.R.layout.simple_spinner_item);
        int position = adapter.getPosition(item);
        spinner.setSelection(position);
    }

    private String getResString(int id, int position) {
        // Get String at position, from string-array resource id
        return mContext.getResources().obtainTypedArray(id).getString(position);
    }

    private boolean isValidId(String sId) {
        try {
            int id = Integer.parseInt(sId);
            for (int p = 0; p < SettingsActivity.NUM_PLAYERS_PREFS; p++) {
                if (aPlayersPrefs.get(p).sId.equals(sId)) {
                    // already used id
                    return false;
                }
            }
        } catch (NumberFormatException nfe) {
            // wrong format
            return false;
        }
        return true;
    }

    private void pickColor(View view, int player) {
        final View v = view;
        final int p = player;
        final int defaultColor;
        final float[] hsv = new float[] { 0f, 1f, 1f };

        if (aPlayersPrefs != null) {
            // Default hue for player p, with saturation/value from player 0
            Color.colorToHSV(aPlayersPrefs.get(0).getPlayerColor(), hsv);
            hsv[0] = PlayerData.defaultHue(player);
            defaultColor = Color.HSVToColor(hsv);

            ColorPickerDialog dialog = new ColorPickerDialog(
                mContext,
                // Set default color
                defaultColor,
                // Set previous color
                aPlayersPrefs.get(p).getPlayerColor(),
                new OnColorSelectedListener() {
                    @Override
                    // When the color is changed in the ColorPicker dialog
                    public void onColorSelected(int color) {
                        aPlayersPrefs.get(p).setPlayerColor(color);
                        v.setBackgroundColor(color);
                        v.invalidate();
                    }
                });
            dialog.show();
        }
    }

}
