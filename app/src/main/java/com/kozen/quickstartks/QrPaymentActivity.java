package com.kozen.quickstartks;

import static com.kozen.quickstartks.TransActivity.TransResult_Amount;
import static com.kozen.quickstartks.TransActivity.TransResult_Code;
import static com.kozen.quickstartks.utils.Utils.CURRENCY_TAG;
import static com.kozen.quickstartks.utils.Utils.USD_TAG;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.view.View;

import com.journeyapps.barcodescanner.CaptureManager;
import com.kozen.quickstartks.utils.NetworkMonitor;
import com.kozen.quickstartks.utils.QrCodeGenerator;
import com.kozen.quickstartks.utils.SecondScreenUtils;
import com.kozen.quickstartks.utils.ServerManager;
import com.kozen.quickstartks.utils.Utils;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

public class QrPaymentActivity extends BaseActivity {
    private static final int QR_SERVER_PORT = 8080;
    private static final String QR_SUCCESS_PATH = "/getQrCode";
    private CaptureManager captureManager;
    private ImageView barcodeScannerView;
    private String TAG = "QrPaymentActivity";
    Bitmap qrBitmap;
    ServerManager serverManager;
    String qrContent;
    public String mAmount;
    public static String mQrAmount;
    String currency;
    PaymentOrder paymentOrder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trans_qr);
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        mAmount = getIntent().getStringExtra("amount");
        currency = getIntent().getStringExtra(Utils.CURRENCY_TAG);
        paymentOrder = PaymentOrder.get(getIntent());
        mQrAmount = (USD_TAG.equals(currency) ? "$" : "€") + mAmount;
        // 获取布局中的ImageView
        ImageView iv_qr_payment = findViewById(R.id.iv_qr_payment);

        TextView tv_amount = findViewById(R.id.tv_amount);
        TextView tv_order_num = findViewById(R.id.tv_order_num);
        TextView tv_order_time = findViewById(R.id.tv_order_time);
        TextView tv_cancel = findViewById(R.id.tv_cancel);


        tv_amount.setText((USD_TAG.equals(currency) ? "$" : "€") + mAmount);
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
        paymentOrder = PaymentOrder.fromContext(paymentOrder, mAmount, currency, displayOrderNo, displayOrderTime, displayOrderInfo);
        tv_cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cancelQrPayment();
                finish();
            }
        });

        String localIpAddress = getLocalIpAddress();
        if (TextUtils.isEmpty(localIpAddress)) {
            Toast.makeText(QrPaymentActivity.this, R.string.sub_trans_net, Toast.LENGTH_SHORT).show();
//                        finish();
        } else {
            qrContent = "http://" + localIpAddress + ":" + QR_SERVER_PORT + QR_SUCCESS_PATH;
            qrBitmap = QrCodeGenerator.generateQrCode(qrContent, 300, 300);
        }
        Log.e(TAG, "QrPaymentActivity.onCreate:" + qrContent);
        serverManager = new ServerManager(QrPaymentActivity.this, localIpAddress);
        serverManager.startServer();
        NetworkMonitor.getInstance().registerListener(listener);

        iv_qr_payment.setImageBitmap(qrBitmap);
        if (TransInitActivity.isExistSecScreen) {
            SecondScreenUtils.showQrView(QrPaymentActivity.this, R.layout.second_qr_code, (USD_TAG.equals(currency) ? "$" : "€") + mAmount);
        }

    }


    NetworkMonitor.NetworkListener listener = new NetworkMonitor.NetworkListener() {
        @Override
        public void onDataReceived(boolean isConnected) {
            Log.e(TAG, "onDataReceived====>>>isConnected:" + isConnected);
            trunToResult(0);
            QrPaymentActivity.this.finish();
        }
    };

    private void trunToResult(int code) {
        Intent intent = new Intent(
                QrPaymentActivity.this, TransScanResultActivity.class);
        intent.putExtra(TransResult_Code, code);
        intent.putExtra(TransResult_Amount, mAmount);
        PaymentOrder.put(intent, paymentOrder);
//                intent.putExtra(TransResult_Data, transData);
//                intent.putExtra(TransResult_Card_Type, Card_Type);
        intent.putExtra(CURRENCY_TAG, currency);
        QrPaymentActivity.this.startActivity(intent);
    }

    @Override
    public void onBackPressed() {
        cancelQrPayment();
        super.onBackPressed();
        Utils.setToast(QrPaymentActivity.this, getString(R.string.trans_user_cancel));

    }

    private void cancelQrPayment() {
        PaymentCallbackDispatcher.dispatch(QrPaymentActivity.this, paymentOrder, "CANCELLED", "QR", null, getString(R.string.trans_user_cancel));
    }

    public String getLocalIpAddress() {
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces();
                 en.hasMoreElements(); ) {
                NetworkInterface intf = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses();
                     enumIpAddr.hasMoreElements(); ) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress() && inetAddress instanceof Inet4Address) {
                        return inetAddress.getHostAddress();
                    }
                }
            }
        } catch (SocketException e) {
            e.printStackTrace();
        }
        return "";
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (serverManager == null) {
            return;
        }
        serverManager.stopServer();
        NetworkMonitor.getInstance().unregisterListener(listener);
    }
}
