package com.tonydicola.bletest.app;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothAdapter.LeScanCallback;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
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
    Timer timer;
    Boolean set_timer = true;
    TimerTask read_rssi_task;


    // UI elements
    private TextView rssi_text_view;
    private ToggleButton lock_toggle;


    // BTLE state
    private BluetoothAdapter adapter;
    private BluetoothGatt gatt;
    private BluetoothGattCharacteristic tx;
    private BluetoothGattCharacteristic rx;

    String rssi_string;
    String unlock_command = "u";
    String lock_command = "l";


    // Main BTLE device callback where much of the logic occurs.
    private BluetoothGattCallback callback = new BluetoothGattCallback() {
        // Called whenever the device connection state changes, i.e. from disconnected to connected.
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            if (newState == BluetoothGatt.STATE_CONNECTED) {
                MainActivity.this.gatt = gatt;
                // Discover services.
                Log.i("ble Connect", "Connected");
                // schedule readRemoteRssi here
                if(set_timer) {
                    timer = new Timer();
                    timer.schedule(read_rssi_task, 0, 1000);
                    set_timer = false;
                    gatt.discoverServices();
                }

            } else if (newState == BluetoothGatt.STATE_DISCONNECTED) {
                //destroy Timer
                Log.i("ble Connect", "Disconnected");
                if(timer != null) {
                        timer.cancel();
                        timer.purge();
                        set_timer = true;
                    }
                Log.i("ble Connect", "Scanning");
                adapter.startLeScan(scanCallback);
            }
        }

        @Override
        public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.i("RSSI Callback", String.format("BluetoothGatt ReadRssi[%d]", rssi));
                rssi_string = "" + rssi + ";";
                writeRSSI(rssi_string);
                if (tx == null) {
                    // Do nothing if there is no device or message to send.
                    Log.i("RSSI Callback", "TX is empty");
                    return;
                }
                Log.i("RSSI Callback","sending");
                tx.setValue(rssi_string.getBytes(Charset.forName("UTF-8")));
                gatt.writeCharacteristic(tx);
            }
        }

        //then put

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
            String lock_state = characteristic.getStringValue(0);

            if(lock_state.equals("l")){
                toggleLock(true);
            }
            else{
                toggleLock(false);
            }

        }
    };

    // OnCreate, called once to initialize the activity.
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // Grab references to UI elements.

        read_rssi_task = new TimerTask() {
            @Override
            public void run() {
                gatt.readRemoteRssi();
            }
        };
        rssi_text_view = (TextView) findViewById(R.id.rssi_text);

        lock_toggle = (ToggleButton) findViewById(R.id.lock_state);

        lock_toggle.setOnClickListener(toggle_handler);
        lock_toggle.setChecked(true);


        adapter = BluetoothAdapter.getDefaultAdapter();
        Log.i("ble Connect", "Scanning");
        adapter.startLeScan(scanCallback);

    }

    View.OnClickListener toggle_handler = new View.OnClickListener(){
        public void onClick(View v){
            Log.i("toggle","toggle pressed");
            boolean locked = lock_toggle.isChecked();

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
                Log.i("ble Connect", "Found Device");
                // Connect to the device.
                // Control flow will now go to the callback functions when BTLE events occur.
                gatt = bluetoothDevice.connectGatt(getApplicationContext(), true, callback);
            }
        }
    };

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
            if (set_timer && timer != null) {
                timer.cancel();
                set_timer = true;
            }
        }
    }

    private void writeRSSI(final CharSequence text) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                rssi_text_view.setText(text);
            }
        });
    }

    private void toggleLock(final boolean locked){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                lock_toggle.setChecked(locked);
            }
        });
    }
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
                            //Log.e(LOG_TAG, e.toString());
                            continue;
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
}
