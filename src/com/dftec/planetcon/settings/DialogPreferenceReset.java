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
import android.preference.DialogPreference;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import com.dftec.planetcon.R;
import com.dftec.planetcon.activities.SettingsActivity;

public class DialogPreferenceReset extends DialogPreference {
    private final Context mContext;

    public DialogPreferenceReset(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        super.onClick(dialog, which);

        if (which == DialogInterface.BUTTON_POSITIVE) {
            SharedPreferences.Editor prefsEditor = PreferenceManager.getDefaultSharedPreferences(mContext).edit();
            // Reset saved preferences, and load default values from the xml
            prefsEditor.clear();
            PreferenceManager.setDefaultValues(mContext, R.layout.activity_settings, true);
            prefsEditor.commit();
            // Reset player preferences not included in the xml
            ListPlayersPreference.resetPreferences(prefsEditor);

            // Restart the SettingsActivity in order to refresh the UI
            ((SettingsActivity) mContext).restart();
        }
    }
}
