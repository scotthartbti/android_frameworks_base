package com.android.systemui.quicksettings;

import android.app.Dialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.net.Uri;
import android.os.Handler;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.service.dreams.DreamService;
import android.service.dreams.IDreamManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.VolumePanel;
import android.view.Window;
import android.view.WindowManager;
import android.view.WindowManagerGlobal;
import android.widget.ImageView;

import com.android.internal.app.ThemeUtils;

import com.android.systemui.R;
import com.android.systemui.statusbar.phone.QuickSettingsController;
import com.android.systemui.statusbar.phone.QuickSettingsContainerView;

public class DaydreamTile extends QuickSettingsTile {
    private final IDreamManager mDreamManager;

    public DaydreamTile(Context context, LayoutInflater inflater,
            QuickSettingsContainerView container, final QuickSettingsController qsc) {
        super(context, inflater, container, qsc);

        mLabel = context.getString(R.string.quick_settings_daydream);
        mDrawable = R.drawable.ic_qs_clock_circle;
        mDreamManager = IDreamManager.Stub.asInterface(
                ServiceManager.getService(DreamService.DREAM_SERVICE));


        mOnClick = new OnClickListener() {

            @Override
            public void onClick(View v) {
                qsc.mBar.collapseAllPanels(true);
                try {
                    mDreamManager.dream();
                } catch (RemoteException e) {
                }
            }
        };

        mOnLongClick = new OnLongClickListener() {

            @Override
            public boolean onLongClick(View v) {
                startSettingsActivity("com.android.settings.DAYDREAM_SETTINGS");
                return true;
            }
        };
    }
}
