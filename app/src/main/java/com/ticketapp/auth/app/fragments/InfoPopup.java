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
import android.widget.TextView;

import com.ticketapp.auth.R;

public class InfoPopup extends DialogFragment {

    private TextView info_text;
    private Button btn_close;

    private String info = "";

    private final View.OnClickListener btn_close_listener = new View.OnClickListener() {
        public void onClick(View v) {
            dismiss();
        }
    };

    public InfoPopup() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.info_popup, container);
        info_text = view.findViewById(R.id.info_text);
        info_text.setText(info);
        btn_close = view.findViewById(R.id.btn_close);

        btn_close.setOnClickListener(btn_close_listener);

        return view;
    }

    public void setInfoText(String text) {
        info = text;

    }
}
