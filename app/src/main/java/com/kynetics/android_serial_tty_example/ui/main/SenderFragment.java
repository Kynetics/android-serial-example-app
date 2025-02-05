/*
 * Copyright (C)  2025 Kynetics, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.kynetics.android_serial_tty_example.ui.main;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.fazecast.jSerialComm.SerialPort;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.kynetics.android_serial_tty_example.R;

/**
 * Sender fragment
 */
public class SenderFragment extends Fragment {
    private static final String ARG_TTY_DEVNAME = "tty_devname";
    private static final String ARG_TTY_BAUDRATE = "tty_baudrate";
    private static final String ARG_TTY_RS485MODE = "tty_rs485mode";
    private static String TAG = "KyneticsTTYExampleApplication:SenderFragment";
    private static SerialPort comPort;
    private FloatingActionButton sendFab;
    private EditText sendTxt;


    public static SenderFragment newInstance(String devName, int baudRate, boolean rs485Mode) {
        SenderFragment fragment = new SenderFragment();
        Bundle bundle = new Bundle();
        bundle.putString(ARG_TTY_DEVNAME, devName);
        bundle.putInt(ARG_TTY_BAUDRATE, baudRate);
        bundle.putBoolean(ARG_TTY_RS485MODE, rs485Mode);
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "Open device: " + getArguments().getString(ARG_TTY_DEVNAME));
        Log.d(TAG, "Set baudrate to: " + getArguments().getInt(ARG_TTY_BAUDRATE));
        Log.d(TAG, "RS485 mode: " + (getArguments().getBoolean(ARG_TTY_RS485MODE) ?
                "enabled" : "disabled"));

        /* Open serial port */
        comPort = SerialPort.getCommPort(getArguments().getString(ARG_TTY_DEVNAME));

        /* Configure serial port - blocking mode, infinite timeout */
        comPort.setComPortParameters(getArguments().getInt(ARG_TTY_BAUDRATE),
                comPort.getNumDataBits(),
                comPort.getNumStopBits(),
                comPort.getParity(),
                getArguments().getBoolean(ARG_TTY_RS485MODE));
        comPort.setComPortTimeouts(SerialPort.TIMEOUT_READ_BLOCKING | SerialPort.TIMEOUT_WRITE_BLOCKING,
                0, 0);

        /* Open serial port */
        comPort.openPort();
    }

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        final View root = inflater.inflate(R.layout.fragment_sender, container, false);

        /* Setup UI elements */
        sendFab = root.findViewById(R.id.fabSend);
        sendTxt = root.findViewById(R.id.editText_message);

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

        /* Setup fab button */
        sendFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String writeStr = sendTxt.getText().toString();
                int numWritten = comPort.writeBytes(writeStr.getBytes(), writeStr.length());
                if (numWritten == -1) {
                    Snackbar.make(getView(), "Error sending data", Snackbar.LENGTH_LONG)
                            .show();
                } else {
                    Snackbar.make(getView(), numWritten + " bytes sent.", Snackbar.LENGTH_SHORT)
                            .show();
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