/*
 * Copyright (C)  2020 Kynetics, LLC
 * SPDX-License-Identifier: Apache-2.0
 */

package com.kynetics.android_serial_tty_example;

import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.tabs.TabLayout;
import com.kynetics.android_serial_tty_example.ui.main.SectionsPagerAdapter;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static String TAG = "KyneticsTTYExampleApplication:MainActivity";
    private static String selectedBaudRate;

    private static List<String> findImxTTYDevices() {
        /* Find all IMX serial devices */
        List<String> devices = new ArrayList<>();

        File dir = new File("/sys/class/tty");
        File[] files = dir.listFiles();

        for (int i = 0; i < files.length; ++i) {
            File file = files[i];
            String fileName = file.getName();
            /* IMX serial devices are mapped as ttymxc/LP* in Android system */
            if (fileName.startsWith("ttymxc") || fileName.startsWith("ttyLP")) {
                Log.d(TAG, "Found imx serial dev: " + fileName);
                devices.add(fileName);
            }
        }

        return devices;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        /* Setup UI elements */
        LayoutInflater inflater = this.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog, null);

        final RadioGroup baudGroup = dialogView.findViewById(R.id.radioGroup_baudRate);
        final RadioButton defaultBaudBtn = dialogView.findViewById(baudGroup.getCheckedRadioButtonId());
        selectedBaudRate = defaultBaudBtn.getText().toString();

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
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    finish();
                                }
                            })
                    .setOnDismissListener(
                            new AlertDialog.OnDismissListener() {

                                @Override
                                public void onDismiss(DialogInterface dialogInterface) {
                                    finish();
                                }
                            })
                    .show();
        } else {
            /* Setup baud rate listener */
            final View finalDialogView = dialogView;
            baudGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {

                @Override
                public void onCheckedChanged(RadioGroup group, int checkedId) {
                    RadioButton rb = finalDialogView.findViewById(baudGroup.getCheckedRadioButtonId());
                    selectedBaudRate = rb.getText().toString();
                    Log.d(TAG, "Baud rate selected: " + selectedBaudRate);
                }
            });

            /* Setup dropdown menu */
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, ttyDevices);
            dropdown.setAdapter(adapter);

            /* Setup dialog */
            final AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this)
                    .setView(dialogView)
                    .setCancelable(false)
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            // Do nothing here - override later
                        }
                    });

            final AlertDialog dialog = dialogBuilder.create();
            dialog.setOnShowListener(new DialogInterface.OnShowListener() {
                @Override
                public void onShow(DialogInterface dialogInterface) {
                    Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
                    positiveButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            String devName = dropdown.getSelectedItem().toString();
                            Log.d(TAG, devName + " selected");

                            /* Check file permissions */
                            File f = new File("/dev/" + devName);
                            if (!f.canWrite() && !f.canRead()) {
                                Log.e(TAG, "Permissions insufficient on this device");
                                Snackbar.make(finalDialogView, "Insufficient permissions on " + devName + " device.",
                                        Snackbar.LENGTH_LONG)
                                        .show();
                                return;
                            }

                            /* Device permissions ok */
                            SectionsPagerAdapter sectionsPagerAdapter = new SectionsPagerAdapter(
                                    getApplicationContext(),
                                    getSupportFragmentManager(),
                                    devName,
                                    Integer.valueOf(selectedBaudRate)
                            );
                            ViewPager viewPager = findViewById(R.id.view_pager);
                            viewPager.setAdapter(sectionsPagerAdapter);
                            TabLayout tabs = findViewById(R.id.tabs);
                            tabs.setupWithViewPager(viewPager);

                            dialog.dismiss();
                        }
                    });
                }
            });
            dialog.show();

        }
    }
}