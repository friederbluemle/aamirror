package com.github.slashmax.aamirror;

import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.IBinder;
import android.provider.Settings;
import android.util.Log;
import android.view.WindowManager;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;

import static android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
import static android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
import static android.content.pm.ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE;
import static android.content.pm.ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT;
import static android.provider.Settings.System.ACCELEROMETER_ROTATION;
import static android.provider.Settings.System.USER_ROTATION;
import static android.view.View.VISIBLE;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;
import static android.view.WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
import static android.view.WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;

public class OrientationService extends Service {
    public static final String METHOD = "Method";
    public static final String ROTATION = "Rotation";
    public static final int METHOD_NONE = 0;
    public static final int METHOD_SUGGEST = 1;
    public static final int METHOD_FORCE = 2;
    public static final int ROTATION_0 = android.view.Surface.ROTATION_0;
    public static final int ROTATION_90 = android.view.Surface.ROTATION_90;
    public static final int ROTATION_180 = android.view.Surface.ROTATION_180;
    public static final int ROTATION_270 = android.view.Surface.ROTATION_270;
    private static final String TAG = "OrientationService";
    private static final int ANDROID_OREO = 26;
    static final int TYPE_SYSTEM_OVERLAY = Build.VERSION.SDK_INT < ANDROID_OREO ?
            WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY : WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;

    private int m_AutoRotation;
    private int m_UserRotation;
    private LinearLayout m_OverlayLayout;

    @Override
    public void onCreate() {
        super.onCreate();
        ReadRotationSettings();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            int method = intent.getIntExtra(METHOD, METHOD_NONE);
            int rotation = intent.getIntExtra(ROTATION, ROTATION_0);

            Reset();

            switch (method) {
                case METHOD_NONE:
                    break;
                case METHOD_SUGGEST:
                    WriteRotationSettings(0, rotation);
                    break;
                case METHOD_FORCE:
                    CreateOverlay(RotationToOrientation(rotation));
                    break;
            }
        }
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        Reset();
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return super.onUnbind(intent);
    }

    @Override
    public void onRebind(Intent intent) {
        super.onRebind(intent);
    }

    private WindowManager getWindowManager() {
        return (WindowManager) getSystemService(WINDOW_SERVICE);
    }

    private void Reset() {
        WriteRotationSettings(m_AutoRotation, m_UserRotation);
        RemoveOverlay();
    }

    private int RotationToOrientation(int rotation) {
        switch (rotation) {
            case ROTATION_0:
                return SCREEN_ORIENTATION_PORTRAIT;
            case ROTATION_90:
                return SCREEN_ORIENTATION_LANDSCAPE;
            case ROTATION_180:
                return SCREEN_ORIENTATION_REVERSE_PORTRAIT;
            case ROTATION_270:
                return SCREEN_ORIENTATION_REVERSE_LANDSCAPE;
        }
        return SCREEN_ORIENTATION_PORTRAIT;
    }

    private boolean CanWriteSettings() {
        return Build.VERSION.SDK_INT < 23 ||
                Settings.System.canWrite(this);
    }

    private void ReadRotationSettings() {
        try {
            m_AutoRotation = Settings.System.getInt(getContentResolver(), ACCELEROMETER_ROTATION);
            m_UserRotation = Settings.System.getInt(getContentResolver(), USER_ROTATION);
        } catch (Exception e) {
            Log.d(TAG, "ReadRotationSettings exception: " + e.toString());
        }
    }

    private void WriteRotationSettings(int autoRotation, int userRotation) {
        if (CanWriteSettings()) {
            Settings.System.putInt(getContentResolver(), ACCELEROMETER_ROTATION, autoRotation);
            Settings.System.putInt(getContentResolver(), USER_ROTATION, userRotation);
        }
    }

    private boolean CanCreateOverlay() {
        return Build.VERSION.SDK_INT < 23 ||
                Settings.canDrawOverlays(this);
    }

    private boolean CreateOverlay(int orientation) {
        WindowManager wm = getWindowManager();
        if (wm == null || !CanCreateOverlay())
            return false;

        m_OverlayLayout = new LinearLayout(this);
        m_OverlayLayout.setVisibility(VISIBLE);

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(WRAP_CONTENT, WRAP_CONTENT, TYPE_SYSTEM_OVERLAY,
                FLAG_NOT_FOCUSABLE | FLAG_NOT_TOUCHABLE, PixelFormat.TRANSPARENT);
        params.alpha = 0f;
        params.screenOrientation = orientation;

        wm.addView(m_OverlayLayout, params);
        return true;
    }

    private void RemoveOverlay() {
        WindowManager wm = getWindowManager();
        if (wm != null && m_OverlayLayout != null) {
            wm.removeView(m_OverlayLayout);
            m_OverlayLayout = null;
        }
    }
}
