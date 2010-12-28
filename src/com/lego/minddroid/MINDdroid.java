/**
 * Copyright 2010 Guenther Hoelzl, Shawn Brown
 *
 * This file is part of MINDdroid.
 *
 * MINDdroid is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * MINDdroid is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * MINDdroid. If not, see <http://www.gnu.org/licenses/>.
 **/

package com.lego.minddroid;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.Toast;

import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.ListIterator;

import com.lego.minddroid.GameConnector.GameThread;

public class MINDdroid extends Activity {
    public static final int UPDATE_TIME = 200;
    public static final int MENU_TOGGLE_CONNECT = Menu.FIRST;
    public static final int MENU_QUIT = Menu.FIRST + 1;
    private static final int REQUEST_CONNECT_DEVICE = 1000;
    private static final int REQUEST_ENABLE_BT = 2000;
    private BTCommunicator myBTCommunicator = null;
    private Toast reusableToast;
    private boolean connected = false;
    private ProgressDialog connectingProgressDialog;
    private Handler btcHandler;
    private Menu myMenu;
    private GameConnector mConnector;
    private View mView;
    private GameThread mThread;
    private Activity thisActivity;
    private boolean bt_error_pending = false;
    boolean pairing;
    private static boolean btOnByUs = false;
    int mRobotType;
    int motorLeft;
    private int directionLeft; // +/- 1
    int motorRight;
    private boolean stopAlreadySent = false;
    private int directionRight; // +/- 1
    private int motorAction;
    private int directionAction; // +/- 1
    private List<String> programList;
    private static final int MAX_PROGRAMS = 20;
    private String programToStart;
    
    public static final String GAME_CONTROLLER = "GAME_CONTROLLER";

    public static boolean isBtOnByUs() {
        return btOnByUs;
    }

    public static void setBtOnByUs(boolean btOnByUs) {
        MINDdroid.btOnByUs = btOnByUs;
    }

    /**
     * Called when the activity is first created. Inititializes all the
     * graphical views.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        thisActivity = this;
        mRobotType = this.getIntent().getIntExtra(SplashMenu.MINDDROID_ROBOT_TYPE, R.id.robot_type_1);
        setUpByType();

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        StartSound mySound = new StartSound(this);
        mySound.start();
        // setup our view, give it focus and display.
        String controllerType = this.getIntent().getStringExtra(GAME_CONTROLLER);
        if (GameConnector.TOUCH.equals(controllerType)) {
        	mConnector = new TouchGameConnector(getApplicationContext(), this);
        } else {
        	mConnector = new TiltGameConnector(getApplicationContext(), this);
        }
        
        mView = mConnector.getView();
        mThread = mConnector.getThread();
        mView.setFocusable(true);
        setContentView(mView);
        reusableToast = Toast.makeText(this, "", Toast.LENGTH_SHORT);

    }

    private void setUpByType() {
        switch (mRobotType) {
            case R.id.robot_type_2:
                motorLeft = BTCommunicator.MOTOR_A;
                directionLeft = 1;
                motorRight = BTCommunicator.MOTOR_C;
                directionRight = 1;
                motorAction = BTCommunicator.MOTOR_B;
                directionAction = 1;
                break;
            case R.id.robot_type_3:
                motorLeft = BTCommunicator.MOTOR_C;
                directionLeft = -1;
                motorRight = BTCommunicator.MOTOR_B;
                directionRight = -1;
                motorAction = BTCommunicator.MOTOR_A;
                directionAction = 1;
                break;
            default:
                // default - robot_type_1
                motorLeft = BTCommunicator.MOTOR_B;
                directionLeft = 1;
                motorRight = BTCommunicator.MOTOR_C;
                directionRight = 1;
                motorAction = BTCommunicator.MOTOR_A;
                directionAction = 1;
                break;
        }
    }

    private void updateButtonsAndMenu() {

        if (myMenu == null)
            return;

        myMenu.removeItem(MENU_TOGGLE_CONNECT);

        if (connected) {
            myMenu.add(0, MENU_TOGGLE_CONNECT, 1, getResources().getString(R.string.disconnect)).setIcon(R.drawable.ic_menu_connected);

        } else {
            myMenu.add(0, MENU_TOGGLE_CONNECT, 1, getResources().getString(R.string.connect)).setIcon(R.drawable.ic_menu_connect);
        }

    }

    public void createBTCommunicator() {
        // interestingly BT adapter needs to be obtained by the UI thread - so we pass it in in the constructor
        myBTCommunicator = new BTCommunicator(this, myHandler, BluetoothAdapter.getDefaultAdapter());
        btcHandler = myBTCommunicator.getHandler();
    }

    public void startBTCommunicator(String mac_address) {

        connectingProgressDialog = ProgressDialog.show(this, "", getResources().getString(R.string.connecting_please_wait), true);

        if (myBTCommunicator == null) {
            createBTCommunicator();
        }

        switch (((Thread) myBTCommunicator).getState()) {
            case NEW:
                myBTCommunicator.setMACAddress(mac_address);
                myBTCommunicator.start();
                break;

                //case RUNNABLE:  //already running - but this can't be in good state after failed connection due to request to pair
                //break;

            default:
                connected=false;
                myBTCommunicator = null;
                createBTCommunicator();
                myBTCommunicator.setMACAddress(mac_address);
                myBTCommunicator.start();
                break;
        }

        updateButtonsAndMenu();

    }

    public void destroyBTCommunicator() {

        if (myBTCommunicator != null) {
            sendBTCmessage(BTCommunicator.NO_DELAY, BTCommunicator.DISCONNECT, 0, 0);
            myBTCommunicator = null;
        }

        connected = false;
        updateButtonsAndMenu();
    }

    public boolean isConnected() {
        return connected;
    }

    public void actionButtonPressed() {
        if (myBTCommunicator != null) {
            mThread.mActionPressed = true;
            // depending on what the robot should when pressing the action button
            // you have to uncomment/comment one of the following lines

            // sendBTCmessage(BTCommunicator.NO_DELAY, BTCommunicator.DO_ACTION, 0, 0);

            // Wolfgang Amadeus Mozart "Zauberfloete - Der Vogelfaenger bin ich ja"
            sendBTCmessage(BTCommunicator.NO_DELAY, BTCommunicator.DO_BEEP, 392, 100);
            sendBTCmessage(200, BTCommunicator.DO_BEEP, 440, 100);
            sendBTCmessage(400, BTCommunicator.DO_BEEP, 494, 100);
            sendBTCmessage(600, BTCommunicator.DO_BEEP, 523, 100);
            sendBTCmessage(800, BTCommunicator.DO_BEEP, 587, 300);
            sendBTCmessage(1200, BTCommunicator.DO_BEEP, 523, 300);
            sendBTCmessage(1600, BTCommunicator.DO_BEEP, 494, 300);

            // MOTOR ACTION: forth an back
            switch (mRobotType) {
                case R.id.robot_type_3:
                    for (int bite=0; bite<3; bite++) {
                        sendBTCmessage(bite*400, motorAction, 75 * directionAction, 0);
                        sendBTCmessage(bite*400+200, motorAction, -75 * directionAction, 0);
                    }    
                    sendBTCmessage(3*400, motorAction, 0, 0);
                    break;
            
                default:
                    sendBTCmessage(BTCommunicator.NO_DELAY, motorAction, 75 * directionAction, 0);
                    sendBTCmessage(500, motorAction, -75 * directionAction, 0);
                    sendBTCmessage(1000, motorAction, 0, 0);
                    break;
            }
        }
    }

    public void actionButtonLongPress() {
        if (myBTCommunicator != null) {
            mThread.mActionPressed = true;

            if (programList.size() == 0)
                showToast(getResources().getString(R.string.no_files_found));                
            
    		FileDialog myFileDialog = new FileDialog(this, programList);    		    	    		
			myFileDialog.show();
        }

    }
    
    public void startProgram(String name) {
        // for .rxe programs: get program name, eventually stop this and start the new one delayed
        // is handled in startRXEprogram()
        if (name.endsWith(".rxe")) {
            programToStart = name;        
            sendBTCmessage(BTCommunicator.NO_DELAY, BTCommunicator.GET_PROGRAM_NAME, 0, 0);
            return;
        }
              
        // for .nxj programs: stop bluetooth communication after starting the program
        if (name.endsWith(".nxj")) {
            sendBTCmessage(BTCommunicator.NO_DELAY, BTCommunicator.START_PROGRAM, name);
            destroyBTCommunicator();
            return;
        }        

    }

    public void startRXEprogram(byte status) {
        if (status == 0x00) {
            sendBTCmessage(BTCommunicator.NO_DELAY, BTCommunicator.STOP_PROGRAM, 0, 0);
            sendBTCmessage(1000, BTCommunicator.START_PROGRAM, programToStart);
        }    
        else {
            sendBTCmessage(BTCommunicator.NO_DELAY, BTCommunicator.START_PROGRAM, programToStart);
        }
    }        

    public void updateMotorControl(int left, int right) {

        if (myBTCommunicator != null) {
            // don't send motor stop twice
            if ((left == 0) && (right == 0)) {
                if (stopAlreadySent)
                    return;
                else
                    stopAlreadySent = true;
            }
            else
                stopAlreadySent = false;         
                        
            // send messages via the handler
            sendBTCmessage(BTCommunicator.NO_DELAY, motorLeft, left * directionLeft, 0);
            sendBTCmessage(BTCommunicator.NO_DELAY, motorRight, right * directionRight, 0);
        }
    }

    void sendBTCmessage(int delay, int message, int value1, int value2) {
        Bundle myBundle = new Bundle();
        myBundle.putInt("message", message);
        myBundle.putInt("value1", value1);
        myBundle.putInt("value2", value2);
        Message myMessage = myHandler.obtainMessage();
        myMessage.setData(myBundle);

        if (delay == 0)
            btcHandler.sendMessage(myMessage);

        else
            btcHandler.sendMessageDelayed(myMessage, delay);
    }
    
    void sendBTCmessage(int delay, int message, String name) {
        Bundle myBundle = new Bundle();
        myBundle.putInt("message", message);
        myBundle.putString("name", name);
        Message myMessage = myHandler.obtainMessage();
        myMessage.setData(myBundle);

        if (delay == 0)
            btcHandler.sendMessage(myMessage);

        else
            btcHandler.sendMessageDelayed(myMessage, delay);
    }

    @Override
    public void onResume() {
        super.onResume();
        mConnector.registerListener();
    }

    @Override
    protected void onStart() {
        super.onStart();

        if (!BluetoothAdapter.getDefaultAdapter().isEnabled()) {
            showToast(getResources().getString(R.string.wait_till_bt_on));
            //Intent enableIntent = new Intent(this, EnableBT.class);
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);

        } else {
            selectNXT();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        destroyBTCommunicator();
    }

    @Override
    public void onPause() {
    	mConnector.unregisterListener();
        destroyBTCommunicator();
        super.onStop();
    }

    @Override
    public void onSaveInstanceState(Bundle icicle) {
        super.onSaveInstanceState(icicle);
        mConnector.unregisterListener();
    }

    /**
     * Creates the menu items
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        myMenu = menu;
        myMenu.add(0, MENU_TOGGLE_CONNECT, 1, getResources().getString(R.string.connect)).setIcon(R.drawable.ic_menu_connect);
        myMenu.add(0, MENU_QUIT, 2, getResources().getString(R.string.quit)).setIcon(R.drawable.ic_menu_exit);
        updateButtonsAndMenu();
        return true;
    }

    /**
     * Handles item selections
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_TOGGLE_CONNECT:

                if (myBTCommunicator == null || connected == false) {
                    selectNXT();

                } else {
                    destroyBTCommunicator();
                    updateButtonsAndMenu();
                }

                return true;
            case MENU_QUIT:
                destroyBTCommunicator();
                finish();

                if (btOnByUs)
                    showToast(getResources().getString(R.string.bt_off_message));

                SplashMenu.quitApplication();
                return true;
        }

        return false;
    }

    private void showToast(String textToShow) {
        reusableToast.setText(textToShow);
        reusableToast.show();
    }

    // receive messages from the BTCommunicator
    final Handler myHandler = new Handler() {
        @Override
        public void handleMessage(Message myMessage) {
            switch (myMessage.getData().getInt("message")) {
                case BTCommunicator.DISPLAY_TOAST:
                    showToast(myMessage.getData().getString("toastText"));
                    break;
                case BTCommunicator.STATE_CONNECTED:
                    connected = true;
                    programList = new ArrayList<String>();
                    connectingProgressDialog.dismiss();
                    updateButtonsAndMenu();
                    sendBTCmessage(BTCommunicator.NO_DELAY, BTCommunicator.GET_FIRMWARE_VERSION, 0, 0);
                    break;
                case BTCommunicator.MOTOR_STATE:

                    if (myBTCommunicator != null) {
                        byte[] motorMessage = myBTCommunicator.getReturnMessage();
                        int position = byteToInt(motorMessage[21]) + (byteToInt(motorMessage[22]) << 8) + (byteToInt(motorMessage[23]) << 16)
                                       + (byteToInt(motorMessage[24]) << 24);
                        showToast(getResources().getString(R.string.current_position) + position);
                    }

                    break;
                case BTCommunicator.STATE_CONNECTERROR:
                    connectingProgressDialog.dismiss();
                case BTCommunicator.STATE_RECEIVEERROR:
                case BTCommunicator.STATE_SENDERROR:
                    destroyBTCommunicator();

                    if (bt_error_pending == false) {
                        bt_error_pending = true;
                        // inform the user of the error with an AlertDialog
                        AlertDialog.Builder builder = new AlertDialog.Builder(thisActivity);
                        builder.setTitle(getResources().getString(R.string.bt_error_dialog_title))
                        .setMessage(getResources().getString(R.string.bt_error_dialog_message)).setCancelable(false)
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int id) {
                                bt_error_pending = false;
                                dialog.cancel();
                                selectNXT();
                            }
                        });
                        builder.create().show();
                    }

                    break;

                case BTCommunicator.FIRMWARE_VERSION:

                    if (myBTCommunicator != null) {
                        byte[] firmwareMessage = myBTCommunicator.getReturnMessage();
                        // afterwards we search for all files on the robot
                        sendBTCmessage(BTCommunicator.NO_DELAY, BTCommunicator.FIND_FILES, 0, 0);
                    }

                    break;

                case BTCommunicator.FIND_FILES:

                    if (myBTCommunicator != null) {
                        byte[] fileMessage = myBTCommunicator.getReturnMessage();
                        String fileName = new String(fileMessage, 4, 20);
                        fileName = fileName.replaceAll("\0","");

                        if (fileName.endsWith(".nxj") || fileName.endsWith(".rxe")) {
                            programList.add(fileName);
                        }

                        // find next entry with appropriate handle, 
                        // limit number of programs (in case of error (endless loop))
                        if (programList.size() <= MAX_PROGRAMS)
                            sendBTCmessage(BTCommunicator.NO_DELAY, BTCommunicator.FIND_FILES,
                                           1, byteToInt(fileMessage[3]));
                    }

                    break;
                    
                case BTCommunicator.PROGRAM_NAME:
                    if (myBTCommunicator != null) {
                        byte[] returnMessage = myBTCommunicator.getReturnMessage();
                        startRXEprogram(returnMessage[2]);
                    }
                    
                    break;
            }
        }
    };

    private int byteToInt(byte byteValue) {
        int intValue = (byteValue & (byte) 0x7f);

        if ((byteValue & (byte) 0x80) != 0)
            intValue |= 0x80;

        return intValue;
    }

    void selectNXT() {
        Intent serverIntent = new Intent(this, DeviceListActivity.class);
        startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CONNECT_DEVICE:

                // When DeviceListActivity returns with a device to connect
                if (resultCode == Activity.RESULT_OK) {
                    // Get the device MAC address
                    String address = data.getExtras().getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
                    pairing = data.getExtras().getBoolean(DeviceListActivity.PAIRING);
                    startBTCommunicator(address);
                }

                break;
            case REQUEST_ENABLE_BT:

                // When the request to enable Bluetooth returns
                switch (resultCode) {
                    case Activity.RESULT_OK:
                        btOnByUs = true;
                        selectNXT();
                        break;
                    case Activity.RESULT_CANCELED:
                        Toast.makeText(this, R.string.bt_needs_to_be_enabled, Toast.LENGTH_SHORT).show();//"You need to enable BT to start!"
                        finish();
                        break;
                    default:
                        Toast.makeText(this, R.string.problem_at_connecting, Toast.LENGTH_SHORT).show();
                        finish();
                        break;
                }
        }
    }


}
