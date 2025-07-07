/*
 * Copyright (C)  2025 Kynetics, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.kynetics.android_serial_tty_example;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortDataListener;
import com.fazecast.jSerialComm.SerialPortEvent;
import com.google.android.material.snackbar.Snackbar;
import com.kynetics.android_serial_tty_example.databinding.ActivityMainBinding;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static String TAG = "KyneticsTTYExampleApplication:MainActivity";
    private ActivityMainBinding binding;

    private SerialPort comPort;
    private String devName;
    private int selectedBaudRate;
    private Boolean isRS485Mode;

    @Override
    protected void onStart() {
        super.onStart();
        setupPortSelectDialog();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.toolbar);

    }

    private void setupPortSelectDialog() {

        /* Setup UI elements */
        LayoutInflater inflater = this.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog, null);

        final RadioGroup baudGroup = dialogView.findViewById(R.id.radioGroup_baudRate);
        final CheckBox checkBoxRS485 = dialogView.findViewById(R.id.checkBox_RS485);

        /* Setup list of serial interfaces */
        final Spinner dropdown = dialogView.findViewById(R.id.spinner_ttyDevs);

        List<String> ttyDevices = findImxTTYDevices();
        Collections.sort(ttyDevices);

        if (ttyDevices.size() == 0) {
            /* No serial devices found */
            dialogView = inflater.inflate(R.layout.dialog_no_devs, null);
            new AlertDialog.Builder(this)
                    .setView(dialogView)
                    .setNegativeButton("Close",
                            (dialogInterface, i) -> finish())
                    .setNeutralButton(R.string.menu_about,
                            (dialogInterface, i) -> startAboutActivity())
                    .setOnDismissListener(
                            dialogInterface -> finish())
                    .show();
        } else {
            /* Setup baud rate listener */
            final View finalDialogView = dialogView;

            /* Setup dropdown menu */
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, ttyDevices);
            dropdown.setAdapter(adapter);

            /* Setup dialog */
            final AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this)
                    .setView(dialogView)
                    .setCancelable(false)
                    .setPositiveButton("OK", (dialogInterface, i) -> {
                        String device = dropdown.getSelectedItem().toString();
                        RadioButton checkedBaudRate = finalDialogView.findViewById(baudGroup.getCheckedRadioButtonId());
                        String baudRate = checkedBaudRate.getText().toString();
                        setupSerialPort(device, baudRate, checkBoxRS485.isChecked());
                        dialogInterface.dismiss();
                    })
                    .setNegativeButton("Close", (dialogInterface, i) -> {
                        finish();
                    })
                    .setNeutralButton(R.string.menu_about, (dialogInterface, i) -> {
                        startAboutActivity();
                    });

            final AlertDialog dialog = dialogBuilder.create();
            dialog.show();

        }
    }

    private void setupSerialPort(String device, String baudRate, Boolean rs485Mode) {
        devName = device;
        Log.d(TAG, devName + " selected");

        /* Check file permissions */
        File f = new File("/dev/" + devName);
        if (!f.canWrite() && !f.canRead()) {
            Log.e(TAG, "Permissions insufficient on this device");
            Snackbar.make(binding.toolbar, "Insufficient permissions on " + devName + " device.",
                            Snackbar.LENGTH_LONG)
                    .show();
            return;
        }

        selectedBaudRate = Integer.valueOf(baudRate);
        isRS485Mode = rs485Mode;

        /* Open serial port */
        comPort = SerialPort.getCommPort(devName);


        /* Configure serial port - blocking mode, infinite timeout */
        comPort.setComPortParameters(selectedBaudRate,
                comPort.getNumDataBits(),
                comPort.getNumStopBits(),
                comPort.getParity(),
                isRS485Mode);
        comPort.setComPortTimeouts(SerialPort.TIMEOUT_READ_BLOCKING | SerialPort.TIMEOUT_WRITE_BLOCKING,
                0, 0);

        /* Open serial port */
        comPort.openPort();


        /* Check if com port has been initialized */
        if (comPort == null) {
            Log.e(TAG, "No serial port found");
            Snackbar.make(binding.toolbar, "No serial port found", Snackbar.LENGTH_LONG)
                    .show();
        }

        /* Check if port is open */
        if (!comPort.isOpen()) {
            Snackbar.make(binding.toolbar, "Error opening serial port", Snackbar.LENGTH_LONG)
                    .show();
        }

        setupViews();
    }

    private void setupViews() {
        setupConfigurationsViews();
        setupSendDataViews();
        setupReceiveDataViews();
    }

    private void setupConfigurationsViews() {
        binding.serialInterfaceSelected.setText(devName);

        /* Display port information */
        String parity = "";
        switch (comPort.getParity()) {
            case SerialPort.NO_PARITY:
                parity = "NO PARITY";
                break;
            case SerialPort.EVEN_PARITY:
                parity = "EVEN PARITY";
                break;
            case SerialPort.ODD_PARITY:
                parity = "ODD PARITY";
                break;
            case SerialPort.MARK_PARITY:
                parity = "MARK PARITY";
                break;
            case SerialPort.SPACE_PARITY:
                parity = "SPACE PARITY";
                break;
        }

        String configurations = "Baud rate: " + comPort.getBaudRate() + "BPS \n" +
                "Parity: " + parity + "\n" +
                "Data Bits: " + comPort.getNumDataBits() + " bits\n" +
                "RS485 mode: " + (isRS485Mode ? "Enabled" : "Disabled");
        binding.textViewConfigurations.setText(configurations);
    }

    private void setupSendDataViews() {
        binding.btnSend.setOnClickListener(v -> {
            String data = binding.editTextFrameData.getText().toString();
            if (data.isEmpty()) {
                Snackbar.make(binding.toolbar, "No data to send", Snackbar.LENGTH_LONG)
                        .show();
                return;
            }

            byte[] dataBytes = data.getBytes();
            int bytesWritten = comPort.writeBytes(dataBytes, data.length());
            if (bytesWritten == -1) {
                Snackbar.make(binding.toolbar, "Error sending data", Snackbar.LENGTH_LONG)
                        .show();
            } else {
                Snackbar.make(binding.toolbar, bytesWritten + " bytes sent.", Snackbar.LENGTH_SHORT)
                        .show();
            }
        });


    }

    private void setupReceiveDataViews() {
        binding.switchDataAcquisition.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                if (comPort == null || !comPort.isOpen()) {
                    Snackbar.make(binding.toolbar, "Port is not open yet!", Snackbar.LENGTH_LONG)
                            .show();
                    binding.switchDataAcquisition.setChecked(false);
                    return;
                }

                binding.textViewSerialMessages.setText("");
                binding.textViewSerialMessages.setVisibility(View.VISIBLE);
                binding.progressBarAcquisition.setVisibility(View.VISIBLE);

                /* Setup serial data listener */
                comPort.addDataListener(new SerialPortDataListener() {
                    @Override
                    public int getListeningEvents() {
                        return SerialPort.LISTENING_EVENT_DATA_AVAILABLE;
                    }

                    @Override
                    public void serialEvent(SerialPortEvent event) {
                        if (event.getEventType() != SerialPort.LISTENING_EVENT_DATA_AVAILABLE)
                            return;
                        byte[] newData = new byte[comPort.bytesAvailable()];
                        int numRead = comPort.readBytes(newData, newData.length);
                        Log.d(TAG, "Read " + numRead + " bytes.");

                        /* Display received data */
                        String oldData = binding.textViewSerialMessages.getText().toString();
                        String newString;
                        if (numRead == 1) {
                            newString = oldData + new String(newData);
                        } else {
                            newString = new String(newData) + oldData;
                        }
                        binding.textViewSerialMessages.setText(newString);
                    }
                });

            } else {
                binding.textViewSerialMessages.setVisibility(View.GONE);
                binding.progressBarAcquisition.setVisibility(View.GONE);
                comPort.removeDataListener();
            }
        });
    }

    private static List<String> findImxTTYDevices() {
        /* Find all IMX serial devices */
        List<String> devices = new ArrayList<>();

        File dir = new File("/sys/class/tty");

        /* Check dir permissions */
        if (!dir.canRead()) {
            Log.e(TAG, "Permissions insufficient on this device");
            return devices;
        }

        File[] files = dir.listFiles();

        for (int i = 0; i < files.length; ++i) {
            File file = files[i];
            String fileName = file.getName();
            /* IMX serial devices are mapped as ttymxc/LP* in Android system */
            if (fileName.startsWith("ttymxc") || fileName.startsWith("ttyLP") || fileName.startsWith("ttyS")) {
                Log.d(TAG, "Found imx serial dev: " + fileName);
                devices.add(fileName);
            }
        }

        return devices;
    }

    private void startAboutActivity() {
        Intent intent = new Intent(MainActivity.this, AboutActivity.class);
        startActivity(intent);
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_recreate) {
            recreate();
            return true;
        } else if (item.getItemId() == R.id.menu_about) {
            startAboutActivity();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (comPort != null) {
            comPort.removeDataListener();
            binding.switchDataAcquisition.setChecked(false);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        /* Close serial port */
        if (comPort != null && comPort.isOpen()) {
            comPort.closePort();
        }
    }
}