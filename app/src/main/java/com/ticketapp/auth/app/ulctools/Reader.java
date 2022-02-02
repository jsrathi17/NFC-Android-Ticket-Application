package com.ticketapp.auth.app.ulctools;
/**
 * Developed for Aalto University course CS-E4300 Network Security.
 * Copyright (C) 2021-2022 Aalto University
 */
import android.nfc.tech.MifareUltralight;
import android.nfc.tech.NfcA;
import android.util.Log;
import android.widget.Toast;

import com.ticketapp.auth.R;
import com.ticketapp.auth.app.main.TicketActivity;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

public class Reader {

    public static String history = "";
    public static String authKey = TicketActivity.outer.getString(R.string.default_auth_key);
    public static NfcA nfcA_card;
    public static boolean safeMode = false;
    public static HashMap<Integer, Integer> pageMap = new HashMap<Integer, Integer>();

    public Reader() {
        super();
        makeMap();
    }

    private static void makeMap() {
        // address page -> where to find it / where to write it
        pageMap.put(2, 36);
        pageMap.put(3, 37);
        pageMap.put(40, 38);
        pageMap.put(41, 39);
    }

    /**
     * Read a single page from card.
     *
     * @param page          number of page
     * @param disregardSafe if true, read the real data no matter if safe mode is on
     * @return byte array containing page data
     */
    public static byte[] readPage(int page, boolean disregardSafe) {
        byte[] cmd_read = new byte[]{(byte) 0x30, (byte) 0x00};
        if (!disregardSafe) {
            if (safeMode && pageMap.containsKey(page)) {
                page = pageMap.get(page);
            }
        }
        cmd_read[1] = (byte) page;

        byte[] response = new byte[0];

        try {
            response = nfcA_card.transceive(cmd_read);
        } catch (IOException e) {
            history += "\n\nReading failed - IOException\n";
            history += "Error when reading page " + page + "\n--------------------------------";
        }
        return response;
    }

    /**
     * Read the card memory to a given array with authentication or without authentication.
     * <p/>
     * Commands and responses are stored into history.
     *
     * @param target  byte array where the data is stored
     * @param auth    boolean value whether to authenticate while reading or not
     * @param display boolean value defining whether to show the commands and responses in the console
     * @return boolean value of success
     */
    public static boolean readMemory(byte[] target, boolean auth, boolean display) {
        if (pageMap.isEmpty()) {
            Log.d("P", "pageMap is empty");
            makeMap();
        }
        byte[] cmd_read = new byte[]{(byte) 0x30, (byte) 0x00};
        boolean auth_result = false;
        Arrays.fill(target, (byte) 0x00);
        String type;
        type = getTagType(nfcA_card);
        // If authentication is enabled, do it
        if (auth) {
            if (display) history += "\nauthentication enabled\n";
            // Boolean value: was authentication successful?
            auth_result = authenticate(display);
            if (!auth_result) {
                Log.d("auth", "authentication ended in IOEx");
                disconnect();
                connect();
            }
        }
        if (type.equals("Ultralight C")) {
            if (!auth_result) {
                if (display) history += "\nreading " + type + "\nwithout authentication \n";
            } else if (display) history += "\nreading " + type + "\nwith authentication\n";

        } else {
            if (display) history += "\nreading " + type + "\n";
        }
        for (int i = 0; i < 44; i++) {
            // i is actual page on card that is read, a could be mapped to elsewhere
            // because of safemode
            int a = i;
            // If safemode maps a to somewhere else, get the other page
            if (safeMode && pageMap.containsKey(a)) {
                a = pageMap.get(a);
            }
            // Setup the read command
            cmd_read[1] = (byte) a;

            if (display) history += "\n" + Dump.hex(cmd_read) + " >> ";

            try {
                // If tag is lost, end reading
                if (!connect()) return false;
                // Try to read a page
                byte[] response = nfcA_card.transceive(cmd_read);
                byte[] page = new byte[4];
                if (response.length > 1)
                    page = new byte[]{response[0], response[1], response[2], response[3]};
                // Save page data in the target array
                System.arraycopy(page, 0, target, i * 4, 4);

                if (display) history += "<< " + Dump.hex(page);

            } catch (IOException e) {
                // If the page reading was interrupted because of auth, the rest
                // of the memory will also be unreadable.
                disconnect();
                Toast.makeText(TicketActivity.outer, "Reading ended on page " + i, Toast.LENGTH_SHORT).show();
                history += "\nreading page " + i + " failed - IOException\n";
                history += "\n\nReading finished on " + type + "\n--------------------------------";
                System.out.println("Error when reading page " + 4 * i);
                return false;
            }
        }
        if (display) {
            history += "\n\nReading finished on " + type + "\n--------------------------------";
        }
        return true;
    }

    /**
     * Try to erase card content from page 4 to page 39.
     *
     * @param auth boolean value whether to authenticate before erasing
     * @return boolean value of success
     */
    public static boolean erase(boolean auth) {
        byte[] cmd_erase = new byte[]{(byte) 0xa2, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00};
        boolean isAuth = false;
        boolean reAuth = false;
        int page;
        ArrayList<Integer> faults = new ArrayList<Integer>();
        history += "\nerasing card...\n";
        if (auth) {
            history += "\ntrying to authenticate card before erase...";
            isAuth = authenticate(false);
            if (!isAuth) {
                history += " authentication failed, trying to erase anyway\n";
            } else history += " authentication OK, proceeding with erase\n";
        }
        for (int i = 4; i < 40; i++) {
            page = i;
            cmd_erase[1] = (byte) i;
            try {
                connect();
                if (reAuth && isAuth) {
                    authenticate(false);
                    reAuth = false;
                }
                nfcA_card.transceive(cmd_erase);
            } catch (IOException e) {
                disconnect();
                reAuth = true;
                history += "\nerasing page " + page + " failed - IOException\n";
                faults.add(page);
            }
        }
        String msg;
        if (faults.size() == 0) {
            msg = "Erase successful";
        } else {
            if (faults.size() > 1)
                msg = "Erase partial - pages " + faults + " could not be erased.";
            else msg = "Erase partial - page " + faults.get(0) + " could not be erased.";

        }
        history += "\n" + msg + "\n--------------------------------";

        return true;
    }

    /**
     * Update card data on a defined page.
     * <p/>
     * Commands and responses are stored into history.
     *
     * @param data byte array where the data is stored
     * @param dst  destination page (0 - 47)
     * @param auth boolean value whether to authenticate while writing or not
     * @return boolean value of success
     */
    public static boolean updatePage(byte[] data, int dst, boolean auth) {
        history += "\nwriting...\n";
        int count = 0;
        byte[] cmd_ulwrite = new byte[]{(byte) 0xa2, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00};
        try {
            if (auth) {
                authenticate(false);
            }
            if (safeMode && pageMap.containsKey(dst)) {
                history += "\nSafe mode on, write to page " + dst + "\nmapped to " + pageMap.get(dst) + "\n";
                dst = pageMap.get(dst);
                byte[] current = readPage(dst, true);
                for (int i = 0; i < 4; i++) {
                    data[i] = (byte) ((int) data[i] | (int) current[i]);
                }
            }
            cmd_ulwrite[1] = (byte) dst;
            System.arraycopy(data, 0, cmd_ulwrite, 2, 4);
            history += "\n" + Dump.hex(cmd_ulwrite) + " >>\n";

            byte[] response = nfcA_card.transceive(cmd_ulwrite);
            history += "\n" + Dump.hex(response) + " <<\n";
            history += "\n" + "writing finished\n--------------------------------";
            return true;

        } catch (IOException e) {
            history += "\n" + "Writing failed: IOException\nTrying to write on protected pages without successful authentication?\n--------------------------------";
            System.out.println("Write error at " + count);
        } catch (Exception e) {
            Log.d("e", "sumthin went wong");
        }
        return false;
    }

    /**
     * Lock pages.
     * Locking is limited to pages 4-39 in this assignment.
     * <p/>
     * Locking pages 4-15 is done by giving the page as a parameter, but
     * pages 16-39 (due Ultralight C to specification) are locked in 4 page series:
     * 16-19
     * 20-23
     * 24-27
     * 28-31
     * 32-35
     * 36-39
     * To lock any of the series give the first page as parameter, giving 16 will lock pages 16-19.
     * <p/>
     * No blocking functionality.
     *
     * @param page page to lock
     * @return boolean value of success
     */
    public static boolean lockPage(int page) {
        byte[] lock_data = {(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00};
        int adr;
        int bit = 0;
        int lockbyte;
        boolean status;
        if (page >= 4 && page <= 15) {
            history += "\nSetting lock to page " + page + "\n";
            adr = 2;
            if (page >= 3 && page <= 7) {
                bit = 1 << page;
                lockbyte = 2;
            } else {
                bit = 1 << (page - 8);
                lockbyte = 3;
            }
            lock_data[lockbyte] = (byte) (bit);
            Log.d("Lock", Dump.binary(lock_data[0]) + " " + Dump.binary(lock_data[1]) + " " + Dump.binary(lock_data[2]) + " " + Dump.binary(lock_data[3]));
            Log.d("Lock write", "lock page " + page);
            history += "to lock bits at page 2 (0x02)\n";
            status = updatePage(lock_data, adr, false);
            if (status) {
                history += "\nLocking successful\n--------------------------------\n";
            } else history += "\nLocking failed\n--------------------------------\n";
            return status;
        } else if (page >= 16 && page <= 36) {
            history += "\nSetting lock to pages " + page + "-" + (page + 4) + "\n";
            if (page % 4 != 0) {
                history += "failed. Invalid parameter: " + page + ", when locking pages 16-39,\ngive the starting page (16,20,24,28,32,36)\n";
                return false;
            }
            adr = 40;
            if (page >= 16 && page <= 24) {
                bit = 1 << ((page / 4) - 3);
            } else if (page >= 24 && page <= 36) {
                bit = 1 << ((page / 4) - 2);
            }
            lockbyte = 0;
            lock_data[lockbyte] = (byte) (bit);
            Log.d("Lock", Dump.binary(lock_data[0]) + " " + Dump.binary(lock_data[1]) + " " + Dump.binary(lock_data[2]) + " " + Dump.binary(lock_data[3]));
            Log.d("Lock write", "lock page " + page);
            history += "to lock bits at page 40 (0x28)\n";
            status = updatePage(lock_data, adr, false);
            if (status) {
                history += "\nLocking successful\n--------------------------------\n";
            } else history += "\nLocking failed\n--------------------------------\n";
            return status;
        }

        return false;
    }

    /**
     * Change the stored authentication key of the reader, which is used when authenticating the card.
     *
     * @param newKey String value of the new authentication key. Starting with "0x" if it is in hexadecimal.
     * @return boolean value telling if the key was changed correctly.
     */
    public static boolean setAuthKey(String newKey) {
        if (newKey.regionMatches(0, "0x", 0, 2) && newKey.length() == 18 || newKey.length() == 16) {
            history += "\n" + "authentication key changed\n" + "old: " + authKey + "\nnew: " + newKey + "\n--------------------------------";
            authKey = newKey;
            return true;
        } else return false;
    }

    /**
     * Helper method for getting a byte array of the String key given as a parameter.
     *
     * @param key String value of the key
     * @return byte array of the key
     */
    private static byte[] getNumeralKey(String key) {
        byte[] r = new byte[16];
        for (int i = 2; i < 18; i++) {
            r[i - 2] = (byte) Integer.parseInt("" + key.charAt(i));
        }
        return r;
    }

    /**
     * Helper method for getting a correctly ordered array of the key given as a parameter.
     * The ordering for default key "BREAKMEIFYOUCAN!" is "IEMKAERB!NACUOYF"
     *
     * @param key String value of the key
     * @return byte array of the key
     */
    private static byte[] getKey(String key) {
        byte[] r = new byte[16];
        if (key.regionMatches(0, "0x", 0, 2) && key.length() == 18) {
            r = getNumeralKey(key);
            return r;
        } else if (key.length() == 16) {
            r = key.getBytes();
            return r;
        } else
            return r;
    }

    private static byte[] getFormattedByteKey(byte[] byteKey) {
        byte[] r = new byte[16];
        byte[] k1 = new byte[8];
        byte[] k2 = new byte[8];
        byte[] k1_reverse = new byte[8];
        byte[] k2_reverse = new byte[8];
        System.arraycopy(byteKey, 0, k1, 0, 8);
        System.arraycopy(byteKey, 8, k2, 0, 8);
        for (int i = 0; i < 8; i++) {
            k1_reverse[i] = k1[7 - i];
            k2_reverse[i] = k2[7 - i];
        }
        System.arraycopy(k1_reverse, 0, r, 0, 8);
        System.arraycopy(k2_reverse, 0, r, 8, 8);
        return r;
    }

    /**
     * Test authenticate with the current key.
     */
    public static boolean testAuthenticate() {
        if (connect()) {
            boolean result = authenticate(true);
            if (result)
                Toast.makeText(TicketActivity.outer, "Authentication succeeded", Toast.LENGTH_SHORT).show();
            else
                Toast.makeText(TicketActivity.outer, "Authentication failed", Toast.LENGTH_SHORT).show();
            disconnect();
            return result;
        } else return false;

    }

    /**
     * Create an authenticated session with the card
     * <p/>
     * Key that is used is stored in Reader class variable "authKey"
     * <p/>
     * To just authenticate and not use stored key use method authenticate(byte[] key) instead.
     *
     * @param display boolean value defining whether to show the commands and responses in the console
     * @return boolean value telling if the authentication worked
     */
    public static boolean authenticate(boolean display) {
        if (display) history += "\nkey: \n" + authKey + "\n";
        byte[] byteKey = getKey(authKey);

        return authenticate(byteKey, display);
    }

    /**
     * Create an authenticated session with the card with a key given as parameter.
     *
     * @param input_key byte array containing the authentication key
     * @param display   boolean value defining whether to show the commands and responses in the console
     * @return boolean value telling if the authentication worked
     */
    public static boolean authenticate(byte[] input_key, boolean display) {
        if (display)
            history += "\n" + "authenticating with key: (in hex) \n" + Dump.hex(input_key, true) + "\n";

        byte[] byteKey = getFormattedByteKey(input_key);

        byte[] iv1 = {0, 0, 0, 0, 0, 0, 0, 0};
        byte[] key = new byte[24];
        String str = "";

        System.arraycopy(byteKey, 0, key, 0, 16);
        System.arraycopy(byteKey, 0, key, 16, 8);

        try {
            // message exchange 1
            byte[] cmd_auth = new byte[]{0x1A, 0x00};

            str = "cmd_auth sent";
            byte[] response1 = nfcA_card.transceive(cmd_auth);
            if (display)
                Reader.history += "\n>>\n" + Dump.hex(cmd_auth) + "\n\n" + Dump.hex(response1) + " <<\n\n";

            byte[] enc_randB = new byte[8];
            System.arraycopy(response1, 1, enc_randB, 0, 8);
            byte[] randB = TripleDES.decrypt(iv1, key, enc_randB);

            if (display) Reader.history += "randB:\n" + Dump.hex(randB) + "\n\n";

            byte[] randA = new byte[8];
            SecureRandom g = new SecureRandom();
            g.nextBytes(randA);

            if (display) Reader.history += "randA:\n" + Dump.hex(randA) + "\n\n";

            byte[] randCon = new byte[16];
            System.arraycopy(randA, 0, randCon, 0, 8);
            System.arraycopy(randB, 1, randCon, 8, 7);
            System.arraycopy(randB, 0, randCon, 15, 1);

            byte[] enc_randCon = TripleDES.encrypt(enc_randB, key, randCon);

            // prepare concat
            byte[] cmd_con = new byte[17];
            cmd_con[0] = (byte) 0xAF;
            System.arraycopy(enc_randCon, 0, cmd_con, 1, 16);

            str = "cmd_con sent";
            byte[] response2 = nfcA_card.transceive(cmd_con);

            if (display)
                Reader.history += "\n>>\n" + Dump.hex(cmd_con) + "\n\n" + Dump.hex(response2) + " <<\n\n";

            if (response2[0] + response2[1] == 0x00) {
                if (display) {
                    Reader.history += "\nAuthentication failed. Wrong key?\n";
                    //Toast.makeText(MyActivity.outer, "Authentication failed", Toast.LENGTH_SHORT).show();
                }
                return false;
            }

            //  verify received randA
            byte[] iv3 = new byte[8];
            System.arraycopy(cmd_con, 9, iv3, 0, 8);
            byte[] enc_randAp = new byte[8];
            System.arraycopy(response2, 1, enc_randAp, 0, 8);

            byte[] dec_randAp = TripleDES.decrypt(iv3, key, enc_randAp);
            byte[] dec_randA = new byte[8];
            System.arraycopy(dec_randAp, 0, dec_randA, 1, 7);
            dec_randA[0] = dec_randAp[7];
            for (int i = 0; i < 8; i++) {
                if (dec_randA[i] != randA[i]) {
                    return false;
                }
            }
            if (display) {
                Reader.history += "decrypted randA:\n" + Dump.hex(dec_randA) + "\nmatches randA\n";
            }
            if (display) {
                Reader.history += "\nAuthentication OK\n--------------------------------\n";
            }
            return true;

        } catch (IndexOutOfBoundsException i) {
            Reader.history += "\nAuthentication failed. Wrong key?\n";
            //Toast.makeText(MyActivity.outer, "Authentication failed", Toast.LENGTH_SHORT).show();
            Log.d("E", i.toString());
            return false;
        } catch (IOException e) {
            Reader.history += "\nAuthentication failed. Wrong key?\n";
            //Toast.makeText(MyActivity.outer, "Authentication failed", Toast.LENGTH_SHORT).show();
            TicketActivity.autoAuth = false;
            Log.d("Exception", "IOException at " + str);
            return false;
        }
    }

    /**
     * Helper method for getting the type of a card.
     *
     * @param card NfcA card to check.
     * @return String value "unknown type", "Ultralight" or "Ultralight C"
     */
    private static String getTagType(NfcA card) {
        int typeID = MifareUltralight.get(card.getTag()).getType();
        String type = "";
        Log.d("Type", "" + typeID);
        switch (typeID) {
            case -1:
                type = "unknown type";
                break;
            case 1:
                type = "Ultralight";
                break;
            case 2:
                type = "Ultralight C";
                break;
        }
        return type;
    }

    public static boolean connect() {
        try {
            if (!nfcA_card.isConnected()) {
                nfcA_card.connect();
            }
            return true;
        } catch (IOException i) {
            i.printStackTrace();
            //Toast.makeText(MyActivity.outer, "Card not found", Toast.LENGTH_SHORT).show();
            return false;
        }
    }

    public static boolean disconnect() {
        try {
            nfcA_card.close();
            return true;
        } catch (IOException i) {
            i.printStackTrace();
            return false;
        }
    }

}
