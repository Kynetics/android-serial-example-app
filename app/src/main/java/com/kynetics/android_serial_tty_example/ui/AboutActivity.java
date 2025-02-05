/*
 * Copyright (C)  2025 Kynetics, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.kynetics.android_serial_tty_example.ui;

import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.widget.TextView;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.kynetics.android_serial_tty_example.BuildConfig;
import com.kynetics.android_serial_tty_example.R;

public class AboutActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        TextView aboutTextView = findViewById(R.id.about_text);
        aboutTextView.setMovementMethod(LinkMovementMethod.getInstance());

        TextView versionTextView = findViewById(R.id.about_app_version);
        versionTextView.setText(getString(R.string.about_app_version, BuildConfig.VERSION_NAME));

    }
}
