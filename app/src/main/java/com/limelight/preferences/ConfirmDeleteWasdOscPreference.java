package com.limelight.preferences;

import android.content.Context;
import android.content.DialogInterface;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.widget.Toast;

import com.limelight.R;
import com.limelight.binding.input.virtual_controller.WasdMouseConfigurationLoader;

public class ConfirmDeleteWasdOscPreference extends DialogPreference {
    public ConfirmDeleteWasdOscPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public ConfirmDeleteWasdOscPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public ConfirmDeleteWasdOscPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ConfirmDeleteWasdOscPreference(Context context) {
        super(context);
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        if (which == DialogInterface.BUTTON_POSITIVE) {
            WasdMouseConfigurationLoader.resetToDefaults(getContext());
            Toast.makeText(getContext(), R.string.toast_reset_wasd_osc_success, Toast.LENGTH_SHORT).show();
        }
    }
}
