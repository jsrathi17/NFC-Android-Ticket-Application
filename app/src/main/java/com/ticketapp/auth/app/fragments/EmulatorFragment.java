package com.ticketapp.auth.app.fragments;
/**
 * Developed for Aalto University course CS-E4300 Network Security.
 * Copyright (C) 2021-2022 Aalto University
 */

import android.app.Fragment;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.ticketapp.auth.app.ulctools.Reader;
import com.ticketapp.auth.app.ulctools.Utilities;
import com.ticketapp.auth.ticket.Ticket;
import com.ticketapp.auth.R;

import java.security.GeneralSecurityException;
import java.util.Date;

public class EmulatorFragment extends Fragment {
    private static Ticket ticket;

    private static TextView ticket_info;

    private Button btn_issue;
    private Button btn_validate;

    public boolean issue_mode = false;
    public boolean active = false;

    public EmulatorFragment() {
        try {
            ticket = new Ticket();
            active = true;
        } catch (GeneralSecurityException g) {
            Utilities.log(g.toString(), true);
        }
    }

    private final View.OnClickListener validate_listener = new View.OnClickListener() {
        public void onClick(View v) {
            btn_validate.setSelected(true);
            btn_issue.setSelected(false);
            issue_mode = false;
        }
    };

    private final View.OnClickListener issue_listener = new View.OnClickListener() {
        public void onClick(View v) {
            btn_issue.setSelected(true);
            btn_validate.setSelected(false);
            issue_mode = true;
        }
    };

    public void setCardAvailable(boolean b) {
        btn_validate.setEnabled(b);
        btn_issue.setEnabled(b);
        active = b;
        if (issue_mode) {
            issue();
        } else {
            use();
        }
    }

    public void issue() {
        if (active && Reader.connect()) {
            try {
                ticket.issue(30, 10);
                ticket_info.setText(Ticket.getInfoToShow());
            } catch (Exception e) {
                e.printStackTrace();
            }
            Reader.disconnect();
        }
    }

    public void use() {
        if (active && Reader.connect()) {
            try {
                int currentTime = (int) ((new Date()).getTime() / 1000 / 60);
                int uses = ticket.getRemainingUses();
                int expiryTime = ticket.getExpiryTime();

                ticket.use();

                Reader.disconnect();
                boolean valid = ticket.isValid();
                String msg;

                if (valid) {
                    msg = "Used ticket successfully. The ticket was valid.";
                } else {
                    msg = "Ticket use FAILED. The following data may be INVALID.";
                }
                System.out.println(msg);
                Reader.history += "\n" + msg + "\n";

                String info = "Current time:\n"
                        + new Date((long) currentTime * 60 * 1000) + "\n\nExpiry time:\n"
                        + new Date((long) expiryTime * 60 * 1000) + "\n\nRemaining uses: " + uses + "\n";
                System.out.println(info);
                Reader.history += "\n" + info + "\n--------------------------------";

                if (valid) {
                    ToneGenerator toneG = new ToneGenerator(AudioManager.STREAM_RING, 100);
                    toneG.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 100);
                } else {
                    ToneGenerator toneG = new ToneGenerator(AudioManager.STREAM_RING, 100);
                    toneG.startTone(ToneGenerator.TONE_CDMA_ABBR_INTERCEPT, 100);
                }
                ticket_info.setText(Ticket.getInfoToShow());
            } catch (GeneralSecurityException g) {
                Log.d("Error", g.toString());
            }
            Reader.disconnect();
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.fragment_emulator, container, false);
        getActivity().getActionBar().setIcon(R.drawable.ic_launcher);

        ticket_info = v.findViewById(R.id.ticket_info);
        ticket_info.setText("Ticket info");

        btn_issue = v.findViewById(R.id.issue_mode);
        btn_validate = v.findViewById(R.id.validate_mode);

        issue_mode = false;
        btn_issue.setSelected(false);
        btn_validate.setSelected(true);

        btn_issue.setOnClickListener(issue_listener);
        btn_validate.setOnClickListener(validate_listener);

        return v;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }
}
