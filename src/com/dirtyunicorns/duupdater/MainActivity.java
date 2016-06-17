/* 
 * Copyright (C) 2013-2014 Jorrit "Chainfire" Jongma
 * Copyright (C) 2013-2015 The OmniROM Project
 * Copyright (C) 2015-2016 The ExodusAndroid Project
 * Copyright (C) 2016 Simao Gomes Viana
 * Copyright (C) 2016 Neil "regalstreak" Agarwal
 */
/* 
 * This file is part of OpenDelta.
 * 
 * OpenDelta is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * OpenDelta is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with OpenDelta. If not, see <http://www.gnu.org/licenses/>.
 */

package com.dirtyunicorns.duupdater;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.StrictMode;
import android.preference.PreferenceManager;
import android.text.Html;
import android.text.format.DateFormat;
import android.text.format.Formatter;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

//import eu.chainfire.libsuperuser.Shell;

import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.common.api.GoogleApiClient;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends Activity {
    private TextView title = null;
    private TextView sub = null;
    private ProgressBar progress = null;
    private Button checkNow = null;
    private Button flashNow = null;
    private TextView updateVersion = null;
    private TextView extra = null;
    private Button buildNow = null;
    private ImageButton stopNow = null;
    private TextView currentVersion = null;
    private TextView lastChecked = null;
    private TextView downloadSize = null;
    private Config config;
    private boolean mPermOk;
    private boolean rememberDevLogin;
    private boolean isDevMode;

    private MenuItem menuItemDevMode;
    private SharedPreferences mPrefs;

    String READ_URL;

    String httpList;
    String httpPref;

    TextView tv2;

    private static final int PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE = 0;
    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    private GoogleApiClient client;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            getActionBar().setIcon(
                    getPackageManager().getApplicationIcon(
                            "com.android.settings"));
        } catch (NameNotFoundException e) {
            // The standard Settings package is not present, so we can't snatch
            // its icon
            Logger.ex(e);
        }
        getActionBar().setDisplayHomeAsUpEnabled(true);
        getActionBar().setElevation(0);

        UpdateService.start(this);

        setContentView(R.layout.activity_main);

        title = (TextView) findViewById(R.id.text_title);
        sub = (TextView) findViewById(R.id.progress_text);
        progress = (ProgressBar) findViewById(R.id.progress_bar);
        checkNow = (Button) findViewById(R.id.button_check_now);
        flashNow = (Button) findViewById(R.id.button_flash_now);
        updateVersion = (TextView) findViewById(R.id.text_update_version);
        extra = (TextView) findViewById(R.id.text_extra);
        buildNow = (Button) findViewById(R.id.button_build_delta);
        stopNow = (ImageButton) findViewById(R.id.button_stop);
        currentVersion = (TextView) findViewById(R.id.text_current_version);
        lastChecked = (TextView) findViewById(R.id.text_last_checked);
        downloadSize = (TextView) findViewById(R.id.text_download_size);

        tv2 = (TextView) findViewById(R.id.TextView2);

        config = Config.getInstance(this);
        mPermOk = false;
        requestPermissions();

        mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        rememberDevLogin = mPrefs.getBoolean(Config.PREFS_REMEMBER_LOGIN, false);
        isDevMode = mPrefs.getBoolean(Config.PREFS_DEV_MODE, false);
        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);

        menuItemDevMode = menu.findItem(R.id.action_dev_mode);

        if (rememberDevLogin) {
            menuItemDevMode.setTitle(getString(R.string.action_dev_mode_on));
            menuItemDevMode.setChecked(true);
        }

        return true;
    }

    private void showDevLogin() {
        final Dialog loginDialog = new Dialog(this);
        loginDialog.setContentView(R.layout.dialog_login);
        loginDialog.setTitle(getString(R.string.dev_mode_login));

        final SharedPreferences.Editor mEditor = mPrefs.edit();

        final String devModePw = config.getDev_mode_pw();

        Button btnLogin = (Button) loginDialog.findViewById(R.id.btnLogin);
        Button btnCancel = (Button) loginDialog.findViewById(R.id.btnCancel);
        final EditText etPassword = (EditText) loginDialog.findViewById(R.id.txtPassword);
        final CheckBox cbRemember = (CheckBox) loginDialog.findViewById(R.id.remember_dev_login);

        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (etPassword.getText().toString().equals(devModePw)) {
                    mEditor.putBoolean(Config.PREFS_DEV_MODE, true).apply();
                    isDevMode = true;

                    if (cbRemember.isChecked()) {
                        mEditor.putBoolean(Config.PREFS_REMEMBER_LOGIN, true).apply();
                        rememberDevLogin = true;
                    }

                    menuItemDevMode.setChecked(true);
                    menuItemDevMode.setTitle(getString(R.string.action_dev_mode_on));

                    Toast.makeText(MainActivity.this, getString(R.string.action_dev_mode_on), Toast.LENGTH_SHORT).show();

                    loginDialog.dismiss();
                } else
                    Toast.makeText(MainActivity.this, getString(R.string.dev_mode_login_wrong_pw), Toast.LENGTH_SHORT).show();
            }
        });

        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loginDialog.dismiss();
            }
        });

        loginDialog.show();
    }

    private void showDevLogout() {
        new AlertDialog.Builder(this)
                .setMessage(getString(R.string.dev_mode_logout_dev_mode))
                .setNegativeButton(getString(R.string.dev_mode_login_logout_cancel), null)
                .setPositiveButton(getString(R.string.dev_mode_logout), new OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        SharedPreferences.Editor mEditor = mPrefs.edit();

                        mEditor.putBoolean(Config.PREFS_DEV_MODE, false)
                                .putBoolean(Config.PREFS_REMEMBER_LOGIN, false).apply();

                        isDevMode = false;
                        rememberDevLogin = false;

                        menuItemDevMode.setTitle(getString(R.string.action_dev_mode))
                                .setChecked(false);
                    }
                }).show();
    }

    private void showAbout() {
        int thisYear = Calendar.getInstance().get(Calendar.YEAR);
        String opendelta = (thisYear == 2013) ? "2013" : "2013-"
                + String.valueOf(thisYear);
        String xdelta = (thisYear == 1997) ? "1997" : "1997-"
                + String.valueOf(thisYear);

        AlertDialog dialog = (new AlertDialog.Builder(this, android.R.style.Theme_Material_Light_Dialog_Alert))
                .setTitle(R.string.app_name)
                .setMessage(
                        Html.fromHtml(getString(R.string.about_content)
                                .replace("_COPYRIGHT_OPENDELTA_", opendelta)
                                .replace("_COPYRIGHT_XDELTA_", xdelta)))
                .setNeutralButton(android.R.string.ok, null)
                .setCancelable(true).show();
        TextView textView = (TextView) dialog
                .findViewById(android.R.id.message);
        if (textView != null)
            textView.setTypeface(title.getTypeface());
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            case R.id.settings:
                Intent settingsActivity = new Intent(this, SettingsActivity.class);
                startActivity(settingsActivity);
                return true;
            case R.id.action_dev_mode:
                if (!isDevMode)
                    showDevLogin();
                else showDevLogout();
                return true;
            case R.id.action_about:
                showAbout();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private IntentFilter updateFilter = new IntentFilter(
            UpdateService.BROADCAST_INTENT);
    private BroadcastReceiver updateReceiver = new BroadcastReceiver() {
        private String formatLastChecked(String filename, long ms) {
            Date date = new Date(ms);
            if (filename == null) {
                if (ms == 0) {
                    return "";
                } else {
                    return getString(
                            R.string.last_checked,
                            DateFormat.getDateFormat(MainActivity.this).format(
                                    date),
                            DateFormat.getTimeFormat(MainActivity.this).format(
                                    date));
                }
            } else {
                if (ms == 0) {
                    return "";
                } else {
                    return String.format(
                            "%s %s",
                            filename,
                            getString(R.string.last_checked,
                                    DateFormat.getDateFormat(MainActivity.this)
                                            .format(date), DateFormat
                                            .getTimeFormat(MainActivity.this)
                                            .format(date)));
                }
            }
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            String title = "";
            String sub = "";
            String updateVersion = "";
            String lastCheckedText = "";
            String extraText = "";
            String downloadSizeText = "";
            long current = 0L;
            long total = 1L;
            boolean enableCheck = false;
            boolean enableFlash = false;
            boolean enableBuild = false;
            boolean enableStop = false;
            boolean deltaUpdatePossible = false;
            boolean fullUpdatePossible = false;
            boolean enableProgress = false;
            final SharedPreferences prefs = PreferenceManager
                    .getDefaultSharedPreferences(MainActivity.this);

            String state = intent.getStringExtra(UpdateService.EXTRA_STATE);
            // don't try this at home
            if (state != null) {
                try {
                    title = getString(R.string.state_action_build);
                } catch (Exception e) {
                    // String for this state could not be found (displays empty
                    // string)
                    Logger.ex(e);
                }
                // check for first start until check button has been pressed
                // use a special title then - but only once
                if (UpdateService.STATE_ACTION_NONE.equals(state)
                        && !prefs.getBoolean(SettingsActivity.PREF_START_HINT_SHOWN, false)) {
                    title = getString(R.string.last_checked_never_title);
                }
                // dont spill for progress
                if (!UpdateService.isProgressState(state)) {
                    Logger.d("onReceive state = " + state);
                }
            }

            if (UpdateService.STATE_ERROR_DISK_SPACE.equals(state)) {
                enableCheck = true;
                progress.setIndeterminate(false);
                current = intent.getLongExtra(UpdateService.EXTRA_CURRENT,
                        current);
                total = intent.getLongExtra(UpdateService.EXTRA_TOTAL, total);

                current /= 1024L * 1024L;
                total /= 1024L * 1024L;

                extraText = getString(R.string.error_disk_space_sub, current,
                        total);
            } else if (UpdateService.STATE_ERROR_UNKNOWN.equals(state)) {
                enableCheck = true;
                progress.setIndeterminate(false);
            } else if (UpdateService.STATE_ERROR_UNOFFICIAL.equals(state)) {
                enableCheck = true;
                progress.setIndeterminate(false);
                title = getString(R.string.state_error_not_official_title);
                extraText = getString(R.string.state_error_not_official_extra,
                        intent.getStringExtra(UpdateService.EXTRA_FILENAME));
            } else if (UpdateService.STATE_ERROR_DOWNLOAD.equals(state)) {
                enableCheck = true;
                progress.setIndeterminate(false);
                extraText = intent.getStringExtra(UpdateService.EXTRA_FILENAME);
            } else if (UpdateService.STATE_ERROR_CONNECTION.equals(state)) {
                enableCheck = true;
                progress.setIndeterminate(false);
            } else if (UpdateService.STATE_ERROR_PERMISSIONS.equals(state)) {
                progress.setIndeterminate(false);
            } else if (UpdateService.STATE_ERROR_FLASH.equals(state)) {
                enableCheck = true;
                enableFlash = true;
                progress.setIndeterminate(false);
            } else if (UpdateService.STATE_ACTION_NONE.equals(state)) {
                enableCheck = true;
                progress.setIndeterminate(false);
                lastCheckedText = formatLastChecked(null,
                        intent.getLongExtra(UpdateService.EXTRA_MS, 0));
            } else if (UpdateService.STATE_ACTION_READY.equals(state)) {
                enableCheck = true;
                enableFlash = true;
                progress.setIndeterminate(false);
                lastCheckedText = formatLastChecked(null,
                        intent.getLongExtra(UpdateService.EXTRA_MS, 0));

                final String flashImage = prefs.getString(
                        UpdateService.PREF_READY_FILENAME_NAME,
                        UpdateService.PREF_READY_FILENAME_DEFAULT);
                String flashImageBase = flashImage != UpdateService.PREF_READY_FILENAME_DEFAULT ? new File(
                        flashImage).getName() : null;
                if (flashImageBase != null) {
                    updateVersion = flashImageBase.substring(0,
                            flashImageBase.lastIndexOf('.'));
                }
            } else if (UpdateService.STATE_ACTION_BUILD.equals(state)) {
                enableCheck = true;
                progress.setIndeterminate(false);
                lastCheckedText = formatLastChecked(null,
                        intent.getLongExtra(UpdateService.EXTRA_MS, 0));

                final String latestFull = prefs.getString(
                        UpdateService.PREF_LATEST_FULL_NAME,
                        UpdateService.PREF_READY_FILENAME_DEFAULT);
                final String latestDelta = prefs.getString(
                        UpdateService.PREF_LATEST_DELTA_NAME,
                        UpdateService.PREF_READY_FILENAME_DEFAULT);

                String latestDeltaZip = latestDelta != UpdateService.PREF_READY_FILENAME_DEFAULT ? new File(
                        latestDelta).getName() : null;
                String latestFullZip = latestFull != UpdateService.PREF_READY_FILENAME_DEFAULT ? latestFull
                        : null;

                deltaUpdatePossible = latestDeltaZip != null;
                fullUpdatePossible = latestFullZip != null;

                if (deltaUpdatePossible) {
                    String latestDeltaBase = latestDelta.substring(0,
                            latestDelta.lastIndexOf('.'));
                    enableBuild = true;
                    updateVersion = latestDeltaBase;
                    title = getString(R.string.state_action_build_delta);
                } else if (fullUpdatePossible) {
                    String latestFullBase = latestFull.substring(0,
                            latestFull.lastIndexOf('.'));
                    enableBuild = true;
                    updateVersion = latestFullBase;
                    title = getString(R.string.state_action_build_full);
                }

            } else if (UpdateService.STATE_ACTION_SEARCHING.equals(state)
                    || UpdateService.STATE_ACTION_CHECKING.equals(state)) {
                enableProgress = true;
                progress.setIndeterminate(true);
                current = 1;
            } else {
                enableProgress = true;
                if (UpdateService.STATE_ACTION_DOWNLOADING.equals(state)) {
                    enableStop = true;
                }
                current = intent.getLongExtra(UpdateService.EXTRA_CURRENT,
                        current);
                total = intent.getLongExtra(UpdateService.EXTRA_TOTAL, total);
                progress.setIndeterminate(false);

                // long --> int overflows FTL (progress.setXXX)
                boolean progressInK = false;
                if (total > 1024L * 1024L * 1024L) {
                    progressInK = true;
                    current /= 1024L;
                    total /= 1024L;
                }

                String filename = intent
                        .getStringExtra(UpdateService.EXTRA_FILENAME);
                if (filename != null) {
                    long ms = intent.getLongExtra(UpdateService.EXTRA_MS, 0);

                    if ((ms <= 500) || (current <= 0) || (total <= 0)) {
                        sub = String.format(Locale.ENGLISH, "%s %.0f %%",
                                filename, intent.getFloatExtra(
                                        UpdateService.EXTRA_PROGRESS, 0));
                    } else {
                        float kibps = ((float) current / 1024f)
                                / ((float) ms / 1000f);
                        if (progressInK)
                            kibps *= 1024f;
                        int sec = (int) (((((float) total / (float) current) * (float) ms) - ms) / 1000f);

                        if (kibps < 10000) {
                            sub = String.format(Locale.ENGLISH,
                                    "%s %.0f %%, %.0f KiB/s, %02d:%02d",
                                    filename, intent.getFloatExtra(
                                            UpdateService.EXTRA_PROGRESS, 0),
                                    kibps, sec / 60, sec % 60);
                        } else {
                            sub = String.format(Locale.ENGLISH,
                                    "%s %.0f %%, %.0f MiB/s, %02d:%02d",
                                    filename, intent.getFloatExtra(
                                            UpdateService.EXTRA_PROGRESS, 0),
                                    kibps / 1024f, sec / 60, sec % 60);
                        }
                    }
                }
            }
            if (updateVersion != null && !("".equals(updateVersion))) {
                long downloadSize = prefs.getLong(
                        UpdateService.PREF_DOWNLOAD_SIZE, -1);
                if (downloadSize == -1) {
                    downloadSizeText = "";
                } else if (downloadSize == 0) {
                    downloadSizeText = getString(R.string.text_download_size_unknown);
                } else {
                    downloadSizeText = Formatter.formatFileSize(context, downloadSize);
                }
            }
            MainActivity.this.title.setText(title);
            MainActivity.this.sub.setText(sub);
            MainActivity.this.updateVersion.setText(updateVersion);
            MainActivity.this.currentVersion.setText(config.getFilenameBase());
            MainActivity.this.lastChecked.setText(lastCheckedText);
            MainActivity.this.extra.setText(extraText);
            MainActivity.this.downloadSize.setText(downloadSizeText);

            progress.setProgress((int) current);
            progress.setMax((int) total);
            progress.setVisibility(!enableProgress ? View.INVISIBLE
                    : View.VISIBLE);
            MainActivity.this.sub
                    .setVisibility(!enableProgress ? View.INVISIBLE
                            : View.VISIBLE);

            checkNow.setEnabled(enableCheck ? true : false);
            buildNow.setEnabled(enableBuild ? true : false);
            flashNow.setEnabled(enableFlash ? true : false);

            flashNow.setVisibility(enableFlash ? View.VISIBLE : View.GONE);
            buildNow.setVisibility(!enableBuild || enableFlash ? View.GONE
                    : View.VISIBLE);
            stopNow.setVisibility(enableStop ? View.VISIBLE : View.GONE);
        }
    };

    @Override
    protected void onStart() {
        super.onStart();
        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client.connect();
        registerReceiver(updateReceiver, updateFilter);
        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        Action viewAction = Action.newAction(
                Action.TYPE_VIEW, // TODO: choose an action type.
                "Main Page", // TODO: Define a title for the content shown.
                // TODO: If you have web page content that matches this app activity's content,
                // make sure this auto-generated web page URL is correct.
                // Otherwise, set the URL to null.
                Uri.parse("http://host/path"),
                // TODO: Make sure this auto-generated app URL is correct.
                Uri.parse("android-app://com.dirtyunicorns.duupdater/http/host/path")
        );
        AppIndex.AppIndexApi.start(client, viewAction);
    }

    @Override
    protected void onStop() {
        unregisterReceiver(updateReceiver);
        super.onStop();
        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        Action viewAction = Action.newAction(
                Action.TYPE_VIEW, // TODO: choose an action type.
                "Main Page", // TODO: Define a title for the content shown.
                // TODO: If you have web page content that matches this app activity's content,
                // make sure this auto-generated web page URL is correct.
                // Otherwise, set the URL to null.
                Uri.parse("http://host/path"),
                // TODO: Make sure this auto-generated app URL is correct.
                Uri.parse("android-app://com.dirtyunicorns.duupdater/http/host/path")
        );
        AppIndex.AppIndexApi.end(client, viewAction);
        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client.disconnect();
    }

    @Override
    protected void onResume() {
        super.onResume();
        UpdateService.startUpdate(this);
    }

    public void onButtonCheckNowClick(View v) {
        final SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences(this);
        prefs.edit().putBoolean(SettingsActivity.PREF_START_HINT_SHOWN, true).commit();
        UpdateService.startCheck(this);
    }

    public void onButton1NowClick(View v) {
        final SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences(this);
        prefs.edit().putBoolean(SettingsActivity.PREF_START_HINT_SHOWN, true).commit();
        httpList = "mixFile.txt";
        httpPref = "m";
        downloadTxt();
        readTxt();
        downloadApk();
    }
    public void onButton2NowClick(View v) {
        final SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences(this);
        prefs.edit().putBoolean(SettingsActivity.PREF_START_HINT_SHOWN, true).commit();
        httpList = "mixAddonFile.txt";
        httpPref = "ma";
        downloadTxt();
        readTxt();
        downloadApk();
    }
    public void onButton3NowClick(View v) {
        final SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences(this);
        prefs.edit().putBoolean(SettingsActivity.PREF_START_HINT_SHOWN, true).commit();
        checkPersist();
        httpList = "persistFile.txt";
        httpPref = "p";
        File cpto = new File(Environment.getExternalStorageDirectory() + "/RomAddon/"+httpList);
        //Shell.SU.run(("cp -p /persist/persistFile.txt"+" " + cpto));
        readTxt();
        downloadApk();
    }
    public void onButton4NowClick(View v) {
        final SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences(this);
        prefs.edit().putBoolean(SettingsActivity.PREF_START_HINT_SHOWN, true).commit();
        (new AlertDialog.Builder(this))
                .setTitle(R.string.persistent_title)
                .setMessage(Html.fromHtml(getString(R.string.persistent_description)))
                .setCancelable(true)
                .setNeutralButton(android.R.string.ok, null).show();
        File sdcard = Environment.getExternalStorageDirectory();
        File check = new File(sdcard, "RomAddon/persistTemplateFile.txt");
        File check2 = new File(sdcard, "RomAddon/persistFile.txt");
        if (!check.exists()) {
            httpList = "persistTemplateFile.txt";
            downloadTxt();
        }   if (check2.exists()) {
            //bt4.setText("Update persistent FileList");
            //Shell.SU.run(("mount -o rw,remount /persist"));
            //Shell.SU.run(("rm" +" "+"/persist/persistFile.txt"));
            //Shell.SU.run(("cp -p "+ check2 +" "+"/persist/persistFile.txt"));
            //Shell.SU.run(("mount -o ro,remount /persist"));
        }
    }

    public void onButtonBuildNowClick(View v) {
        UpdateService.startBuild(this);
    }

    public void onButtonFlashNowClick(View v) {
        flashRecoveryWarning.run();
    }

    public void onButtonStopClick(View v) {
        stopDownload();
    }

    private Runnable flashRecoveryWarning = new Runnable() {
        @Override
        public void run() {
            // Show a warning message about recoveries we support, depending
            // on the state of secure mode and if we've shown the message before

            final Runnable next = flashWarningFlashAfterUpdateZIPs;

            CharSequence message = null;
            if (!config.getSecureModeCurrent()
                    && !config.getShownRecoveryWarningNotSecure()) {
                message = Html
                        .fromHtml(getString(R.string.recovery_notice_description_not_secure));
                config.setShownRecoveryWarningNotSecure();
            } else if (config.getSecureModeCurrent()
                    && !config.getShownRecoveryWarningSecure()) {
                message = Html
                        .fromHtml(getString(R.string.recovery_notice_description_secure));
                config.setShownRecoveryWarningSecure();
            }

            if (message != null) {
                (new AlertDialog.Builder(MainActivity.this))
                        .setTitle(R.string.recovery_notice_title)
                        .setMessage(message)
                        .setCancelable(true)
                        .setNegativeButton(android.R.string.cancel, null)
                        .setPositiveButton(android.R.string.ok,
                                new OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog,
                                                        int which) {
                                        next.run();
                                    }
                                }).show();
            } else {
                next.run();
            }
        }
    };

    private Runnable flashWarningFlashAfterUpdateZIPs = new Runnable() {
        @Override
        public void run() {
            // If we're in secure mode, but additional ZIPs to flash have been
            // detected, warn the user that these will not be flashed

            final Runnable next = flashStart;

            if (config.getSecureModeCurrent()
                    && (config.getFlashAfterUpdateZIPs().size() > 0)) {
                (new AlertDialog.Builder(MainActivity.this))
                        .setTitle(R.string.flash_after_update_notice_title)
                        .setMessage(
                                Html.fromHtml(getString(R.string.flash_after_update_notice_description)))
                        .setCancelable(true)
                        .setNegativeButton(android.R.string.cancel, null)
                        .setPositiveButton(android.R.string.ok,
                                new OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog,
                                                        int which) {
                                        next.run();
                                    }
                                }).show();
            } else {
                next.run();
            }
        }
    };

    private Runnable flashStart = new Runnable() {
        @Override
        public void run() {
            checkNow.setEnabled(false);
            flashNow.setEnabled(false);
            buildNow.setEnabled(false);
            UpdateService.startFlash(MainActivity.this);
        }
    };

    private void stopDownload() {
        final SharedPreferences Prefs = PreferenceManager
                .getDefaultSharedPreferences(this);
        mPrefs.edit()
                .putBoolean(
                        UpdateService.PREF_STOP_DOWNLOAD,
                        !mPrefs.getBoolean(UpdateService.PREF_STOP_DOWNLOAD,
                                false)).commit();
    }

    private void requestPermissions() {
        if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE);
        } else {
            mPermOk = true;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    mPermOk = true;
                }
            }
        }
    }

    private void checkPersist() {
        File check = new File("persist/persistFile.txt");
        if (!check.exists()) {
            //onButton3NowClick.setText("no persistent FileList found");
            //bt3.setClickable(false);
        }
    }

    private void downloadTxt() {

        if (android.os.Build.VERSION.SDK_INT > 9)
        {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
        }

        try {
            URL url = new URL("http://android.comtek-wiebe.de/.mix/" + httpList);
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("GET");
            urlConnection.setDoOutput(true);
            urlConnection.connect();

            File folder = new File(Environment.getExternalStorageDirectory() + "/RomAddon");
            boolean success = true;
            if (!folder.exists()) {
                Toast.makeText(MainActivity.this, "Directory Does Not Exist, Create It", Toast.LENGTH_SHORT).show();
                success = folder.mkdir();
            }
            if (success) {
                Toast.makeText(MainActivity.this, "Directory Created", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(MainActivity.this, "Failed - Error", Toast.LENGTH_SHORT).show();
            }

            File sdcard = Environment.getExternalStorageDirectory();
            File file = new File(sdcard, "RomAddon/" + httpList);

            FileOutputStream fileOutput = new FileOutputStream(file);
            InputStream inputStream = urlConnection.getInputStream();

            byte[] buffer = new byte[1024];
            int bufferLength = 0;

            while ((bufferLength = inputStream.read(buffer)) > 0) {
                fileOutput.write(buffer, 0, bufferLength);
            }
            fileOutput.close();

        } catch (MalformedURLException e) {
            Log.e("RomAddon", e.getMessage());
            e.printStackTrace();
        } catch (IOException e) {
            Log.e("RomAddon", e.getMessage());
            e.printStackTrace();
        }
    }

    private void readTxt() {

        final SharedPreferences.Editor mEditor = mPrefs.edit();

        File sdcard = Environment.getExternalStorageDirectory();
        File file = new File(sdcard, "RomAddon/" + httpList);
        mEditor.clear();
        StringBuilder text = new StringBuilder();

        try {
            BufferedReader br = new BufferedReader(new FileReader(file));
            String line;
            int linenumber = 1;

            while ((line = br.readLine()) != null) {
                text.append(line);
                text.append('\n');
                mEditor.putString("url" + String.valueOf(linenumber), line);
                mEditor.commit();
                linenumber = linenumber + 1;
            }
            br.close();
        } catch (IOException e) {
            Log.e("RomAddon", e.getMessage());
            e.printStackTrace();
        }

        tv2.setText(text.toString());
        //et1.setText(text.toString());
    }

    private void downloadApk() {

        if (android.os.Build.VERSION.SDK_INT > 9)
        {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
        }

        final SharedPreferences.Editor mEditor = mPrefs.edit();

        try {
            int inst = 1;
            READ_URL = mPrefs.getString("url" + String.valueOf(inst), "Keine URL gespeichert");
            String newurl;
            while (READ_URL != null) {
                newurl = mPrefs.getString("url" + String.valueOf(inst), "Keine URL gespeichert");
                URL url = new URL(newurl);
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.setDoOutput(true);
                urlConnection.connect();

                File sdcard = Environment.getExternalStorageDirectory();
                File file = new File(sdcard, "RomAddon/" + httpPref + String.valueOf(inst) + "name.apk");

                FileOutputStream fileOutput = new FileOutputStream(file);
                InputStream inputStream = urlConnection.getInputStream();

                Intent intent = new Intent(Intent.ACTION_VIEW);
                Uri uri = Uri.fromFile(new File(sdcard, "RomAddon/" + httpPref + String.valueOf(inst) + "name.apk"));
                intent.setDataAndType(uri, "application/vnd.android.package-archive");

                byte[] buffer = new byte[1024];
                int bufferLength = 0;

                while ((bufferLength = inputStream.read(buffer)) > 0) {
                    fileOutput.write(buffer, 0, bufferLength);
                }
                fileOutput.close();

                startActivity(intent);
                Log.i("RomAddon", "installed");
                inst = inst + 1;
            }

        } catch (MalformedURLException e) {
            Log.e("RomAddon", e.getMessage());
            e.printStackTrace();
        } catch (IOException e) {
            Log.e("RomAddon", e.getMessage());
            e.printStackTrace();
        }

    }


}

