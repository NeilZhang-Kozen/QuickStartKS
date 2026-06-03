package com.kozen.quickstartks.utils;

import android.content.Context;
import android.os.Build;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.kozen.component.secondaryScreen.IResultCallback;
import com.kozen.component.secondaryScreen.ISecondaryScreen;
import com.kozen.component_client.ComponentEngine;
import com.kozen.quickstartks.R;
import com.kozen.quickstartks.TransInitActivity;

public class SecondScreenUtils {

    private static String TAG = "SecondScreenUtils";
    private static final String NO_SECOND_SCREEN_SN_PREFIX = "AA66A23";
    private String RES_ROOT_PATH =
            "${Environment.getExternalStorageDirectory().absolutePath}/ViceScreen";
    private int brightnessState = 0; //屏幕亮度等级，第一次默认60%
    private static ISecondaryScreen secondaryScreen = null;

    private static ISecondaryScreen getSecondaryScreen() {
        if (!TransInitActivity.isComponenInit) {
            secondaryScreen = null;
            return null;
        }
        if (secondaryScreen == null) {
            secondaryScreen = ComponentEngine.INSTANCE.getSecondaryScreenManager();
        }
        return secondaryScreen;
    }

    public static boolean isAvailable() {
        return isAvailable(TransInitActivity.getInstance());
    }

    public static boolean isAvailable(Context context) {
        String serialNo = getDeviceSerialNo();
        if (serialNo != null && serialNo.toUpperCase().startsWith(NO_SECOND_SCREEN_SN_PREFIX)) {
            Log.i(TAG, "secondary screen disabled by SN prefix, sn=" + maskSerial(serialNo));
            secondaryScreen = null;
            return false;
        }
        ISecondaryScreen screen = getSecondaryScreen();
        if (screen == null) {
            return false;
        }
        try {
            int ret = screen.setBrightness(100);
            int[] resolution = screen.getScreenResolution();
            boolean available = ret == 0 && isValidResolution(resolution);
            Log.i(TAG, "secondary screen probe ret=" + ret
                    + ", resolution=" + resolutionToString(resolution)
                    + ", available=" + available);
            if (!available) {
                secondaryScreen = null;
            }
            return available;
        } catch (Exception e) {
            Log.e(TAG, "secondary screen probe failed", e);
            secondaryScreen = null;
            return false;
        }
    }

    private static int[] getScreenResolution() {
        ISecondaryScreen screen = getSecondaryScreen();
        if (screen == null) {
            return null;
        }
        try {
            int[] resolution = screen.getScreenResolution();
            if (isValidResolution(resolution)) {
                return resolution;
            }
        } catch (Exception e) {
            Log.e(TAG, "secondary screen unavailable", e);
            secondaryScreen = null;
        }
        return null;
    }

    private static String getDeviceSerialNo() {
        String serialNo = getSystemProperty("ro.serialno");
        if (!isUsableSerial(serialNo)) {
            serialNo = getSystemProperty("ro.boot.serialno");
        }
        if (!isUsableSerial(serialNo)) {
            try {
                serialNo = Build.getSerial();
            } catch (Exception e) {
                Log.d(TAG, "Build.getSerial unavailable: " + e.getMessage());
            }
        }
        if (!isUsableSerial(serialNo)) {
            serialNo = Build.SERIAL;
        }
        return isUsableSerial(serialNo) ? serialNo.trim() : "";
    }

    private static String getSystemProperty(String key) {
        try {
            Class<?> systemProperties = Class.forName("android.os.SystemProperties");
            java.lang.reflect.Method get = systemProperties.getMethod("get", String.class);
            Object value = get.invoke(null, key);
            return value == null ? "" : String.valueOf(value);
        } catch (Exception e) {
            Log.d(TAG, "SystemProperties.get failed for " + key + ": " + e.getMessage());
            return "";
        }
    }

    private static boolean isUsableSerial(String serialNo) {
        return serialNo != null
                && serialNo.trim().length() > 0
                && !"unknown".equalsIgnoreCase(serialNo.trim());
    }

    private static String maskSerial(String serialNo) {
        if (serialNo == null) {
            return "";
        }
        String value = serialNo.trim();
        if (value.length() <= 7) {
            return value;
        }
        return value.substring(0, 7) + "***";
    }

    private static boolean isValidResolution(int[] resolution) {
        return resolution != null && resolution.length >= 2 && resolution[0] > 0 && resolution[1] > 0;
    }

    private static String resolutionToString(int[] resolution) {
        if (resolution == null) {
            return "null";
        }
        if (resolution.length < 2) {
            return "length=" + resolution.length;
        }
        return resolution[0] + "x" + resolution[1];
    }

    private static void markUnavailable(String reason) {
        Log.w(TAG, "secondary screen unavailable: " + reason);
        secondaryScreen = null;
        TransInitActivity.isExistSecScreen = false;
    }

    /**
     * 按一次上电，再按下电
     */
    public static void powerControl(boolean powerState) {
        ISecondaryScreen screen = getSecondaryScreen();
        if (screen != null) {
            screen.power(powerState);
        }
    }

    public static boolean showPic() {
        String picPath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/ViceScreen" + "/img4.png";
        ISecondaryScreen screen = getSecondaryScreen();
        if (screen != null) {
            screen.showPic(picPath);
            return true;
        }
        return false;
    }

    public static boolean showView(Context context, int layoutId) {
        ISecondaryScreen screen = getSecondaryScreen();
        int[] resolution = getScreenResolution();
        if (screen != null && resolution != null) {
            View layoutV =
                    layoutToView(context, layoutId, resolution[0], resolution[1]);
            screen.show(layoutV, new IResultCallback() {
                        @Override
                        public void onSuccess() {

                        }

                        @Override
                        public void onFailure(int i, String s) {
                            markUnavailable("showView failed code=" + i + ", msg=" + s);
                        }
                    }
            );
            return true;
        }
        return false;
    }

    public static boolean showQrView(Context context, int layoutId, String text) {
        ISecondaryScreen screen = getSecondaryScreen();
        int[] resolution = getScreenResolution();
        if (screen != null && resolution != null) {
            View layoutV =
                    layoutToView(context, layoutId, resolution[0], resolution[1]);
            TextView textView = layoutV.findViewById(R.id.tv_amount);
            textView.setText(text);
            screen.show(layoutV, new IResultCallback() {
                        @Override
                        public void onSuccess() {

                        }

                        @Override
                        public void onFailure(int i, String s) {
                            markUnavailable("showQrView failed code=" + i + ", msg=" + s);
                        }
                    }
            );
            return true;
        }
        return false;
    }

    public static boolean showView(Context context, int layoutId, String text) {
        ISecondaryScreen screen = getSecondaryScreen();
        int[] resolution = getScreenResolution();
        if (screen != null && resolution != null) {
            View layoutV =
                    layoutToView(context, layoutId, resolution[0], resolution[1]);
            TextView textView = layoutV.findViewById(R.id.tv_amount);
            Log.e(TAG, "showView===>>>text:" + text);
            textView.setText(text);
            screen.show(layoutV, new IResultCallback() {
                        @Override
                        public void onSuccess() {

                        }

                        @Override
                        public void onFailure(int i, String s) {
                            markUnavailable("showView text failed code=" + i + ", msg=" + s);
                        }
                    }
            );
            return true;
        }
        return false;
    }

    public static boolean showDefaultImage() {
        String newPath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/Pictures/default.png";
        ISecondaryScreen screen = getSecondaryScreen();
        if (screen != null) {
            screen.showPic(newPath);
            return true;
        }
        return false;

    }


    public static View layoutToView(Context context, int layoutId, int widthPX, int heightPX) {
        LayoutInflater inflater = LayoutInflater.from(context);
        ViewGroup rootView = (ViewGroup) inflater.inflate(layoutId, null);

        // 精确测量和布局
        int widthSpec = View.MeasureSpec.makeMeasureSpec(widthPX, View.MeasureSpec.EXACTLY);
        int heightSpec = View.MeasureSpec.makeMeasureSpec(heightPX, View.MeasureSpec.EXACTLY);

        rootView.measure(widthSpec, heightSpec);
        rootView.layout(0, 0, widthPX, heightPX);

        return rootView;
    }
}
