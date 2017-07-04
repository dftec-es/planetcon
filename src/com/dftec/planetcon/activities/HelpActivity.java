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
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import com.dftec.planetcon.R;

public class HelpActivity extends Activity {

    private static final int FONT_OFFSET = 4;
    private WebView mHelpView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_help);

        mHelpView = (WebView) findViewById(R.id.help_view);
        mHelpView.loadDataWithBaseURL("x-data://base", getResources().getString(R.string.help_content), "text/html", "utf-8", null);
        mHelpView.setBackgroundColor(Color.BLACK);
        setTextSize();
    }

    private void setTextSize() {
        WebSettings webSettings = mHelpView.getSettings();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        int textSize = SettingsActivity.getNumericPref(prefs, SettingsActivity.KEY_TEXT_SIZE, SettingsActivity.DEF_TEXT_SIZE);

        if (textSize >= 16) {
            webSettings.setDefaultFontSize(16 + FONT_OFFSET);
        } else if (textSize >= 14) {
            webSettings.setDefaultFontSize(14 + FONT_OFFSET);
        } else if (textSize >= 12) {
            webSettings.setDefaultFontSize(12 + FONT_OFFSET);
        } else if (textSize >= 10) {
            webSettings.setDefaultFontSize(10 + FONT_OFFSET);
        } else if (textSize >= 8) {
            webSettings.setDefaultFontSize(8 + FONT_OFFSET);
        } else {
            webSettings.setDefaultFontSize(16);
        }
    }
}
