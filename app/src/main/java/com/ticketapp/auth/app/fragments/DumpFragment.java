package com.ticketapp.auth.app.fragments;
/**
 * Developed for Aalto University course CS-E4300 Network Security.
 * Copyright (C) 2021-2022 Aalto University
 */

import android.app.ActionBar;
import android.app.Fragment;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.TextView;

import com.ticketapp.auth.app.main.TicketActivity;
import com.ticketapp.auth.app.ulctools.Dump;
import com.ticketapp.auth.app.ulctools.Reader;
import com.ticketapp.auth.R;

public class DumpFragment extends Fragment {

    public static int card_auth0;
    public static int card_auth1;
    private static String received_data = "";
    private static TextView card_data;
    private static final byte[] data = new byte[192];
    private static boolean stringAsBinary = false;
    private static ActionBar actionBar;
    private static TextView safeMode_indicator;
    private static TextView auth_info;
    private static TextView tag_hint;
    private static ImageButton btn_tools;
    private MenuItem string_switch;
    private final View.OnClickListener tool_popup_listener = new View.OnClickListener() {
        public void onClick(View v) {
            tool_popup();
        }
    };

    public DumpFragment() {
        // Required empty public constructor
    }

    public static void update() {
        if (TicketActivity.nfcA_available) DumpFragment.read(false);
    }

    public static void read(boolean display) {
        boolean autoAuth = TicketActivity.autoAuth;
        if (TicketActivity.nfcA_available) {
            String info = "";
            if (Reader.connect()) {
                Reader.readMemory(data, autoAuth, display);
                Reader.disconnect();
                int mode = 0;
                if (stringAsBinary) mode = 1;
                received_data = Dump.hexView(data, mode);
                card_data.setText(received_data);
                card_auth0 = (int) data[42 * 4];
                card_auth1 = (int) data[43 * 4];
                if (card_auth0 > 2 && card_auth0 <= 48) {
                    if (card_auth1 == 1) info += "write protected starting from page " + card_auth0;
                    else if (card_auth1 == 0)
                        info += "R/W protected starting from page " + card_auth0;
                }
                auth_info.setText(info);
                auth_info.setEllipsize(TextUtils.TruncateAt.MARQUEE);
                auth_info.setSelected(true);
                tag_hint.setVisibility(View.GONE);
            }
        }
    }

    public static void erase() {
        ErasePopup erasePopup = new ErasePopup();
        erasePopup.show(TicketActivity.fm, "erase_popup");
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        setHasOptionsMenu(true);
        super.onCreate(savedInstanceState);

    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.fragment_tools, menu);

        string_switch = menu.findItem(R.id.action_switch_view);
        if (stringAsBinary) string_switch.setTitle("BIN");
        else string_switch.setTitle("HEX");
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_switch_view) {
            switchStringView();
            return true;
        }
        if (id == R.id.action_refresh) {
            update();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        actionBar = getActivity().getActionBar();

        actionBar.setIcon(R.drawable.ic_launcher);
        View v = inflater.inflate(R.layout.fragment_tools, container, false);

//        safeMode_indicator = v.findViewById(R.id.safemode_indicator);
        auth_info = v.findViewById(R.id.auth_info);

//        if (Reader.safeMode) {
//            safeMode_indicator.setEnabled(true);
//            safeMode_indicator.setText("safe mode on");
//        } else {
//            safeMode_indicator.setEnabled(false);
//            safeMode_indicator.setText("safe mode off");
//        }

        card_data = v.findViewById(R.id.tools_data_view);
        card_data.setText(received_data);

        btn_tools = v.findViewById(R.id.tool_list);
        btn_tools.setOnClickListener(tool_popup_listener);

        tag_hint = v.findViewById(R.id.tag_hint);
        if (card_data.getText().length() > 5) tag_hint.setVisibility(View.GONE);

        if (TicketActivity.nfcA_available) {
            btn_tools.setEnabled(true);
        }

        return v;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    private void tool_popup() {
        PopupMenu pum = new PopupMenu(getActivity(), getActivity().findViewById(R.id.tool_list));
        pum.inflate(R.menu.popup_menu);
        if (TicketActivity.nfcA_available) {
            pum.getMenu().getItem(0).setEnabled(true);
            pum.getMenu().getItem(1).setEnabled(true);
        } else {
            pum.getMenu().getItem(0).setEnabled(false);
            pum.getMenu().getItem(1).setEnabled(false);
        }
        pum.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.action_erase:
                        erase();
                        break;
                    case R.id.action_auth_test:
                        Reader.testAuthenticate();
                        break;
                    default:
                        break;
                }
                return false;

            }
        });
        pum.show();

    }

    private void switchStringView() {
        stringAsBinary = !stringAsBinary;
        if (stringAsBinary) {
            string_switch.setTitle("BIN");
            received_data = Dump.hexView(data, 1);
            card_data.setText(received_data);
            btn_tools.setAlpha(150);
        } else {
            string_switch.setTitle("HEX");
            received_data = Dump.hexView(data, 0);
            card_data.setText(received_data);
            btn_tools.setAlpha(255);
        }
        if (card_data.getText().length() > 5) tag_hint.setVisibility(View.GONE);
    }
}
