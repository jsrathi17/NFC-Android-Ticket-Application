package com.ticketapp.auth.ticket;

import android.os.Build;
import android.support.annotation.RequiresApi;
import android.util.Log;

import java.nio.ByteOrder;
import java.security.MessageDigest;
import com.ticketapp.auth.R;
import com.ticketapp.auth.app.main.TicketActivity;
import com.ticketapp.auth.app.ulctools.Commands;
import com.ticketapp.auth.app.ulctools.Utilities;

import java.security.GeneralSecurityException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.time.*;
import java.util.Date;
import java.util.Calendar;



/**
 * TODO:
 * Complete the implementation of this class. Most of the code are already implemented. You
 * will need to change the keys, design and implement functions to issue and validate tickets. Keep
 * you code readable and write clarifying comments when necessary.
 */
public class Ticket {

    /** Default keys are stored in res/values/secrets.xml **/
    private static final byte[] defaultAuthenticationKey = TicketActivity.outer.getString(R.string.default_auth_key).getBytes();
    private static final byte[] defaultHMACKey = TicketActivity.outer.getString(R.string.default_hmac_key).getBytes();

    /** TODO: Change these according to your design. Diversify the keys. */
    private static final byte[] authenticationKey = defaultAuthenticationKey; // 16-byte key
    private static final byte[] hmacKey = defaultHMACKey; // 16-byte key

    public static byte[] data = new byte[192];

    private static TicketMac macAlgorithm; // For computing HMAC over ticket data, as needed
    private static Utilities utils;
    private static Commands ul;



    private final Boolean isValid = false;
    private final int remainingUses = 0;
    private final int expiryTime = 0;

    private static byte [] masterMac="@networksecurity".getBytes();

    private static String infoToShow = "-"; // Use this to show messages

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void writeExpiryTime()
    {
        LocalDateTime current_Time=java.time.LocalDateTime.now().plusDays(5);
        DateTimeFormatter myFormatObj = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");
        String current_Time_format = current_Time.format(myFormatObj);
        byte[] message = current_Time_format.getBytes();
        boolean res;
        res = utils.writePages(message, 0, 34, 3);
        if (res) {
            infoToShow = "Wrote: " + new String(message);
        } else {
            infoToShow = "Failed to write";
        }
    }

    private void loadExpiryTimeToCard(){
        Date date = new Date(); // your date
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.add(Calendar.DAY_OF_MONTH,1);
        int year = cal.get(Calendar.YEAR);
        int month = cal.get(Calendar.MONTH);
        int day = cal.get(Calendar.DAY_OF_MONTH);
        byte[] dateByte = {(byte)(year - 2000), (byte)(month + 1), (byte)day, (byte)0};
        utils.writePages(dateByte, 0, 34, 1 );
    }


    private byte[] getCurrentTime(){
        Date date = new Date(); // your date
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        int year = cal.get(Calendar.YEAR);
        int month = cal.get(Calendar.MONTH);
        int day = cal.get(Calendar.DAY_OF_MONTH);
        byte[] dateByte = {(byte)(year - 2000), (byte)(month + 1), (byte)day, (byte)0};
        return dateByte;
    }

    public boolean checkwithNumberoftickets()
    {
        boolean res;
        byte[] message_41 = new  byte[4];
        byte[] message_38 = new  byte[4];
        byte[] message_39 = new  byte[4];
        message_41 = intToByte(1);
        res = utils.readPages(39, 1, message_39, 0);

        if (res) {
            byte[] current_counter = new byte[4];
            res = utils.readPages(41, 1, current_counter, 0);
            res = utils.readPages(39, 1, message_39, 0);
            res = utils.readPages(38, 1, message_38, 0);

            if (res) {
                //if (bytesToInt(current_counter))-message_39<message_38{
                if (bytesToInt(current_counter)-bytesToInt(message_39)<bytesToInt(message_38)){
                    res = utils.writePages(message_41, 0, 41, 1);

                    //infoToShow = "rides: "+  currentRides(bytesToInt(message_38))+" initial_counter: "+  bytesToInt(message_39)+" current_counter: "+  bytesToInt(current_counter)+" VALIDATED";
                    infoToShow = "Ticket VALIDATED! Remaining rides:" + (int)currentRides(bytesToInt(message_38));

                    return true;
                }
                else {
                    //infoToShow ="rides: "+  currentRides(bytesToInt(message_38))+" initial_counter: "+  bytesToInt(message_39)+" current_counter: "+  bytesToInt(current_counter)+" NOT VALIDATED";
                    infoToShow = "Ticket NOT VALIDATED! Remaining rides: 0. Please issue more tickets.";
                }
            }


        }
        return false;
    }

    public float currentRides(int rides){
        byte[] initial_counter = new byte[4];
        byte[] current_counter = new byte[4];
//        byte[] rides = new byte[4];
        boolean res;
        res = utils.readPages(39, 1, initial_counter, 0);
        res = utils.readPages(41, 1, current_counter, 0);
//        res = utils.readPages(38, 1, rides, 0);
        Log.d("asdfg", String.valueOf(rides));
        Log.d("asdfg1", String.valueOf(bytesToInt(current_counter)));
        Log.d("asdfg12", String.valueOf(bytesToInt(initial_counter)));
        return (rides-(bytesToInt(current_counter)-bytesToInt(initial_counter)));

    }


    public void issueTickets(){

        byte[] issue_ticket = new byte[4];
        byte[] current_rides = new byte[4];
        int rides;
        issue_ticket = intToByte(5);
        boolean res;


        res = utils.readPages(38, 1, current_rides, 0);
        if (res){
            rides=bytesToInt(current_rides);
            byte[] initial_counter = new byte[4];
            byte[] current_counter = new byte[4];

            res = utils.readPages(39, 1, initial_counter, 0);
            res = utils.readPages(41, 1, current_counter, 0);

            float noofrides= currentRides(rides);

            rides= (int) noofrides+5;
            writeInitialcounter();
            res = utils.writePages(intToByte(rides), 0, 38, 1);
            if (res){
                infoToShow = "You have issued 5 tickets! Available tickets:" + String.valueOf(rides);
                //infoToShow="You get five more tickets " + String.valueOf(rides)+ " current rides:"+"  initial counter:"+bytesToInt(initial_counter)+" current_counter:"+bytesToInt(current_counter)+"+5thing"+rides+" memory rides:"+bytesToInt(current_rides);
                Log.d("rides","you get 5 more rides");
                //expiry time
                if (res){
//                    infoToShow="written 0 to page 36 expiry time" + String.valueOf(rides);
                    Log.d("expiry time","written 0 to page 36 expiry time");
                }
                //issue time
                byte[] issueTime;
                issueTime=getCurrentTime();
                res = utils.writePages(issueTime, 0, 37, 1);
                if (res){
//                    infoToShow="written issue time to page 37" + String.valueOf(rides);
                    Log.d("issue time","written issue time to page 37");
                }
            }
        }
    }

    private byte[] getSha256Hash(byte[] secretmessage){
        byte[] res = null;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            res = digest.digest(secretmessage);

        }
        catch(Exception e){
            e.printStackTrace();
        }
        return res;
    }

    //creates and returns new key from uid and secret message
    private byte[] NewKey(){
        //return resetKey;
        byte[] uid = new byte[12];
        utils.readPages(0, 3, uid, 0);
        uid = Arrays.copyOf(uid, 9);
        byte[] SecretMessage = addAll(uid, "Marjays".getBytes());
        byte[]  new_Key = getSha256Hash(SecretMessage);
        return  truncated(new_Key);

    }

    private byte[] NewKeyforMac(){
        //return resetKey;
        byte[] uid = new byte[12];
        utils.readPages(0, 3, uid, 0);
        uid = Arrays.copyOf(uid, 9);
        byte[] SecretMessage = addAll(uid, "NETSEC0".getBytes());
        byte[]  new_Key = getSha256Hash(SecretMessage);
        return  truncated(new_Key);

    }

    //formatting the card for the first time with application tag, version
    //Formatting the card with Newkey, and write the initial value of counter to the card
    private void formatthecardfisttime() {

        byte[] key = NewKey();
        boolean res = utils.writePages(key, 0, 44, 4);
        if (res) {
            infoToShow = "Formatted the card with new key" + new String("KEY OK");
        } else {
            Log.d("TAG:", "Cannot format the card with new key");
            infoToShow = "Failed to write key to the Card";
        }
        byte[] TAG = "APPT".getBytes();
        byte[] VERSION = {(byte) 223, (byte) 0, (byte) 0, (byte) 0};
        utils.writePages(TAG, 0, 36, 1);
        utils.writePages(VERSION, 0, 35, 1);
        utils.writePages(intToByteArray(4),0,42,1);
        utils.writePages(intToByteArray(1),0,43,1);

//        byte[] initial_counter = new byte[4];
//        res = utils.readPages(41, 1, initial_counter, 0);
//        Log.d("Reading initial counter", "Writing initial counter");
//
//        if (res) {
//            //writing initial counter to page 39
//            res = utils.writePages(initial_counter, 0, 39, 1);
//        }
    }
    private boolean firstUseFlag()
    {
        boolean res;
        byte[] read = new byte[4];
        res = utils.readPages(36, 1, read, 0);
        if (res && bytesToInt(read)==0) {
            return true;
        }
        return false;
    }


    private  boolean checkExpiryDate(){

        byte[] dateInCard = new byte[4];
        utils.readPages(34,1,dateInCard,0);
        Date date = new Date(); // your date
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        int year = cal.get(Calendar.YEAR);
        int month = cal.get(Calendar.MONTH);
        int day = cal.get(Calendar.DAY_OF_MONTH);
        byte[] now = {(byte)(year - 2000), (byte)(month + 1), (byte)day, (byte)0};
        return !compareArraySmallerOrEqual(now, dateInCard); //true if expired

    }

    private  boolean compareArraySmallerOrEqual(byte[] a1, byte[] a2){
        try {
            for (int i = 0; i < a1.length; i++){
                if (a1[i] > a2[i])
                    return false;
            }
        }
        catch (ArrayIndexOutOfBoundsException e){
            e.printStackTrace();
        }
        return true;
    }

    private byte[] intToByte(int counter)
    {
        //byte[] counterByte = {(byte)((counter >> 8) & 0xff), (byte)(counter & 0xff)};
        byte[] counterByte = {(byte)(counter  & 0xff), (byte)((counter >> 8 ) & 0xff)};
        byte[] zero = {(byte)0, (byte)0};
        counterByte = addAll(counterByte,zero);
        return counterByte;
    }


    private int bytesToInt(byte[] a){
        //return ((int)a[0] << 8)  + (int)a[1];
        return ((int)a[1] << 8)  + (int)a[0];
    }

    public static byte[] intToByteArray(int number) {
        return ByteBuffer.allocate(Integer.SIZE / 8).order(ByteOrder.LITTLE_ENDIAN).putInt(number).array();
    }

    private byte[] addAll(byte[] a1, byte[] a2){
        byte[] res = new byte[a1.length + a2.length];
        for(int i = 0; i < res.length;i++){
            if (i < a1.length)
                res[i] = a1[i];
            else
                res[i] = a2[i-a1.length];
        }
        return res;
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////
    //MAC//
    ////////////////////////////////////////////////////////////////////////////////////////////////
    private boolean compareArray(byte[] a1, byte[] a2) {
        return Arrays.equals(a1, a2);
    }

    private byte[] truncated(byte[] data){
        byte[] res = Arrays.copyOf(data, data.length / 2);
        return res;
    }

    private  byte[] calculateMac() throws GeneralSecurityException {
        byte[] data = new byte[20];
        boolean res = utils.readPages(35, 5, data, 0);
        if(res){
            Log.d("TAG:", "readMac " + new Integer(data.length).toString());
        }
        byte[] diverse_key = NewKeyforMac();
        macAlgorithm.setKey(diverse_key);
        byte[] mac = macAlgorithm.generateMac(data);
        Log.d("TAG111:", "data length: " + new Integer(mac.length));
//        byte[] trunctedMac = truncated(truncated(truncated(mac)));
        byte[] trunctedMac = Arrays.copyOfRange(data, 0, 4);
        Log.d("TAG1233:" ,new Integer(trunctedMac.length).toString());
        if (res){
            Log.d("MAC:" ,"written mac to page 33");
        }
        return trunctedMac;

    }

    private  byte[] calculateMacBackup() throws GeneralSecurityException {
        byte[] data = new byte[24];
        boolean res = utils.readPages(34, 6, data, 0);
        if(res){
            Log.d("TAG:", "readMac " + new Integer(data.length).toString());
        }

        macAlgorithm.setKey(masterMac);
        byte[] mac = macAlgorithm.generateMac(data);
        Log.d("TAG111:", "data length: " + new Integer(mac.length));
//        byte[] trunctedMac = truncated(truncated(truncated(mac)));
        byte[] trunctedMac = Arrays.copyOfRange(data, 0, 4);
        Log.d("TAG1233:" ,new Integer(trunctedMac.length).toString());

        if (res){
            Log.d("MAC:" ,"written mac to page 33");
        }
        return trunctedMac;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    //MAC//
    ////////////////////////////////////////////////////////////////////////////////////////////////

    /** Create a new ticket */
    public Ticket() throws GeneralSecurityException {
        // Set HMAC key for the ticket
        macAlgorithm = new TicketMac();
        macAlgorithm.setKey(hmacKey);

        ul = new Commands();
        utils = new Utilities(ul);
    }

    /** After validation, get ticket status: was it valid or not? */
    public boolean isValid() {
        return isValid;
    }

    /** After validation, get the number of remaining uses */
    public int getRemainingUses() {
        return remainingUses;
    }

    public void ticketValidation() throws GeneralSecurityException {
        byte[] message_38 = new byte[4];
        byte[] initial_counter = new byte[4];
        byte[] current_counter = new byte[4];
        boolean res = utils.readPages(38, 1, message_38, 0);
        boolean res1 = utils.readPages(39, 1, initial_counter, 0);
        boolean res2 = utils.readPages(41, 1, current_counter, 0);
        Log.d("read 38","true");

        if (res) {
            byte[] message_39 = new byte[4];

            if ((bytesToInt(initial_counter))==(bytesToInt(current_counter)))
            {
                Log.d("firstUseFlag","TRue");
                //write the first use in to the memory page 36
                loadExpiryTimeToCard();
                Log.d("loadExpiryTimeToCard","TRue");
                res = utils.writePages(intToByte(1), 0, 41, 1);
                infoToShow = "Validated first time. Your rides will expire in 1 day. Remaining rides:"+  (int)currentRides(bytesToInt(message_38));
                byte [] trunctedMac=calculateMacBackup();
                res = utils.writePages(trunctedMac, 0, 32, 1);
            }
            else{
                byte [] val_mac_backup=calculateMacBackup();
                byte [] memory_mac_backup=new byte[4];
                res = utils.readPages(32, 1, memory_mac_backup, 0);
                Log.d("val_mac_backup", String.valueOf(bytesToInt(val_mac_backup)));
                Log.d("memory_mac_backup", String.valueOf(bytesToInt(memory_mac_backup)));

                if (bytesToInt(val_mac_backup)!=bytesToInt(memory_mac_backup)){
                    byte [] memory_mac=new byte[4];
                    byte [] val_mac=calculateMac();
                    res = utils.readPages(33, 1, memory_mac, 0);

                    if (bytesToInt(val_mac)==bytesToInt(memory_mac)){
                        val_function();

                    }
                    else{
//                        formatthecardfisttime();
                        infoToShow="card has been conpromised, formatting";
                    }

                }
                else{
//                    infoToShow="val mac==memory mac backup";
                    Log.d("macbackup", "equal");
                    val_function();

                }


            }
        }
    }

    private void writeInitialcounter(){
        byte[] initial_counter = new byte[4];
        Log.d("Reading initial counter", "Writing initial counter");
        boolean res;

        //writing initial counter to page 39
        res = utils.readPages(41, 1, initial_counter, 0);

        res = utils.writePages(initial_counter, 0, 39, 1);
    }


    private void val_function(){
        //calculate backup mac
        byte[] data = new byte[20];


        // check the expiry time by comparing the first use time and the current
        //checkValidityofTickets();
        if (!checkExpiryDate()){ //false is not expired
            boolean ticket;
//            infoToShow = "move forward card not expired";
            ticket=checkwithNumberoftickets();

        }
        else{
//            infoToShow="expired";
        }
    }
    /** After validation, get the expiry time */
    public int getExpiryTime() {
        return expiryTime;
    }

    /** After validation/issuing, get information */
    public static String getInfoToShow() {
        return infoToShow;
    }
    private boolean authNewKey(){
        byte[] key = NewKey();
        Log.d("Authentication" ,"trying from new key");
        boolean res = utils.authenticate(key) ;
        if (res){
            Log.d("TAG:", "Authenticated");
            return true;
        } else {
            infoToShow = "Failed to authenticate";
            return false;
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public boolean issue(int daysValid, int uses) throws GeneralSecurityException {
        boolean res;
        res = utils.authenticate(authenticationKey);
        if (res) {
            formatthecardfisttime();
            Log.d("Authentication", "Formatting first time");
            writeInitialcounter();


            return true;
        }
        if (!authNewKey())
        {
            Log.d("Authentication", "failed from default key:");
            Utilities.log("Authentication failed in issue()", true);
            infoToShow = "Authentication failed";
            Log.d("Authentication", "failed from new key:");
            return false;
        }

        issueTickets();
        //TODO: set AUTH0 AND AUTH1 PARAMETER

        byte[] mac = calculateMac();

        res = utils.writePages(mac, 0, 33, 1);

        if (res) {
            Log.d("MAC:", "Writing Backup MAC");
//            infoToShow="writing mac to 33";
        }

        return true;

    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public boolean use() throws GeneralSecurityException {
        boolean res;

        // Authenticate
        res = utils.authenticate(authenticationKey);
        if (res) {
            infoToShow = "You have to issue the tickets first!";
            return false;
        }

        res = authNewKey();
        if (!res)
        {
//            infoToShow = "Failed to authenticate!";
            return false;
        }
//        res = utils.writePages(intToByte(0), 0, 32, 1);
//        res = utils.writePages(intToByte(0), 0, 33, 1);
//        res = utils.writePages(intToByte(0), 0, 34, 1);
//        res = utils.writePages(intToByte(0), 0, 35, 1);
//        res = utils.writePages(intToByte(0), 0, 36, 1);
//        res = utils.writePages(intToByte(0), 0, 37, 1);
//        res = utils.writePages(intToByte(0), 0, 38, 1);
//        writeInitialcounter();

//        res = utils.writePages(intToByte(0), 0, 38, 1);
//        if (res){
//            infoToShow = "done zeros";
//        }

///////////////////////////////////////////////////////////////////////////////////////////////////
//        ok
///////////////////////////////////////////////////////////////////////////////////////////////////

        ticketValidation();
//
//        formatthecardfisttime();


        return true;
    }

}
