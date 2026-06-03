package com.kozen.quickstartks;

import static com.kozen.quickstartks.TransInitActivity.isUseDukpt;
import static com.kozen.quickstartks.utils.SharePreferenceUtils.CAN_MAGSTRIPE;
import static com.kozen.quickstartks.utils.Utils.CURRENCY_TAG;
import static com.kozen.quickstartks.utils.Utils.USD_TAG;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.AnimationUtils;
import android.view.animation.TranslateAnimation;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.kozen.component.constant.KeyboardConstant;
import com.kozen.component.keyboard.InputCallback;
import com.kozen.component_client.ComponentEngine;
import com.kozen.financial.constant.ConstantEmv;
import com.kozen.financial.constant.ConstantEmv.POIEmvCoreManager;
import com.kozen.quickstartks.emv.EmvListenerImplPOI;
import com.kozen.financial.constant.ConstantEmv.POIEmvCoreManager.EmvCardInfoConstraints;
import com.kozen.financial.constant.ConstantEmv.POIEmvCoreManager.EmvOnlineConstraints;
import com.kozen.financial.constant.ConstantEmv.POIEmvCoreManager.EmvPinConstraints;
import com.kozen.financial.constant.ConstantEmv.POIEmvCoreManager.EmvTransDataConstraints;
import com.kozen.financial.constant.ConstantEmv.PosEmvErrorCode;
import com.kozen.financial.constant.ConstantSecurity;
import com.kozen.financial.emv.IEmvListener;
import com.kozen.financial.emv.IEmvManager;
import com.kozen.financial.engine.FinancialEngine;
import com.kozen.financial.general.IGeneralManager;
import com.kozen.quickstartks.emv.EmvListenerImpl;
import com.kozen.quickstartks.pinpad.MaterialDialog;
import com.kozen.quickstartks.pinpad.PinPadDialog;
import com.kozen.quickstartks.pinpad.PinPadPHY;
import com.kozen.quickstartks.utils.AppExecutors;
import com.kozen.quickstartks.utils.BundleUtil;
import com.kozen.quickstartks.utils.DialogUtils;
import com.kozen.quickstartks.utils.HexUtil;
import com.kozen.quickstartks.utils.LogPrintUtil;
import com.kozen.quickstartks.utils.ParameterInit;
import com.kozen.quickstartks.utils.PosUtils;
import com.kozen.quickstartks.utils.ScreenUtils;
import com.kozen.quickstartks.utils.SecondScreenUtils;
import com.kozen.quickstartks.utils.SharePreferenceUtils;
import com.kozen.quickstartks.utils.Utils;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class TransActivity extends BaseActivity {

    private static final String TAG = "TransActivity";
    public static TextView tvMessage1;
    private TextView tv_cancel;
    private static int transType = -1;
    private static TransActivity sInstance;

    public boolean isFallBack;
    public com.pos.sdk.emvcore.POIEmvCoreManager emvPoiManager;
    public IEmvManager emvManager;
    private EmvListenerImpl emvListener;
    private EmvListenerImplPOI emvListenerPoi;

    public String eAmount = "1500";
    private boolean transactionFinishing = false;
    public boolean paymentCallbackDispatched = false;
    private int mTransResult = -1;
    public byte[] transData;
    private LinearLayout ll_light;
    private ImageView iv1;
    private ImageView iv2;
    private ImageView iv3;
    private ImageView iv4;
    private ImageView iv_present_hand;
    private TextView tvReaderStatus;
    private TextView tvReaderHint;

    public final static int TRANS_CONTACT = 1;
    public final static int TRANS_CONTACTLESS = 2;
    public final static String TransResult_Amount = "TransResult_Amount";
    public final static String TransResult_Data = "TransResult_Data";
    public final static String TransResult_Code = "TransResult_Code";
    public final static String TransResult_Card_Type = "TransResult_Card_Type";
    private int Card_Type;
    private final Executor executor = Executors.newSingleThreadExecutor();
    boolean detectedIcc = false;
    boolean detectedPicc = false;
    public String currency = "";
    public PaymentOrder paymentOrder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sInstance = this;
        setContentView(R.layout.activity_trans_card);
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }
        getWindow().setStatusBarColor(getResources().getColor(R.color.card_dark_mid));
        View decor = getWindow().getDecorView();
        decor.setSystemUiVisibility(decor.getSystemUiVisibility() & ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);

        String amount = getIntent().getStringExtra("amount");
        currency = getIntent().getStringExtra(CURRENCY_TAG);
        paymentOrder = PaymentOrder.get(getIntent());
        if (!TextUtils.isEmpty(amount)) {
            eAmount = amount;
        }
        ll_light = findViewById(R.id.ll_light);
        ll_light.setVisibility(View.GONE);
        iv1 = findViewById(R.id.iv1);
        iv2 = findViewById(R.id.iv2);
        iv3 = findViewById(R.id.iv3);
        iv4 = findViewById(R.id.iv4);
        iv_present_hand = findViewById(R.id.iv_present_hand);
        tvReaderStatus = findViewById(R.id.tv_reader_status);
        tvReaderHint = findViewById(R.id.tv_reader_hint);


        tvMessage1 = findViewById(R.id.tvMessage1);
        tv_cancel = findViewById(R.id.tv_cancel);
        TextView tv_amount = findViewById(R.id.tv_amount);
        TextView tv_order_num = findViewById(R.id.tv_order_num);
        TextView tv_order_time = findViewById(R.id.tv_order_time);

        tv_amount.setText((USD_TAG.equals(currency) ? "$" : "€") + eAmount);
        String displayOrderNo = paymentOrder != null && !TextUtils.isEmpty(paymentOrder.orderNo)
                ? paymentOrder.orderNo
                : Utils.getCurrentTime2() + Utils.getRandomData();
        String displayOrderTime = paymentOrder != null && !TextUtils.isEmpty(paymentOrder.orderTime)
                ? paymentOrder.orderTime
                : Utils.getCurrentTime();
        String displayOrderInfo = paymentOrder != null && !TextUtils.isEmpty(paymentOrder.orderInfo)
                ? paymentOrder.orderInfo
                : getString(R.string.label_demo_items);
        tv_order_num.setText(displayOrderNo);
        tv_order_time.setText(displayOrderTime);
        paymentOrder = PaymentOrder.fromContext(paymentOrder, eAmount, currency, displayOrderNo, displayOrderTime, displayOrderInfo);
        transType = 0;


        tv_cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                transactionFinishing = true;
                if (TransInitActivity.isPOISdk) {
                    if (emvPoiManager != null) {
                        emvPoiManager.stopTransaction();
                    }
                } else {
                    if (emvManager != null) {
                        emvManager.stopTransaction();
                    }
                }
                dispatchPaymentCallback("CANCELLED", "CARD", null, getString(R.string.trans_user_cancel));
                finish();
            }
        });
        tv_cancel.setVisibility(View.VISIBLE);

        if (TransInitActivity.isPOISdk) {
            emvListenerPoi = new EmvListenerImplPOI(TransActivity.this);
            emvPoiManager = com.pos.sdk.emvcore.POIEmvCoreManager.getDefault();
        } else {
            emvListener = new EmvListenerImpl(TransActivity.this);
            emvManager = TransInitActivity.isFinancialInit ? FinancialEngine.INSTANCE.getEmvManager() : null;
            if (TransInitActivity.isExistSecScreen) {
                SecondScreenUtils.showView(TransActivity.this, R.layout.second_swipe_card_waiting, (USD_TAG.equals(currency) ? "$" : "€") + amount);
                if ("K1211".equals(Build.MODEL)) {
                    int code = ComponentEngine.INSTANCE.getKeyboardManager().startPhysicalKeyboard(new InputCallback() {
                        @Override
                        public void onKey(KeyboardConstant.KeyCode keyCode, KeyboardConstant.KeyAction keyAction) {
//                                    String text = keyCode.getValue()+"=="+((keyAction.getAction() == 0)? "按下":"起");
//                                    Toast.makeText(TransInitActivity.this,text,Toast.LENGTH_SHORT).show();

                            if ((keyAction.getAction() != 0)) {
                                if (TransActivity.this.getWindow().getDecorView().getVisibility() == View.VISIBLE) {
                                    if (KeyboardConstant.KeyCode.BUTTON_ENTER == keyCode) {

                                    } else if (KeyboardConstant.KeyCode.BUTTON_ESC == keyCode) {
                                        transactionFinishing = true;
                                        if (emvManager != null) {
                                            emvManager.stopTransaction();
                                        }
                                        dispatchPaymentCallback("CANCELLED", "CARD", null, getString(R.string.trans_user_cancel));
                                        finish();
                                    }
                                }
                            }

                        }
                    });
                }
            }
        }
        onTransStart();
    }

    public static TransActivity getInstance() {
        return sInstance;
    }

    public void dispatchPaymentCallback(String status, String paymentMethod,
                                        ReceiptBuilder.ReceiptData receiptData, String errorMessage) {
        if (paymentCallbackDispatched) {
            return;
        }
        paymentCallbackDispatched = true;
        PaymentCallbackDispatcher.dispatch(TransActivity.this, paymentOrder, status, paymentMethod, receiptData, errorMessage);
    }

    public void showOnlineAuthorizing() {
        tvMessage1.setText(R.string.card_online_authorizing);
        tvReaderStatus.setText(R.string.card_online_authorizing);
        tvReaderHint.setText(R.string.card_online_authorizing_hint);
        iv_present_hand.clearAnimation();
        ll_light.setVisibility(View.GONE);
    }

    private Bundle getEncryptConfigByDupktTDES() {
        Bundle bundle = new Bundle();
        bundle.putInt(EmvTransDataConstraints.ENCRYPT_KEY_INDEX, ParameterInit.KeyIndexConstants.DUKPT_PIN_KEY_INDEX);
        bundle.putInt(EmvTransDataConstraints.ENCRYPT_TYPE, EmvTransDataConstraints.ENCRYPT_TYPE_DUKPT_DATA_REQUEST);
        bundle.putByte(EmvTransDataConstraints.ENCRYPT_PADDING, (byte)0x30);
        bundle.putInt(EmvTransDataConstraints.ENCRYPT_MODE, EmvTransDataConstraints.ENCRYPT_MODE_CBC);
        bundle.putByteArray(EmvTransDataConstraints.ENCRYPT_VECTOR, HexUtil.parseHex("0000000000000000"));
        bundle.putBoolean(EmvTransDataConstraints.ENCRYPT_EMV_DATA, true);
        bundle.putBoolean(EmvTransDataConstraints.ENCRYPT_BASE64, false);
        return bundle;
    }
    public void onTransStart() {

        TranslateAnimation translateAnimation = (TranslateAnimation) AnimationUtils.loadAnimation(TransActivity.this, R.anim.anim_move);
        iv_present_hand.startAnimation(translateAnimation);

        long amount = 1500;
        long amountOther = 0;
        if (!TextUtils.isEmpty(eAmount)) {
            amount = PosUtils.strAmount2Long(eAmount);
            Log.d(TAG, "eAmount:" + eAmount + " amount:" + amount);
        }
        ll_light.setVisibility(View.GONE);

        tvMessage1.setText(getString(R.string.trans_present));
//        TextView tv_loading_dot1 = findViewById(R.id.tv_loading_dot1);
//        TextView tv_loading_dot2 = findViewById(R.id.tv_loading_dot2);
//        TextView tv_loading_dot3 = findViewById(R.id.tv_loading_dot3);
//        startLightAnim(tv_loading_dot1,1200,0,-1);
//        startLightAnim(tv_loading_dot2,1200,1,-1);
//        startLightAnim(tv_loading_dot3,1200,2,-1);


        if (!TransInitActivity.isPOISdk) {
            FinancialEngine.INSTANCE.getGeneralManager().setSystemProperty("persist.sys.pos_pin_sound", "1");
        }
        try {
            Bundle bundle = new Bundle();

            bundle.putInt(EmvTransDataConstraints.TRANS_TYPE, transType);
            bundle.putLong(EmvTransDataConstraints.TRANS_AMOUNT, amount);
            bundle.putLong(EmvTransDataConstraints.TRANS_AMOUNT_OTHER, amountOther);

            if (isUseDukpt) {
                Bundle encryptBundle = getEncryptConfigByDupktTDES();
                if (isFallBack) {
                    bundle.putInt(EmvTransDataConstraints.OPEN_ENCRYPT, EmvTransDataConstraints.ENCRYPT_OPEN_CONTACT);
                    bundle.putBundle(EmvTransDataConstraints.ENCRYPT_MAGSTRIPE, encryptBundle);
                } else {
                    int mode = 0;
                    //SupportContact
                    mode |= EmvTransDataConstraints.ENCRYPT_OPEN_CONTACT;
                    //SupportContactless
                    mode |= EmvTransDataConstraints.ENCRYPT_OPEN_CONTACTLESS;
                    //SupportMagstripe
                    mode |= EmvTransDataConstraints.ENCRYPT_OPEN_MAGSTRIPE;
                    bundle.putInt(EmvTransDataConstraints.OPEN_ENCRYPT, mode);

                    bundle.putBundle(EmvTransDataConstraints.ENCRYPT_CONTACT, encryptBundle);
                    bundle.putBundle(EmvTransDataConstraints.ENCRYPT_CONTACTLESS, encryptBundle);
                    bundle.putBundle(EmvTransDataConstraints.ENCRYPT_MAGSTRIPE, encryptBundle);
                }

            } else {
                if (isFallBack) {
                    bundle.putInt(EmvTransDataConstraints.TRANS_MODE, POIEmvCoreManager.DEVICE_MAGSTRIPE);

                } else {
                    int mode = 0;
                    //SupportContact
                    mode |= POIEmvCoreManager.DEVICE_CONTACT;
                    //SupportContactless
                    mode |= POIEmvCoreManager.DEVICE_CONTACTLESS;
                    //SupportMagstripe
                    mode |= POIEmvCoreManager.DEVICE_MAGSTRIPE;
                    bundle.putInt(EmvTransDataConstraints.TRANS_MODE, mode);
                }
            }

            bundle.putBoolean(EmvTransDataConstraints.TRANS_FALLBACK, true);
            bundle.putInt(EmvTransDataConstraints.TRANS_TIMEOUT, 60);

            //Todo// When the magstripe or contact card recognized as contactless, when detect contactless, whether wait magstripe(or contact) card
            bundle.putBoolean(EmvTransDataConstraints.SPECIAL_CONTACT, false);  //normal is true
            bundle.putBoolean(EmvTransDataConstraints.SPECIAL_MAGSTRIPE, false);  //normal is true

//            Todo// When the magstripe or contact card recognized as contactless,when detect contactless, then wait magstripe(or contact) card time(ms)
//            Todo this time will delay the contactless
//            bundle.putInt(EmvTransDataConstraints.SPECIAL_CONTACT_TIME, 500);
//            bundle.putInt(EmvTransDataConstraints.SPECIAL_MAGSTRIPE_TIME, 500);

//            bundle.putBoolean(EmvTransDataConstraints.TRANS_FALLBACK, true);
//            transType = POIEmvCoreManager.EMV_REFUND;
            transType = POIEmvCoreManager.EMV_GOODS;
            bundle.putInt(EmvTransDataConstraints.TRANS_TYPE, transType);
//            bundle.putByte(EmvTransDataConstraints.SPECIAL_TYPE,
//                    PosUtils.hexStringToBytes("20")[0]);

//            bundle.putByte(EmvTransDataConstraints.SPECIAL_TYPE, HexUtil.parseHex("30")[0]);

            bundle.putBoolean(EmvTransDataConstraints.USE_FILTER, true);
            bundle.putBoolean(EmvTransDataConstraints.USE_DELAY_PIN, true);
            bundle.putBoolean(EmvTransDataConstraints.USE_SELECT_KERNEL, false);
//            getSdkVersion();
            //bundle.putBoolean(EmvTransDataConstraints.SPECIAL_CONTACT, true);
            //bundle.putInt(EmvTransDataConstraints.SPECIAL_CONTACT_TIME, 500);
            boolean isMag = SharePreferenceUtils.getBoolean(TransActivity.this, CAN_MAGSTRIPE, false);
//            if (isMag) {
//                bundle.putBoolean(EmvTransDataConstraints.SPECIAL_MAGSTRIPE, true);
//                bundle.putInt(EmvTransDataConstraints.SPECIAL_MAGSTRIPE_TIME, 800);
//            } else {
//                bundle.putBoolean(EmvTransDataConstraints.SPECIAL_MAGSTRIPE, false);
//                bundle.putInt(EmvTransDataConstraints.SPECIAL_MAGSTRIPE_TIME, 20);
//            }


            bundle.putBoolean(EmvTransDataConstraints.USE_CARD_READ_SUCCESS, true);

            Log.d("onTransStart", "onTransStart bundle: " +
                    BundleUtil.showKeyTypesInBundle(bundle));
            int result;
            if (TransInitActivity.isPOISdk) {
                if (emvPoiManager == null) {
                    showTransactionStartFailed();
                    return;
                }
                result = emvPoiManager.startTransaction(bundle, emvListenerPoi);
            } else {
                if (emvManager == null) {
                    showTransactionStartFailed();
                    return;
                }
                result = emvManager.startTransaction(bundle, emvListener);
            }


            isFallBack = false;

            if (PosEmvErrorCode.EXCEPTION_ERROR == result) {
                Toast.makeText(this, R.string.toast_trans_error, Toast.LENGTH_LONG).show();
                showTransactionStartFailed();
            } else if (PosEmvErrorCode.EMV_ENCRYPT_ERROR == result) {
                Toast.makeText(this, R.string.toast_encrypt_error, Toast.LENGTH_LONG).show();
                showTransactionStartFailed();
            } else if (result != 0) {
                Toast.makeText(this, R.string.toast_trans_error, Toast.LENGTH_LONG).show();
                showTransactionStartFailed();
            }
        } catch (Exception e) {
            e.printStackTrace();
            showTransactionStartFailed();
        }
    }

    private void showTransactionStartFailed() {
        if (transactionFinishing || isFinishing()) {
            return;
        }
        transactionFinishing = true;
        iv_present_hand.clearAnimation();
        ll_light.setVisibility(View.GONE);
        Intent intent = new Intent(TransActivity.this, TransResultActivity.class);
        intent.putExtra(TransResult_Code, -1);
        intent.putExtra(TransResult_Amount, eAmount);
        intent.putExtra(CURRENCY_TAG, currency);
        PaymentOrder.put(intent, paymentOrder);
        startActivity(intent);
        finish();
    }


    @Override
    protected void onStop() {
        super.onStop();
        DialogUtils.dismissProgressDialog();
    }

    public static void startLightAnim(View view, int time, int index) {
        AlphaAnimation anim = new AlphaAnimation(0, 1);
        anim.setDuration(time);
        anim.setFillAfter(true);
        anim.setStartOffset(time * index);
        view.startAnimation(anim);
    }

    private void startLightAnim(View view, int time, int index, int repeat) {
        AlphaAnimation anim = new AlphaAnimation(0, 1);
        anim.setDuration(time);
        anim.setFillAfter(true);
        anim.setRepeatCount(repeat);
        anim.setStartOffset(time * index);
        view.startAnimation(anim);
    }

    public void scrollingLight(int type) {
        ll_light.setVisibility(View.VISIBLE);
        int time = 110;
        TransActivity.startLightAnim(iv1, time, 0);
        TransActivity.startLightAnim(iv2, time, 1);
        TransActivity.startLightAnim(iv3, time, 2);
        TransActivity.startLightAnim(iv4, time, 3);
        iv_present_hand.clearAnimation();
        switch (type) {
            case ConstantEmv.POIEmvCoreManager.DEVICE_CONTACT:
                tvMessage1.setText(R.string.trans_contact);
                break;
            case ConstantEmv.POIEmvCoreManager.DEVICE_CONTACTLESS:
                tvMessage1.setText(R.string.trans_contactless);
                break;
            case ConstantEmv.POIEmvCoreManager.DEVICE_MAGSTRIPE:
                tvMessage1.setText(R.string.trans_magstripe);
                break;
            case ConstantEmv.PosEmvErrorCode.EMV_MULTI_CONTACTLESS:
                onTransStart();
                return;
            default:
                break;
        }
    }

    public void selectApplication(List<String> list) {
        String[] names = list.toArray(new String[0]);
        MaterialDialog dialog = new MaterialDialog(TransActivity.this);
        dialog.showListConfirmChoseDialog(getString(R.string.trans_select_app), names,
                new MaterialDialog.OnChoseListener() {
                    @Override
                    public void onChose(int position) {
                        if (TransInitActivity.isPOISdk) {
                            emvPoiManager.onSetSelectResponse(position);
                        } else {
                            emvManager.setSelectApplicationResponse(position);
                        }
                    }
                });
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_UP) {
            return false;
        }
        return super.onKeyUp(keyCode, event);
    }
}
