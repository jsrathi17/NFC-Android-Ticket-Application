package com.ticketapp.auth.app.fragments;
/**
 * Developed for Aalto University course CS-E4300 Network Security.
 * Copyright (C) 2021-2022 Aalto University
 */

import android.app.ListFragment;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import com.ticketapp.auth.app.main.FileManager;
import com.ticketapp.auth.app.main.TicketActivity;
import com.ticketapp.auth.R;

import java.util.ArrayList;
import java.util.Collections;

public class ArchiveFragment extends ListFragment {

    public static ArchiveAdapter adapter;
    public static ArrayList<String> selected;
    private static ArrayList<String> valueList;
    private ActionMode actionMode;
    private boolean multiSelect = false;

    private View openChild;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setHasOptionsMenu(true);
        valueList = FileManager.getFileNames(TicketActivity.outer);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getActivity().getActionBar().setIcon(R.drawable.ic_archive);
        getView().setBackgroundColor(getResources().getColor(R.color.background_color));

        getListView().setDivider(getResources().getDrawable(R.drawable.divider));
        getListView().setDividerHeight(2);

        Collections.reverse(valueList);

        adapter = new ArchiveAdapter(getActivity(), valueList);

        adapter.setNotifyOnChange(true);

        selected = new ArrayList<String>();

        setEmptyText(getString(R.string.archive_empty_hint));

        setListAdapter(adapter);

        getListView().setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {

            @Override
            public boolean onItemLongClick(AdapterView parent, View view, int position, long id) {
                if (!multiSelect) {
                    selected.add(adapter.getItem(position));
                    actionMode = getActivity().startActionMode(new ActionBarCallBack());
                } else {
                    return false;
                }
                return true;
            }
        });

    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.fragment_archive, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_show_console) {
            showConsoleWindow();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void onListItemClick(ListView l, View v, int position, long id) {
        if (!multiSelect) {
            OpenFileTask task = new OpenFileTask();
            task.execute(adapter.getItem(position));
            openChild = v;
            toggleLoadIcon(true);
        } else {
            adapter.notifyDataSetChanged();
            if (!selected.contains(adapter.getItem(position))) {
                selected.add(adapter.getItem(position));
            } else selected.remove(adapter.getItem(position));
            actionMode.setTitle(getString(R.string.selected_msg) + selected.size());
        }
    }

    private void toggleLoadIcon(boolean load) {
        if (load) {
            openChild.findViewById(R.id.list_item_progress).setVisibility(View.VISIBLE);
            openChild.findViewById(R.id.list_item_icon).setVisibility(View.GONE);
        } else {
            openChild.findViewById(R.id.list_item_progress).setVisibility(View.GONE);
            openChild.findViewById(R.id.list_item_icon).setVisibility(View.VISIBLE);
        }
    }

    private void openItem(String data) {
        DataViewFragment dataViewFragment = new DataViewFragment();
        Bundle args = new Bundle();
        args.putString("param1", data);
        dataViewFragment.setArguments(args);
        TicketActivity.fm.beginTransaction()
                .setCustomAnimations(R.animator.slide_in, R.animator.slide_out, R.animator.slide_in, R.animator.slide_out)
                .add(R.id.container, dataViewFragment).addToBackStack("").commit();
        toggleLoadIcon(false);
    }

    private class OpenFileTask extends AsyncTask<String, Long, Boolean> {
        private String data;

        @Override
        protected Boolean doInBackground(String... params) {
            data = FileManager.readFile(TicketActivity.outer, params[0]);
            return true;
        }

        @Override
        protected void onPostExecute(Boolean aBoolean) {
            openItem(data);
        }

    }

    private void deleteSelected() {
        for (String item : selected) {
            if (!adapter.isEmpty()) {
                adapter.remove(item);
                TicketActivity.outer.deleteFile(item);
            }
        }
        adapter.notifyDataSetChanged();
    }

    private void selectAll() {
        selected = new ArrayList<String>(valueList);
        actionMode.setTitle(getString(R.string.selected_msg) + selected.size());
        adapter.notifyDataSetChanged();
    }

    private void showConsoleWindow() {
        ((TicketActivity) getActivity()).showConsoleWindow();
    }

    class ActionBarCallBack implements ActionMode.Callback {
        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            switch (item.getItemId()) {
                case R.id.action_delete:
                    deleteSelected();
                    actionMode.finish();
                    break;
                case R.id.action_select_all:
                    selectAll();
                    break;
            }
            return true;
        }

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            getListView().setLongClickable(false);
            mode.getMenuInflater().inflate(R.menu.archive_cbar, menu);
            multiSelect = true;
            adapter.setPaintSelected(true);
            adapter.notifyDataSetChanged();
            return true;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            getListView().setLongClickable(true);
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