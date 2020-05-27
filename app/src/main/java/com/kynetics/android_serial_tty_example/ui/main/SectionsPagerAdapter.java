/*
 * Copyright (C)  2020 Kynetics, LLC
 * SPDX-License-Identifier: Apache-2.0
 */

package com.kynetics.android_serial_tty_example.ui.main;

import android.content.Context;

import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

import com.kynetics.android_serial_tty_example.R;

/**
 * A [FragmentPagerAdapter] that returns a fragment corresponding to
 * one of the sections/tabs/pages.
 */
public class SectionsPagerAdapter extends FragmentPagerAdapter {

    @StringRes
    private static final int[] TAB_TITLES = new int[]{R.string.tab_text_1, R.string.tab_text_2};
    private final Context mContext;
    private String devName;
    private int baudRate;
    private boolean rs485Mode;

    public SectionsPagerAdapter(Context context, FragmentManager fm, String devName, int baudRate,
                                boolean rs485Mode) {
        super(fm);
        mContext = context;
        this.devName = devName;
        this.baudRate = baudRate;
        this.rs485Mode = rs485Mode;
    }

    @Override
    public Fragment getItem(int position) {
        // getItem is called to instantiate the fragment for the given page.
        switch (position)
        {
            /* Receiver tab */
            case 0:
                return ReceiverFragment.newInstance(this.devName, this.baudRate, this.rs485Mode);
            /* Sender tab */
            case 1:
                return SenderFragment.newInstance(this.devName, this.baudRate, this.rs485Mode);
            default:
                return null;
        }
    }

    @Nullable
    @Override
    public CharSequence getPageTitle(int position) {
        return mContext.getResources().getString(TAB_TITLES[position]);
    }

    @Override
    public int getCount() {
        // Show 2 total pages.
        return 2;
    }
}