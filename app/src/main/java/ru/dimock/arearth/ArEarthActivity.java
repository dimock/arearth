package ru.dimock.arearth;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.display.DisplayManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.util.Size;
import android.view.Display;
import android.view.WindowManager;
import android.widget.Toast;

import com.google.ar.core.ArCoreApk;
import com.google.ar.core.CameraConfig;
import com.google.ar.core.CameraConfigFilter;
import com.google.ar.core.Config;
import com.google.ar.core.Session;
import com.google.ar.core.exceptions.CameraNotAvailableException;
import com.google.ar.core.exceptions.UnavailableApkTooOldException;
import com.google.ar.core.exceptions.UnavailableArcoreNotInstalledException;
import com.google.ar.core.exceptions.UnavailableDeviceNotCompatibleException;
import com.google.ar.core.exceptions.UnavailableSdkTooOldException;
import com.google.ar.core.exceptions.UnavailableUserDeclinedInstallationException;

import java.util.List;

public class ArEarthActivity extends AppCompatActivity implements DisplayManager.DisplayListener {

    private ArEarthGlSurfaceView earthView;
    private boolean viewportChanged = false;
    private Session session;
    private Display display;
    private boolean installRequested;
    private static final int CAMERA_PERMISSION_CODE = 0;
    private static final String CAMERA_PERMISSION = Manifest.permission.CAMERA;
    private long splashDuration = 5000;
    private Handler handler = new Handler();
    private Runnable runnable = new Runnable() {
        @Override
        public void run() {
            if(handler != null) {
                handler.removeCallbacks(runnable);
                handler = null;
            }
        }
    };

    @Override
    public void onDisplayAdded(int displayId) {}

    @Override
    public void onDisplayRemoved(int displayId) {}

    @Override
    public void onDisplayChanged(int displayId) {
        viewportChanged = true;
    }

    public void changeViewport() {
        viewportChanged = true;
    }

    public void updateViewport() {
        if(viewportChanged && session != null) {
            int displayRotation = display.getRotation();
            session.setDisplayGeometry(displayRotation, earthView.viewportWidth(), earthView.viewportHeight());
            viewportChanged = false;
        }
    }

    public void startSplash() {
        handler.postDelayed(runnable, splashDuration);
    }

    public boolean splashCompleted() {
        return handler == null;
    }

    public Session getSession() {
        return session;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        earthView = new ArEarthGlSurfaceView(this);
        display = this.getSystemService(WindowManager.class).getDefaultDisplay();
        setContentView(earthView);
    }

    @Override
    public void onPause() {
        super.onPause();
        if (session != null) {
            this.getSystemService(DisplayManager.class).unregisterDisplayListener(this);
            session.pause();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (session == null) {
            String message = null;
            try {
                switch (ArCoreApk.getInstance().requestInstall(this, !installRequested)) {
                    case INSTALL_REQUESTED:
                        installRequested = true;
                        return;
                    case INSTALLED:
                        break;
                }

                if (ContextCompat.checkSelfPermission(this, CAMERA_PERMISSION) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(
                            this, new String[] {CAMERA_PERMISSION}, CAMERA_PERMISSION_CODE);
                    return;
                }

                session = new Session(this);
                CameraConfigFilter cameraConfigFilter = new CameraConfigFilter(session);
                List<CameraConfig> configs = session.getSupportedCameraConfigs(cameraConfigFilter);
                CameraConfig config = session.getCameraConfig();
                for(CameraConfig cfg : configs) {
                    Size imSize= cfg.getImageSize();
                    Size currentSize = config.getImageSize();
                    if(imSize.getWidth() > currentSize.getWidth()) {
                        config = cfg;
                    }
                }
                Config sessionConfig = new Config(session);
                sessionConfig.setFocusMode(Config.FocusMode.AUTO);
                sessionConfig.setLightEstimationMode(Config.LightEstimationMode.ENVIRONMENTAL_HDR);
                session.configure(sessionConfig);
                if(config != null) {
                    session.setCameraConfig(config);
                }

            } catch (UnavailableArcoreNotInstalledException
                    | UnavailableUserDeclinedInstallationException e) {
                message = getResources().getString(R.string.install_arcore);
            } catch (UnavailableApkTooOldException e) {
                message = getResources().getString(R.string.update_arcore);
            } catch (UnavailableSdkTooOldException e) {
                message = getResources().getString(R.string.update_app);
            } catch (UnavailableDeviceNotCompatibleException e) {
                message = getResources().getString(R.string.does_not_support_ar);
            } catch (Exception e) {
                message = getResources().getString(R.string.ar_failed);
            }

            if (message != null) {
                return;
            }
        }

        try {
            session.resume();
        } catch (CameraNotAvailableException e) {
            session = null;
            return;
        }
        this.getSystemService(DisplayManager.class).registerDisplayListener(this, null);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) {
        if (ContextCompat.checkSelfPermission(this, CAMERA_PERMISSION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, getResources().getString(R.string.camera_permision), Toast.LENGTH_LONG).show();
            if (!ActivityCompat.shouldShowRequestPermissionRationale(this, CAMERA_PERMISSION)) {
                Intent intent = new Intent();
                intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                intent.setData(Uri.fromParts("package", this.getPackageName(), null));
                this.startActivity(intent);
            }
            finish();
        }
    }
}
