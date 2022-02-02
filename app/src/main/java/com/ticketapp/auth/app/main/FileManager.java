package com.ticketapp.auth.app.main;
/**
 * Developed for Aalto University course CS-E4300 Network Security.
 * Copyright (C) 2021-2022 Aalto University
 */

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.ticketapp.auth.app.fragments.FileNameComparator;
import com.ticketapp.auth.app.fragments.KeyListFragment;
import com.ticketapp.auth.app.ulctools.Reader;
import com.ticketapp.auth.R;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;

public class FileManager {

    public static ArrayList<String> keys = new ArrayList<String>();
    public static ArrayList<String> fileNames = new ArrayList<String>();
    private static final String defaultKey = "default,BREAKMEIFYOUCAN!";

    public static void writeKeys(Context context) {
        try {
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(context.openFileOutput("keys.txt", Context.MODE_PRIVATE));
            for (String key : keys) {
                String[] parts = key.split(",");
                String timeStamp = parts[0];
                String authKey = parts[1];
                outputStreamWriter.write(timeStamp + "," + authKey + "\n");
            }
            outputStreamWriter.close();
        } catch (IOException e) {
            Reader.history += "\nKey write error " + Calendar.getInstance().getTime() + "\n";
        }
    }

    public static void removeKey(Context context, String key) {
        keys.remove(key);
        writeKeys(context);
    }

    public static void addKey(Context context, String key) {
        getKeys(context);
        Long tsLong = System.currentTimeMillis() / 10;
        String ts = tsLong.toString();
        String s;
        if (key.length() == 16)
            s = key.toUpperCase();
        else
            s = key;
        keys.add(ts + "," + s);
        KeyListFragment.adapter.add(ts + "," + key);
        KeyListFragment.adapter.notifyDataSetChanged();
        writeKeys(context);
    }

    public static ArrayList<String> getKeys(Context context) {
        try {
            InputStream inputStream = context.openFileInput("keys.txt");
            keys = new ArrayList<String>();

            if (inputStream != null) {
                InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                String receiveString;

                while ((receiveString = bufferedReader.readLine()) != null) {
                    keys.add(receiveString);
                }

                inputStream.close();
            }
            if (!keys.contains(defaultKey)) {
                keys.add(defaultKey);
                OutputStreamWriter outputStreamWriter = new OutputStreamWriter(context.openFileOutput("keys.txt", Context.MODE_PRIVATE));
                outputStreamWriter.write(defaultKey + "\n");
            }
        } catch (FileNotFoundException e) {
            Reader.history += "\nKey file FileNotFoundException " + Calendar.getInstance().getTime() + "\n";
            Log.e("log activity", "File not found: " + e.toString());
            writeKeys(context);

        } catch (IOException e) {
            Reader.history += "\nKey file IOException " + Calendar.getInstance().getTime() + "\n";
            Log.e("log activity", "Can not read file: " + e.toString());
        }
        return keys;

    }

    public static ArrayList<String> getFileNames(Context context) {
        fileNames = new ArrayList<String>();
        ArrayList<String> fileList = new ArrayList<String>(Arrays.asList(context.fileList()));
        Log.d("filedir", "" + context.getFilesDir());
        for (String file : fileList) {
            Log.d("File path", file);
            if (file.contains("card_data") || file.contains("log")) {
                fileNames.add(file);
            }
        }
        Collections.sort(fileNames, new FileNameComparator());
        return fileNames;
    }

    public static String getUID(Context context, String fileName) {
        String content = "";
        try {
            InputStream inputStream = context.openFileInput(fileName);

            if (inputStream != null) {
                InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);

                bufferedReader.readLine();
                content = bufferedReader.readLine();

                inputStream.close();
            }
        } catch (FileNotFoundException e) {
            Log.e("log activity", "File not found: " + e.toString());
            writeKeys(context);

        } catch (IOException e) {
            Log.e("log activity", "Can not read file: " + e.toString());
        }
        return content;
    }


    public static void saveLog(Context context) {
        if (Reader.history.length() > 0) {
            String currentTimeStamp;
            Toast.makeText(context, context.getString(R.string.log_saved_msg), Toast.LENGTH_SHORT).show();

            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd-HH:mm:ss");
            currentTimeStamp = dateFormat.format(new Date());

            try {
                OutputStreamWriter outputStreamWriter = new OutputStreamWriter(context.openFileOutput("log_" + currentTimeStamp + ".txt", Context.MODE_PRIVATE));

                outputStreamWriter.write(Reader.history);

                outputStreamWriter.close();
            } catch (IOException e) {
            }
        }
    }

    public static String readFile(Context context, String fileName) {
        String content = "";
        try {
            BufferedReader in = new BufferedReader(new FileReader(new File("data/data/com.example.auth/files/" + fileName)));
            StringBuilder builder = new StringBuilder();

            String line = "";

            while((line = in.readLine()) != null)
                builder.append(line + "\n");

            content = builder.toString();

        } catch (FileNotFoundException e) {
            Log.e("log activity", "File not found: " + e.toString());
            writeKeys(context);

        } catch (IOException e) {
            Log.e("log activity", "Can not read file: " + e.toString());
        }
        return content;

    }
}