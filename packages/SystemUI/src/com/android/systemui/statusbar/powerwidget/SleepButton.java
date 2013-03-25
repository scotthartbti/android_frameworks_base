package com.android.systemui.statusbar.powerwidget;

import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;
import android.os.SystemClock;
import android.hardware.input.InputManager;
import android.view.KeyEvent;

import com.android.systemui.R;

public class SleepButton extends PowerButton {
    public SleepButton() { mType = BUTTON_SLEEP; }

    @Override
    protected void updateState(Context context) {
        mIcon = R.drawable.stat_power;
        mState = STATE_DISABLED;
    }

    @Override
    protected void toggleState(Context context) {
        PowerManager pm = (PowerManager)
                context.getSystemService(Context.POWER_SERVICE);
        pm.goToSleep(SystemClock.uptimeMillis());
    }

    @Override
    protected boolean handleLongClick(Context context) {
        triggerVirtualKeypress(KeyEvent.KEYCODE_POWER, true);
        return true;
    }

    private void triggerVirtualKeypress(final int keyCode, final boolean longPress) {
        new Thread(new Runnable() {
            public void run() {
                InputManager im = InputManager.getInstance();
                KeyEvent keyEvent;
                if (longPress) {
                    keyEvent = new KeyEvent(KeyEvent.ACTION_DOWN, keyCode);
                    keyEvent.changeFlags(keyEvent, KeyEvent.FLAG_FROM_SYSTEM | KeyEvent.FLAG_LONG_PRESS);
                } else {
                    keyEvent = new KeyEvent(KeyEvent.ACTION_UP, keyCode);
                    keyEvent.changeFlags(keyEvent, KeyEvent.FLAG_FROM_SYSTEM);
                }
                im.injectInputEvent(keyEvent, InputManager.INJECT_INPUT_EVENT_MODE_WAIT_FOR_RESULT);
            }
        }).start();
    }
}
