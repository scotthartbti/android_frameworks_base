package com.android.systemui.quicksettings;

import android.app.Dialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.media.AudioManager;
import android.media.AudioService;
import android.media.IAudioService;
import android.net.Uri;
import android.os.Handler;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.VolumePanel;
import android.view.Window;
import android.view.WindowManager;
import android.view.WindowManagerGlobal;
import android.widget.ImageView;

import com.android.internal.app.ThemeUtils;

import com.android.systemui.R;
import com.android.systemui.statusbar.phone.QuickSettingsController;
import com.android.systemui.statusbar.phone.QuickSettingsContainerView;
import com.android.systemui.statusbar.policy.VolumeController;
import com.android.systemui.statusbar.policy.ToggleSlider;

public class VolumeTile extends QuickSettingsTile {

    public VolumeTile(Context context, LayoutInflater inflater,
            QuickSettingsContainerView container, final QuickSettingsController qsc) {
        super(context, inflater, container, qsc);

        mLabel = context.getString(R.string.quick_settings_volume);
        mDrawable = R.drawable.ic_qs_ring_on;

        mOnClick = new OnClickListener() {

            @Override
            public void onClick(View v) {
                qsc.mBar.collapseAllPanels(true);
                final AudioManager am = (AudioManager)mContext.getSystemService(Context.AUDIO_SERVICE);
                final int stream = am.isMusicActive() ? AudioManager.STREAM_MUSIC :
                        AudioManager.STREAM_NOTIFICATION;
                final int volume = am.getStreamVolume(stream);
                am.setStreamVolume(stream, volume, AudioManager.FLAG_SHOW_UI);
            }
        };
    }
}
