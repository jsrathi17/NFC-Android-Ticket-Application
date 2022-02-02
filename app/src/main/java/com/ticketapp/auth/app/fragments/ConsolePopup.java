package com.ticketapp.auth.app.fragments;
/**
 * Developed for Aalto University course CS-E4300 Network Security.
 * Copyright (C) 2021-2022 Aalto University
 */

import android.app.DialogFragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;

import com.ticketapp.auth.app.main.FileManager;
import com.ticketapp.auth.app.main.TicketActivity;
import com.ticketapp.auth.app.ulctools.Reader;
import com.ticketapp.auth.R;

public class ConsolePopup extends DialogFragment {

    private TextView console;
    private ScrollView scrollView;
    private Button btn_archive;
    private TextView console_hint;
    private final View.OnClickListener btn_archive_listener = new View.OnClickListener() {
        public void onClick(View v) {
            FileManager.saveLog(TicketActivity.outer);
            console.setText("");
            Reader.history = "";
        }
    };

    public ConsolePopup() {
        // Empty constructor required for DialogFragment
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.console_popup, container);
        console = view.findViewById(R.id.console);
        scrollView = view.findViewById(R.id.scrollView);
        console.setText(Reader.history);
        btn_archive = view.findViewById(R.id.btn_console_archive);

        btn_archive.setOnClickListener(btn_archive_listener);

        console_hint = view.findViewById(R.id.console_hint);

        if (console.getText().length() >= 1) {
            console_hint.setVisibility(View.GONE);
        }

        this.update();

        return view;
    }

    public void update() {
        console.setText(Reader.history);
        if (console.getText().length() >= 1) {
            console_hint.setVisibility(View.GONE);
        }
        scrollView.post(new Runnable() {
            @Override
            public void run() {
                scrollView.fullScroll(View.FOCUS_DOWN);
            }
        });

    }
}
