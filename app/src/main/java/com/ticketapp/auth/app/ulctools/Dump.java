package com.ticketapp.auth.app.ulctools;

/**
 * Dump data in hexadecimal and other formats.
 *
 * @author Daniel Andrade
 */
public class Dump {

    /**
     * Hex dump of the argument.
     *
     * @param b a byte
     * @return hex dump of the byte
     */
    public static String hex(byte b) {
        return String.format("%02x", b);
    }

    /**
     * Hex dump a byte array. A space is added between bytes.
     *
     * @param a the byte array
     * @return the hex dump
     */
    public static String hex(byte[] a) {
        return hex(a, true);
    }

    /**
     * Hex dump a byte array with or without spaces in between bytes.
     *
     * @param a     the byte array
     * @param space <code>true</code> to include a space between values
     * @return the hexadecimal representation of the byte array
     */
    public static String hex(byte[] a, boolean space) {
        StringBuilder sb = new StringBuilder();

        if (space) {
            int x = 0;
            for (byte b : a) {
                x += 1;
                sb.append(hex(b).toUpperCase());
                if (x < 4)
                    sb.append(" ");
                if (x == 4) {
                    x = 0;
                    sb.append("\n");
                }
            }
            if (sb.length() > 0) {
                sb.deleteCharAt(sb.length() - 1);
            }
        } else {
            for (byte b : a) {
                sb.append(hex(b));
            }
        }

        return sb.toString();
    }

    /**
     * @param a byte
     * @return byte a as binary string
     */
    public static String binary(byte a) {
        String r = String.format("%8s", Integer.toBinaryString(a & 0xFF)).replace(' ', '0');
        return r;
    }

    /**
     * Get the byte array as string data formatted to be shown to users.
     *
     * @param a        the byte array
     * @param mode int value: 0 means hex, 1 means binary, 2 means both
     * @return formatted String data
     */
    public static String hexView(byte[] a, int mode) {
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < a.length / 4; i++) {
            byte[] row = new byte[4];
            System.arraycopy(a, i * 4, row, 0, 4);
            String number = "";
            String hex = "";
            String ascii = "";
            String asciiSep = "|";
            String binary = "";
            String data = "";

            if (i < 10) number += "0" + i + ": ";
            else number += i + ": ";

            for (byte b : row) {
                hex += (hex(b).toUpperCase() + " ");
                String asciiPart = "";
                char c = (char) Integer.parseInt(hex(b), 16);
                if (Character.isISOControl(c) || c >= 128) {
                    asciiPart = ".";
                } else {
                    asciiPart = String.format("%c", c);
                }
                ascii += asciiPart;
                binary += (binary(b) + " ");
            }
            if (Reader.safeMode && Reader.pageMap.containsValue(i)) {
                ascii = "safe";
            }
            if (mode == 0) data += number + hex + asciiSep + ascii + asciiSep + "\n";
            else if (mode == 1) data += number + binary + asciiSep + ascii + asciiSep + "\n";
            else if (mode == 2) data += number + hex + binary + asciiSep + ascii + asciiSep + "\n";

            sb.append(data);
        }

        return sb.toString();
    }
}