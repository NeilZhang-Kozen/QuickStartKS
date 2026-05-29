package com.kozen.quickstartks.pinpad;

import static com.kozen.quickstartks.TransInitActivity.isUseDukpt;

import android.app.Activity;
import android.app.Dialog;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.OrientationEventListener;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.constraintlayout.widget.ConstraintLayout;

import com.kozen.component_client.ComponentEngine;
import com.kozen.financial.constant.ConstantEmv.POIEmvCoreManager;
import com.kozen.financial.constant.ConstantEmv.POIEmvCoreManager.EmvPinConstraints;
import com.kozen.financial.constant.ConstantSecurity;
import com.kozen.financial.emv.IEmvManager;
import com.kozen.financial.engine.FinancialEngine;
import com.kozen.financial.pinpad.IPinpadManager;
import com.kozen.financial.pinpad.PinViewEnum;
import com.kozen.financial.pinpad.PinpadInputCallback;
import com.kozen.financial.security.ISecurityManager;
import com.kozen.quickstartks.R;
import com.kozen.quickstartks.TransInitActivity;
import com.kozen.quickstartks.utils.HexUtil;
import com.kozen.quickstartks.utils.LogPrintUtil;
import com.kozen.quickstartks.utils.PinpadUtils;
import com.kozen.quickstartks.utils.PosUtils;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class PinPadDialog {

    public static final int PLAIN_PIN = 1;
    public static final int ONLINE_PIN = 2;
    public static final int ENCIPHER_PIN = 3;
    public static final int DEFAULT_KEY_NUMS = 12;
    private String DEFAULT_EXP_PIN_LEN_IND = "0,4,5,6,7,8,9,10,11,12";
    private int DEFAULT_TIMEOUT_MS = 30000;

    private int keyIndex;
    private int keyMode;

    // TODO: 2024/8/2 for DUKPT-AES testing
    private boolean aes = false;
    private int aesKeyLen = 256;
    public static final int PED_PINBLOCK_FETCH_MODE_DUKPT_AES = 0x03;

    // TODO: 2024/8/2 for AES-TPK testing
//    private boolean aes = true;
//    private int aesKeyLen = TR31KeyUtils.TR31KeyLength; // 128
//    private int keyMode = POIHsmManage.PED_PINBLOCK_FETCH_MODE_TPK;//using MK/SK, will use TPK to encrypt the PINBlock

    //    private int keyMode = POIHsmManage.PED_PINBLOCK_FETCH_MODE_DUKPT;
    private boolean isKeyboardFix = true;
    private boolean isEncrypt;
    private String pinCard;
    private int pinType;
    private boolean pinBypass;
    private int pinCounter;
    private byte[] pinRandom;
    private byte[] pinModule;
    private byte[] pinExponent;

    private String title;
    private String message;

    private IEmvManager emvManager;
    private IPinpadManager pinpadManager;
    private PinEventListener pinEventListener;
    private Dialog dialog;
    private TextView tvMessage;
    private EditText etPin;
    private Button btnConfirm;
    private View btnClear;
    private View[] pinDots;
    private TextView tvAmount;
    private TextView cardChip;
    private TextView btnEsc, btn0, btn1, btn2, btn3, btn4, btn5, btn6, btn7, btn8, btn9;
    PinInputFinish pinInputFinish;

    private static final String TAG = "PinPadAESDialog";
    private OrientationEventListener mOrientationListener;
    private int mOrientation = 0;
    private boolean isOrientation = false;
    private Bundle pinInfo;

    public interface PinInputFinish {
        void onSuccess(byte[] pinBlock, byte[] pinKsn);

        void onError(int verifyResult, int pinTryCntOut);
    }

    public PinPadDialog(Activity context, Bundle bundle, int keyMode, int keyIndex, PinInputFinish pinInputFinish) {
        this.emvManager = TransInitActivity.isFinancialInit ? FinancialEngine.INSTANCE.getEmvManager() : null;
        this.pinpadManager = TransInitActivity.isFinancialInit ? FinancialEngine.INSTANCE.getPinpadManager() : null;
        this.pinEventListener = new PinEventListener();
        this.keyMode = keyMode;
        this.keyIndex = keyIndex;
        this.pinInputFinish = pinInputFinish;
        pinInfo = bundle;
        pinInfo.putInt(EmvPinConstraints.PIN_KEY_INDEX, keyIndex);
        if (isUseDukpt){
            pinInfo.putInt(EmvPinConstraints.PIN_BLOCK_FORMAT, ConstantSecurity.PINBLOCK_DUKPT_FMT_ISO9564_0);
            pinInfo.putInt(EmvPinConstraints.PIN_KEY_MODE, ConstantSecurity.ENCRYPTION_MECHANISM_DUKPT);
        }else {
            pinInfo.putInt(EmvPinConstraints.PIN_BLOCK_FORMAT, ConstantSecurity.PINBLOCK_TPK_FMT_ISO9564_0);
            pinInfo.putInt(EmvPinConstraints.PIN_KEY_MODE, ConstantSecurity.ENCRYPTION_MECHANISM_MK_SK);
        }
        pinInfo.putInt(EmvPinConstraints.PIN_KEY_ALGORITHM, ConstantSecurity.ENCRYPTION_ALGORITHM_TDES);

        pinInfo.putInt(EmvPinConstraints.PIN_TIMEOUT, 60);
        pinInfo.putByteArray(EmvPinConstraints.PIN_LENGTH_LIMIT, new byte[]{0x0, 0x04, 0x05, 0x06});


        switch (bundle.getInt(EmvPinConstraints.PIN_TYPE, -1)) {
            case POIEmvCoreManager.PIN_PLAIN_PIN:
                pinType = PLAIN_PIN;
                break;
            case POIEmvCoreManager.PIN_ONLINE_PIN:
                pinType = ONLINE_PIN;
                break;
            case POIEmvCoreManager.PIN_ENCIPHER_PIN:
                pinType = ENCIPHER_PIN;
                break;
            default:
                break;
        }

        if (bundle.containsKey(EmvPinConstraints.PIN_ENCRYPT)) {
            isEncrypt = bundle.getBoolean(EmvPinConstraints.PIN_ENCRYPT);
        }
        if (bundle.containsKey(EmvPinConstraints.PIN_CARD)) {
            pinCard = bundle.getString(EmvPinConstraints.PIN_CARD);
        }
        if (bundle.containsKey(EmvPinConstraints.PIN_BYPASS)) {
            pinBypass = bundle.getBoolean(EmvPinConstraints.PIN_BYPASS);
        }
        if (bundle.containsKey(EmvPinConstraints.PIN_COUNTER)) {
            pinCounter = bundle.getInt(EmvPinConstraints.PIN_COUNTER);
        }
        if (bundle.containsKey(EmvPinConstraints.PIN_CARD_RANDOM)) {
            pinRandom = bundle.getByteArray(EmvPinConstraints.PIN_CARD_RANDOM);
        }
        if (bundle.containsKey(EmvPinConstraints.PIN_MODULE)) {
            pinModule = bundle.getByteArray(EmvPinConstraints.PIN_MODULE);
        }
        if (bundle.containsKey(EmvPinConstraints.PIN_EXPONENT)) {
            pinExponent = bundle.getByteArray(EmvPinConstraints.PIN_EXPONENT);
        }

        switch (pinType) {
            case ONLINE_PIN:
                title = "Online PIN";
                break;
            case PLAIN_PIN:
            case ENCIPHER_PIN:
                title = "Offline PIN";
                if (pinCounter > 1) {
                    message = "PIN " + pinCounter + " ";
                } else if (pinCounter == 1) {
                    message = "PIN Last Times";
                }
                break;
            default:
                break;
        }

        LayoutInflater inflater = LayoutInflater.from(context);
        ConstraintLayout view = (ConstraintLayout) inflater.inflate(R.layout.layout_password, null);
        TextView tvTitle = view.findViewById(R.id.tvTitle);
        tvMessage = view.findViewById(R.id.tvMessage);
        tvAmount = view.findViewById(R.id.tvAmount);
        cardChip = view.findViewById(R.id.cardChip);
        etPin = view.findViewById(R.id.etPin);
        btnConfirm = view.findViewById(R.id.btnConfirm);
        btnClear = view.findViewById(R.id.btnClear);
        btnEsc = view.findViewById(R.id.btnEsc);
        btn0 = view.findViewById(R.id.btn0);
        btn1 = view.findViewById(R.id.btn1);
        btn2 = view.findViewById(R.id.btn2);
        btn3 = view.findViewById(R.id.btn3);
        btn4 = view.findViewById(R.id.btn4);
        btn5 = view.findViewById(R.id.btn5);
        btn6 = view.findViewById(R.id.btn6);
        btn7 = view.findViewById(R.id.btn7);
        btn8 = view.findViewById(R.id.btn8);
        btn9 = view.findViewById(R.id.btn9);

        pinDots = new View[]{
                view.findViewById(R.id.pin_dot_0),
                view.findViewById(R.id.pin_dot_1),
                view.findViewById(R.id.pin_dot_2),
                view.findViewById(R.id.pin_dot_3),
                view.findViewById(R.id.pin_dot_4),
                view.findViewById(R.id.pin_dot_5),
        };

//        Group groupKeyboard = view.findViewById(R.id.groupKeyboard);
//        if (DeviceConfig.isHardwareKeyboard) {
//            groupKeyboard.setVisibility(View.GONE);
//        }

        if (cardChip != null && title != null && !title.isEmpty()) {
            cardChip.setText(title);
        }
        if (message != null && !message.isEmpty()) {
            tvMessage.setText(message);
        }
        if (tvAmount != null) {
            com.kozen.quickstartks.TransActivity host = com.kozen.quickstartks.TransActivity.getInstance();
            if (host != null) {
                String prefix = com.kozen.quickstartks.utils.Utils.USD_TAG.equals(host.currency) ? "$" : "€";
                tvAmount.setText(prefix + host.eAmount);
            }
        }

        dialog = new Dialog(context, android.R.style.Theme_Translucent_NoTitleBar);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(view);
        Window window = dialog.getWindow();
        WindowManager.LayoutParams wlp = window.getAttributes();

        wlp.gravity = Gravity.CENTER;
        wlp.flags &= ~WindowManager.LayoutParams.FLAG_BLUR_BEHIND;
        window.setAttributes(wlp);
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        window.setGravity(Gravity.BOTTOM);
        dialog.show();
    }

    public int showDialog() {
        int result;

        switch (pinType) {
            case PLAIN_PIN:
                result = onVerifyPlainPin();
                break;
            case ONLINE_PIN:
                result = onOnlinePin();
                break;
            case ENCIPHER_PIN:
                result = onVerifyEncipherPin();
                break;
            default:
                result = -1;
                break;
        }

        return result;
    }

    public void closeDialog() {
        dialog.dismiss();
        mOrientationListener.disable();
    }

    private Map<String, View> buildViewMap() {
        Map<String, View> viewMap = new LinkedHashMap<>();
        viewMap.put(PinViewEnum.BUTTON0.getType(), btn0);
        viewMap.put(PinViewEnum.BUTTON1.getType(), btn1);
        viewMap.put(PinViewEnum.BUTTON2.getType(), btn2);
        viewMap.put(PinViewEnum.BUTTON3.getType(), btn3);
        viewMap.put(PinViewEnum.BUTTON4.getType(), btn4);
        viewMap.put(PinViewEnum.BUTTON5.getType(), btn5);
        viewMap.put(PinViewEnum.BUTTON6.getType(), btn6);
        viewMap.put(PinViewEnum.BUTTON7.getType(), btn7);
        viewMap.put(PinViewEnum.BUTTON8.getType(), btn8);
        viewMap.put(PinViewEnum.BUTTON9.getType(), btn9);
        viewMap.put(PinViewEnum.BUTTON_ESC.getType(), btnEsc);
        viewMap.put(PinViewEnum.BUTTON_BACKSPACE.getType(), btnClear);
        viewMap.put(PinViewEnum.BUTTON_ENTER.getType(), btnConfirm);
        return viewMap;
    }

    private void startInputPinDeferred() {
        Runnable invoke = new Runnable() {
            @Override
            public void run() {
                Map<String, View> viewMap = buildViewMap();
                Log.d(TAG, "startInputPin deferred: btn0 w=" + btn0.getWidth() + " h=" + btn0.getHeight());
                pinpadManager.startInputPin(pinInfo, viewMap, pinEventListener);
            }
        };
        View decor = dialog.getWindow() != null ? dialog.getWindow().getDecorView() : null;
        if (decor != null) {
            decor.post(invoke);
        } else {
            invoke.run();
        }
    }

    private int onVerifyPlainPin() {
        startInputPinDeferred();
        return 0;
    }

    private int onVerifyEncipherPin() {
        ISecurityManager securityManager = TransInitActivity.isFinancialInit ? FinancialEngine.INSTANCE.getSecurityManager() : null;
        if (pinModule == null || securityManager == null) {
            return -1;
        }

        securityManager.writeRsaKey(keyIndex, pinModule, pinExponent);
        startInputPinDeferred();
        return 0;
    }

    private int onOnlinePin() {
        // XCSW add for PinpadRotate start
        mOrientationListener = new OrientationEventListener(dialog.getContext(), SensorManager.SENSOR_DELAY_NORMAL) {
            @Override
            public void onOrientationChanged(int orientation) {
                int curOrientation = PinpadUtils.doGetScreenOrientation(dialog.getContext());
                if (curOrientation != mOrientation) {
                    mOrientation = curOrientation;
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            isOrientation = true;
                            startInputPinDeferred();
                        }
                    }, 100);
                }
            }
        };
        if (PinpadUtils.doCheckSupportRotate() && mOrientationListener.canDetectOrientation()) {
            mOrientation = PinpadUtils.doGetScreenOrientation(dialog.getContext());
            mOrientationListener.enable();
        } else {
            mOrientationListener.disable();
        }


        byte[] data = new byte[24];
        byte pinblockFormat;
        // this part is for AES PINBLOCK handling
        if (aes) {
            if (!isEncrypt) {
                byte[] temp = HexUtil.parseHex(CalcPIN.calcPinBlock(pinCard));
                System.arraycopy(temp, 0, data, 0, 16);
            } else {
                byte[] temp = pinCard.getBytes();
                System.arraycopy(temp, 0, data, 0, 16);
            }
            //AES PINBLOCK Format
            pinblockFormat = 0x04;
            if (keyMode == PED_PINBLOCK_FETCH_MODE_DUKPT_AES) {
                if (aesKeyLen == 128) {
                    keyMode = 0x03 | (0x02 << 4);
                } else if (aesKeyLen == 192) {
                    keyMode = 0x03 | (0x03 << 4);
                } else if (aesKeyLen == 256) {
                    keyMode = 0x03 | (0x04 << 4);
                }
            } else {
                keyMode = 1;
            }
        }
        // this part is for 3DES PINBLOCK handling
        else {
            if (!isEncrypt) {
                byte[] temp = CalcPinBlock.calcPinBlock(pinCard).getBytes();
                System.arraycopy(temp, 0, data, 0, 16);
            } else {
                byte[] temp = pinCard.getBytes();
                System.arraycopy(temp, 0, data, 0, 16);
            }
            pinblockFormat = 0x00;
        }

        byte[] formatData = {0, 0, 0, 0, 0, 0, 0, 0};
        System.arraycopy(formatData, 0, data, 16, 8);
        Log.d(TAG, "onOnlinePin keyMode:" + keyMode);
        Log.d(TAG, "onOnlinePin keyIndex:" + keyIndex);
        Log.d(TAG, "onOnlinePin pinblockFormat:" + pinblockFormat);
        Log.d(TAG, "onOnlinePin data:" + HexUtil.toHexString(data));

//        return hsmManage.PedGetPinBlock(keyMode, keyIndex, 0, DEFAULT_TIMEOUT_MS, data, DEFAULT_EXP_PIN_LEN_IND);
        LogPrintUtil.printBundle(pinInfo, TAG);
        startInputPinDeferred();
        return 0;
    }

    private class PinEventListener implements PinpadInputCallback {

        private String TAG = "PinEventListener";


        @Override
        public void onInput(int len, int key) {
            Log.d(TAG, "onInput====>>len:" + len + " key:" + key);
            //FinancialEngine.INSTANCE.getGeneralManager().setBeep(true, 200, 200);
            final int filled = len;
            if (etPin != null) {
                final StringBuilder sb = new StringBuilder();
                for (int i = 0; i < filled; i++) {
                    sb.append('*');
                }
                etPin.post(new Runnable() {
                    @Override
                    public void run() {
                        etPin.setText(sb.toString());
                    }
                });
            }
            if (pinDots != null && pinDots.length > 0) {
                pinDots[0].post(new Runnable() {
                    @Override
                    public void run() {
                        for (int i = 0; i < pinDots.length; i++) {
                            if (pinDots[i] == null) continue;
                            pinDots[i].setBackgroundResource(i < filled
                                    ? R.drawable.bg_pin_dot_filled
                                    : R.drawable.bg_pin_dot_empty);
                        }
                    }
                });
            }
        }

        @Override
        public void onPinSuccess(int verifyResult, byte[] pinBlock, String ksn) {
            Log.d(TAG, "onPinSuccess====>>verifyResult:" + verifyResult);
            myOnPinSuccess(pinBlock, PosUtils.hexStringToBytes(ksn));
            closeDialog();
        }

        @Override
        public void onPinError(int verifyResult, int pinTryCntOut) {
            Log.e(TAG, "onPinError====>>verifyResult:" + verifyResult);
            myOnPinError(verifyResult, pinTryCntOut);
            closeDialog();
        }

        @Override
        public void onScreenRotation() {
            closeDialog();
        }
    }

    private void myOnPinSuccess(byte[] pinBlock, byte[] pinKsn) {
        Bundle bundle = new Bundle();
        bundle.putInt(EmvPinConstraints.OUT_PIN_VERIFY_RESULT, EmvPinConstraints.VERIFY_SUCCESS);
        bundle.putInt(EmvPinConstraints.OUT_PIN_TRY_COUNTER, 0);
        if (pinBlock != null) {
            bundle.putByteArray(EmvPinConstraints.OUT_PIN_BLOCK, pinBlock);
        }
        if (emvManager != null) {
            emvManager.setPinResponse(bundle);
        }
        pinInputFinish.onSuccess(pinBlock, pinKsn);
    }

    private void myOnPinError(int verifyResult, int pinTryCntOut) {
        Bundle bundle = new Bundle();
        bundle.putInt(EmvPinConstraints.OUT_PIN_VERIFY_RESULT, verifyResult);
        bundle.putInt(EmvPinConstraints.OUT_PIN_TRY_COUNTER, pinTryCntOut);
        if (emvManager != null) {
            emvManager.setPinResponse(bundle);
        }
        pinInputFinish.onError(verifyResult, pinTryCntOut);
    }

    private byte[] calculation(byte[] keys) {
        int key_nums = DEFAULT_KEY_NUMS;
        if (key_nums == 11) {
            btnClear.setVisibility(View.GONE);
        }

        HashMap<String, String> map = new HashMap<>();
//        ByteBuffer coordinate = ByteBuffer.allocate(104);
//        ByteBuffer coordinate = ByteBuffer.allocate(96);
        ByteBuffer coordinate = ByteBuffer.allocate((key_nums + 1) * 8);

        String esc = "Esc";
        String enter = "Enter";
        String clear = "Clear";

        map.put("0", "0");
        map.put("1", "1");
        map.put("2", "2");
        map.put("3", "3");
        map.put("4", "4");
        map.put("5", "5");
        map.put("6", "6");
        map.put("7", "7");
        map.put("8", "8");
        map.put("9", "9");
        map.put("-21", esc);
        map.put("-35", enter);
        map.put("-40", clear);

        TextView[] keyView = new TextView[11];

        if (isKeyboardFix) {
            keyView[0] = btn1;
            keyView[1] = btn2;
            keyView[2] = btn3;
            keyView[3] = btn4;
            keyView[4] = btn5;
            keyView[5] = btn6;
            keyView[6] = btn7;
            keyView[7] = btn8;
            keyView[8] = btn9;
            keyView[9] = btn0;
        } else {
            keyView[0] = btn0;
            keyView[1] = btn1;
            keyView[2] = btn2;
            keyView[3] = btn3;
            keyView[4] = btn4;
            keyView[5] = btn5;
            keyView[6] = btn6;
            keyView[7] = btn7;
            keyView[8] = btn8;
            keyView[9] = btn9;
            keyView[10] = btnEsc;
        }

        View ivClear = btnClear;
        Button btnConfirm = this.btnConfirm;
        int viewIndex = 0;

        for (int i = 0; i <= 12; i++) {
            String value = map.get(String.valueOf(keys[i] - 0x30));
            View tv;

            if (value == null) {
                continue;
            } else if (value.equals(enter)) {
                tv = btnConfirm;
            } else if (value.equals(clear)) {
                tv = ivClear;
            } else {
                if (value.equals(esc)) {
                    tv = btnEsc;
                } else {
                    keyView[viewIndex].setText(value);
                    tv = keyView[viewIndex++];
                }
            }

            byte[] pos = new byte[8];
            int[] location = new int[2];
            tv.getLocationOnScreen(location);
            int leftX = location[0];
            int leftY = location[1];
            int rightX = location[0] + tv.getWidth();
            int rightY = location[1] + tv.getHeight();
            byte[] tmp0 = intToBytes(leftX);
            byte[] tmp1 = intToBytes(leftY);
            byte[] tmp2 = intToBytes(rightX);
            byte[] tmp3 = intToBytes(rightY);
            pos[0] = tmp0[2];
            pos[1] = tmp0[3];
            pos[2] = tmp1[2];
            pos[3] = tmp1[3];
            pos[4] = tmp2[2];
            pos[5] = tmp2[3];
            pos[6] = tmp3[2];
            pos[7] = tmp3[3];
            coordinate.put(pos);
        }
        // XCSW add for PinpadRotate start
        if (PinpadUtils.doCheckSupportRotate()) {
            coordinate = PinpadUtils.doTransformData(dialog.getContext(), coordinate, key_nums + 1);
        }
        return coordinate.array();
    }

    private void switchFixToRandom(byte[] fixKeyLayout, byte[] random, byte[] randomKeyLayout) {
        int position;
        System.arraycopy(fixKeyLayout, 0, randomKeyLayout, 0, fixKeyLayout.length);
        for (int i = 0; i < random.length; i++) {
            if (i != 9 && i <= 10) {
                if ((random[i] - 0x30) != 0) {
                    position = (random[i] - 0x30 - 1) * 8;
                } else {
                    position = 10 * 8;
                }
                System.arraycopy(fixKeyLayout, position, randomKeyLayout, i * 8, 8);
            }
        }
    }

    private byte[] intToBytes(int value) {
        return new byte[]{(byte) (value >> 24 & 255), (byte) (value >> 16 & 255), (byte) (value >> 8 & 255), (byte) (value & 255)};
    }

    static class CalcPIN {

        private static String calcPinBlock(String account) {
            String bcd = convert(account);
            int len = bcd.length() / 2;
            String str = String.format("%02X%s", len, account);
            return zero(str, 32);
        }

        private static String convert(String input) {
            StringBuilder sb = new StringBuilder();
            for (char c : input.toCharArray()) {
                sb.append(String.format("%02X", Character.getNumericValue(c)));
            }
            return sb.toString();
        }

        private static String zero(String str, int len) {
            StringBuilder sb = new StringBuilder(len);
            sb.append(str);
            int fill = len - str.length();
            while (fill-- > 0) {
                sb.append("0");
            }
            return sb.toString();
        }
    }

    static class CalcPinBlock {

        static String calcPinBlock(String accountNumber) {
            return "0000" + extractAccountNumberPart(accountNumber);
        }

        static String extractAccountNumberPart(String accountNumber) {
            String accountNumberPart;
            accountNumberPart = takeLastN(accountNumber, 13);
            accountNumberPart = takeFirstN(accountNumberPart, 12);
            return accountNumberPart;
        }

        static String takeLastN(String str, int n) {
            if (str.length() > n) {
                return str.substring(str.length() - n);
            } else {
                if (str.length() < n) {
                    return zero(str, n);
                } else {
                    return str;
                }
            }
        }

        static String takeFirstN(String str, int n) {
            if (str.length() > n) {
                return str.substring(0, n);
            } else {
                if (str.length() < n) {
                    return zero(str, n);
                } else {
                    return str;
                }
            }
        }

        static String zero(String str, int len) {
            str = str.trim();
            StringBuilder builder = new StringBuilder(len);
            int fill = len - str.length();
            while (fill-- > 0) {
                builder.append((char) 0);
            }
            builder.append(str);
            return builder.toString();
        }
    }
}
