package com.ticketapp.auth.app.fragments;
/**
 * Developed for Aalto University course CS-E4300 Network Security.
 * Copyright (C) 2021-2022 Aalto University
 */
import java.util.Comparator;

public class FileNameComparator implements Comparator<String> {

    @Override
    public int compare(String s1, String s2) {
        if (s1.contains("log") && s2.contains("log")) {
            return s1.compareTo(s2);
        } else if (s1.contains("card_data") && s2.contains("card_data")) {
            return s1.compareTo(s2);
        } else if (s1.contains("log") && s2.contains("card_data")) {
            return -1;
        } else return 1;
    }
}