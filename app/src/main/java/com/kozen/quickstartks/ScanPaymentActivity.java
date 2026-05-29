package com.kozen.quickstartks;

import static com.kozen.quickstartks.TransInitActivity.currentCurrency;
import static com.kozen.quickstartks.utils.Utils.USD_TAG;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.journeyapps.barcodescanner.CaptureManager;
import com.journeyapps.barcodescanner.DecoratedBarcodeView;
import com.kozen.quickstartks.utils.SecondScreenUtils;
import com.kozen.quickstartks.utils.Utils;

public class ScanPaymentActivity extends BaseActivity {
    private CaptureManager captureManager;
    private DecoratedBarcodeView barcodeScannerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trans_scan);


        barcodeScannerView = findViewById(R.id.zxing_barcode_scanner);
        captureManager = new CaptureManager(this, barcodeScannerView);
        captureManager.initializeFromIntent(getIntent(), savedInstanceState);
        captureManager.decode();


        TextView tv_amount = findViewById(R.id.tv_amount);
        TextView tv_order_num = findViewById(R.id.tv_order_num);
        TextView tv_order_time = findViewById(R.id.tv_order_time);
        TextView tv_cancel = findViewById(R.id.tv_cancel);

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
        captureManager.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        captureManager.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        captureManager.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        captureManager.onSaveInstanceState(outState);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Utils.setToast(ScanPaymentActivity.this, getString(R.string.trans_user_cancel));
    }
}