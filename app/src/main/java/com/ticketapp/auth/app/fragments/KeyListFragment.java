package com.ticketapp.auth.app.fragments;
/**
 * Developed for Aalto University course CS-E4300 Network Security.
 * Copyright (C) 2021-2022 Aalto University
 */
import android.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.ticketapp.auth.app.main.FileManager;
import com.ticketapp.auth.app.main.TicketActivity;
import com.ticketapp.auth.app.ulctools.Reader;
import com.ticketapp.auth.R;

import java.util.ArrayList;

public class KeyListFragment extends Fragment {

    public static KeyListAdapter adapter;
    public static ArrayList<String> selected;
    private static ArrayList<String> valueList;
    private ActionMode actionMode;
    private boolean multiSelect = false;

    private static ListView listView;
    private static TextView key_in_use;
    private static Switch auto_auth_switch;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        valueList = FileManager.getKeys(TicketActivity.outer);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.fragment_keylist, menu);

        // Setup animation
        Animation fade_in = AnimationUtils.loadAnimation(getActivity(), android.R.anim.fade_in);
        fade_in.setInterpolator(new AccelerateInterpolator());
        fade_in.setDuration(20);

        // Animate
        MenuItem action_newKey = menu.findItem(R.id.action_new_key);
        action_newKey.setActionView(R.layout.newkey_actionview);
        View itemView = action_newKey.getActionView();
        itemView.startAnimation(fade_in);

        action_newKey.getActionView().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                newKey();
            }
        });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_key_list, container, false);

        listView = view.findViewById(R.id.key_list);

        key_in_use = view.findViewById(R.id.key_in_use);
        key_in_use.setText(getString(R.string.key_in_use) + Reader.authKey);

        auto_auth_switch = view.findViewById(R.id.auto_auth_switch);
        auto_auth_switch.setChecked(TicketActivity.autoAuth);

        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        getActivity().getActionBar().setIcon(R.drawable.ic_keys);
        getView().setBackgroundColor(getResources().getColor(R.color.background_color));

        super.onActivityCreated(savedInstanceState);

        selected = new ArrayList<String>();

        listView.setDivider(getResources().getDrawable(R.drawable.divider));
        listView.setDividerHeight(2);

        adapter = new KeyListAdapter(getActivity(), valueList);

        listView.setLongClickable(true);

        listView.setAdapter(adapter);

        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {

            @Override
            public boolean onItemLongClick(AdapterView parent, View view, int position, long id) {
                if (!multiSelect) {
                    selected.add(adapter.getItem(position));
                    actionMode = getActivity().startActionMode(new ActionBarCallBack());
                }
                return true;
            }
        });

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (!multiSelect) {
                    Log.d("Key", adapter.getItem(position));
                    String[] parts = adapter.getItem(position).split(",");
                    Reader.setAuthKey(parts[1]);
                    key_in_use.setText(getString(R.string.key_in_use) + Reader.authKey);
                    Toast.makeText(TicketActivity.outer, getString(R.string.key_in_use) + Reader.authKey, Toast.LENGTH_SHORT).show();
                    adapter.notifyDataSetChanged();
                    TicketActivity.autoAuth = true;
                    auto_auth_switch.setChecked(true);
                } else {
                    adapter.notifyDataSetChanged();
                    if (!selected.contains(adapter.getItem(position))) {
                        selected.add(adapter.getItem(position));
                    } else selected.remove(adapter.getItem(position));
                    actionMode.setTitle(getString(R.string.selected_msg) + selected.size());
                }
            }
        });

        auto_auth_switch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                TicketActivity.autoAuth = isChecked;
            }
        });

    }

    private void deleteSelected() {
        for (String item : selected) {
            if (!adapter.isEmpty()) {
                if (!item.contains("default")) {
                    adapter.remove(item);
                    FileManager.removeKey(TicketActivity.outer, item);
                }
                if (adapter.isEmpty()) {
                    String[] parts = adapter.getItem(0).split(",");
                    Reader.setAuthKey(parts[1]);
                    adapter.notifyDataSetChanged();
                }
            }
        }
        adapter.notifyDataSetChanged();
    }

    private void newKey() {
        AuthKeyPopup authenticationWindow = AuthKeyPopup.newInstance(2);
        authenticationWindow.show(TicketActivity.fm, "key_popup");
    }

    class ActionBarCallBack implements ActionMode.Callback {

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            switch (item.getItemId()) {
                case R.id.action_delete:
                    deleteSelected();
                    actionMode.finish();
                    break;
            }
            return false;
        }

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            listView.setLongClickable(false);
            mode.getMenuInflater().inflate(R.menu.key_list_cbar, menu);
            multiSelect = true;
            adapter.setPaintSelected(true);
            adapter.notifyDataSetChanged();
            return true;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            listView.setLongClickable(true);
            multiSelect = false;
            selected = new ArrayList<String>();
            adapter.setPaintSelected(false);
            adapter.notifyDataSetChanged();
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            mode.setTitle(getString(R.string.cbar_menu_title));
            return false;
        }
    }
}