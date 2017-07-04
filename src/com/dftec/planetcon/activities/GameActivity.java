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
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TabHost;
import android.widget.TabHost.OnTabChangeListener;
import android.widget.TabHost.TabSpec;
import android.widget.TabWidget;
import android.widget.TableLayout;
import android.widget.TextView;
import android.widget.Toast;
import com.dftec.planetcon.R;
import com.dftec.planetcon.ai.AIData;
import com.dftec.planetcon.data.FleetData;
import com.dftec.planetcon.data.GalaxyData;
import com.dftec.planetcon.data.PlanetData;
import com.dftec.planetcon.data.PlayerData;
import com.dftec.planetcon.ui.GalaxyView;
import com.dftec.planetcon.ui.GalaxyView.OnEventListenerGalaxyView;
import com.dftec.planetcon.ui.InfoView;
import com.dftec.planetcon.ui.InfoView.IEventListenerInfoView;
import com.dftec.planetcon.ui.TextAdapter;
import com.dftec.planetcon.ui.TextBuilder;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.LineNumberReader;
import java.io.PrintWriter;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;

public class GameActivity extends Activity {

    private static final String HASHED_DEVICE_ID = "BC7670511910";    // Use it for testing
    private static final boolean DEBUG = false;
    private static final String DEBUG_TAG = "Game";
    private static final int SCREEN_FONT_RATIO = 40;
    private static final int MSG_START = 0;
    private static final int MSG_END = 1;
    private static final String SAVE_INFO = "Save_0";
    private static final String SAVE_MENU = "Save_1";
    private static final String SAVE_TURN = "Save_2";
    private static final String SAVE_END = "Save_3";
    public static final String INTENT_ID = "Id";
    public static final String INTENT_FILE = "File";
    public static final int ID_NEW = 0;
    public static final int ID_CONTINUE = 1;
    public static final int ID_LOAD = 2;
    public static final int ID_RESTORE = 3;
    public static final String AUTO_SAVE = "Autosave";
    public static final String MANUAL_SAVE = "Save";
    public static final String FILE_TYPE = ".sav";

    private GalaxyView mViewGalaxy;
    private TabHost mLayoutInfoTab;
    private TabHost mLayoutInfoExpanded;
    private GridView mGridInfoTab;
    private GridView mGridInfoExpanded;
    private ScrollView mScrollInfoExpanded;
    private TableLayout mTableInfoTab;
    private TableLayout mTableInfoExpanded;
    private TextView mTextInfoTurn;
    private TextView mTextInfoPlanet;
    private Button mButtonEndTurn;
    private Button mButtonOpenMenu;
    private Button mButtonShipsSend;
    private SeekBar mSeekBarShips;
    private TextView mTextShipsMax;
    private TextView mTextShipsSent;
    private TextView mTextShipsInfo;
    private LinearLayout mLayoutShips;
    private LinearLayout mLayoutShipsFrame;

    private AsyncTask mTask;
    private AsyncTask mTaskAI;
    private AlertDialog mMenu;
    private AlertDialog mDialog;
    private Drawable mBackgroundTurn;
    private Drawable mBackgroundShips;

    private GalaxyData mGalaxy;
    private TextBuilder mInfoBuilder;
    private TextAdapter mInfoAdapter;

    private Resources mRes;
    private SharedPreferences mPrefs;

    private boolean mAutosave = false;
    private int mTextSize = 0;
    private int mGridAlpha = 0;

    private boolean mUpdatedInfoGame = false;
    private boolean mShowedTurnReport = false;
    private boolean mShowedEndReport = false;
    private boolean mRestoreMenuOpened = false;
    private boolean mExpandedInfoTab = false;
    private int mStateInfoTab = TextAdapter.INFO_STATS;
    private int mRestoreInfoTab = TextAdapter.INFO_NO;


////// Inner static classes //////

    // Handler static to prevent memory leak when activity finished while pending messages in queue.
    private static class MyHandler extends Handler {
        // Instances of static inner classes do not hold an implicit reference to their outer class.
        private final WeakReference<GameActivity> mActivity;

        private MyHandler(GameActivity activity) {
            mActivity = new WeakReference<GameActivity>(activity);
        }
        // Handles messages related to turn sequence (start and end of each player turn)
        @Override
        public void handleMessage(Message msg) {
            GameActivity activity = mActivity.get();
            if (activity != null) {
                switch (msg.what) {
                    case MSG_START:
                        activity.startLocalTurn(msg.arg1);
                        break;
                    case MSG_END:
                        activity.endTurn(msg.arg1);
                        break;
                }
            }
        }
    }

    private final MyHandler mHandler = new MyHandler(this);


////// On Event methods //////

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_game);

        mViewGalaxy = (GalaxyView) findViewById(R.id.galaxy_view);
        mLayoutInfoTab = (TabHost) findViewById(R.id.info_game);
        mLayoutInfoExpanded = (TabHost) findViewById(R.id.info_expanded);
        mGridInfoTab = (GridView) findViewById(R.id.info_game_grid);
        mGridInfoExpanded = (GridView) findViewById(R.id.info_expanded_grid);
        mScrollInfoExpanded = (ScrollView) findViewById(R.id.info_expanded_scroll);
        mTableInfoTab = (TableLayout) findViewById(R.id.info_game_table);
        mTableInfoExpanded = (TableLayout) findViewById(R.id.info_expanded_table);
        mTextInfoTurn = (TextView) findViewById(R.id.info_turn);
        mTextInfoPlanet = (TextView) findViewById(R.id.info_planet);
        mButtonEndTurn = (Button) findViewById(R.id.end_turn);
        mButtonOpenMenu = (Button) findViewById(R.id.open_menu);
        mButtonShipsSend = (Button) findViewById(R.id.ships_send);
        mSeekBarShips = (SeekBar) findViewById(R.id.ships_seekbar);
        mTextShipsMax = (TextView) findViewById(R.id.ships_max);
        mTextShipsSent = (TextView) findViewById(R.id.ships_sent);
        mTextShipsInfo = (TextView) findViewById(R.id.ships_info);
        mLayoutShips = (LinearLayout) findViewById(R.id.ships_layout);
        mLayoutShipsFrame = (LinearLayout) findViewById(R.id.ships_frame);

        mRes = getResources();
        mPrefs = PreferenceManager.getDefaultSharedPreferences(this);

        mInfoBuilder = new TextBuilder(this);
        mInfoAdapter = new TextAdapter(this, mInfoBuilder);

        // Check whether we're recreating a previously destroyed instance
        if (savedInstanceState != null) {
            // Restore values from saved instance state
            mRestoreInfoTab = savedInstanceState.getInt(SAVE_INFO);
            mRestoreMenuOpened = savedInstanceState.getBoolean(SAVE_MENU);
            mShowedTurnReport = savedInstanceState.getBoolean(SAVE_TURN);
            mShowedEndReport = savedInstanceState.getBoolean(SAVE_END);
            startGame(ID_RESTORE, null);
        } else {
            // Initialize members with default values for a new instance
            int id = ID_NEW;
            String file = null;
            if (getIntent().getExtras() != null) {
                id = getIntent().getExtras().getInt(INTENT_ID);
                file = getIntent().getExtras().getString(INTENT_FILE);
            }
            startGame(id, file);
        }

        mBackgroundTurn = getDrawable(this, R.drawable.shape_bg_turn);
        mBackgroundShips = getDrawable(this, R.drawable.shape_bg_frame);
        createTabs(mLayoutInfoTab, R.id.info_game_grid, R.id.info_game_scroll);
        createTabs(mLayoutInfoExpanded, R.id.info_expanded_grid, R.id.info_expanded_scroll);

        mLayoutInfoTab.setOnTabChangedListener(new OnTabChangeListener() {
            @Override
            public void onTabChanged(String tabId) {
                setInfoTab(mLayoutInfoTab.getCurrentTab());
                // Refresh only once when both tabs change
                refreshInfoViews(true);
            }
        });

        mLayoutInfoExpanded.setOnTabChangedListener(new OnTabChangeListener() {
            @Override
            public void onTabChanged(String tabId) {
                setInfoTab(mLayoutInfoExpanded.getCurrentTab());
                // Refresh only once when both tabs change
            }
        });

        mViewGalaxy.setEventListener(new OnEventListenerGalaxyView() {
            @Override
            public void onPlanetFocused() {
                refreshInfoPlanet();
            }
            @Override
            public void onPlanetSelected() {
                // InfoTab changed to stop animations when own planet selected
                if ( mViewGalaxy.getSelectedPlanet() != null ) {
                    if (mExpandedInfoTab) {
                        toggleExpandedLayout();
                    }
                    if (mStateInfoTab != TextAdapter.INFO_FLEETS && mStateInfoTab != TextAdapter.INFO_STATS) {
                        setInfoTab(TextAdapter.INFO_STATS);
                    }
                }
                refreshGalaxyDrawing();
            }
            @Override
            public void onPlanetTargeted() {
                refreshSeekBarShips();
            }
        });

        mSeekBarShips.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromTouch) {
                mTextShipsMax.setText(String.valueOf(seekBar.getMax()));
                mTextShipsSent.setText(String.valueOf(progress));
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        // Always call the superclass so it can save the view hierarchy state
        super.onSaveInstanceState(savedInstanceState);

        // Save the user's current game state
        if (mRestoreInfoTab == TextAdapter.INFO_NO) {
            savedInstanceState.putInt(SAVE_INFO, mStateInfoTab);
        } else {
            // When rotated again before loading of info tabs finishes
            savedInstanceState.putInt(SAVE_INFO, mRestoreInfoTab);
        }
        if (mMenu != null && mMenu.isShowing()) {
            savedInstanceState.putBoolean(SAVE_MENU, true);
        } else {
            // Might be true when rotated again before opening of menu finishes
            savedInstanceState.putBoolean(SAVE_MENU, mRestoreMenuOpened);
        }
        savedInstanceState.putBoolean(SAVE_TURN, mShowedTurnReport);
        savedInstanceState.putBoolean(SAVE_END, mShowedEndReport);
    }

//    protected void onStart() {
//        super.onStart();
//    }

//    protected void onRestart() {
//        super.onRestart();
//    }

    @Override
    protected void onResume() {
        super.onResume();
        applyUiOptions();
        if (mRestoreMenuOpened) {
            // Restoring saved state with menu opened
            mRestoreMenuOpened = false;
            openMenuButton(mButtonOpenMenu);
            refreshInfoViews(true);
        } else {
            startPlayerTurn();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mMenu != null && mMenu.isShowing()) {
            mRestoreMenuOpened = true;
        }
        // Halt AI, and UI tasks
        if (mTaskAI != null) mTaskAI.cancel(true);
        if (mTask != null) mTask.cancel(true);
        // Disable animations
        mViewGalaxy.setDrawing(false, false, false, false, false);
    }

    @Override
    // Always called
    protected void onStop() {
        super.onStop();
        // Autosave file
        saveGame(false, false, AUTO_SAVE + FILE_TYPE);
    }

    @Override
    // Not always called
    protected void onDestroy() {
        super.onDestroy();
        if (mMenu != null) mMenu.dismiss();
        if (mDialog != null) mDialog.dismiss();
        mHandler.removeCallbacksAndMessages(null);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // The preferences returned if the request code is what we had given earlier in startActivityForResult
        if (requestCode == SettingsActivity.INTENT_CODE_SETTINGS) {
            // Apply game options, in case they were changed by settings activity
            applyGameOptions(false);
            if (mRestoreInfoTab == TextAdapter.INFO_NO) {
                // Save current info tab only when coming back to same instance (else already saved and not yet restored)
                mRestoreInfoTab = mStateInfoTab;
            }
            mUpdatedInfoGame = false;
        } else if (requestCode == SettingsActivity.INTENT_CODE_HELP) {
        }
        if (mMenu != null) mMenu.dismiss();
        mRestoreMenuOpened = false;
        // OnResume is executed afterwards
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        // Hardware menu button
        if (keyCode == KeyEvent.KEYCODE_MENU) {
            openMenuButton(mButtonOpenMenu);
            return true;
        } else
        // Hardware back button
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (mViewGalaxy.getSelectedPlanet() != null) {
                mViewGalaxy.clearSelectedPlanet();
                refreshInfoViews(false);
            } else {
                exitGameDialog();
            }
            return true;
        } else {
            return super.onKeyUp(keyCode, event);
        }
    }


////// Tabs methods //////

    private View createSingleTab(int textId) {
        View view = LayoutInflater.from(this).inflate(R.layout.layout_tab, null);
        TextView tv = (TextView) view.findViewById(R.id.tab_text);
        tv.setText(mRes.getString(textId).toUpperCase());
        return view;
    }

    private void createTabs(final TabHost tabHost, int grid, int scroll) {
        tabHost.setup();
        // Set divider only works if called before adding the tabs
        if (android.os.Build.VERSION.SDK_INT >= 11) {
            tabHost.getTabWidget().setShowDividers(TabWidget.SHOW_DIVIDER_MIDDLE);
            tabHost.getTabWidget().setDividerDrawable(R.drawable.tab_divider_dark);
        } else {
            tabHost.getTabWidget().setDividerDrawable(R.drawable.tab_divider);
        }

        for (int index = TextAdapter.INFO_PREV + 1; index < TextAdapter.INFO_NEXT; index++) {
            switch (index) {
                case TextAdapter.INFO_ARRIVALS:
                TabSpec spec = tabHost.newTabSpec("arrivals");
                // Set the content of the tab
                spec.setIndicator(createSingleTab(R.string.info_arrivals));
                // Set the content of the framelayout when this tab is selected
                spec.setContent(grid);
                tabHost.addTab(spec);
                break;

                case TextAdapter.INFO_FLEETS:
                spec = tabHost.newTabSpec("fleets");
                spec.setIndicator(createSingleTab(R.string.info_fleets));
                spec.setContent(grid);
                tabHost.addTab(spec);
                break;

                case TextAdapter.INFO_STATS:
                spec = tabHost.newTabSpec("stats");
                spec.setIndicator(createSingleTab(R.string.info_stats));
                spec.setContent(scroll);
                tabHost.addTab(spec);
                break;

                case TextAdapter.INFO_THREATS:
                spec = tabHost.newTabSpec("threats");
                spec.setIndicator(createSingleTab(R.string.info_threats));
                spec.setContent(grid);
                tabHost.addTab(spec);
                break;
            }
        }
        tabHost.setCurrentTab(mStateInfoTab);

        InfoView tabContent = (InfoView) tabHost.findViewById(android.R.id.tabcontent);
        tabContent.setEventListener(new IEventListenerInfoView() {
            @Override
            public void onSingleTap() {
                setInfoTab(TextAdapter.INFO_NEXT);
            }
            @Override
            public void onLongPress() {
                // InfoTab changed to stop animations on long press
                if (mExpandedInfoTab) {
                    toggleExpandedLayout();
                }
                if (mStateInfoTab != TextAdapter.INFO_FLEETS && mStateInfoTab != TextAdapter.INFO_STATS) {
                    setInfoTab(TextAdapter.INFO_STATS);
                }
            }
            @Override
            public void onFling(){
                toggleExpandedLayout();
            }
        });

    }


////// On click button methods //////

    public void reduceShipsButton(View view) {
        mSeekBarShips.setProgress(mSeekBarShips.getProgress() - 1);
        mSeekBarShips.invalidate();
    }

    public void increaseShipsButton(View view) {
        mSeekBarShips.setProgress(mSeekBarShips.getProgress() + 1);
        mSeekBarShips.invalidate();
    }

    public void sendShipsButton(View view) {
        PlayerData playerStats = mGalaxy.getCurrentPlayerData();
        if ( (mGalaxy.getCurrentTurn() <= mGalaxy.getMaxTurns()) && playerStats.local && mUpdatedInfoGame ) {
            sendShips();
        }
    }

    public void endTurnButton(View view) {
        PlayerData playerStats = mGalaxy.getCurrentPlayerData();
        if ( (mGalaxy.getCurrentTurn() <= mGalaxy.getMaxTurns()) && playerStats.local && mUpdatedInfoGame ) {
            // End this turn, or show the end of game
            if (showEnd(playerStats.index) == false) {
                endTurn(playerStats.index);
            }
        }
    }

    public void openMenuButton(View view) {
        // Halt AI
        if (mTaskAI != null) mTaskAI.cancel(true);
        // Create Menu as custom dialog
        if (mMenu == null) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(mRes.getString(R.string.dialog_options));
            LinearLayout layout = (LinearLayout) LayoutInflater.from(this).inflate(R.layout.layout_menu, null);
            builder.setView(layout);
            builder.setNegativeButton(mRes.getString(R.string.dialog_cancel),
                    new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    dialog.cancel();
                }
            });
            builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialog) {
                    startPlayerTurn();
                }
            });
            mMenu = builder.create();
            mMenu.setCanceledOnTouchOutside(true);
        }
        if (mMenu.isShowing() == false) {
            mMenu.show();
        }
    }

    public void onMenuButtonClicked(View view) {
        int id = view.getId();

        if (id == R.id.menu_save_as) {
            saveAsDialog();
        } else if (id == R.id.menu_save) {
            saveFileDialog();
        } else if (id == R.id.menu_load) {
            loadFileDialog();
        } else if (id == R.id.menu_delete) {
            deleteFileDialog();
        } else if (id == R.id.menu_restart) {
            restartTurnDialog();
        } else if (id == R.id.menu_settings) {
            Intent settingsIntent = new Intent(this, SettingsActivity.class);
            settingsIntent.putExtra(SettingsActivity.INTENT_NUM_PLAYERS, mGalaxy.getNumPlayers());
            startActivityForResult(settingsIntent, SettingsActivity.INTENT_CODE_SETTINGS);
        } else if (id == R.id.menu_help) {
            Intent helpIntent = new Intent(this, HelpActivity.class);
            startActivityForResult(helpIntent, SettingsActivity.INTENT_CODE_HELP);
        } else if (id == R.id.menu_exit) {
            exitGameDialog();
        }
    }

    private String[] getFileList() {
        final File path = this.getExternalFilesDir(null);

        if (path != null && path.exists()) {
            FilenameFilter filter = new FilenameFilter() {
                public boolean accept(File dir, String filename) {
                    return filename.contains(FILE_TYPE);
                }
            };
            // list of available savegames in app folder at external memory
            return path.list(filter);
        } else {
            return new String[0];
        }
    }

    private void loadFileDialog() {
        final String[] fileList = getFileList();

        if (mDialog != null) mDialog.dismiss();
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(mRes.getString(R.string.dialog_load));
        builder.setIcon(getDrawable(this, R.drawable.ic_menu_archive));
        builder.setItems(fileList, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                // Restart the game activity
                Intent gameIntent = new Intent(GameActivity.this, GameActivity.class);
                gameIntent.putExtra(INTENT_ID, ID_LOAD);
                gameIntent.putExtra(INTENT_FILE, fileList[which]);
                finish();
                startActivity(gameIntent);
            }
        });
        builder.setNegativeButton(mRes.getString(R.string.dialog_cancel),
                new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
            }
        });
        mDialog = builder.create();
        mDialog.show();
    }

    private void saveFileDialog() {
        final String[] fileList = getFileList();

        if (mDialog != null) mDialog.dismiss();
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(mRes.getString(R.string.dialog_save));
        builder.setIcon(getDrawable(this, R.drawable.ic_menu_save));
        builder.setItems(fileList, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                overwriteFileDialog(fileList[which]);
            }
        });
        builder.setNegativeButton(mRes.getString(R.string.dialog_cancel),
                new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
            }
        });
        mDialog = builder.create();
        mDialog.show();
    }

    private void saveAsDialog() {
        final EditText input = new EditText(this);
        String path = getExternalFilesDir(null).toString();

        if (mDialog != null) mDialog.dismiss();
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(mRes.getString(R.string.dialog_save_as));
        builder.setIcon(getDrawable(this, R.drawable.ic_menu_edit));
        builder.setMessage(path);
        builder.setView(input);
        input.setText(MANUAL_SAVE);
        builder.setPositiveButton(mRes.getString(R.string.dialog_ok),
                new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                String savename = input.getText().toString().trim() + FILE_TYPE;
                File file = new File(getExternalFilesDir(null), savename);
                if (file.exists()) {
                    overwriteFileDialog(savename);
                } else {
                    saveGame(true, true, savename);
                    if (mMenu != null) mMenu.cancel();
                }
            }
        });
        builder.setNegativeButton(mRes.getString(R.string.dialog_cancel),
                new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
            }
        });
        mDialog = builder.create();
        mDialog.show();
    }

    private void overwriteFileDialog(final String savename) {
        if (mDialog != null) mDialog.dismiss();
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(mRes.getString(R.string.dialog_overwrite));
        builder.setIcon(getDrawable(this, R.drawable.ic_dialog_alert));
        builder.setMessage(mRes.getString(R.string.dialog_overwrite_confirm));
        builder.setCancelable(true);
        builder.setPositiveButton(mRes.getString(R.string.dialog_yes),
                new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                saveGame(true, true, savename);
                if (mMenu != null) mMenu.cancel();
            }
        });
        builder.setNegativeButton(mRes.getString(R.string.dialog_no),
                new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
            }
        });
        mDialog = builder.create();
        mDialog.show();
    }

    private void deleteFileDialog() {
        final String[] fileList = getFileList();

        if (mDialog != null) mDialog.dismiss();
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(mRes.getString(R.string.dialog_delete));
        builder.setIcon(getDrawable(this, R.drawable.ic_menu_delete));
        builder.setItems(fileList, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                confirmDeleteFileDialog(fileList[which]);
            }
        });
        builder.setNegativeButton(mRes.getString(R.string.dialog_cancel),
                new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
            }
        });
        mDialog = builder.create();
        mDialog.show();
    }

    private void confirmDeleteFileDialog (final String savename) {
        final File file = new File(getExternalFilesDir(null), savename);
        final Date date = new Date(file.lastModified());

        if (mDialog != null) mDialog.dismiss();
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(mRes.getString(R.string.dialog_delete_confirm));
        builder.setIcon(getDrawable(this, R.drawable.ic_dialog_alert));
        builder.setMessage(file.toString() + " (" + date.toString() + ")");
        builder.setCancelable(true);
        builder.setPositiveButton(mRes.getString(R.string.dialog_yes),
                new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                if (file != null) file.delete();
            }
        });
        builder.setNegativeButton(mRes.getString(R.string.dialog_no),
                new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
            }
        });
        mDialog = builder.create();
        mDialog.show();
    }

    private void restartTurnDialog() {
        if (mDialog != null) mDialog.dismiss();
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(mRes.getString(R.string.dialog_restart));
        builder.setCancelable(true);
        builder.setPositiveButton(mRes.getString(R.string.dialog_yes),
                new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                restartTurn();
                if (mMenu != null) mMenu.cancel();
            }
        });
        builder.setNegativeButton(mRes.getString(R.string.dialog_no),
                new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
            }
        });
        mDialog = builder.create();
        mDialog.show();
    }

    private void exitGameDialog() {
        if (mDialog != null) mDialog.dismiss();
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(mRes.getString(R.string.dialog_exit));
        builder.setCancelable(true);
        builder.setPositiveButton(mRes.getString(R.string.dialog_yes),
                new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                Intent mainIntent = new Intent(GameActivity.this, MainActivity.class);
                // Exits to main menu
                mainIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                finish();
                startActivity(mainIntent);

            }
        });
        builder.setNegativeButton(mRes.getString(R.string.dialog_no),
                new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
            }
        });
        mDialog = builder.create();
        mDialog.show();
    }


////// Menu methods //////

    private void startGame(int id, String filename) {

        if (id == ID_LOAD) {
            loadGame(true, true, filename);
        } else if (id == ID_CONTINUE || id == ID_RESTORE) {
            loadGame(false, false, AUTO_SAVE);
        } else {    //id == ID_NEW
            newGame();
        }
        if (mGalaxy == null) {
            finish();
        } else {
            mInfoBuilder.setGalaxy(mGalaxy);
            mViewGalaxy.setGalaxy(mGalaxy);
            if (id == ID_LOAD || id == ID_CONTINUE) {
                // Apply game options from loaded game
                applyGameOptions(true);
            } else  if (id == ID_RESTORE) {
                // Apply game options from preferences
                applyGameOptions(false);
            } else {    //id == ID_NEW
                applyGameOptions(false);
                // Important to apply the game options before the calculations of the first turn
                startGameTurn();
            }
            mGridInfoTab.setAdapter(mInfoAdapter);
            mGridInfoExpanded.setAdapter(mInfoAdapter);
        }
    }

    private void newGame() {
        int radius = getNumericPref(SettingsActivity.KEY_RADIUS, SettingsActivity.DEF_RADIUS, 0, SettingsActivity.MAX_RADIUS);
        if (radius == 0) {
            radius = getNumericPref(SettingsActivity.KEY_RADIUS_CUSTOM, SettingsActivity.DEF_RADIUS_CUSTOM, 0, SettingsActivity.MAX_RADIUS);
        }
        int players = getNumericPref(SettingsActivity.KEY_PLAYERS, SettingsActivity.DEF_PLAYERS, 0, SettingsActivity.MAX_PLAYERS);
        int startpos = getNumericPref(SettingsActivity.KEY_POSITIONS, SettingsActivity.DEF_POSITIONS, 0, SettingsActivity.MAX_POSITIONS);
        if (startpos > 0) {
            startpos = Math.round((float)(radius * startpos) / SettingsActivity.MAX_POSITIONS);
        }
        int neutrals = 0;
        int density = getNumericPref(SettingsActivity.KEY_DENSITY, SettingsActivity.DEF_DENSITY, 0, SettingsActivity.MAX_DENSITY);
        if (density == 0) {
            neutrals = getNumericPref(SettingsActivity.KEY_PLANETS_CUSTOM, SettingsActivity.DEF_PLANETS_CUSTOM, 0, SettingsActivity.MAX_PLANETS);
        }
        int[] options = new int[5];
        // Options: turns, fow, defense, upkeep, order
        options[0] = getNumericPref(SettingsActivity.KEY_TURNS, SettingsActivity.DEF_TURNS, 0, SettingsActivity.MAX_TURNS);
        options[1] = getNumericPref(SettingsActivity.KEY_FOW, SettingsActivity.DEF_FOW);
        options[2] = getNumericPref(SettingsActivity.KEY_DEFENSE, SettingsActivity.DEF_DEFENSE);
        options[3] = getNumericPref(SettingsActivity.KEY_UPKEEP, SettingsActivity.DEF_UPKEEP);
        options[4] = getNumericPref(SettingsActivity.KEY_PRODUCTION, SettingsActivity.DEF_PRODUCTION);

        try {
            mGalaxy = new GalaxyData(radius, players, neutrals, startpos, density, options);
        } catch (Exception e) {
            showToast(e.getMessage());
            finish();
            return;
        }
        // Rename non-home planets using alphabet from resources
        mGalaxy.renamePlanets(this);
        mGalaxy.setCurrentPlayer(0);
        mGalaxy.setCurrentTurn(1);
    }

    private void loadGame(final boolean external, final boolean toast, final String filename) {
        File file;
        String loadname;
        String message;

        try {
            if (external) {
                if (filename == null) {
                    loadname = MANUAL_SAVE + FILE_TYPE;
                } else {
                    loadname = filename;
                }
                file = new File(getExternalFilesDir(null), loadname);
            } else {
                loadname = AUTO_SAVE + FILE_TYPE;
                file = new File(getFilesDir(), loadname);
            }

            LineNumberReader fileReader = new LineNumberReader(new FileReader(file));
            PackageInfo packageInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            GalaxyData tempGalaxy = new GalaxyData(packageInfo, fileReader);
            fileReader.close();

            // Replace previous game only if there were no exceptions while reading the file
            mGalaxy = tempGalaxy;
            message = mRes.getString(R.string.msg_loaded);
        } catch (Exception e) {
            message = mRes.getString(R.string.msg_loaded_error) + ": " + e.getMessage();
        }
        if (toast) {
            showToast(message);
        }
    }

    private void saveGame(final boolean external, final boolean toast, final String filename) {
        File file;
        String savename;
        String message;
        try {
            if (external) {
                // save to external storage: /storage/sdcard0/Android/data/package/files
                if (filename == null) {
                    savename = MANUAL_SAVE + FILE_TYPE;
                } else {
                    savename = filename;
                }
                file = new File(getExternalFilesDir(null), savename);
            } else {
                // save to internal memory: /data/data/package/files
                savename = AUTO_SAVE + FILE_TYPE;
                file = new File(getFilesDir(), savename);
            }

            PrintWriter fileWriter = new PrintWriter(new FileWriter(file));
            PackageInfo packageInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            mGalaxy.save(packageInfo, fileWriter);
            fileWriter.close();

            message = mRes.getString(R.string.msg_saved);
        } catch (Exception e) {
            message = mRes.getString(R.string.msg_saved_error) + ": " + e.getMessage();
        }
        if (toast) {
            showToast(message);
        }
    }


////// View methods //////

    private void setInfoTab(int newState) {
        if ( (newState > TextAdapter.INFO_PREV) && (newState < TextAdapter.INFO_NEXT) ) {
            mStateInfoTab = newState;
        }
        // Switch to previous info state
        else if (newState == TextAdapter.INFO_PREV) {
            mStateInfoTab --;
            if ( mStateInfoTab <=  TextAdapter.INFO_PREV) {
                mStateInfoTab = TextAdapter.INFO_NEXT - 1;
            }
        }
        // Switch to next info state
        else if (newState == TextAdapter.INFO_NEXT) {
            mStateInfoTab ++;
            if ( mStateInfoTab >=  TextAdapter.INFO_NEXT) {
                mStateInfoTab = TextAdapter.INFO_PREV + 1;
            }
        }
        mLayoutInfoTab.setCurrentTab(mStateInfoTab);
        mLayoutInfoExpanded.setCurrentTab(mStateInfoTab);
    }

    private void toggleExpandedLayout() {
        if (mExpandedInfoTab) {
            mExpandedInfoTab = false;
            mLayoutInfoTab.setVisibility(View.VISIBLE);
            mLayoutInfoExpanded.setVisibility(View.INVISIBLE);
        } else {
            mExpandedInfoTab = true;
            mLayoutInfoTab.setVisibility(View.INVISIBLE);
            mLayoutInfoExpanded.setVisibility(View.VISIBLE);
            mGridInfoExpanded.setSelection(0);
            mScrollInfoExpanded.scrollTo(0, 0);
        }
        mViewGalaxy.invalidate();
    }

    private void refreshGalaxyDrawing() {
        if (mGalaxy.getCurrentPlayerData().local == false) {
            // No local player: INFO_STATS
            mViewGalaxy.setDrawing(false, false, false, false, false);
        } else {
            switch (mStateInfoTab) {
            case TextAdapter.INFO_STATS:
                // setDrawing (defenses, ships, fleets, arrivals, threats)
                mViewGalaxy.setDrawing(true, true, false, false, false);
                break;
            case TextAdapter.INFO_FLEETS:
                // setDrawing (defenses, ships, fleets, arrivals, threats)
                mViewGalaxy.setDrawing(true, true, true, false, false);
                break;
            case TextAdapter.INFO_ARRIVALS:
                // setDrawing (defenses, ships, fleets, arrivals, threats)
                mViewGalaxy.setDrawing(true, true, false, true, false);
                break;
            case TextAdapter.INFO_THREATS:
                // setDrawing (defenses, ships, fleets, arrivals, threats)
                mViewGalaxy.setDrawing(false, false, false, false, true);
                break;
            }
        }
        mViewGalaxy.invalidate();
    }

    private void updateSeekBarShips(int ships) {
        int shipsMax = ships;
        int shipsProgress = ships;
        FleetData previousFleet = mViewGalaxy.getPreviousFleet();
        // Allow to add ships from previous fleet to the new fleet
        if (previousFleet != null) {
            shipsMax += previousFleet.ships;
            shipsProgress = previousFleet.ships;
        }
        mSeekBarShips.setProgress(0);
        mSeekBarShips.setMax(shipsMax);
        mSeekBarShips.setProgress(shipsProgress);
        mSeekBarShips.invalidate();
        mTextShipsMax.setText(String.valueOf(shipsMax));
    }

    private void refreshSeekBarShips() {
        if ( (mViewGalaxy.getTargetedPlanet() == null) || (mGalaxy.getCurrentPlayerData().local == false) ) {
            mLayoutShips.setVisibility(View.INVISIBLE);
            if (mExpandedInfoTab) {
                mLayoutInfoTab.setVisibility(View.INVISIBLE);
                mLayoutInfoExpanded.setVisibility(View.VISIBLE);
            } else {
                mLayoutInfoTab.setVisibility(View.VISIBLE);
                mLayoutInfoExpanded.setVisibility(View.INVISIBLE);
            }
        } else {
            updateSeekBarShips(mViewGalaxy.getSelectedPlanet().shipsNow);
            refreshInfoShips();
            mLayoutInfoTab.setVisibility(View.INVISIBLE);
            mLayoutInfoExpanded.setVisibility(View.INVISIBLE);
            mLayoutShips.setVisibility(View.VISIBLE);
        }
    }

    private void refreshInfoShips() {
        PlanetData planetFrom = mViewGalaxy.getSelectedPlanet();
        PlanetData planetTo = mViewGalaxy.getTargetedPlanet();
        int turn = mGalaxy.getCurrentTurn() + mGalaxy.getDistance(planetFrom, planetTo);
        CharSequence infoShips = mInfoBuilder.getInfoShips(planetFrom, planetTo, turn);

        mTextShipsInfo.setText(infoShips);
    }

    private void refreshButtons() {
        if ( (mGalaxy.getCurrentTurn() <= mGalaxy.getMaxTurns()) && (mGalaxy.getCurrentPlayerData().local) && mUpdatedInfoGame ) {
            mButtonEndTurn.setEnabled(true);
            mButtonShipsSend.setEnabled(true);
        } else {
            mButtonEndTurn.setEnabled(false);
            mButtonShipsSend.setEnabled(false);
        }
    }

//    @SuppressWarnings("deprecation")
    private void refreshInfoTurn() {
        // do not hide the turn bar color completely (halved effect of mGridAlpha)
        int alphaColor = getAlphaColor((255 + mGridAlpha)/2, mGalaxy.getCurrentPlayerData().color);
        CharSequence infoTurn = mInfoBuilder.getInfoTurn();

        mBackgroundTurn.setColorFilter(alphaColor, PorterDuff.Mode.MULTIPLY);
        mTextInfoTurn.setBackgroundDrawable(mBackgroundTurn);
        mTextInfoTurn.setText(infoTurn);
    }

    private void refreshInfoPlanet() {
        CharSequence infoPlanet = mInfoBuilder.getInfoPlanet(mViewGalaxy.getFocusedPlanet());

        mTextInfoPlanet.setText(infoPlanet);
    }

    private void refreshInfoTab() {
        mInfoAdapter.setState(mStateInfoTab);
        mGridInfoExpanded.setVisibility(View.INVISIBLE);
        mGridInfoTab.setVisibility(View.INVISIBLE);

        if (mGalaxy.getCurrentPlayerData().local && mUpdatedInfoGame) {
            // showInfoGame has the worst performance of all UI methods !!

            if (mStateInfoTab == TextAdapter.INFO_STATS) {
                mInfoAdapter.showInfoGame(mTableInfoTab);
                mInfoAdapter.showInfoGame(mTableInfoExpanded);
            } else {
                mInfoAdapter.showInfoGame(mGridInfoTab);
                mInfoAdapter.showInfoGame(mGridInfoExpanded);
            }
            mInfoAdapter.notifyDataSetInvalidated();
            mGridInfoTab.setVisibility(View.VISIBLE);
            mGridInfoExpanded.setVisibility(View.VISIBLE);
        }
    }

    private void refreshInfoViews(boolean info) {
        refreshButtons();
        refreshInfoTurn();
        refreshInfoPlanet();
        refreshSeekBarShips();
        refreshGalaxyDrawing();
        if (info) refreshInfoTab();
    }


////// Resources methods //////

//    @SuppressWarnings("deprecation")
    private int getColor(Context context, int id) {
        if (android.os.Build.VERSION.SDK_INT >= 23) {
            return context.getColor(id);
        } else {
            return context.getResources().getColor(id);
        }
    }

//    @SuppressWarnings("deprecation")
    private Drawable getDrawable(Context context, int id) {
        if (android.os.Build.VERSION.SDK_INT >= 21) {
            return context.getDrawable(id);
        } else {
            return context.getResources().getDrawable(id);
        }
    }

////// Preferences methods //////

    private String getPlayerKey(String key, int nPreference) {
        // For player in position 1, it converts the key player# to player1
        return key.replace("#", String.valueOf(nPreference));
    }

    private int getNumericPref(String key, String def) {
        return (SettingsActivity.getNumericPref(mPrefs, key, def));
    }

    private int getNumericPref(String key, String def, int min, int max) {
        int num = SettingsActivity.getNumericPref(mPrefs, key, def);
        return Math.min(max, Math.max(min, num));
    }

    private int getIntPref(String key, int def, int min, int max) {
        int num = mPrefs.getInt(key, def);
        return Math.min(max, Math.max(min, num));
    }

    private int getAlphaColor(int alpha, int color) {
        return Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color));
    }

    private void setTextOptions(TextView view) {
        view.setTypeface(Typeface.MONOSPACE);
        view.setTextColor(Color.WHITE);
        view.setTextSize(mTextSize);
    }

//    @SuppressWarnings("deprecation")
    private void applyUiOptions() {
        // alpha 0 = settings 255 (transparent), alpha 255 = settings 0 (solid).
        int textAlpha = 255 - getIntPref(SettingsActivity.KEY_TEXT_ALPHA, SettingsActivity.DEF_TEXT_ALPHA, 0, 255);
        mGridAlpha = 255 - getIntPref(SettingsActivity.KEY_GRID_ALPHA, SettingsActivity.DEF_GRID_ALPHA, 0, 255);
        int alphaColor;
        int screenHeight = mRes.getDisplayMetrics().heightPixels;
        int screenWidth = mRes.getDisplayMetrics().widthPixels;
        float screenDensity = mRes.getDisplayMetrics().scaledDensity;

        if (mPrefs.getBoolean(SettingsActivity.KEY_BACKGROUND, SettingsActivity.DEF_BACKGROUND)) {
            getWindow().setBackgroundDrawable(getDrawable(this, R.drawable.bm_bg_stars));
        }

        if (mPrefs.getBoolean(SettingsActivity.KEY_SCREEN, SettingsActivity.DEF_SCREEN)) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        } else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }

        mTextSize = getNumericPref(SettingsActivity.KEY_TEXT_SIZE, SettingsActivity.DEF_TEXT_SIZE, 0, 16);
        // Automatic text size
        if (mTextSize == 0) {
            // Text size proportional to Screen size, adjusted by settings
            mTextSize = (int)(Math.min(screenHeight, screenWidth) / (screenDensity * SCREEN_FONT_RATIO));
        }
        // Landscape screen
        if (screenWidth > screenHeight) {
            // Desired width of infoTurn set by the hint text from xml.
            // Max desired width of infoTurn set to (screen width - screen height).
            mTextInfoTurn.setMaxWidth(screenWidth - screenHeight);
            // Final size of info layout will be Max(minEms of infoPlanet, desired width of infoTurn)
        }

        alphaColor = getAlphaColor(mGridAlpha, getColor(this, R.color.info));
        mLayoutInfoTab.setBackgroundColor(alphaColor);
        mLayoutInfoExpanded.setBackgroundColor(alphaColor);
        alphaColor = getAlphaColor(mGridAlpha, Color.BLACK);
        mTextInfoPlanet.setBackgroundColor(alphaColor);
        mBackgroundShips.setColorFilter(alphaColor, PorterDuff.Mode.SRC_OVER);
        mLayoutShipsFrame.setBackgroundDrawable(mBackgroundShips);

        setTextOptions(mTextInfoTurn);
        setTextOptions(mTextInfoPlanet);
        setTextOptions(mTextShipsInfo);

        mTextShipsMax.setTextSize(mTextSize);
        mTextShipsSent.setTextSize(mTextSize);
        mButtonShipsSend.setTextSize(mTextSize);
        mButtonEndTurn.setTextSize(mTextSize);
        mButtonOpenMenu.setTextSize(mTextSize);
        mInfoAdapter.setTextSize(mTextSize);

        TabWidget tabsGame = mLayoutInfoTab.getTabWidget();
        TabWidget tabsExpanded = mLayoutInfoExpanded.getTabWidget();
        // Get the TextView of each tab
        for (int i = 0; i < tabsGame.getChildCount() && i < tabsExpanded.getChildCount(); i++) {
            TextView tab;
            LinearLayout layout = (LinearLayout) tabsGame.getChildTabViewAt(i);
            if (layout != null && layout.getChildCount() > 0) {
                tab = (TextView) layout.getChildAt(0);
                tab.setTextSize(mTextSize);
            }
            layout = (LinearLayout) tabsExpanded.getChildTabViewAt(i);
            if (layout != null && layout.getChildCount() > 0) {
                tab = (TextView) layout.getChildAt(0);
                tab.setTextSize(mTextSize);
            }
        }

        // Text size scaled by TextView values (device density and user preferences)
        mViewGalaxy.setOptions(mPrefs.getBoolean(SettingsActivity.KEY_SCROLL, SettingsActivity.DEF_SCROLL),
                getIntPref(SettingsActivity.KEY_ZOOM, SettingsActivity.DEF_ZOOM, 0, SettingsActivity.ZOOM_MAX),
                mGridAlpha, textAlpha, (int)mTextInfoTurn.getTextSize());

    }

    private void applyGameOptions(boolean commit) {
        String key, def;
        float hue, sat, value;
        int nPlayer, nPreference, num;
        int aiControl, aiHostility;
        boolean prefExists, customIDs;
        PlayerData playerStats;
        SharedPreferences.Editor prefsEditor = mPrefs.edit();

        mAutosave = mPrefs.getBoolean(SettingsActivity.KEY_AUTOSAVE, SettingsActivity.DEF_AUTOSAVE);
        aiControl = getNumericPref(SettingsActivity.KEY_GLOBAL_AI, SettingsActivity.DEF_GLOBAL_AI);
        aiHostility = getNumericPref(SettingsActivity.KEY_GLOBAL_HOSTILITY, SettingsActivity.DEF_GLOBAL_HOSTILITY);
        // If more players than persistent preferences, enable custom ids
        customIDs = mGalaxy.getNumPlayers() > SettingsActivity.NUM_PLAYERS_PREFS;
        if (commit) {
            // save current value to preferences
            prefsEditor.putString(SettingsActivity.KEY_TURNS, String.valueOf(mGalaxy.getMaxTurns()));
        } else {
            // load value from preferences
            mGalaxy.setMaxTurns(getNumericPref(SettingsActivity.KEY_TURNS, SettingsActivity.DEF_TURNS, 0, SettingsActivity.MAX_TURNS));

        }
        for (nPlayer = 1; nPlayer < mGalaxy.getPlayers().size(); nPlayer++) {
            playerStats = mGalaxy.getPlayerData(nPlayer);
            prefExists = false;

            // For each player, search a matching preference (same num)
            for (nPreference = 1; nPreference <= SettingsActivity.NUM_PLAYERS_PREFS; nPreference++) {
                key = getPlayerKey(SettingsActivity.KEY_PLAYER0_ID, nPreference);
                def = getPlayerKey(SettingsActivity.DEF_PLAYER0_ID, nPreference);
                num = getNumericPref(key, def);
                if (customIDs == false) {
                    num = nPreference;
                }

                // If it exists a preference for this player
                if (num == nPlayer) {
                    prefExists = true;

                    key = getPlayerKey(SettingsActivity.KEY_PLAYER0_NAME, nPreference);
                    def = mRes.getString(R.string.player) + String.valueOf(playerStats.index);
                    if (commit) {
                        prefsEditor.putString(key, playerStats.sName);
                    } else {
                        playerStats.sName = mPrefs.getString(key, def);
                    }

                    key = getPlayerKey(SettingsActivity.KEY_PLAYER0_HEXCOLOR, nPreference);
                    def = PlayerData.defaultHexColor(nPlayer);
                    if (commit) {
                        if ((mGalaxy.getNumPlayers() > SettingsActivity.NUM_PLAYERS_DEF) && playerStats.ai) {
                            prefsEditor.putString(key, playerStats.getPlayerAIColor());
                        } else {
                            prefsEditor.putString(key, playerStats.getPlayerColor());
                        }
                    } else {
                        playerStats.setPlayerColor(mPrefs.getString(key, def));
                    }

                    key = getPlayerKey(SettingsActivity.KEY_PLAYER0_AI, nPreference);
                    if (commit) {
                        prefsEditor.putString(key, String.valueOf(playerStats.getPlayerAI()));
                    } else {
                        playerStats.setPlayerAI(getNumericPref(key, SettingsActivity.DEF_PLAYER_AI));
                    }

                    key = getPlayerKey(SettingsActivity.KEY_PLAYER0_HOSTILITY, nPreference);
                    if (commit) {
                        prefsEditor.putString(key, String.valueOf(playerStats.getAIHostility()));
                    } else {
                        playerStats.setAIHostility(getNumericPref(key, SettingsActivity.DEF_PLAYER_HOSTILITY), null);
                    }
                }
            }

            // If it doesn't exist preference for this player
            if (prefExists == false) {
                playerStats.sName = mRes.getString(R.string.player) + String.valueOf(playerStats.index);
                playerStats.setPlayerAI(aiControl);
                if ( (aiHostility >= -SettingsActivity.HOSTILITY_MAX) && (aiHostility <= SettingsActivity.HOSTILITY_MAX) ) {
                    // Use global AI level (overwrites initial random values)
                    playerStats.setAIHostility(aiHostility, null);
                } else { // HOSTILITY_RANDOM or HOSTILITY_CUSTOM
                    // Use initial random values (until global value overwrites them)
                }
                // Generic color that alternates red, green, blue variations, with pseudo-uniform hue distribution
                hue = ( (nPlayer - 1) * (120f + 120f / mGalaxy.getNumPlayers()) ) % 360;
                // The more hostility, the more the color saturation
                sat = (float)(playerStats.aiHostility + SettingsActivity.HOSTILITY_MAX) / (2 * SettingsActivity.HOSTILITY_MAX);
                // The more the players, the lower the color value
                value = 1 - (float)(nPlayer) / (mGalaxy.getNumPlayers());
                playerStats.setPlayerHSV(hue, (1 + sat) / 2, (1 + value) / 2);
            }
            // If there are extra players, modify the final colors of AIs (ignore the value of color preference)
            else if ((mGalaxy.getNumPlayers() > SettingsActivity.NUM_PLAYERS_DEF) && playerStats.ai) {
                value = 1 - (float)(nPlayer) / (mGalaxy.getNumPlayers());
                playerStats.setPlayerHSV(-1, -1, (1 + value) / 2);
            }

            if (commit) {
                prefsEditor.commit();
            }
        }
    }


////// Player Turn methods //////

    private void sendShips() {
        PlayerData playerLocal = mGalaxy.getCurrentPlayerData();
        PlanetData planetFrom = mViewGalaxy.getSelectedPlanet();
        PlanetData planetTo = mViewGalaxy.getTargetedPlanet();
        int sentShips = mSeekBarShips.getProgress();
        FleetData previousFleet = mViewGalaxy.getPreviousFleet();

        if ( (planetFrom != null) && (planetTo != null) ) {
            // If a fleet was sent in same turn from same origin to same destination, remove it
            // (even if the new fleet has no ships)

            if (previousFleet != null) {
                planetFrom.shipsNow += previousFleet.ships;
                mGalaxy.getFleets().remove(previousFleet);
            }
            mGalaxy.createFleet(planetFrom, planetTo, sentShips);
        }
        playerLocal.ended = true;
        mViewGalaxy.clearTargetedPlanet();
        // Update (defenses, ships, fleets, arrivals)
        mInfoBuilder.updateInfoGame(false, false, true, false);

        refreshInfoViews(true);
    }

    private void restartTurn() {
        PlayerData playerLocal = mGalaxy.getCurrentPlayerData();
        if (playerLocal.local) {
            Iterator<FleetData> itr;
            FleetData fleet;

            // Remove fleets sent this turn by this player
            itr = mGalaxy.getFleets().iterator();
            while(itr.hasNext()) {
                fleet = itr.next();
                if  ( (fleet.turn == mGalaxy.getCurrentTurn()) && (fleet.player == playerLocal.index)) {
                    mGalaxy.getPlanetData(fleet.from).shipsNow += fleet.ships;
                    itr.remove();
                }
            }
            playerLocal.ended = false;
            mShowedEndReport = false;
            mShowedTurnReport = false;
            mUpdatedInfoGame = false;
            refreshButtons();
        }
    }

    private void endTurn(int player) {
        int nextPlayer;
        int currentPlayer = mGalaxy.getCurrentPlayer();
        // Return in case the end message arrived late or duplicated
        if (currentPlayer != player) return;

        mShowedEndReport = false;
        mShowedTurnReport = false;
        mUpdatedInfoGame = false;
        refreshButtons();
        mViewGalaxy.clearSelectedPlanet();
        setInfoTab(TextAdapter.INFO_STATS);

        // Advance to Next player
        nextPlayer = currentPlayer + 1;
        if (nextPlayer < mGalaxy.getPlayers().size()) {
            mGalaxy.setCurrentPlayer(nextPlayer);
            startPlayerTurn();
        }
        // If this was the Last player: start next turn
        else if (mGalaxy.getCurrentTurn() < mGalaxy.getMaxTurns()) {
            mGalaxy.setCurrentPlayer(0);
            // Autosave to SD at end of turn
            if (mAutosave) saveGame(true, false, AUTO_SAVE + FILE_TYPE);
            // Start of next turn and resolve fleet battles
            mGalaxy.setCurrentTurn(mGalaxy.getCurrentTurn() + 1);
            startGameTurn();
            startPlayerTurn();
        }
    }

    private void startPlayerTurn() {
        PlayerData playerStats = mGalaxy.getCurrentPlayerData();

        refreshInfoViews(true);
        // Neutral or Eliminated
        if ( (playerStats.index == 0) || (playerStats.eliminated) ) {
            // End turn
            Message msg = Message.obtain(mHandler, MSG_END, playerStats.index, 0);
            msg.sendToTarget();
        } else {
            // AI or Observer
            if (playerStats.ai) {
                if (playerStats.ended) {
                    continueTurn(playerStats);
                } else {
                    startAiTurn(playerStats);
                }
            }
            // Human or Idle
            else {
                continueTurn(playerStats);
            }
        }
    }

    private void continueTurn(PlayerData playerStats) {
        Message msg;
        if (playerStats.local) {
            // Start local turn
            msg = Message.obtain(mHandler, MSG_START, playerStats.index, 0);
            msg.sendToTarget();
        } else {
            // End turn
            msg = Message.obtain(mHandler, MSG_END, playerStats.index, 0);
            msg.sendToTarget();
        }
    }

    private void startAiTurn(final PlayerData playerAI) {
        final int currentPlayer = playerAI.index;
        final int currentTurn = mGalaxy.getCurrentTurn();

        if (mTaskAI != null) mTaskAI.cancel(true);
        // Each AI starts a new task, but they are executed sequentially
        mTaskAI = new AsyncTask< Integer, Integer, ArrayList<FleetData> >() {
            @Override
            protected void onPreExecute() {
                mButtonEndTurn.setText(mRes.getString(R.string.end_turn_AI));
                mButtonEndTurn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_menu_recent_history, 0, 0, 0);
                mUpdatedInfoGame = false;
                // Disable buttons (when mUpdatedInfoGame is false)
                refreshButtons();
            }

            @Override
            protected ArrayList<FleetData> doInBackground(Integer... params) {
                AIData ai = new AIData(mGalaxy, currentPlayer, currentTurn);
                return ai.processAiData();
            }

            @Override
            protected void onProgressUpdate(Integer... progress) {
            }

            @Override
            protected void onPostExecute(ArrayList<FleetData> result) {
                mButtonEndTurn.setText(mRes.getString(R.string.end_turn));
                mButtonEndTurn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_menu_mark, 0, 0, 0);
                if (playerAI.ended == false) {
                    playerAI.ended = true;
                    // Update the state of the galaxy once AI task finishes
                    for (FleetData fleetAI : result ) {
                        PlanetData planetFrom = mGalaxy.getPlanetData(fleetAI.from);
                        PlanetData planetTo = mGalaxy.getPlanetData(fleetAI.to);
                        mGalaxy.createFleet(planetFrom, planetTo, fleetAI.ships);
                    }
                    continueTurn(playerAI);
                }
            }

            @Override
            protected void onCancelled() {
                mButtonEndTurn.setText(mRes.getString(R.string.end_turn));
                mButtonEndTurn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_menu_mark, 0, 0, 0);
            }
        }.execute();
    }

    private void startLocalTurn (int player) {
        final PlayerData playerLocal = mGalaxy.getCurrentPlayerData();
        // Return in case the start message arrived late (different player)
        if (playerLocal.index != player) return;

        if (mUpdatedInfoGame == false) {
            if (mTask != null) mTask.cancel(true);
            mTask = new AsyncTask< Void, Void, Void >() {
                @Override
                protected void onPreExecute() {
                    mDialog = ProgressDialog.show(GameActivity.this, null, mRes.getString(R.string.dialog_starting), true, true);
                }

                @Override
                protected Void doInBackground(Void... params) {
                    // Update (defenses, ships, fleets, arrivals)
                    mInfoBuilder.updateInfoGame(true, true, true, true);
                    return null;
                }

                @Override
                protected void onPostExecute(Void result) {
                    String title = mRes.getString(R.string.dialog_turn) + ": " + coloredMessage(playerLocal.colorText, playerLocal.sName);
                    // Autosave file
                    saveGame(false, false, AUTO_SAVE + FILE_TYPE);
                    // Set flags before refreshViews
                    mUpdatedInfoGame = true;
//                    refreshButtons();
                    if (mRestoreInfoTab != TextAdapter.INFO_NO) {
                        // Restoring saved state of info tab
                        if (mStateInfoTab != mRestoreInfoTab) {
                            setInfoTab(mRestoreInfoTab);
                        } else {
                            refreshInfoViews(true);
                        }
                        mRestoreInfoTab = TextAdapter.INFO_NO;
                    } else {
                        if (mStateInfoTab != TextAdapter.INFO_ARRIVALS) {
                            setInfoTab(TextAdapter.INFO_ARRIVALS);
                        } else {
                            refreshInfoViews(true);
                        }
                    }
                    if (mDialog != null) mDialog.dismiss();
                    if (mShowedTurnReport == false) {
                        // Restoring saved state with turn report not showed
                        showTurnDialog(title);
                    }
                }
            }.execute();
        }
    }


////// Game turn methods //////

    private void startGameTurn() {
        // Reset Player Stats
        resetPlayerStats();
        // Arrival of Fleets
        processFleets();
        // Resolve Battles
        processBattles();
        // Ship production
        processProduction();
        // Update Player Stats
        processPlayers();
    }

    private void resetPlayerStats() {
        for (PlayerData playerStats : mGalaxy.getPlayers()) {
            // A player without planets nor fleets is eliminated at start of turn (once notified to local players)
            if ( (playerStats.planetsNum == 0) && (playerStats.fleetsNow == 0) ) {
                playerStats.eliminated = true;
            }
            playerStats.ended = false;
            playerStats.planetsNum = 0;
            playerStats.planetsProd = 0;
            playerStats.shipsPlanetPrev = playerStats.shipsPlanetNow;
            playerStats.shipsPlanetNow = 0;
            playerStats.shipsFleetPrev = playerStats.shipsFleetNow;
            playerStats.shipsFleetNow = 0;
            playerStats.fleetsPrev = playerStats.fleetsNow;
            playerStats.fleetsNow = 0;
            playerStats.defenseTotal = 0;
            playerStats.defenseCount = 0;
            playerStats.upkeepTotal = 0;
            playerStats.upkeepCount = 0;
            playerStats.upkeepNextTotal = 0;
            playerStats.upkeepNextCount = 0;
        }
    }

    private void processFleets() {
        Iterator<FleetData> itr;
        FleetData fleet;
        PlayerData playerStats;
        PlanetData planetTo;
        boolean threatDuplicated;
        boolean waveDuplicated;

        // Calculate threats and waves before fleets are removed
        for (FleetData fleet1 : mGalaxy.getFleets()) {
            fleet1.threat = 0;
            fleet1.wave = 0;
            threatDuplicated = false;
            waveDuplicated = false;
            for (FleetData fleet2 : mGalaxy.getFleets()) {
                // Threats: sum all ships sent in same turn from same planet
                if ( (fleet2.turn == fleet1.turn) && (fleet2.from == fleet1.from)
                        && (fleet1.at != mGalaxy.getCurrentTurn()) && (fleet2.at != mGalaxy.getCurrentTurn()) ) {
                        // Ignore fleets that will arrive this turn
                    fleet1.threat += fleet2.ships;
                    // If this threat was already calculated for another fleet
                    if ( (fleet1.equals(fleet2) == false) && (fleet2.threat != 0) ) {
                        threatDuplicated = true;
                    }
                }
                // Waves: sum all ships from same player arriving to same planet at same turn
                if ( (fleet2.at == fleet1.at) && (fleet2.to == fleet1.to) && (fleet2.player == fleet1.player)) {
                    fleet1.wave += fleet2.ships;
                    // If this wave was already calculated for another fleet
                    if ( (fleet1.equals(fleet2) == false) && (fleet2.wave != 0) ) {
                        waveDuplicated = true;
                    }
                }
            }
            if (threatDuplicated) fleet1.threat = 0;
            if (waveDuplicated) fleet1.wave = 0;
        }

        // Removes the arrivals of this turn from the Sorted list (aFleets),
        // and moves them to an ArrayList (aArrivals) to be shuffled
        mGalaxy.getArrivals().clear();
        itr = mGalaxy.getFleets().iterator();
        while(itr.hasNext()) {
            fleet = itr.next();

            if (fleet.at == mGalaxy.getCurrentTurn()) {
                mGalaxy.getArrivals().add(fleet);
                itr.remove();
            } else {
                // Player stats: fleets
                playerStats = mGalaxy.getPlayerData(fleet.player);
                playerStats.fleetsNow ++;
                playerStats.shipsFleetNow += fleet.ships;
            }
        }
        //Collections.shuffle(mGalaxy.getArrivals(), mRandom);

        // Process Arrivals
        for (FleetData arrival : mGalaxy.getArrivals() ) {
            planetTo = mGalaxy.getPlanetData(arrival.to);
            // Sum incoming ships from same owner
            if (planetTo.player == arrival.player) {
                planetTo.supportingShips += arrival.ships;
            } else {
                // The largest enemy wave performs the attack (the nearest, if equal ships)
                if (arrival.wave > planetTo.attackingShips) {
                    planetTo.opposingShips += planetTo.attackingShips;
                    planetTo.attackingShips = arrival.wave;
                    planetTo.attackingPlayer = arrival.player;
                }
                // Sum rest of enemy waves
                else if (arrival.wave > 0) {
                    planetTo.opposingShips += arrival.wave;
                }

                // Historical stats: ships arrived
                playerStats = mGalaxy.getPlayerData(arrival.player);
                playerStats.shipsArrived += arrival.ships;
            }
        }
    }

    private void processBattles() {
        PlayerData playerStats;
        double defending, attacking;

        for (PlanetData planet : mGalaxy.getPlanets()) if (planet.index != 0) {
            // Planet stats: owner and ships in previous turn (stored before battles)
            planet.playerPrev = planet.player;
            if (planet.player != 0) {
                planet.shipsPrev = planet.shipsNow;
            }

            // Reinforcements
            if (planet.supportingShips > 0 ) {
                planet.shipsNow += planet.supportingShips;
                planet.supportingShips = 0;
            }
            // Battles
            if (planet.attackingShips > 0 ) {
                // Take into account Planet Defense multiplier
                defending = mGalaxy.multiplyDefense(planet.shipsNow, planet);
                attacking = mGalaxy.divideDefense(planet.attackingShips, planet);
                // rounded randomly: 1 attacking ship has 50% chance to kill 1 ship with defense 2.0
                if (planet.attackingShips > defending) {
                    // Attack: conquest
                    planet.shipsNow = planet.attackingShips - mGalaxy.roundRandom(defending);
                    planet.player = planet.attackingPlayer;
                } else {
                    // Attack: defeat
                    planet.shipsNow -= mGalaxy.roundRandom(attacking);
                }
                planet.attackingPlayer = 0;
                planet.attackingShips = 0;
            }
            // The other waves reduce ships of the winner, but do not conquer
            if (planet.opposingShips > 0 ) {
                attacking = mGalaxy.divideDefense(planet.opposingShips, planet);
                if (planet.player != 0) {
                    // Neutral ships not reduced by secondary attacks
                    planet.shipsNow = Math.max(0, planet.shipsNow - mGalaxy.roundRandom(attacking));
                }
                planet.opposingShips = 0;
            }

            // Player stats: planets
            playerStats = mGalaxy.getPlayerData(planet.player);
            playerStats.planetsNum ++;
            playerStats.planetsProd += planet.production;
            playerStats.shipsPlanetNow += planet.shipsNow;
        }
    }

    private void processProduction() {
        PlayerData playerStats;
        double shipsTotal;
        int shipsBuilt;

        for (PlanetData planet : mGalaxy.getPlanets()) if (planet.index != 0) {
            // Reset planet Defense when defense option disabled
            if (mGalaxy.getRuleDefense() == SettingsActivity.DEFENSE_NONE) {
                planet.defense = PlanetData.MIN_DEF;
            }

            if (planet.player != 0) {
                playerStats = mGalaxy.getPlayerData(planet.player);

                if (mGalaxy.getRuleUpkeep() > 0) {
                    // Calculate player total Upkeep (before attacks): total ships / upkeep cost (round down)
                    if (playerStats.upkeepTotal == 0) {
                        shipsTotal = (playerStats.shipsPlanetPrev + playerStats.shipsFleetPrev);
                        playerStats.upkeepTotal = (int)Math.ceil( shipsTotal / mGalaxy.getRuleUpkeep() );
                    }
                    // Calculate planet Upkeep (before attacks): proportionial to production
                    if (playerStats.planetsProd > 0) {
                        planet.upkeep = (int)((double)(planet.production * playerStats.upkeepTotal) / playerStats.planetsProd);
                        planet.upkeep = Math.min(planet.production, planet.upkeep);
                        playerStats.upkeepCount += planet.upkeep;   // counter to avoid rounding errors
                    }
                }

                if (mGalaxy.getRuleDefense() == SettingsActivity.DEFENSE_PLAYER) {
                    // Calculate player total Defense, when defense linked to player
                    if (playerStats.defenseTotal == 0) {
                        playerStats.defenseTotal = PlanetData.MAX_DEF - PlanetData.MIN_DEF;
                    }
                    // Calculate planet Defense, when defense linked to player
                    if (playerStats.planetsNum > 0) {
                        // Defense +10 with 1 planet, +1 with 10 planets (same total sum)
                        planet.defense = PlanetData.MIN_DEF + (playerStats.defenseTotal / playerStats.planetsNum);
                        playerStats.defenseCount += (planet.defense - PlanetData.MIN_DEF);  // counter to avoid rounding errors
                    }
                }
            }
        }
        for (PlanetData planet : mGalaxy.getPlanets()) if (planet.index != 0) {
            if (planet.player != 0) {
                playerStats = mGalaxy.getPlayerData(planet.player);

                // Calculate planet Upkeep (before attacks): rest of upkeep assigned first to central planets
                if ( (playerStats.upkeepCount < playerStats.upkeepTotal) && (planet.upkeep < planet.production) ) {
                    planet.upkeep ++;
                    playerStats.upkeepCount ++;
                }
                // Calculate planet Defense: rest of defense assigned first to central planets
                if ( (playerStats.defenseCount < playerStats.defenseTotal) ) {
                    planet.defense ++;
                    playerStats.defenseCount ++;
                }
                // Add ship production (after attacks), minus upkeep (calculated before attacks)
                shipsBuilt = planet.production - planet.upkeep;
                planet.shipsNow += shipsBuilt;
                planet.shipsPublicPrev = planet.shipsPublicNow;
                planet.shipsPublicNow = planet.shipsNow;
                // Player stats: planets
                playerStats.shipsPlanetNow += shipsBuilt;
                // Historical stats: ships produced
                playerStats.shipsProduced += shipsBuilt;

                // Estimate planet Upkeep for next turn (with rounding error from +0 to +1)
                if ( (mGalaxy.getRuleUpkeep() > 0) && (playerStats.planetsProd > 0) )  {
                    if (playerStats.upkeepNextTotal == 0) {
                        shipsTotal = (playerStats.shipsPlanetNow + playerStats.shipsFleetNow);
                        playerStats.upkeepNextTotal = (int)Math.ceil( shipsTotal / mGalaxy.getRuleUpkeep() );
                    }
                    planet.upkeepNext = (int)((double)(planet.production * playerStats.upkeepNextTotal) / playerStats.planetsProd);
                    planet.upkeepNext = Math.min(planet.production, planet.upkeepNext);
                }
            } else {
                // Max remaining ships in neutral planets: 2*production - ships arrived
                planet.shipsPublicPrev = planet.shipsPublicNow;
                planet.shipsPublicNow = 2 * planet.production - (planet.shipsPrev - planet.shipsNow);
            }
            planet.updateDefense();
        }
    }

    private void processPlayers() {
        int activeHumans = 0;
        int activePlayers = 0;
        long globalProduction = 0;
        long globalShipsPlanet = 0;
        long globalShipsFleet = 0;
        PlayerData winnerPlayer = null;

        for (PlayerData playerStats : mGalaxy.getPlayers()) {
            // Total production of the galaxy
            globalProduction += playerStats.planetsProd;

            // Active players (non neutral)
            if ( (playerStats.index != 0) && ((playerStats.planetsNum > 0) || (playerStats.fleetsNow > 0)) ) {
                // Total ships in the galaxy (non neutral)
                globalShipsPlanet += playerStats.shipsPlanetNow;
                globalShipsFleet += playerStats.shipsFleetNow;

                // Count active players
                activePlayers ++;
                if (playerStats.local) {
                    activeHumans ++;
                }

                if (winnerPlayer == null) {
                    winnerPlayer = playerStats;
                } else if (playerStats.compareTo(winnerPlayer) > 0) {
                    winnerPlayer = playerStats;
                }
            }
        }
        mGalaxy.setActivePlayers(activePlayers);
        mGalaxy.setActiveHumans(activeHumans);
        if (winnerPlayer != null) {
            mGalaxy.setActiveWinner(winnerPlayer.index);
        }
        mGalaxy.setGlobalProduction(globalProduction);
        mGalaxy.setGlobalShipsPlanet(globalShipsPlanet);
        mGalaxy.setGlobalShipsFleet(globalShipsFleet);

    }

////// Dialogs methods //////

    private String coloredMessage(int color, String value) {
        return "<font color='" + color + "'>" + value + "</font>";
    }

    private String winMessage(PlayerData winnerPlayer) {
        int ships = winnerPlayer.shipsPlanetNow + winnerPlayer.shipsFleetNow;
        String title = mRes.getString(R.string.msg_player) + " "
                 + coloredMessage(winnerPlayer.colorText, winnerPlayer.sName) + " "
                 + mRes.getString(R.string.msg_player_winner_1) + " " + winnerPlayer.planetsProd + " "
                 + mRes.getString(R.string.msg_player_winner_2) + " " + ships + " "
                 + mRes.getString(R.string.msg_player_winner_3) + ".";
        return title;
    }

    private void showTurnDialog(final String title) {
        if (mDialog != null) mDialog.dismiss();
        // Continue turn dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(TextBuilder.fromHtml(title));
        String tip = getNextTip(mRes.obtainTypedArray(R.array.help_tips));
        if (tip != null) {
            builder.setMessage(TextBuilder.fromHtml(tip));
            builder.setNeutralButton(mRes.getString(R.string.dialog_tip), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int id) {
                    showTurnDialog(title);
                }
            });
        }
        builder.setPositiveButton(mRes.getString(R.string.dialog_ok), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
            }
        });
        builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                if (showEliminated(1) == false) {
                    mShowedTurnReport = true;
                }
            }
        });
        mDialog = builder.create();
        mDialog.setCanceledOnTouchOutside(true);
        mDialog.show();
    }

    private boolean showEliminated(int nextPlayer) {
        String title;
        PlayerData playerStats;

        // Show eliminated players
        for (int nPlayer = nextPlayer; nPlayer < mGalaxy.getPlayers().size(); nPlayer++) {
            playerStats = mGalaxy.getPlayerData(nPlayer);
            // A player has been eliminated this turn
            if ( (playerStats.eliminated == false) && (playerStats.planetsNum == 0) && (playerStats.fleetsNow == 0) ) {
                if (playerStats.index == mGalaxy.getCurrentPlayer()) {
                    title = mRes.getString(R.string.msg_defeat) + ".";
                    showEliminatedDialog(title, nPlayer);
                } else {
                    title = mRes.getString(R.string.msg_player) + " "
                             + coloredMessage(playerStats.colorText, playerStats.sName) + " "
                             + mRes.getString(R.string.msg_player_out) + ".";
                    showEliminatedDialog(title, nPlayer);
                }
                return true;
            }
        }
        return false;
    }

    private void showEliminatedDialog(String title, final int player) {
        if (mDialog != null) mDialog.dismiss();
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        if (title != null) builder.setTitle(TextBuilder.fromHtml(title));
        builder.setCancelable(true);
        builder.setPositiveButton(mRes.getString(R.string.dialog_continue),
                new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                if (showEliminated(player + 1) == false) {
                    mShowedTurnReport = true;
                }
            }
        });
        builder.setNegativeButton(mRes.getString(R.string.dialog_cancel),
                new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                mShowedTurnReport = true;
            }
        });
        mDialog = builder.create();
        mDialog.setCanceledOnTouchOutside(true);
        mDialog.show();
    }

    private boolean showEnd(int currentPlayer) {
        String title, message;
        PlayerData winnerPlayer = mGalaxy.getActiveWinnerData();

        if (mShowedEndReport) {
            return false;
        }
        // End game: AI wins
        else if (mGalaxy.getActiveHumans() == 0) {
            title = mRes.getString(R.string.msg_game_over);
            message = mRes.getString(R.string.msg_machines) + ". " + winMessage(winnerPlayer);
            showEndDialog(title, message);
            return true;
        }
        // End game: player wins
        else if (mGalaxy.getActivePlayers() == 1) {
            if (winnerPlayer.index == currentPlayer) {
                title = mRes.getString(R.string.msg_victory);
            } else {
                title = mRes.getString(R.string.msg_game_over);
            }
            message = winMessage(winnerPlayer);
            showEndDialog(title, message);
            return true;
        }
        // End game: last turn
        else if (mGalaxy.getCurrentTurn() == mGalaxy.getMaxTurns() ) {
            if (winnerPlayer.index == currentPlayer) {
                title = mRes.getString(R.string.msg_victory);
            } else {
                title = mRes.getString(R.string.msg_game_over);
            }
            message = mRes.getString(R.string.msg_last_turn) + ". " + winMessage(winnerPlayer);
            showEndDialog(title, message);
            return true;
        } else {
            return false;
        }
    }

    private void showEndDialog(String title, String message) {
        if (mDialog != null) mDialog.dismiss();
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(TextBuilder.fromHtml(title));
        builder.setCancelable(true);
        builder.setIcon(getDrawable(this, R.drawable.ic_launcher));
        // Inflate player stats table
        LinearLayout layout = (LinearLayout) LayoutInflater.from(this).inflate(R.layout.layout_end, null);
        TextView text = (TextView) layout.findViewById(R.id.info_end_text);
        setTextOptions(text);
        text.setText(TextBuilder.fromHtml(message));
        TableLayout table = (TableLayout) layout.findViewById(R.id.info_end_table);
        mInfoAdapter.setState(TextAdapter.INFO_STATS);
        mInfoAdapter.showInfoGame(table);
        builder.setView(layout);

        builder.setPositiveButton(mRes.getString(R.string.dialog_continue),
                new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
            }
        });
        builder.setNegativeButton(mRes.getString(R.string.dialog_end),
                new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                finish();
            }
        });
        builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                mShowedEndReport = true;
            }
        });
        mDialog = builder.create();
        mDialog.setCanceledOnTouchOutside(true);
        mDialog.show();
    }

    private String getNextTip(final TypedArray msg) {
        SharedPreferences.Editor prefsEditor = mPrefs.edit();
        boolean showTips = mPrefs.getBoolean(SettingsActivity.KEY_TIPS, SettingsActivity.DEF_TIPS);
        int nextTip = mPrefs.getInt(SettingsActivity.KEY_TIPS_NUM, SettingsActivity.DEF_TIPS_NUM);

        if (msg == null || showTips == false) {
            return null;
        } else {
            if (nextTip < 0 || nextTip >= msg.length()) {
                nextTip = 0;
            }
            // search for next tip in xml file
            String tip = msg.getString(nextTip);
            prefsEditor.putInt(SettingsActivity.KEY_TIPS_NUM, ++nextTip).commit();
            return tip;
        }
    }

    private void showToast(final String msg) {
        Toast.makeText(GameActivity.this, msg, Toast.LENGTH_SHORT).show();
        //toast.setGravity(Gravity.TOP|Gravity.LEFT, 0, 0);
    }
}
