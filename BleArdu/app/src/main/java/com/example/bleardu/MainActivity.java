package com.example.bleardu;

import androidx.appcompat.app.AppCompatActivity;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


import static android.bluetooth.BluetoothGattCharacteristic.FORMAT_SINT16;
import static android.bluetooth.BluetoothGattCharacteristic.FORMAT_UINT32;
import static android.bluetooth.BluetoothGattCharacteristic.FORMAT_UINT8;

public class MainActivity extends AppCompatActivity {

    private boolean running = false;
    /* Start BLE stuff */
    private BluetoothLeScanner scanner;
    private BluetoothDevice device;
    private BluetoothGatt blegatt;
    private BluetoothGattService service;
    private BluetoothGattCharacteristic proxy_char;
    private BluetoothGattCharacteristic rgbc_char;
    private BluetoothGattCharacteristic temp_char;
    private BluetoothGattCharacteristic humid_char;
    private BluetoothGattCharacteristic baro_char;

    private List<BluetoothGattCharacteristic> characteristics = new ArrayList<>();

    private final static int REQUEST_ENABLE_BT = 1;
    private boolean founddevice = false;
    private boolean notconnected = true;

    private final static UUID UUID_SENSOR_SERVICE = UUID.fromString("19B10000-E8F2-537E-4F6C-D104768A1214");
    private final static UUID UUID_PROXY_CHARACTERISTIC = UUID.fromString("19B10000-E8F2-537E-4F6C-D104768A1215");
    private final static UUID UUID_RGBC_CHARACTERISTIC = UUID.fromString("19B10000-E8F2-537E-4F6C-D104768A1216");
    private final static UUID UUID_TEMP_CHARACTERISTIC = UUID.fromString("19B10000-E8F2-537E-4F6C-D104768A1217");
    private final static UUID UUID_HUMID_CHARACTERISTIC = UUID.fromString("19B10000-E8F2-537E-4F6C-D104768A1218");
    private final static UUID UUID_BARO_CHARACTERISTIC = UUID.fromString("19B10000-E8F2-537E-4F6C-D104768A1219");
    private final static String TAG = MainActivity.class.getSimpleName();

    private int proximity = 0;
    private long hex_col = 0x0;
    private float temperature = 0.f;
    private float humidity = 0.f;
    private float barometric = 0.f;

    private TextView proxy_tv;
    private TextView col_tv;
    private TextView temp_tv;
    private TextView humid_tv;
    private TextView baro_tv;

    private int curr_char_index = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        proxy_tv = findViewById(R.id.textView0);
        col_tv = findViewById(R.id.textView);
        temp_tv = findViewById(R.id.textView2);
        humid_tv =  findViewById(R.id.textView3);
        baro_tv = findViewById(R.id.textView4);

        BluetoothManager manager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        BluetoothAdapter adapter = manager.getAdapter();
        if (adapter != null)
            scanner = adapter.getBluetoothLeScanner();

        if (!adapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        }

        if (!running) {
            Log.i(TAG, "startScanning");
            startScanning();
            running = true;
        }
        if (foundDevice()) {
            Log.i(TAG, "!foundDevice");
            stopScanning();
        }

        Thread t = new Thread() {

            @Override
            public void run() {
                try {
                    while (!isInterrupted()) {
                        Thread.sleep(1000);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                updateTexts();
                            }
                        });
                    }
                } catch (InterruptedException e) {
                    Log.e(TAG, e.getMessage());
                }
            }
        };

        t.start();
    }

    private final BluetoothGattCallback gattCallback;
    {
        gattCallback = new BluetoothGattCallback() {
            @Override
            public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
               // Log.i(TAG, "!onConnectionStateChanged");
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    Log.i(TAG, "Connected to GATT server.");
                    // Attempts to discover services after successful connection.
                    Log.i(TAG, "Attempting to start service discovery:" +
                            blegatt.discoverServices());
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    Log.i(TAG, "Disconnected from GATT server.");
                }
            }

            @Override
            public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                Log.i(TAG, "onServiceDiscovered");
                service = gatt.getService(UUID_SENSOR_SERVICE);
                if (service != null) {
                    Log.i(TAG, "Found service" + service.toString());
                    // TODO use service.getCharacteristics() to get the List instead
                    // but then it will be difficult to format the texts...
                    proxy_char = service.getCharacteristic(UUID_PROXY_CHARACTERISTIC);
                    rgbc_char = service.getCharacteristic(UUID_RGBC_CHARACTERISTIC);
                    temp_char = service.getCharacteristic(UUID_TEMP_CHARACTERISTIC);
                    humid_char = service.getCharacteristic(UUID_HUMID_CHARACTERISTIC);
                    baro_char = service.getCharacteristic(UUID_BARO_CHARACTERISTIC);
                    Log.i(TAG, "Found characteristic" + proxy_char.toString());

                    characteristics.add(baro_char);
                    characteristics.add(proxy_char);
                    characteristics.add(rgbc_char);
                    characteristics.add(temp_char);
                    characteristics.add(humid_char);

                    subscribeToCharacteristics(gatt);

                    notconnected = false;
                }
            }

            @Override
            public void onCharacteristicRead(BluetoothGatt gatt,
                                             BluetoothGattCharacteristic c,
                                             int status) {
                //Log.i(TAG, "!!!onCharacteristicRead " + c.toString());
            }

            @Override
            public void onCharacteristicWrite(BluetoothGatt gatt,
                                              BluetoothGattCharacteristic characteristic,
                                              int status) {
               // Log.i(TAG, "!!!!onCharacteristicWrite " + characteristic.toString());
            }

            @Override
            public void onCharacteristicChanged(BluetoothGatt gatt,
                                                BluetoothGattCharacteristic c) {
                //       Log.i(TAG, "!!!onCharacteristicChanged " + c.toString());

                if (UUID_PROXY_CHARACTERISTIC.equals(c.getUuid())) {
                    proximity = proxy_char.getIntValue(FORMAT_UINT8, 0);
                    Log.i(TAG, "proxy: " + proximity);
                }
                if (UUID_RGBC_CHARACTERISTIC.equals(c.getUuid())) {
                    hex_col = rgbc_char.getIntValue(FORMAT_UINT32, 0);
                    Log.i(TAG, "rgbc: " + String.format("0x%08X", hex_col));
                }
                if (UUID_TEMP_CHARACTERISTIC.equals(c.getUuid())) {
                    temperature = ((float) temp_char.getIntValue(FORMAT_SINT16, 0) / 1000.f);
                    Log.i(TAG, "temp: " + temperature);
                }
                if (UUID_HUMID_CHARACTERISTIC.equals(c.getUuid())) {
                    humidity = ((float) humid_char.getIntValue(FORMAT_UINT32, 0) / 1000.f);
                    Log.i(TAG, "humid: " + humidity);
                }
                if (UUID_BARO_CHARACTERISTIC.equals(c.getUuid())) {
                    barometric = ((float) baro_char.getIntValue(FORMAT_UINT32, 0) / 1000.f);
                    Log.i(TAG, "baro: " + barometric);
                }
                super.onCharacteristicChanged(gatt, c);
            }

            public void onDescriptorWrite(BluetoothGatt gatt,
                                          BluetoothGattDescriptor descriptor,
                                          int status) {
                super.onDescriptorWrite(gatt, descriptor, status);
                //characteristics.remove(0);
                subscribeToCharacteristics(gatt);
            }
        };
    }

    private void subscribeToCharacteristics(BluetoothGatt gatt) {

        BluetoothGattCharacteristic characteristic = characteristics.get(curr_char_index);
        gatt.setCharacteristicNotification(characteristic, true);
        characteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);

        UUID uuid = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
        BluetoothGattDescriptor descriptor = characteristic.getDescriptor(uuid);
        if (descriptor != null) {
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            gatt.writeDescriptor(descriptor);
        }
        curr_char_index++;
        if (curr_char_index >= characteristics.size())
            curr_char_index = 0;
    }

    private final ScanCallback leScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            // Log.d(TAG, "Addr: " + result.getDevice().getAddress() + " Name: " + result.getDevice().getName());
            if (result.getDevice().getName() != null) {
                // TODO search for UUID of servie instead
                if (result.getDevice().getAddress().equals("D6:67:5C:C8:C4:71")) {
                    Log.d(TAG, "Found my Arduino");
                    device = result.getDevice();
                    founddevice = true;
                    foundDevice();
                    stopScanning();
                }
            }
        }
    };

    private void startScanning() {
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                scanner.startScan(leScanCallback);
            }
        });
    }

    private void stopScanning() {
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                scanner.stopScan(leScanCallback);
            }
        });
    }

    private boolean foundDevice() {
        if (founddevice && notconnected) {
            blegatt = device.connectGatt(this, false, gattCallback);
            Log.i(TAG, "Found Device" + blegatt.toString());
        }
        return founddevice;
    }

    // TODO this is stupid but good enough until "real" UI
    private void updateTexts() {

        proxy_tv.setText("proximity " + proximity);
        proxy_tv.invalidate();

        col_tv.setText("color " + String.format("0x%08X", hex_col));
        col_tv.invalidate();

        temp_tv.setText("temp " + temperature);
        temp_tv.invalidate();

        humid_tv.setText("humid " + humidity + "%");
        humid_tv.invalidate();

        baro_tv.setText("baro " + barometric);
        baro_tv.invalidate();

    }
    /* End BLE stuff */
}
