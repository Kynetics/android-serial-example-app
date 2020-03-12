/*
 * Copyright (C)  2020 Kynetics, LLC
 * SPDX-License-Identifier: Apache-2.0
 */

package com.kynetics.android_serial_tty_example.ui.main;

import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.ToggleButton;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortDataListener;
import com.fazecast.jSerialComm.SerialPortEvent;
import com.google.android.material.snackbar.Snackbar;
import com.kynetics.android_serial_tty_example.R;

/**
 * Receiver fragment
 */
public class ReceiverFragment extends Fragment {
    private static final String ARG_TTY_DEVNAME = "tty_devname";
    private static final String ARG_TTY_BAUDRATE = "tty_baudrate";
    private static String TAG = "KyneticsTTYExampleApplication:ReceiverFragment";
    private static SerialPort comPort;
    private ToggleButton acquisitionToggleButton;
    private ProgressBar acquisitionProgressBar;
    private TextView devNameTextView;
    private TextView devBaudTextView;
    private TextView devParityBitsTextView;
    private TextView devDataBitsTextView;
    private TextView rcvDataTextView;

    public ReceiverFragment() {
    }

    public static ReceiverFragment newInstance(String devName, int baudRate) {
        ReceiverFragment fragment = new ReceiverFragment();
        Bundle bundle = new Bundle();
        bundle.putString(ARG_TTY_DEVNAME, devName);
        bundle.putInt(ARG_TTY_BAUDRATE, baudRate);
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "Open device: " + getArguments().getString(ARG_TTY_DEVNAME));
        Log.d(TAG, "Set baudrate to: " + getArguments().getInt(ARG_TTY_BAUDRATE));

        /* Open serial port */
        comPort = SerialPort.getCommPort(getArguments().getString(ARG_TTY_DEVNAME));

        /* Configure serial port - blocking mode, infinite timeout */
        comPort.setBaudRate(getArguments().getInt(ARG_TTY_BAUDRATE));
        comPort.setComPortTimeouts(SerialPort.TIMEOUT_READ_BLOCKING | SerialPort.TIMEOUT_WRITE_BLOCKING,
                0, 0);

        /* Open serial port */
        comPort.openPort();
    }

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_receiver, container, false);

        /* Setup UI elements */
        devNameTextView = root.findViewById(R.id.textView_configDevName);
        devBaudTextView = root.findViewById(R.id.textView_configDevBaud);
        devParityBitsTextView = root.findViewById(R.id.textView_configDevParity);
        devDataBitsTextView = root.findViewById(R.id.textView_configDevDataBits);
        acquisitionToggleButton = root.findViewById(R.id.toggleButton_acquisition);
        acquisitionProgressBar = root.findViewById(R.id.progressBar_acquisition);
        rcvDataTextView = root.findViewById(R.id.textView_messages);
        rcvDataTextView.setMovementMethod(new ScrollingMovementMethod());

        /* Check if com port has been initialized */
        if (comPort == null) {
            Log.e(TAG, "No serial port found");
            Snackbar.make(root, "No serial port found", Snackbar.LENGTH_LONG)
                    .show();
        }

        /* Check if port is open */
        if (!comPort.isOpen()) {
            Snackbar.make(root, "Error opening serial port", Snackbar.LENGTH_LONG)
                    .show();
        }

        /* Display port information */
        devNameTextView.setText(comPort.getSystemPortName());
        devBaudTextView.setText(String.valueOf(comPort.getBaudRate()) + " bps");

        switch (comPort.getParity()) {
            case SerialPort.NO_PARITY:
                devParityBitsTextView.append("NO PARITY");
                break;
            case SerialPort.EVEN_PARITY:
                devParityBitsTextView.append("EVEN PARITY");
                break;
            case SerialPort.ODD_PARITY:
                devParityBitsTextView.append("ODD PARITY");
                break;
            case SerialPort.MARK_PARITY:
                devParityBitsTextView.append("MARK PARITY");
                break;
            case SerialPort.SPACE_PARITY:
                devParityBitsTextView.append("SPACE PARITY");
                break;
        }

        devDataBitsTextView.setText(comPort.getNumDataBits() + " bits");

        /* Setup data acquisition button */
        acquisitionToggleButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                if (isChecked) {
                    acquisitionToggleButton.setChecked(true);
                    acquisitionProgressBar.setVisibility(View.VISIBLE);

                    /* Clear text view */
                    rcvDataTextView.setText("");

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
                            rcvDataTextView.append(new String(newData));

                        }
                    });
                } else {
                    acquisitionToggleButton.setChecked(false);
                    acquisitionProgressBar.setVisibility(View.INVISIBLE);
                    /* Remove serial data listener */
                    comPort.removeDataListener();
                }
            }
        });

        return root;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        /* Close serial port */
        if (comPort.isOpen()) {
            comPort.closePort();
        }
    }
}