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
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.animation.AnimationUtils;
import com.dftec.planetcon.R;
import java.io.File;
import java.io.FilenameFilter;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        PreferenceManager.setDefaultValues(this, R.layout.activity_settings, false);
        setContentView(R.layout.activity_main);

        Resources res = getResources();
        int screenHeight = res.getDisplayMetrics().heightPixels;
        int screenWidth = res.getDisplayMetrics().widthPixels;
        int headerWidth = Math.min(screenHeight, screenWidth);
        findViewById(R.id.header).getLayoutParams().width = headerWidth;
    }

    @Override
    protected void onResume() {
        super.onResume();
        findViewById(R.id.animated).startAnimation(AnimationUtils.loadAnimation(this, R.anim.slide_in_left));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == SettingsActivity.INTENT_CODE_SETTINGS) {
            // Restart activity to apply possible changes to settings
            Intent intent = getIntent();
            finish();
            startActivity(intent);
        }
    }

    public void newGame(View view) {
        Intent intent = new Intent(this, StartActivity.class);
        findViewById(R.id.animated).startAnimation(AnimationUtils.loadAnimation(this, R.anim.slide_out_left));
        startActivity(intent);
    }

    public void continueGame(View view) {
        Intent gameIntent = new Intent(this, GameActivity.class);
        gameIntent.putExtra(GameActivity.INTENT_ID, GameActivity.ID_CONTINUE);
        startActivity(gameIntent);
    }

    public void loadGame(View view) {
        loadFile();
    }

    public void showSettings(View view) {
        Intent settingsIntent = new Intent(this, SettingsActivity.class);
        settingsIntent.putExtra(SettingsActivity.INTENT_NUM_PLAYERS, 0);
        startActivityForResult(settingsIntent, SettingsActivity.INTENT_CODE_SETTINGS);
    }

    public void showHelp(View view) {
        Intent intent = new Intent(this, HelpActivity.class);
        startActivity(intent);
    }

    public void exitGame(View view) {
        moveTaskToBack(true);
        if (android.os.Build.VERSION.SDK_INT >= 21) {
            finishAndRemoveTask();
        } else {
            finish();
        }
        // Force close app
//        android.os.Process.killProcess(android.os.Process.myPid());
//        System.exit(0);
    }

    private String[] getFileList() {
        final File path = this.getExternalFilesDir(null);

        if (path.exists()) {
            FilenameFilter filter = new FilenameFilter() {
                public boolean accept(File dir, String filename) {
                    return filename.contains(GameActivity.FILE_TYPE);
                }
            };
            // list of available savegames in app folder at external memory
            return path.list(filter);
        } else {
            return new String[0];
        }
    }

    private void loadFile() {
        final String[] fileList = getFileList();
        final Context context = this;

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getResources().getString(R.string.dialog_load));
        builder.setItems(fileList, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                Intent gameIntent = new Intent(context, GameActivity.class);
                gameIntent.putExtra(GameActivity.INTENT_ID, GameActivity.ID_LOAD);
                gameIntent.putExtra(GameActivity.INTENT_FILE, fileList[which]);
                startActivity(gameIntent);
            }
        });
        builder.setNegativeButton(getResources().getString(R.string.dialog_cancel),
                new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
            }
        });
        builder.show();
    }

}
