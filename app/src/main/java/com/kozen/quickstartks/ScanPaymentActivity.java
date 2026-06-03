package com.kozen.quickstartks;

import static com.kozen.quickstartks.TransInitActivity.currentCurrency;
import static com.kozen.quickstartks.utils.Utils.USD_TAG;

import android.hardware.Camera;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.journeyapps.barcodescanner.CaptureManager;
import com.journeyapps.barcodescanner.DecoratedBarcodeView;
import com.journeyapps.barcodescanner.camera.CameraSettings;
import com.kozen.quickstartks.utils.SecondScreenUtils;
import com.kozen.quickstartks.utils.Utils;

import java.util.ArrayList;
import java.util.List;

public class ScanPaymentActivity extends BaseActivity {
    private static final String TAG = "ScanPaymentActivity";

    private CaptureManager captureManager;
    private DecoratedBarcodeView barcodeScannerView;
    private TextView tvScanCameraError;
    private TextView tvSwitchCamera;
    private final List<Integer> cameraCandidates = new ArrayList<>();
    private int cameraCandidateIndex = 0;
    private boolean unrecoverableCameraError = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trans_scan);
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }
        getWindow().setStatusBarColor(getResources().getColor(R.color.card_dark_mid));
        View decor = getWindow().getDecorView();
        decor.setSystemUiVisibility(decor.getSystemUiVisibility() & ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);


        barcodeScannerView = findViewById(R.id.zxing_barcode_scanner);
        tvScanCameraError = findViewById(R.id.tv_scan_camera_error);
        tvSwitchCamera = findViewById(R.id.tv_switch_camera);
        buildCameraCandidates();
        if (cameraCandidates.isEmpty()) {
            showCameraUnavailable();
        } else {
            applyCamera(cameraCandidates.get(cameraCandidateIndex));
        }

        captureManager = new SafeCaptureManager(this, barcodeScannerView);
        captureManager.setShowMissingCameraPermissionDialog(false);
        captureManager.initializeFromIntent(getIntent(), savedInstanceState);
        captureManager.decode();


        TextView tv_amount = findViewById(R.id.tv_amount);
        TextView tv_order_num = findViewById(R.id.tv_order_num);
        TextView tv_order_time = findViewById(R.id.tv_order_time);
        TextView tv_cancel = findViewById(R.id.tv_cancel);
        if (tvSwitchCamera != null) {
            tvSwitchCamera.setVisibility(cameraCandidates.size() > 1 ? View.VISIBLE : View.GONE);
            tvSwitchCamera.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    switchCameraManually();
                }
            });
        }

        tv_amount.setText((USD_TAG.equals(currentCurrency) ? "$" : "€") + TransInitActivity.mAmount);
        tv_order_num.setText(Utils.getCurrentTime2() + Utils.getRandomData());
        tv_order_time.setText(Utils.getCurrentTime());
        tv_cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Utils.setToast(ScanPaymentActivity.this, getString(R.string.trans_user_cancel));

                finish();
            }
        });
        if (TransInitActivity.isExistSecScreen) {
            SecondScreenUtils.showView(ScanPaymentActivity.this, R.layout.second_qr_trans, (USD_TAG.equals(currentCurrency) ? "$" : "€") + TransInitActivity.mAmount);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!unrecoverableCameraError) {
            captureManager.onResume();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (captureManager != null) {
            captureManager.onPause();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (captureManager != null) {
            captureManager.onDestroy();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (captureManager != null) {
            captureManager.onSaveInstanceState(outState);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (captureManager != null) {
            captureManager.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Utils.setToast(ScanPaymentActivity.this, getString(R.string.trans_user_cancel));
    }

    private void buildCameraCandidates() {
        cameraCandidates.clear();
        int frontCameraId = findCameraId(Camera.CameraInfo.CAMERA_FACING_FRONT);
        int backCameraId = findCameraId(Camera.CameraInfo.CAMERA_FACING_BACK);

        addCameraCandidate(frontCameraId);
        addCameraCandidate(backCameraId);

        int cameraCount = getCameraCount();
        for (int i = 0; i < cameraCount; i++) {
            addCameraCandidate(i);
        }
    }

    private int getCameraCount() {
        try {
            return Camera.getNumberOfCameras();
        } catch (RuntimeException e) {
            Log.e(TAG, "Unable to query cameras", e);
            return 0;
        }
    }

    private int findCameraId(int facing) {
        int cameraCount = getCameraCount();
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        for (int i = 0; i < cameraCount; i++) {
            try {
                Camera.getCameraInfo(i, cameraInfo);
                if (cameraInfo.facing == facing) {
                    return i;
                }
            } catch (RuntimeException e) {
                Log.w(TAG, "Unable to read camera info for camera " + i, e);
            }
        }
        return -1;
    }

    private void addCameraCandidate(int cameraId) {
        if (cameraId < 0 || cameraCandidates.contains(cameraId)) {
            return;
        }
        cameraCandidates.add(cameraId);
    }

    private void applyCamera(int cameraId) {
        CameraSettings cameraSettings = barcodeScannerView.getCameraSettings();
        if (cameraSettings == null) {
            cameraSettings = new CameraSettings();
        }
        cameraSettings.setRequestedCameraId(cameraId);
        cameraSettings.setAutoFocusEnabled(true);
        cameraSettings.setContinuousFocusEnabled(true);
        barcodeScannerView.setCameraSettings(cameraSettings);
        Log.i(TAG, "Using camera id: " + cameraId);
    }

    private void handleCameraError(String message) {
        Log.e(TAG, "Camera error: " + message);
        if (tryNextCamera()) {
            Utils.setToast(ScanPaymentActivity.this, getString(R.string.scan_camera_switching));
            tvScanCameraError.setVisibility(View.GONE);
            barcodeScannerView.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (!unrecoverableCameraError) {
                        barcodeScannerView.resume();
                        captureManager.decode();
                    }
                }
            }, 300);
            return;
        }
        showCameraUnavailable();
    }

    private boolean tryNextCamera() {
        if (cameraCandidateIndex + 1 >= cameraCandidates.size()) {
            return false;
        }
        cameraCandidateIndex++;
        applyCamera(cameraCandidates.get(cameraCandidateIndex));
        return true;
    }

    private void switchCameraManually() {
        if (cameraCandidates.size() <= 1) {
            return;
        }
        cameraCandidateIndex = (cameraCandidateIndex + 1) % cameraCandidates.size();
        unrecoverableCameraError = false;
        if (tvScanCameraError != null) {
            tvScanCameraError.setVisibility(View.GONE);
        }
        if (barcodeScannerView != null) {
            barcodeScannerView.pause();
            applyCamera(cameraCandidates.get(cameraCandidateIndex));
            barcodeScannerView.postDelayed(new Runnable() {
                @Override
                public void run() {
                    barcodeScannerView.resume();
                    if (captureManager != null) {
                        captureManager.decode();
                    }
                }
            }, 250);
        }
    }

    private void showCameraUnavailable() {
        unrecoverableCameraError = true;
        if (tvScanCameraError != null) {
            tvScanCameraError.setText(R.string.scan_camera_unavailable);
            tvScanCameraError.setVisibility(View.VISIBLE);
        }
        Utils.setToast(ScanPaymentActivity.this, getString(R.string.scan_camera_unavailable));
        if (barcodeScannerView != null) {
            barcodeScannerView.pause();
        }
    }

    private class SafeCaptureManager extends CaptureManager {
        SafeCaptureManager(ScanPaymentActivity activity, DecoratedBarcodeView barcodeView) {
            super(activity, barcodeView);
        }

        @Override
        protected void displayFrameworkBugMessageAndExit(String message) {
            handleCameraError(message);
        }
    }
}
