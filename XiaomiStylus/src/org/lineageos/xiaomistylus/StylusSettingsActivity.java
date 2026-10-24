/*
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.xiaomistylus;

import android.os.Bundle;

import org.lineageos.xiaomistylus.StylusSettingsFragment;

import com.android.settingslib.collapsingtoolbar.CollapsingToolbarBaseActivity;

/**
 * Focus Pen button actions and status.
 */
public class StylusSettingsActivity extends CollapsingToolbarBaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(com.android.settingslib.collapsingtoolbar.R.id.content_frame,
                            new StylusSettingsFragment())
                    .commit();
        }
    }
}
