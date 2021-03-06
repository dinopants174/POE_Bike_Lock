package com.tonydicola.bletest.app;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothAdapter.LeScanCallback;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.ToggleButton;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

public class MainActivity extends Activity {

    // UUIDs for UAT service and associated characteristics.
    public static UUID UART_UUID = UUID.fromString("6E400001-B5A3-F393-E0A9-E50E24DCCA9E");
    public static UUID TX_UUID = UUID.fromString("6E400002-B5A3-F393-E0A9-E50E24DCCA9E");
    public static UUID RX_UUID = UUID.fromString("6E400003-B5A3-F393-E0A9-E50E24DCCA9E");
    // UUID for the BTLE client characteristic which is necessary for notifications.
    public static UUID CLIENT_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    // Rssi timer task variables
    private Timer rssi_timer;
    private Boolean set_timer = true;
    private TimerTask read_rssi_task;
    private static Boolean mode = true; //mode

    // Call Response timer task variables
    private Timer call_timer;
    private Boolean call = true;
    private TimerTask call_task;

    private Timer call_timer_timeout;
    private boolean call_timeout = true;
    private TimerTask call_timeout_task;

    // UI elements
    private static ToggleButton lock_toggle;
    private static ToggleButton mode_toggle; //for auto-mode on/off
    private static Button settings_button;
    private RelativeLayout layout;


    // BTLE Variables
    private BluetoothAdapter adapter;
    private static BluetoothGatt gatt;
    private static BluetoothGattCharacteristic tx;
    private BluetoothGattCharacteristic rx;
    private static int REQUEST_ENABLE_BT = 1;
    String ble_state = "disconnected";


    // Call Response Constants
    String rssi_string;
    static String  unlock_command = "u";
    static String lock_command = "l";
    String   call_char = "a";

    String passcode;

    //Device Blacklist
    private List<BluetoothDevice> blacklist = new ArrayList<BluetoothDevice>();


    // OnCreate, called once to initialize the activity.
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //Apply layout settings
        setContentView(R.layout.activity_main);
        // Grab references to UI elements.
        Log.i("test", "starting");
        //Load passcode from file
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        passcode = sharedPref.getString("Passcode", "");
        passcode = passcode + ":";

        //Initialize rssi timer
        read_rssi_task = new TimerTask() {
            @Override
            public void run() {
                gatt.readRemoteRssi();
            }
        };

        call_task = new TimerTask() {
            @Override
            public void run() {
                sendCall();
            }
        };

        //Initialize UI Elements
        settings_button = (Button) findViewById(R.id.settings_button);
        settings_button.setOnClickListener(settings_handler);

        lock_toggle = (ToggleButton) findViewById(R.id.lock_state);
        lock_toggle.setOnClickListener(toggle_handler);
        lock_toggle.setChecked(true);

            //for mode toggle button
        mode_toggle = (ToggleButton) findViewById(R.id.mode_state);
        mode_toggle.setOnClickListener(mode_toggle_handler);
        mode_toggle.setChecked(true); //we probably save the mode last time to local drive.\
        updateState();

        layout = (RelativeLayout) findViewById(R.id.layout);
        changeColor(ble_state);

        Log.i("test", "starting");

        // Initialize adapter
        adapter = BluetoothAdapter.getDefaultAdapter();
        Log.i("ble Connect", "Scanning");

        //Force turning on Bluetooth
        if (!adapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }

        // Start Initial scan
        adapter.startLeScan(scanCallback);

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (gatt != null) {
            // For better reliability be careful to disconnect and close the connection.
            gatt.disconnect();
            gatt.close();
            gatt = null;
            tx = null;
            rx = null;

            // Destroy timers
            if (rssi_timer != null) {
                read_rssi_task.cancel();
                rssi_timer.cancel();
                set_timer = true;
            }
            if (call_timer != null) {
                call_task.cancel();
                call_timer.cancel();
                call_timer.purge();
                call = true;
            }
            if (call_timer_timeout != null) {
                call_timeout_task.cancel();
                call_timer_timeout.cancel();
                call_timer_timeout.purge();
                call_timeout = true;
            }
        }
    }

    @Override
    protected void onResume(){
        super.onResume();

        // Force bluetooth enable
        if (!adapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
    }

    // Main BTLE device callback where much of the logic occurs.
    private BluetoothGattCallback callback = new BluetoothGattCallback() {
        // Called whenever the device connection state changes, i.e. from disconnected to connected.
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt1, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                //If connected
                if (newState == BluetoothGatt.STATE_CONNECTED) {
                    gatt = gatt1;
                    // Discover services.
                    Log.i("ble Connect", "Connected");
                    // schedule call response here
                    if(call){
                            if (call_timer != null) {
                                call_task.cancel();
                                call_timer.cancel();
                                call_timer.purge();
                            }
                            if (call_timer_timeout != null) {
                                call_timeout_task.cancel();
                                call_timer_timeout.cancel();
                                call_timer_timeout.purge();
                                call_timeout = true;
                            }
                            call_task = new TimerTask() {
                                @Override
                                public void run() {
                                    sendCall();
                                }
                            };
                            call_timer = new Timer();
                            call_timer.schedule(call_task, 0, 1000);
                            call = false;

                            call_timeout_task = new TimerTask() {
                                @Override
                                public void run() {
                                    Log.i("timeout", "BOOOOOOOOOOM");
                                    if (call_timer != null) {
                                        call_task.cancel();
                                        call_timer.cancel();
                                        call_timer.purge();
                                        call = true;
                                    }
                                    if (call_timer_timeout != null) {
                                        call_timeout_task.cancel();
                                        call_timer_timeout.cancel();
                                        call_timer_timeout.purge();
                                        call_timeout = true;
                                    }

                                    if(gatt != null) {
                                        gatt.disconnect();
                                        gatt.close();
                                        gatt = null;
                                        tx = null;
                                        rx = null;
                                    }

                                    ble_state = "disconnected";
                                    changeColor(ble_state);

                                    adapter.startLeScan(scanCallback);
                                }
                            };

                            call_timer_timeout = new Timer();
                            call_timer.schedule(call_timeout_task, 10000);
                            call_timeout = false;
                            Log.i("Call Response Protocol", "Sending Call");
                        }

                    //Discover services and set color
                    gatt.discoverServices();
                    ble_state = "connected";
                    changeColor(ble_state);
                    }

                // If disconnected
                else if (newState == BluetoothGatt.STATE_DISCONNECTED) {
                    Log.i("ble Connect", "Disconnected");
                    //Change state and color
                    ble_state = "disconnected";
                    changeColor(ble_state);
                    //destroy Timers
                    if (rssi_timer != null) {
                        read_rssi_task.cancel();
                        rssi_timer.cancel();
                        rssi_timer.purge();
                        set_timer = true;
                    }
                    if (call_timer != null) {
                        call_task.cancel();
                        call_timer.cancel();
                        call_timer.purge();
                        call = true;
                    }
                    //Destroy connection
                    if(gatt != null) {
                        gatt.disconnect();
                        gatt.close();
                        gatt = null;
                        tx = null;
                        rx = null;
                    }

                    ble_state = "disconnected";
                    changeColor(ble_state);

                    //Start new scan
                    Log.i("ble Connect", "Scanning");
                    adapter.startLeScan(scanCallback);
                }
                updateState();
            }
        }

        @Override
        public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS && mode) { //mode condition added
                Log.i("RSSI Callback", String.format("BluetoothGatt ReadRssi[%d]", rssi));
                rssi_string = "" + rssi + ";";
                if (tx == null) {
                    // Do nothing if there is no device or message to send.
                    Log.i("RSSI Callback", "TX is empty");
                }
                //Log.i("RSSI Callback","sending");
                //Set and send rssi value
                tx.setValue(rssi_string.getBytes(Charset.forName("UTF-8")));
                gatt.writeCharacteristic(tx);
            }
        }

        // Called when services have been discovered on the remote device.
        // It seems to be necessary to wait for this discovery to occur before
        // manipulating any services or characteristics.
        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);
            // Save reference to each characteristic.
            tx = gatt.getService(UART_UUID).getCharacteristic(TX_UUID);
            rx = gatt.getService(UART_UUID).getCharacteristic(RX_UUID);
            // Setup notifications on RX characteristic changes (i.e. data received).
            // First call setCharacteristicNotification to enable notification.
            gatt.setCharacteristicNotification(rx, true);
            // Next update the RX characteristic's client descriptor to enable notifications.
            if (rx.getDescriptor(CLIENT_UUID) != null) {
                BluetoothGattDescriptor desc = rx.getDescriptor(CLIENT_UUID);
                desc.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                gatt.writeDescriptor(desc);
            }
        }

        // Called when a remote characteristic changes (like the RX characteristic).
        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
            if (characteristic.getUuid().equals(RX_UUID)) {
                String input = characteristic.getStringValue(0);
                Log.i("arduino in comms", input);
                // If Arduino locked
                if (input.equals("l")) {
                    updateLockButton(true);
                }
                // If Arduino unlocked
                else if (input.equals("u")) {
                    updateLockButton(false);
                }
                // If call received
                else if (input.equals("z")) {
                    onResponse();
                }
                // If authenticated
                else if (input.equals("f")) {
                    onAuthentication();
                }
                // If deauthenticated
                else if (input.equals("d")){
                    onDeauthentication();
                }

                updateState();
            }
            else{
                Log.i("arduino in comms", characteristic.getUuid().toString());
            }
        }
    };

    private LeScanCallback scanCallback = new LeScanCallback() {
        // Called when a device is found.
        @Override
        public void onLeScan(BluetoothDevice bluetoothDevice, int i, byte[] bytes) {
            // Check if the device has the UART service.
            if (parseUUIDs(bytes).contains(UART_UUID)) {
                // Found a device, stop the scan.
                adapter.stopLeScan(scanCallback);
                // Connect to the device.
                // Control flow will now go to the callback functions when BTLE events occur.

                //Check if device on blacklist before connecting
                if (!blacklist.contains(bluetoothDevice)) {
                    gatt = bluetoothDevice.connectGatt(getApplicationContext(), true, callback);
                    ble_state = "found";
                }
                else{
                    // start new scan, trying to connect to bad lock
                    adapter.startLeScan(scanCallback);
                    Log.i("Deauthentication", "Attempted to access blacklisted device");
                    ble_state = "disconnected";
                }
                Log.i("ble Connect", "Found Device");
                changeColor(ble_state);
            }
        }
    };

    //On click listeners for buttons
    View.OnClickListener settings_handler = new View.OnClickListener(){
        public void onClick(View v){
            //Start settings activity
            Intent i = new Intent(getApplicationContext(), SettingsActivity.class);
            startActivity(i);
        }
    };

    View.OnClickListener toggle_handler = new View.OnClickListener(){
        public void onClick(View v){
            //toggle that lock!
            toggleLock(false);
            updateState();
        }
    };

    public static boolean toggleMode(boolean called_by_widget){
        //toggle mode
        if (called_by_widget) {
            mode_toggle.setChecked(!mode_toggle.isChecked());
        }
        mode = mode_toggle.isChecked();
        return mode;
    }
    //on click listener for mode toggle button
    View.OnClickListener mode_toggle_handler = new View.OnClickListener() {
        public void onClick(View v) {
            toggleMode(false);
            updateState();
        }
    };

    // Filtering by custom UUID is broken in Android 4.3 and 4.4, see:
    //   http://stackoverflow.com/questions/18019161/startlescan-with-128-bit-uuids-doesnt-work-on-native-android-ble-implementation?noredirect=1#comment27879874_18019161
    // This is a workaround function from the SO thread to manually parse advertisement data.
    private List<UUID> parseUUIDs(final byte[] advertisedData) {
        List<UUID> uuids = new ArrayList<UUID>();

        int offset = 0;
        while (offset < (advertisedData.length - 2)) {
            int len = advertisedData[offset++];
            if (len == 0)
                break;

            int type = advertisedData[offset++];
            switch (type) {
                case 0x02: // Partial list of 16-bit UUIDs
                case 0x03: // Complete list of 16-bit UUIDs
                    while (len > 1) {
                        int uuid16 = advertisedData[offset++];
                        uuid16 += (advertisedData[offset++] << 8);
                        len -= 2;
                        uuids.add(UUID.fromString(String.format("%08x-0000-1000-8000-00805f9b34fb", uuid16)));
                    }
                    break;
                case 0x06:// Partial list of 128-bit UUIDs
                case 0x07:// Complete list of 128-bit UUIDs
                    // Loop through the advertised 128-bit UUID's.
                    while (len >= 16) {
                        try {
                            // Wrap the advertised bits and order them.
                            ByteBuffer buffer = ByteBuffer.wrap(advertisedData, offset++, 16).order(ByteOrder.LITTLE_ENDIAN);
                            long mostSignificantBit = buffer.getLong();
                            long leastSignificantBit = buffer.getLong();
                            uuids.add(new UUID(leastSignificantBit,
                                    mostSignificantBit));
                        } catch (IndexOutOfBoundsException e) {
                            // Defensive programming.
                            Log.e("UUID Parsing", e.toString());
                        } finally {
                            // Move the offset to read the next uuid.
                            offset += 15;
                            len -= 16;
                        }
                    }
                    break;
                default:
                    offset += (len - 1);
                    break;
            }
        }
        return uuids;
    }

    //UI Thread Section

    private void updateLockButton(final boolean locked){
        // Force a change of the button (caused by arduino)
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                lock_toggle.setChecked(locked);
                //Vibrate
                Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                v.vibrate(500);
            }
        });
    }

    public static boolean toggleLock(boolean called_by_widget){
        Log.i("toggle", "toggle pressed");
        // It's because clicking widget's button doesn't toggle the "real" button.
        if (called_by_widget) {
            lock_toggle.setChecked(!lock_toggle.isChecked());
        }
        boolean locked = lock_toggle.isChecked();

        //Send toggle commands to the arduino
        if(tx != null) {
            if(locked) {
                tx.setValue(lock_command.getBytes(Charset.forName("UTF-8")));
                Log.i("toggle", "sending lock");
            }
            else{
                tx.setValue(unlock_command.getBytes(Charset.forName("UTF-8")));
                Log.i("toggle", "sending unlock");

            }
            gatt.writeCharacteristic(tx);
        }
        return locked;
    }

    private void changeColor(final String state){
        //Change the color of the background based off of connection state
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if(state.equals("disconnected")) {
                    layout.setBackgroundColor(Color.parseColor("#EBEBEB"));
                }
                else if(state.equals("found")) {
                    layout.setBackgroundColor(Color.parseColor("#A2D39C"));
                }
                else if(state.equals("connected")) {
                    layout.setBackgroundColor(Color.parseColor("#82CA9D"));
                }
            }
        });
    }

    // Widget Section
    public void updateState() {
        // Load SharedPreference and save current states to it.
        SharedPreferences pref =  this.getSharedPreferences("ble", Activity.MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();
        editor.putBoolean("mode",mode);
        editor.putBoolean("lock_state",lock_toggle.isChecked());
        editor.putString("ble_state",ble_state);
        editor.apply();

        // Call onUpdate function of the widget.
        AppWidgetManager mgr = AppWidgetManager.getInstance(this);
        Intent update = new Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        update.setClass(this,bletestapp.class);
        update.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS,mgr.getAppWidgetIds(new ComponentName(this,bletestapp.class)));
        this.sendBroadcast(update);
        Log.i("update","updated");
    }

    //State controll section

    public void onResponse(){
        //Cancel call timer
        Log.i("Call Response Protocol", "Received Call");
        if (call_timer != null) {
            call_task.cancel();
            call_timer.cancel();
            call_timer.purge();
            call = true;
        }
        if (call_timer_timeout != null) {
            call_timeout_task.cancel();
            call_timer_timeout.cancel();
            call_timer_timeout.purge();
            call_timeout = true;
        }
        //Send passcode
        sendPasscode();
    }

    public void onAuthentication(){
        //schedule rssi timer
        if (set_timer) {
            if (rssi_timer != null) {
                read_rssi_task.cancel();
                rssi_timer.cancel();
                rssi_timer.purge();
            }
            read_rssi_task = new TimerTask() {
                @Override
                public void run() {
                    gatt.readRemoteRssi();
                }
            };
            rssi_timer = new Timer();
            rssi_timer.schedule(read_rssi_task, 0, 250);
            set_timer = false;
        }

    }

    public void onDeauthentication(){
        Log.i("Deauthentication", "Deauthenticated");
        // Add device to blacklist if its not there already
        if (gatt != null) {
            if(!blacklist.contains(gatt.getDevice())){
                blacklist.add(gatt.getDevice());
            }
            //Change connection state
            ble_state = "disconnected";
            changeColor(ble_state);
            //End timers and connection
            gatt.disconnect();
            gatt.close();
            gatt = null;
            tx = null;
            rx = null;
            if (rssi_timer != null) {
                read_rssi_task.cancel();
                rssi_timer.cancel();
                set_timer = true;
            }
            if (call_timer != null) {
                call_task.cancel();
                call_timer.cancel();
                call_timer.purge();
                call = true;
            }
            if (call_timer_timeout != null) {
                call_timeout_task.cancel();
                call_timer_timeout.cancel();
                call_timer_timeout.purge();
                call_timeout = true;
            }
        }
        adapter.startLeScan(scanCallback);
    }
    public void sendPasscode(){
        //Read passcode from prefs
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        passcode = sharedPref.getString("Passcode", "");
        passcode = passcode + ":";

        //Send Passcode
        if(tx != null) {
            Log.i("Sending passcode", passcode);
            tx.setValue(passcode.getBytes(Charset.forName("UTF-8")));
            gatt.writeCharacteristic(tx);
        }
        else{
            Log.i("Sending Passcode", "Couldn't Send");
        }
    }

    public void sendCall(){
        //Send call character if possible
        if(tx != null) {
            Log.i("Sending call", call_char);
            tx.setValue(call_char.getBytes(Charset.forName("UTF-8")));
            gatt.writeCharacteristic(tx);
        }
        else{
            Log.i("Sending call", "Couldn't Send");
        }
    }
}

