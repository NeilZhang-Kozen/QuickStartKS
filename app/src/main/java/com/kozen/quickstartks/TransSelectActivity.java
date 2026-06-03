package com.kozen.quickstartks;

import static com.kozen.quickstartks.utils.Utils.CURRENCY_TAG;
import static com.kozen.quickstartks.utils.Utils.EUR_TAG;
import static com.kozen.quickstartks.utils.Utils.TRANS_QR;
import static com.kozen.quickstartks.utils.Utils.USD_TAG;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.kozen.quickstartks.utils.SecondScreenUtils;

public class TransSelectActivity extends BaseActivity {

    private static final String TAG = "TransActivity";
    private TextView tvMessage0, tvMessage1, tvMessage2, tvMessage3;
    private boolean initing = false;
    boolean isTimer = false;
    private static final int MENU_ITEM_ID = 1;
    private static final int TIME_30S = 10 * 1000;
    public String amount = "0";
    public String currentCurrency = USD_TAG;
    private LinearLayout ll_cover;
    private int idleTime = 30 * 1000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.second_select_transmode);

        Intent intent = getIntent();
        amount = intent.getStringExtra("amount");
        currentCurrency = intent.getStringExtra(CURRENCY_TAG);

        TextView tv_amount = findViewById(R.id.tv_amount);
        tv_amount.setText("Trans Amount :  " + amount);

        if (TransInitActivity.isExistSecScreen) {
            SecondScreenUtils.showView(TransSelectActivity.this, R.layout.second_select_waiting);
        }
    }


    public void showPic(View view) {
    }

    public void onCardTrans(View view) {
        if (TextUtils.isEmpty(amount)) {
            amount = "1500.00";
        }
        if (EUR_TAG.equals(currentCurrency)) {
            amount = convertToEur(amount);
        }

        Intent intent = new Intent(TransSelectActivity.this, TransActivity.class);
        intent.putExtra("amount", amount);
        intent.putExtra(CURRENCY_TAG, currentCurrency);
        TransSelectActivity.this.startActivity(intent);
        finish();
    }

    public void onQRTrans(View view) {
        TRANS_QR = true;
        if (TextUtils.isEmpty(amount)) {
            amount = "1500.00";
        }
        if (EUR_TAG.equals(currentCurrency)) {
            amount = convertToEur(amount);
        }
        Intent intent = new Intent(TransSelectActivity.this, QrPaymentActivity.class);
        intent.putExtra("amount", amount);
        intent.putExtra(CURRENCY_TAG, currentCurrency);
        TransSelectActivity.this.startActivity(intent);
        finish();

    }

    private String convertToEur(String value) {
        return new java.math.BigDecimal(value)
                .divide(new java.math.BigDecimal("1.13"), 2, java.math.RoundingMode.DOWN)
                .toPlainString();
    }

}
