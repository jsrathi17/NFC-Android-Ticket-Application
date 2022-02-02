package com.ticketapp.auth.app.main;
/**
 * Developed for Aalto University course CS-E4300 Network Security.
 * Copyright (C) 2021-2022 Aalto University
 */

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.NfcA;
import android.os.Bundle;
import android.os.Vibrator;
import android.provider.Settings;
import android.view.Menu;
import android.widget.ArrayAdapter;

import com.ticketapp.auth.app.fragments.ArchiveFragment;
import com.ticketapp.auth.app.fragments.ConsolePopup;
import com.ticketapp.auth.app.fragments.DumpFragment;
import com.ticketapp.auth.app.fragments.EmulatorFragment;
import com.ticketapp.auth.app.fragments.KeyListFragment;
import com.ticketapp.auth.app.ulctools.Reader;
import com.ticketapp.auth.R;

import java.util.Calendar;

public class TicketActivity extends Activity implements ActionBar.OnNavigationListener {

    private static final String STATE_SELECTED_NAVIGATION_ITEM = "selected_navigation_item";
    public static FileManager fileManager;
    public static boolean autoAuth = true;
    public static Context outer;

    // Fragments
    public static FragmentManager fm;
    public static boolean nfcA_available = false;
    private static EmulatorFragment userMode;
    private static KeyListFragment keyList;
    private static DumpFragment dumpMode;
    private static ArchiveFragment archiveFragment;

    private static Vibrator vibrator;
    private NfcAdapter adapter;
    private PendingIntent pendingIntent;
    private IntentFilter[] filters;
    private String[][] techLists;
    private Context context;
    private final ConsolePopup consoleWindow = new ConsolePopup();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_my);

        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        context = getApplicationContext();
        outer = context;

        fileManager = new FileManager();
        FileManager.getKeys(outer);
        fm = getFragmentManager();

        adapter = NfcAdapter.getDefaultAdapter(this);
        if (adapter == null || !adapter.isEnabled()) {
            promptNfc();
        }
        pendingIntent = PendingIntent.getActivity(this, 0, new Intent(this,
                getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
        IntentFilter ndef = new IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED);
        try {
            ndef.addDataType("*/*");
        } catch (IntentFilter.MalformedMimeTypeException e) {
            throw new RuntimeException("fail", e);
        }
        filters = new IntentFilter[]{ndef,};

        techLists = new String[][]{new String[]{NfcA.class.getName()}};

        // Set up the action bar to show a dropdown list.
        final ActionBar actionBar = getActionBar();
        actionBar.setDisplayShowTitleEnabled(false);
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);

        dumpMode = new DumpFragment();
        userMode = new EmulatorFragment();
        keyList = new KeyListFragment();
        archiveFragment = new ArchiveFragment();

        // Set up the dropdown list navigation in the action bar.
        actionBar.setListNavigationCallbacks(
                // Specify a SpinnerAdapter to populate the dropdown list.
                new ArrayAdapter<String>(
                        actionBar.getThemedContext(),
                        android.R.layout.simple_list_item_1,
                        android.R.id.text1,
                        new String[]{
                                getString(R.string.menu_normal_mode),
                                getString(R.string.menu_log),
                                getString(R.string.menu_keys),
                                getString(R.string.menu_dump)
                        }
                ),
                this
        );
        actionBar.setSelectedNavigationItem(0);
    }

    @Override
    public void onResume() {
        super.onResume();
        Intent intent = getIntent();
        resolveIntent(intent);
        adapter.enableForegroundDispatch(this, pendingIntent, filters,
                techLists);
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public void onNewIntent(Intent intent) {
        resolveIntent(intent);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        // Serialize the current dropdown position.
        outState.putInt(STATE_SELECTED_NAVIGATION_ITEM,
                getActionBar().getSelectedNavigationIndex());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.my, menu);
        return true;
    }

    @SuppressLint("ResourceType")
    @Override
    public boolean onNavigationItemSelected(int position, long id) {
        // When the given dropdown item is selected, show its contents in the
        // container view.
        Fragment newFragment = dumpMode;
        int enter = R.animator.fragment_slide_down;
        fm.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        switch (position) {
            case 0:
                newFragment = userMode;
                enter = R.animator.fragment_slide_up;
                break;
            case 1:
                showConsoleWindow();
                newFragment = archiveFragment;
                enter = R.animator.fragment_slide_up;
                break;
            case 2:
                newFragment = keyList;
                enter = R.animator.fragment_slide_up;
                break;
            case 3:
                newFragment = dumpMode;
                enter = R.animator.fragment_slide_up;
                break;
            default:
                return true;
        }
        getFragmentManager().beginTransaction()
                .setCustomAnimations(enter, R.animator.fragment_slide_out)
                .replace(R.id.container, newFragment)
                .commit();

        return true;
    }

    void resolveIntent(Intent intent) {

        String action = intent.getAction();

        if (NfcAdapter.ACTION_TECH_DISCOVERED.equals(action)) {

            Tag tagFromIntent = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            String timestamp = "" + Calendar.getInstance().getTime();
            Reader.history += "\nNew tag discovered on\n" + timestamp + "\n";

            for (int k = 0; k < tagFromIntent.getTechList().length; k++) {
                if (tagFromIntent.getTechList()[k]
                        .equals("android.nfc.tech.NfcA")) {
                    vibrator.vibrate(50);
                    nfcA_available = true;
                    Reader.nfcA_card = NfcA.get(tagFromIntent);
                    userMode.setCardAvailable(userMode.isVisible());
                    if (dumpMode.isVisible()) {
                        DumpFragment.update();
                    }
                }
            }
        }
    }

    @Override
    public void onAttachFragment(Fragment fragment) {
        super.onAttachFragment(fragment);
    }

    public void showConsoleWindow() {
        consoleWindow.show(fm, "console_popup");
    }

    private void promptNfc() {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(
                this);
        alertDialogBuilder
                .setMessage(getString(R.string.nfc_disabled_msg))
                .setCancelable(false)
                .setPositiveButton(getString(R.string.action_nfc_settings),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog,
                                                int id) {
                                Intent callNFCSettingsIntent = new Intent(
                                        Settings.ACTION_NFC_SETTINGS);
                                startActivity(callNFCSettingsIntent);
                            }
                        }
                );
        alertDialogBuilder.setNegativeButton(getString(R.string.action_cancel),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                }
        );
        AlertDialog alert = alertDialogBuilder.create();
        alert.show();
    }
}
