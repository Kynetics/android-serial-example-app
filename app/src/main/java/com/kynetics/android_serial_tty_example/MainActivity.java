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
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.tabs.TabLayout;
import com.kynetics.android_serial_tty_example.ui.AboutActivity;
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

        /* Check dir permissions */
        if (!dir.canRead())
        {
            Log.e(TAG, "Permissions insufficient on this device");
            return devices;
        }

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
    protected void onStart() {
        super.onStart();
        setupPortSelectDialog();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

    }

    private void setupPortSelectDialog() {

        /* Setup UI elements */
        LayoutInflater inflater = this.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog, null);

        final RadioGroup baudGroup = dialogView.findViewById(R.id.radioGroup_baudRate);
        final RadioButton defaultBaudBtn = dialogView.findViewById(baudGroup.getCheckedRadioButtonId());
        selectedBaudRate = defaultBaudBtn.getText().toString();
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
            baudGroup.setOnCheckedChangeListener((group, checkedId) -> {
                RadioButton rb = finalDialogView.findViewById(baudGroup.getCheckedRadioButtonId());
                selectedBaudRate = rb.getText().toString();
                Log.d(TAG, "Baud rate selected: " + selectedBaudRate);
            });

            /* Setup dropdown menu */
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, ttyDevices);
            dropdown.setAdapter(adapter);

            /* Setup dialog */
            final AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this)
                    .setView(dialogView)
                    .setCancelable(false)
                    .setPositiveButton("OK", (dialogInterface, i) -> {
                        // Do nothing here - override later
                    })
                    .setNegativeButton("Close", (dialogInterface, i) -> {
                        finish();
                    })
                    .setNeutralButton(R.string.menu_about, (dialogInterface, i) -> {
                        startAboutActivity();
                    });

            final AlertDialog dialog = dialogBuilder.create();
            dialog.setOnShowListener(dialogInterface -> {
                Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);

                positiveButton.setOnClickListener(view -> {
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
                            Integer.valueOf(selectedBaudRate),
                            checkBoxRS485.isChecked()
                    );
                    ViewPager viewPager = findViewById(R.id.view_pager);
                    viewPager.setAdapter(sectionsPagerAdapter);
                    TabLayout tabs = findViewById(R.id.tabs);
                    tabs.setupWithViewPager(viewPager);

                    dialog.dismiss();
                });
            });
            dialog.show();

        }
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
}