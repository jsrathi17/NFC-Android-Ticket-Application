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
import android.widget.CheckBox;
import android.widget.TextView;

import com.ticketapp.auth.app.ulctools.Reader;
import com.ticketapp.auth.R;

public class ErasePopup extends DialogFragment {

    private Button btn_erase;
    private Button btn_erase_cancel;
    private CheckBox checkBox_erase_auth;
    private TextView current_key;

    private boolean erase_auth = false;

    private final View.OnClickListener btn_erase_listener = new View.OnClickListener() {
        public void onClick(View v) {
            if (Reader.connect()) {
                Reader.erase(erase_auth);
                Reader.disconnect();
                DumpFragment.update();
            }
            dismiss();
        }
    };
    private final View.OnClickListener btn_erase_cancel_listener = new View.OnClickListener() {
        public void onClick(View v) {
            dismiss();
        }
    };

    public ErasePopup() {
        // Empty constructor required for DialogFragment
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.erase_popup, container);

        btn_erase = view.findViewById(R.id.btn_erase);
        btn_erase_cancel = view.findViewById(R.id.btn_erase_cancel);

        current_key = view.findViewById(R.id.current_auth_key);

        checkBox_erase_auth = view.findViewById(R.id.check_box_erase_auth);
        checkBox_erase_auth.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                if (((CheckBox) v).isChecked()) {
                    erase_auth = true;
                    current_key.setText("Key in use: " + Reader.authKey);
                    current_key.setAlpha(1);
                } else {
                    erase_auth = false;
                    current_key.setAlpha(0);
                }

            }
        });

        btn_erase.setOnClickListener(btn_erase_listener);
        btn_erase_cancel.setOnClickListener(btn_erase_cancel_listener);

        return view;
    }

}
