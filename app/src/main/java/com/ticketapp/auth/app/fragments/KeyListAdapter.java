package com.ticketapp.auth.app.fragments;
/**
 * Developed for Aalto University course CS-E4300 Network Security.
 * Copyright (C) 2021-2022 Aalto University
 */
import android.content.Context;
import android.graphics.PorterDuff;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.ticketapp.auth.app.main.TicketActivity;
import com.ticketapp.auth.app.ulctools.Reader;
import com.ticketapp.auth.R;

import java.util.ArrayList;
import java.util.Date;

public class KeyListAdapter extends ArrayAdapter<String> {
    private final Context context;
    private final ArrayList values;
    private boolean paintSelected = false;

    public KeyListAdapter(Context context, ArrayList<String> values) {
        super(context, R.layout.fragment_list_item, values);
        this.context = context;
        this.values = values;
    }

    public void setPaintSelected(boolean paintSelected) {
        this.paintSelected = paintSelected;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View rowView = inflater.inflate(R.layout.fragment_list_item, parent, false);
        TextView date_view = rowView.findViewById(R.id.date_string);
        TextView key_view = rowView.findViewById(R.id.key_string);

        String[] days = TicketActivity.outer.getResources().getStringArray(R.array.weekdays);
        int[] colors = context.getResources().getIntArray(R.array.daycolors);

        String[] parts = values.get(position).toString().split(",");
        String dateString;
        ImageView colorStrip = rowView.findViewById(R.id.list_item_icon);
        ImageView is_selected = rowView.findViewById(R.id.is_selected);

        int color;

        if (parts[0].contains("d")) {
            dateString = TicketActivity.outer.getString(R.string.default_key);
            colorStrip.setColorFilter(R.color.default_key, PorterDuff.Mode.MULTIPLY);
        } else {
            Date date = new Date(Long.parseLong(parts[0]) * 10);
            @SuppressWarnings("deprecation")
            String hours = "" + date.getHours();
            String minutes = "" + date.getMinutes();
            if (hours.length() == 1) hours = "0" + hours;
            if (minutes.length() == 1) minutes = "0" + minutes;
            dateString = "added on " + days[(date.getDay())] + " " + date.getDate() + "." + (date.getMonth() + 1)
                    + ": " + hours + ":" + minutes;
            color = colors[(date.getDay())];
            colorStrip.setColorFilter(color, PorterDuff.Mode.MULTIPLY);
        }

        if (paintSelected && KeyListFragment.selected.contains(getItem(position))) {
            rowView.setBackgroundColor(rowView.getResources().getColor(R.color.selected));
        }

        if (Reader.authKey.contains(parts[1])) {
            is_selected.setImageDrawable(rowView.getResources().getDrawable(R.drawable.ic_is_selected));
            is_selected.setColorFilter(R.color.default_key, PorterDuff.Mode.MULTIPLY);
        }

        key_view.setText(parts[1].toUpperCase());
        date_view.setText(dateString);

        return rowView;
    }
}
