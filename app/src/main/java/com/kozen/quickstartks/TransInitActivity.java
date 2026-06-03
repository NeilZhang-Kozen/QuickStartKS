package com.kozen.quickstartks;

import static com.kozen.quickstartks.TransActivity.TransResult_Amount;
import static com.kozen.quickstartks.TransActivity.TransResult_Code;
import static com.kozen.quickstartks.utils.SharePreferenceUtils.CAN_MAGSTRIPE;
import static com.kozen.quickstartks.utils.SharePreferenceUtils.KEY_INIT;
import static com.kozen.quickstartks.utils.SharePreferenceUtils.USE_DUKPT;
import static com.kozen.quickstartks.utils.Utils.CURRENCY_TAG;
import static com.kozen.quickstartks.utils.Utils.EUR_TAG;
import static com.kozen.quickstartks.utils.Utils.USD_TAG;
import static com.kozen.quickstartks.utils.Utils.TRANS_QR;

import android.animation.ValueAnimator;
import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.animation.AlphaAnimation;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.kozen.component.constant.KeyboardConstant;
import com.kozen.component.keyboard.InputCallback;
import com.kozen.component_client.ComponentEngine;
import com.kozen.financial.engine.FinancialEngine;
import com.kozen.financial.engine.InitListener;
import com.kozen.quickstartks.utils.AppExecutors;
import com.kozen.quickstartks.utils.AppInstallChecker;
import com.kozen.quickstartks.utils.DialogUtils;
import com.kozen.quickstartks.utils.ParameterInit;
import com.kozen.quickstartks.utils.ParameterInitPOI;
import com.kozen.quickstartks.utils.PosUtils;
import com.kozen.quickstartks.utils.ScreenUtils;
import com.kozen.quickstartks.utils.SecondScreenUtils;
import com.kozen.quickstartks.utils.SharePreferenceUtils;
import com.kozen.quickstartks.utils.Utils;

import java.lang.ref.WeakReference;
import java.lang.reflect.Method;

public class TransInitActivity extends BaseActivity {

    private static final String TAG = "TransActivity";
    private static final String ZERO_AMOUNT_TEXT = "0.00";
    private static final int MAX_AMOUNT_CENTS_DIGITS = 7;
    private TextView tvMessage0, tvMessage1, tvMessage2, tvMessage3;
    private EditText edtAmount;
    private boolean isZeroAmountWarningShowing = false;
    private boolean initing = false;
    boolean isTimer = false;
    private static final int MENU_ITEM_ID = 1;
    private static final int MENU_DUKPT_ID = 2;
    public static boolean isFinancialInit = false;
    public static boolean isComponenInit = false;
    public static boolean isExistSecScreen = false;
    private static WeakReference<TransInitActivity> sInstance;
    private static final int TIME_30S = 10 * 1000;
    public static String mAmount = "0";
    public static String currentCurrency = USD_TAG;
    private LinearLayout ll_cover = null;
    private int idleTime = 30 * 1000;
    private PaymentOrder currentOrder;
    private TextView tvOrderBadge;
    private TextView tvOrderSummary;
    private TextView tvOrderAmount;

    public static boolean isPhysicalKeyboard = false;
    public static boolean isPOISdk = true;
    public static boolean isCopServiceExist = false;
    private boolean isFinServiceExist = false;

    public static boolean isUseDukpt = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.e(TAG,"=========>>>>Build.MODEL:"+Build.MODEL);
        if ("K1211".equals(Build.MODEL)) {
            setContentView(R.layout.activity_trans_init_n0211);
            ll_cover = findViewById(R.id.ll_cover);
		} else if ("K1112".equals(Build.MODEL)) {
            setContentView(R.layout.activity_trans_init_k1112);
        } else {
            setContentView(R.layout.activity_trans_init);
		}
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }
        if ("K1211".equals(Build.MODEL) || "K1112".equals(Build.MODEL)) {
            isPhysicalKeyboard = true;
        } else {
            isPhysicalKeyboard = false;
        }
        isFinServiceExist = AppInstallChecker.isAppInstalled(TransInitActivity.this,"com.kozen.financial.service");
        Log.e(TAG,"=====isFinServiceExist======>>:"+isFinServiceExist);
        if (isFinServiceExist){
            isPOISdk = false;
        }else {
            isPOISdk = true;
        }
        isCopServiceExist = AppInstallChecker.isAppInstalled(TransInitActivity.this,"com.kozen.component_service");
        Log.e(TAG,"=====isCopServiceExist======>>:"+isCopServiceExist);
        init();
        sInstance = new WeakReference<>(TransInitActivity.this);
        edtAmount = findViewById(R.id.edtAmount);
        tvOrderBadge = findViewById(R.id.tv_order_badge);
        tvOrderSummary = findViewById(R.id.tv_order_summary);
        tvOrderAmount = findViewById(R.id.tv_order_amount);
        Button btn_init_next = findViewById(R.id.btn_init_next);
        ImageView iv_del = findViewById(R.id.iv_del);
        View btnDelete = (View) iv_del.getParent();
        TableLayout tableLayout = findViewById(R.id.tableLayout);
        edtAmount.getPaint().setFlags(Paint.ANTI_ALIAS_FLAG | Paint.UNDERLINE_TEXT_FLAG);
        btnDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                animateDeleteButton(v);
                deleteLastAmountDigit();
            }
        });
        btnDelete.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                animateDeleteButton(v);
                isZeroAmountWarningShowing = false;
                edtAmount.setText("");
                return true;
            }
        });
        if (isSecondScreenModel()) {
            showCover(false);
        }
        isExistSecScreen = false;
        edtAmount.setShowSoftInputOnFocus(false);
        edtAmount.requestFocus();
        edtAmount.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {

                InputMethodManager imm = (InputMethodManager) getSystemService(getApplicationContext().INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(edtAmount.getWindowToken(), 0);
//                AppExecutors.getInstance().mainThread().execute(new Runnable() {
//                    @Override
//                    public void run() {
//
//                    }
//                });


            }
        });

        edtAmount.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                bindOrderSummary(currentOrder);

                if (shouldUseSecondScreen()) {
                    idleTimer.cancel();
                    idleTimer.start();
                    hideCover();
                    SecondScreenUtils.showView(TransInitActivity.this, R.layout.second_input, s.toString());

                } else {
                    if (!isTimer && !TextUtils.isEmpty(s.toString()) && (!"0".equals(s.toString()))) {
                        isTimer = true;
                        Log.i("amount=", System.currentTimeMillis() + "");
                    }
                }

            }
        });
        edtAmount.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ValueAnimator animator = ValueAnimator.ofFloat(1f, 0f, 1f);
                animator.setDuration(500);
                animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                    @Override
                    public void onAnimationUpdate(ValueAnimator animation) {
                        tableLayout.setAlpha(animation.getAnimatedFraction());
                    }
                });
                animator.start();
            }
        });
        btn_init_next.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String amount = normalizeAmount(edtAmount.getText().toString());
                if (ZERO_AMOUNT_TEXT.equals(amount)) {
                    showZeroAmountWarning();
                    return;
                }
                edtAmount.setText(amount);
                edtAmount.setSelection(edtAmount.getText().length());
                if (shouldUseSecondScreen()) {
                    DialogUtils.showProgressDialog(getString(R.string.waiting_select_currency), TransInitActivity.this);
                    boolean shown = SecondScreenUtils.showView(TransInitActivity.this, R.layout.second_select_currency, getString(R.string.sub_trans_amount) + " $" + amount);
                    if (!shown) {
                        DialogUtils.dismissProgressDialog();
                        startCardTransaction(amount);
                    }
                } else {
                    showMainScreenPaymentMethodDialog(amount);

                }

            }
        });
        handlePaymentIntent(getIntent());

    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handlePaymentIntent(intent);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, android.content.Intent data) {
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (result != null) {
            if (result.getContents() == null) {
                // 用户取消了扫描
                TRANS_QR = false;
                PaymentCallbackDispatcher.dispatch(TransInitActivity.this, currentOrder, "CANCELLED", "QR", null, getString(R.string.trans_user_cancel));
            } else {

                String amount = normalizeAmount(edtAmount.getText().toString());
                if (EUR_TAG.equals(currentCurrency)) {
                    amount = convertToEur(amount);
                }
                Intent intent = new Intent(TransInitActivity.this, TransScanResultActivity.class);
                intent.putExtra(TransResult_Code, 0);
                intent.putExtra(TransResult_Amount, amount);
                PaymentOrder.put(intent, currentOrder);
//                intent.putExtra(TransResult_Data, transData);
//                intent.putExtra(TransResult_Card_Type, Card_Type);
                intent.putExtra(CURRENCY_TAG, currentCurrency);
                TransInitActivity.this.startActivity(intent);
                // 处理扫描结果
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    public void onClick(View v) {
        String key = ((TextView) v).getText().toString();
        appendAmountDigits(key);
    }

    private void appendAmountDigits(String key) {
        if (isZeroAmountWarningShowing) {
            isZeroAmountWarningShowing = false;
            edtAmount.setText("");
        }
        if (key == null || !key.matches("\\d+")) {
            return;
        }
        String digits = digitsOnly(edtAmount.getText().toString());
        if (digits.length() + key.length() > MAX_AMOUNT_CENTS_DIGITS) {
            return;
        }
        digits = digits + key;
        String stripped = digits.replaceFirst("^0+(?!$)", "");
        edtAmount.setText(formatCents(stripped));
        edtAmount.setSelection(edtAmount.getText().length());
    }

    private void deleteLastAmountDigit() {
        if (isZeroAmountWarningShowing) {
            isZeroAmountWarningShowing = false;
            edtAmount.setText("");
            return;
        }
        String digits = digitsOnly(edtAmount.getText().toString());
        if (digits.isEmpty()) {
            showZeroAmountWarning();
            return;
        }
        digits = digits.substring(0, digits.length() - 1);
        if (digits.isEmpty() || Long.parseLong(digits) == 0L) {
            edtAmount.setText("");
        } else {
            edtAmount.setText(formatCents(digits));
            edtAmount.setSelection(edtAmount.getText().length());
        }
    }

    private void animateDeleteButton(View view) {
        view.animate().cancel();
        view.animate()
                .scaleX(1.14f)
                .scaleY(1.14f)
                .setDuration(80)
                .withEndAction(new Runnable() {
                    @Override
                    public void run() {
                        view.animate().scaleX(1f).scaleY(1f).setDuration(100).start();
                    }
                })
                .start();
    }

    private void shakeAmount() {
        if (edtAmount == null) return;
        android.animation.ObjectAnimator shake = android.animation.ObjectAnimator.ofFloat(
                edtAmount, "translationX", 0f, -18f, 18f, -14f, 14f, -8f, 8f, -4f, 4f, 0f);
        shake.setDuration(420);
        shake.start();
    }

    private void showZeroAmountWarning() {
        if (edtAmount == null) return;
        edtAmount.setText("");
        edtAmount.setHint(ZERO_AMOUNT_TEXT);
        edtAmount.requestFocus();
        isZeroAmountWarningShowing = true;
        shakeAmount();
    }

    private String normalizeAmount(String amount) {
        if (TextUtils.isEmpty(amount)) {
            return ZERO_AMOUNT_TEXT;
        }
        try {
            return new java.math.BigDecimal(amount)
                    .setScale(2, java.math.RoundingMode.DOWN)
                    .toPlainString();
        } catch (NumberFormatException e) {
            return ZERO_AMOUNT_TEXT;
        }
    }

    private String digitsOnly(String s) {
        if (s == null) return "";
        return s.replaceAll("[^0-9]", "");
    }

    private String formatCents(String digits) {
        if (digits == null || digits.isEmpty()) return ZERO_AMOUNT_TEXT;
        while (digits.length() < 3) {
            digits = "0" + digits;
        }
        String whole = digits.substring(0, digits.length() - 2);
        String frac = digits.substring(digits.length() - 2);
        return whole + "." + frac;
    }

    void showCover(boolean isTimer) {
        if (ll_cover != null) {
            ll_cover.setVisibility(View.VISIBLE);
        }
        if (isTimer) {
            SecondScreenUtils.showView(TransInitActivity.this, R.layout.second_default);
        }
    }

    void hideCover() {
        if (ll_cover != null) {
            ll_cover.setVisibility(View.GONE);
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        boolean isMag = SharePreferenceUtils.getBoolean(TransInitActivity.this, CAN_MAGSTRIPE, false);
        menu.add(Menu.NONE, MENU_ITEM_ID, Menu.NONE, "SPECIAL_MAGSTRIPE")
                .setCheckable(true)
                .setChecked(isMag); // 设置初始状态为选中

        isUseDukpt = SharePreferenceUtils.getBoolean(TransInitActivity.this, USE_DUKPT, false);
        menu.add(Menu.NONE, MENU_DUKPT_ID, Menu.NONE, "DUKPT_ENCRYPTION")
                .setCheckable(true)
                .setChecked(isUseDukpt); // 设置初始状态为选中

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        boolean isChecked = false;
        switch (item.getItemId()) {
            case MENU_ITEM_ID:
                isChecked = item.isChecked();
                item.setChecked(!isChecked);
                if (!item.isChecked()) {
                    SharePreferenceUtils.putBoolean(TransInitActivity.this, CAN_MAGSTRIPE, false);
                } else {
                    SharePreferenceUtils.putBoolean(TransInitActivity.this, CAN_MAGSTRIPE, true);
                }
                // 切换选中状态
                // 这里可以添加其他逻辑
                return true;
            case MENU_DUKPT_ID:
                isChecked = item.isChecked();
                item.setChecked(!isChecked);
                if (!item.isChecked()) {
                    isUseDukpt = false;
                    SharePreferenceUtils.putBoolean(TransInitActivity.this, USE_DUKPT, false);
                } else {
                    isUseDukpt = true;
                    SharePreferenceUtils.putBoolean(TransInitActivity.this, USE_DUKPT, true);
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    public void onUSD(View view) {
        String amount = normalizeAmount(edtAmount.getText().toString());
        DialogUtils.updateProgressDialog(getString(R.string.waiting_select_payment_method));
        currentCurrency = USD_TAG;
        boolean shown = SecondScreenUtils.showView(TransInitActivity.this, R.layout.second_select_transmode, getString(R.string.sub_trans_amount) + " $" + amount);
        if (!shown) {
            DialogUtils.dismissProgressDialog();
            showMainScreenPaymentMethodDialog(amount);
        }
//        Intent intent = new Intent(TransInitActivity.this, TransActivity.class);
//        intent.putExtra("amount", edtAmount.getText().toString());
//        intent.putExtra(CURRENCY_TAG, USD_TAG);
//        TransInitActivity.this.startActivity(intent);
    }

    public void onEUR(View view) {
        String amount = convertToEur(normalizeAmount(edtAmount.getText().toString()));
        DialogUtils.updateProgressDialog(getString(R.string.waiting_select_payment_method));
        currentCurrency = EUR_TAG;
        boolean shown = SecondScreenUtils.showView(TransInitActivity.this, R.layout.second_select_transmode, getString(R.string.sub_trans_amount) + " €" + amount);
        if (!shown) {
            DialogUtils.dismissProgressDialog();
            showMainScreenPaymentMethodDialog(amount);
        }
//        Intent intent = new Intent(TransInitActivity.this, TransActivity.class);
//        intent.putExtra("amount", String.valueOf((int)(Integer.valueOf(edtAmount.getText().toString())/1.13)));
//        intent.putExtra(CURRENCY_TAG, EUR_TAG);
//        TransInitActivity.this.startActivity(intent);
    }

    public void onCardTrans(View view) {
        String amount = normalizeAmount(edtAmount.getText().toString());
        if (EUR_TAG.equals(currentCurrency)) {
            amount = convertToEur(amount);
        }

        DialogUtils.dismissProgressDialog();

        if (!ensureTransactionReady()) {
            return;
        }
        Intent intent = new Intent(TransInitActivity.this, TransActivity.class);
        intent.putExtra("amount", amount);
        intent.putExtra(CURRENCY_TAG, currentCurrency);
        PaymentOrder.put(intent, currentOrder);
        TransInitActivity.this.startActivity(intent);
    }

    public void onQRTrans(View view) {
        TRANS_QR = true;
        String amount = normalizeAmount(edtAmount.getText().toString());
        if (EUR_TAG.equals(currentCurrency)) {
            amount = convertToEur(amount);
        }
        DialogUtils.dismissProgressDialog();

        startQrScanTransaction(amount);

    }

    private void startQrScanTransaction(String amount) {
        if (!ensureTransactionReady()) {
            TRANS_QR = false;
            return;
        }
        mAmount = amount;
        ensureCurrentOrder(amount);
        IntentIntegrator integrator = new IntentIntegrator(TransInitActivity.this);
        integrator.setBeepEnabled(false);
        integrator.setBarcodeImageEnabled(true);
        integrator.setCaptureActivity(ScanPaymentActivity.class);
        integrator.initiateScan();
    }

    private void startQrDisplayTransaction(String amount) {
        if (!ensureTransactionReady()) {
            TRANS_QR = false;
            return;
        }
        mAmount = amount;
        ensureCurrentOrder(amount);
        Intent intent = new Intent(TransInitActivity.this, QrPaymentActivity.class);
        intent.putExtra("amount", amount);
        intent.putExtra(CURRENCY_TAG, currentCurrency);
        PaymentOrder.put(intent, currentOrder);
        TransInitActivity.this.startActivity(intent);
    }

    private void ensureCurrentOrder(String amount) {
        if (currentOrder == null) {
            currentOrder = PaymentOrder.fromContext(
                    currentOrder,
                    amount,
                    currentCurrency,
                    Utils.getCurrentTime2() + Utils.getRandomData(),
                    Utils.getCurrentTime(),
                    getString(R.string.label_demo_items));
        } else {
            if (TextUtils.isEmpty(currentOrder.orderNo)) {
                currentOrder.orderNo = Utils.getCurrentTime2() + Utils.getRandomData();
            }
            if (TextUtils.isEmpty(currentOrder.orderTime)) {
                currentOrder.orderTime = Utils.getCurrentTime();
            }
            if (TextUtils.isEmpty(currentOrder.orderInfo)) {
                currentOrder.orderInfo = getString(R.string.label_demo_items);
            }
        }
    }

    public void showPic(View view) {
    }

    private String convertToEur(String amount) {
        return new java.math.BigDecimal(amount)
                .divide(new java.math.BigDecimal("1.13"), 2, java.math.RoundingMode.DOWN)
                .toPlainString();
    }

    private void startCardTransaction(String amount) {
        if (!ensureTransactionReady()) {
            return;
        }
        Intent intent = new Intent(TransInitActivity.this, TransActivity.class);
        intent.putExtra("amount", amount);
        intent.putExtra(CURRENCY_TAG, currentCurrency);
        PaymentOrder.put(intent, currentOrder);
        TransInitActivity.this.startActivity(intent);
    }

    private void showMainScreenPaymentMethodDialog(String amount) {
        Dialog dialog = new Dialog(TransInitActivity.this);
        dialog.setContentView(R.layout.dialog_select_transmode);
        dialog.setCanceledOnTouchOutside(true);
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        TextView tvAmount = dialog.findViewById(R.id.tv_payment_method_amount);
        LinearLayout llCard = dialog.findViewById(R.id.ll_payment_card);
        LinearLayout llQr = dialog.findViewById(R.id.ll_payment_qr);
        LinearLayout llQrDisplay = dialog.findViewById(R.id.ll_payment_qr_display);

        tvAmount.setText((USD_TAG.equals(currentCurrency) ? "$" : "€") + amount);
        llCard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                startCardTransaction(amount);
            }
        });
        llQr.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                TRANS_QR = true;
                startQrScanTransaction(amount);
            }
        });
        llQrDisplay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                TRANS_QR = true;
                startQrDisplayTransaction(amount);
            }
        });
        dialog.show();
        window = dialog.getWindow();
        if (window != null) {
            int width = (int) (getResources().getDisplayMetrics().widthPixels * 0.94f);
            window.setLayout(width, LinearLayout.LayoutParams.WRAP_CONTENT);
        }
    }

    private void handlePaymentIntent(Intent intent) {
        PaymentOrder order = PaymentOrder.fromIntent(intent);
        if (order == null) {
            currentOrder = null;
            bindOrderSummary(null);
            return;
        }
        currentOrder = order;
        String currency = TextUtils.isEmpty(order.currency) ? USD_TAG : order.currency.toUpperCase(java.util.Locale.US);
        currentCurrency = EUR_TAG.equals(currency) ? EUR_TAG : USD_TAG;
        edtAmount.setText(order.amount);
        edtAmount.setSelection(edtAmount.getText().length());
        bindOrderSummary(order);
        startPreferredPaymentIfRequested(order);
    }

    private void startPreferredPaymentIfRequested(PaymentOrder order) {
        if (order == null || TextUtils.isEmpty(order.paymentMethod)) {
            return;
        }
        String amount = normalizeAmount(order.amount);
        if (ZERO_AMOUNT_TEXT.equals(amount)) {
            return;
        }
        if ("CARD".equals(order.paymentMethod)) {
            startCardTransaction(amount);
        } else if ("QR_SCAN".equals(order.paymentMethod) || "SCAN".equals(order.paymentMethod)) {
            TRANS_QR = true;
            startQrScanTransaction(amount);
        } else if ("QR_DISPLAY".equals(order.paymentMethod) || "QR_CODE".equals(order.paymentMethod)) {
            TRANS_QR = true;
            startQrDisplayTransaction(amount);
        }
    }

    private void bindOrderSummary(PaymentOrder order) {
        if (tvOrderBadge == null || tvOrderSummary == null || tvOrderAmount == null) {
            return;
        }
        String amount = normalizeAmount(edtAmount == null ? "" : edtAmount.getText().toString());
        String symbol = USD_TAG.equals(currentCurrency) ? "$" : "€";
        if (order == null || !order.hasOrderInfo()) {
            tvOrderBadge.setText(getString(R.string.label_demo_order_no));
            tvOrderSummary.setText(getString(R.string.label_demo_items));
            tvOrderAmount.setText(symbol + amount);
            return;
        }
        tvOrderBadge.setText(TextUtils.isEmpty(order.orderNo) ? "External order" : "Order " + order.orderNo);
        tvOrderSummary.setText(TextUtils.isEmpty(order.orderInfo) ? "External payment request" : order.orderInfo);
        tvOrderAmount.setText(symbol + order.amount);
    }

    private boolean ensureTransactionReady() {
        if (SharePreferenceUtils.getBoolean(TransInitActivity.this, KEY_INIT, false)) {
            return true;
        }
        Toast.makeText(TransInitActivity.this, R.string.toast_trans_error, Toast.LENGTH_LONG).show();
        if (!initing) {
            initing = true;
            DialogUtils.showAlertDialog(TransInitActivity.this, "", "", new DialogUtils.DialogCallback() {
                @Override
                public void onConfirm() {
                    DialogUtils.dismissLoadingDialog();
                }

                @Override
                public void onCancel() {
                    DialogUtils.dismissLoadingDialog();
                }
            });
        }
        return false;
    }

    private boolean isSecondScreenModel() {
        return "K1211".equals(Build.MODEL)
                || "K1352".equals(Build.MODEL)
                || "K1141".equals(Build.MODEL)
                || "K1362".equals(Build.MODEL);
    }

    private boolean shouldUseSecondScreen() {
        isExistSecScreen = SecondScreenUtils.isAvailable(TransInitActivity.this);
        return isExistSecScreen;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!SharePreferenceUtils.getBoolean(TransInitActivity.this, KEY_INIT, false) && !initing) {
            initing = true;
            DialogUtils.showAlertDialog(TransInitActivity.this, "", "", new DialogUtils.DialogCallback() {
                @Override
                public void onConfirm() {
                    DialogUtils.dismissLoadingDialog();
                }

                @Override
                public void onCancel() {
                    DialogUtils.dismissLoadingDialog();
                }
            });
        }
        if (shouldUseSecondScreen()) {
            idleTimer.start();
            CountDownTimer localTimer = new CountDownTimer(300, 100) {
                @Override
                public void onTick(long millisUntilFinished) {

                }

                @Override
                public void onFinish() {

                    try {
                        if (isPhysicalKeyboard) {
                            int code = ComponentEngine.INSTANCE.getKeyboardManager().startPhysicalKeyboard(new InputCallback() {
                                String amount = "";

                                @Override
                                public void onKey(KeyboardConstant.KeyCode keyCode, KeyboardConstant.KeyAction keyAction) {
//                                    String text = keyCode.getValue()+"=="+((keyAction.getAction() == 0)? "按下":"起");
//                                    Toast.makeText(TransInitActivity.this,text,Toast.LENGTH_SHORT).show();

                                    if ((keyAction.getAction() == KeyboardConstant.KeyAction.ACTION_UP.getAction())) {
                                        amount = edtAmount.getText().toString();
                                        Log.e(TAG, "onKey==========>>>>>keyCode:" + keyCode.toString() + " amount:" + amount);

                                        if (TransInitActivity.this.getWindow().getDecorView().getVisibility() == View.VISIBLE) {

                                            switch (keyCode) {
                                                case BUTTON_ENTER:
                                                    amount = normalizeAmount(amount);
                                                    if (ZERO_AMOUNT_TEXT.equals(amount)) {
                                                        showZeroAmountWarning();
                                                        break;
                                                    } else if (!ensureTransactionReady()) {
                                                        break;
                                                    } else {
                                                        startCardTransaction(amount);
                                                    }
                                                    break;
                                                case BUTTON_0:
                                                case BUTTON_1:
                                                case BUTTON_2:
                                                case BUTTON_3:
                                                case BUTTON_4:
                                                case BUTTON_5:
                                                case BUTTON_6:
                                                case BUTTON_7:
                                                case BUTTON_8:
                                                case BUTTON_9:
                                                    appendAmountDigits(String.valueOf(keyCode.getValue() - '0'));
                                                    break;
                                                case BUTTON_DOT:
                                                    appendAmountDigits("00");
                                                    break;
                                                case BUTTON_ESC:
                                                    edtAmount.setText("");
                                                    SecondScreenUtils.showView(TransInitActivity.this, R.layout.second_default);
                                                    break;
                                                case BUTTON_BACKSPACE:
                                                    deleteLastAmountDigit();
                                                    break;
                                                case BUTTON_FN:
                                                    //FUN KEY
                                                case BUTTON_USER_DEFINED:
                                                    //KOZEN KEY
                                                case BUTTON_PLUS:
                                                    //+ KEY
                                                    edtAmount.setText(amount);
                                                    edtAmount.setSelection(amount.length());
                                                    break;

                                            }


                                        }
                                    }

                                }
                            });
                        }

                        String amount = edtAmount.getText().toString();
//                            SecondScreenUtils.showDefaultImage();
//                            SecondScreenUtils.showView(TransInitActivity.this,R.layout.second_default);
//
                        SecondScreenUtils.showView(TransInitActivity.this, R.layout.second_input, amount);
                    } catch (Exception e) {
                        Toast.makeText(TransInitActivity.this, R.string.sub_screen_unavailable, Toast.LENGTH_SHORT).show();
                        finish();
                    }
                }
            };
            localTimer.start();
		} else if (isPhysicalKeyboard) {
            idleTimer.start();
            CountDownTimer localTimer = new CountDownTimer(300, 100) {
                @Override
                public void onTick(long millisUntilFinished) {

                }

                @Override
                public void onFinish() {

                    try {
                        if (isPhysicalKeyboard) {
                            int code = ComponentEngine.INSTANCE.getKeyboardManager().startPhysicalKeyboard(new InputCallback() {
                                String amount = "";

                                @Override
                                public void onKey(KeyboardConstant.KeyCode keyCode, KeyboardConstant.KeyAction keyAction) {
//                                    String text = keyCode.getValue()+"=="+((keyAction.getAction() == 0)? "按下":"起");
//                                    Toast.makeText(TransInitActivity.this,text,Toast.LENGTH_SHORT).show();

                                    if ((keyAction.getAction() == KeyboardConstant.KeyAction.ACTION_UP.getAction())) {
                                        amount = edtAmount.getText().toString();
                                        Log.e(TAG, "onKey==========>>>>>keyCode:" + keyCode.toString() + " amount:" + amount);

                                        if (TransInitActivity.this.getWindow().getDecorView().getVisibility() == View.VISIBLE) {

                                            switch (keyCode) {
                                                case BUTTON_ENTER:
                                                    amount = normalizeAmount(amount);
                                                    if (ZERO_AMOUNT_TEXT.equals(amount)) {
                                                        showZeroAmountWarning();
                                                        break;
                                                    } else if (!ensureTransactionReady()) {
                                                        break;
                                                    } else {
                                                        startCardTransaction(amount);
                                                    }
                                                    break;
                                                case BUTTON_0:
                                                case BUTTON_1:
                                                case BUTTON_2:
                                                case BUTTON_3:
                                                case BUTTON_4:
                                                case BUTTON_5:
                                                case BUTTON_6:
                                                case BUTTON_7:
                                                case BUTTON_8:
                                                case BUTTON_9:
                                                    appendAmountDigits(String.valueOf(keyCode.getValue() - '0'));
                                                    break;
                                                case BUTTON_DOT:
                                                    appendAmountDigits("00");
                                                    break;
                                                case BUTTON_ESC:
                                                    edtAmount.setText("");
                                                    SecondScreenUtils.showView(TransInitActivity.this, R.layout.second_default);
                                                    break;
                                                case BUTTON_BACKSPACE:
                                                    deleteLastAmountDigit();
                                                    break;
                                                case BUTTON_FN:
                                                    //FUN KEY
                                                case BUTTON_USER_DEFINED:
                                                    //KOZEN KEY
                                                case BUTTON_PLUS:
                                                    //+ KEY
                                                    edtAmount.setText(amount);
                                                    edtAmount.setSelection(amount.length());
                                                    break;

                                            }


                                        }
                                    }

                                }
                            });
                        }

                        String amount = edtAmount.getText().toString();
//                            SecondScreenUtils.showDefaultImage();
//                            SecondScreenUtils.showView(TransInitActivity.this,R.layout.second_default);
//
                        SecondScreenUtils.showView(TransInitActivity.this, R.layout.second_input, amount);
                    } catch (Exception e) {
                        Toast.makeText(TransInitActivity.this, R.string.sub_screen_unavailable, Toast.LENGTH_SHORT).show();
                        finish();
                    }
                }
            };
            localTimer.start();
        }

    }

    CountDownTimer idleTimer = new CountDownTimer(idleTime, 1000) {
        @Override
        public void onTick(long millisUntilFinished) {

        }

        @Override
        public void onFinish() {
            showCover(true);
//            idleTimer

        }
    };

    private void initKey() {
        AppExecutors.getInstance().diskIOThread().execute(new Runnable() {
            @Override
            public void run() {
                boolean EraseAllKey = true;
                int result;
                if (isPOISdk){
                    result = ParameterInitPOI.initKey(EraseAllKey);
                    ParameterInitPOI.initEMVConifg(true);
                }else {
                    result = ParameterInit.initKey(EraseAllKey);
                    ParameterInit.initEMVConifg(true);
                }
                AppExecutors.getInstance().mainThread().execute(new Runnable() {
                    @Override
                    public void run() {
                        initing = false;
                        if (result == 0) {
//                            btnInit.setEnabled(false);
//                            btnInit.setText("Init Success");
                            DialogUtils.setIndeterminateDrawable(TransInitActivity.this, 0, R.drawable.loading_init_success);
                            SharePreferenceUtils.putBoolean(TransInitActivity.this, KEY_INIT, true);
//                            tvMessage1.setText("Init Success");
//                            btnTrans.setEnabled(true);
//                            Toast.makeText(TransActivity.this, "succeed", Toast.LENGTH_SHORT).show();
                        } else {
                            DialogUtils.setIndeterminateDrawable(TransInitActivity.this, -1, R.drawable.loading_init_failed);
                            SharePreferenceUtils.putBoolean(TransInitActivity.this, KEY_INIT, false);
//                            btnInit.setEnabled(true);
//                            btnInit.setText("INIT");
//                            tvMessage1.setText("Init Failure");
//                            Toast.makeText(TransInitActivity.this, "failure", Toast.LENGTH_SHORT).show();
                        }

                    }
                });
            }
        });
    }

    private void init() {
        if (isPOISdk) {
            initKey();
        } else {
            //初始化FinancialLib_1.1.2_release.aar
            FinancialEngine.INSTANCE.init(TransInitActivity.this, new InitListener() {
                @Override
                public void onResult(int result, String errorMsg) {
                    if (result == 0) {
                        Log.e(TAG, "financialEngine.init succes");
                        isFinancialInit = true;
                        Toast.makeText(TransInitActivity.this, "financialEngine.init success", Toast.LENGTH_SHORT).show();
                    } else {
                        Log.e(TAG, "financialEngine.init failed,ret:" + result + " errMsg:" + errorMsg);
                        isFinancialInit = false;
                        Toast.makeText(TransInitActivity.this, "financialEngine.init failed errMsg:" + errorMsg, Toast.LENGTH_SHORT).show();
                    }
                    initKey();
                }
            });
        }

        if (isCopServiceExist){
            ComponentEngine.INSTANCE.init(TransInitActivity.this, new com.kozen.component.engine.InitListener() {
                @Override
                public void onResult(int ret, String s) {
                    Log.e(TAG, "ComponentEngine.init ret:" + ret);

                    if (ret == 0) {
                        isComponenInit = true;
                        Toast.makeText(TransInitActivity.this, "ComponentEngine.init success", Toast.LENGTH_SHORT).show();
                        isExistSecScreen = SecondScreenUtils.isAvailable(TransInitActivity.this);
                        if (isExistSecScreen) {
                            ComponentEngine.INSTANCE.getSecondaryScreenManager().setBrightness(100);
                        }
                    } else {
                        isComponenInit = false;
                        isExistSecScreen = false;
                        Toast.makeText(TransInitActivity.this, "ComponentEngine.init failed errMsg:" + s, Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }
    }

    public static TransInitActivity getInstance() {
        return sInstance != null ? sInstance.get() : null;
    }

    @Override
    public void onBackPressed() {
        SecondScreenUtils.showView(TransInitActivity.this, R.layout.second_default);
//        SecondScreenUtils.showDefaultImage();
        // 如果需要默认的返回行为，可以调用父类的方法
        super.onBackPressed();
    }
}
