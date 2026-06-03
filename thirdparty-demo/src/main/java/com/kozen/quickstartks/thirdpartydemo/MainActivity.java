package com.kozen.quickstartks.thirdpartydemo;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends Activity {
    private static final String QUICKSTART_PACKAGE = "com.kozen.quickstartks_new";
    private static final String QUICKSTART_ACTION_PAY = "com.kozen.quickstartks.action.PAY";
    private static final String EXTRA_PAYMENT_JSON = "payment_json";
    private static final String CALLBACK_ACTION = "com.kozen.quickstartks.thirdpartydemo.PAYMENT_RESULT";
    private static final String EXTRA_RESULT_JSON = "payment_result_json";

    private TextView resultView;
    private final BroadcastReceiver paymentResultReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String resultJson = intent.getStringExtra(EXTRA_RESULT_JSON);
            resultView.setText(resultJson == null ? "" : resultJson);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(buildContentView());

        IntentFilter filter = new IntentFilter(CALLBACK_ACTION);
        if (Build.VERSION.SDK_INT >= 33) {
            registerReceiver(paymentResultReceiver, filter, Context.RECEIVER_EXPORTED);
        } else {
            registerReceiver(paymentResultReceiver, filter);
        }
    }

    @Override
    protected void onDestroy() {
        unregisterReceiver(paymentResultReceiver);
        super.onDestroy();
    }

    private View buildContentView() {
        ScrollView scrollView = new ScrollView(this);
        scrollView.setBackgroundColor(Color.rgb(244, 245, 247));

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(28), dp(20), dp(24));
        scrollView.addView(root);

        TextView title = new TextView(this);
        title.setText("Third Party Pay Demo");
        title.setTextColor(Color.rgb(32, 33, 36));
        title.setTextSize(24);
        title.setGravity(Gravity.START);
        title.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        root.addView(title, matchWrap());

        TextView subtitle = new TextView(this);
        subtitle.setText("Send payment_json to QuickStartKS and receive payment_result_json.");
        subtitle.setTextColor(Color.rgb(119, 123, 130));
        subtitle.setTextSize(14);
        subtitle.setPadding(0, dp(6), 0, dp(18));
        root.addView(subtitle, matchWrap());

        root.addView(button("Start card payment", "CARD"));
        root.addView(button("Start QR scan payment", "QR_SCAN"));
        root.addView(button("Start QR display payment", "QR_DISPLAY"));

        TextView resultTitle = new TextView(this);
        resultTitle.setText("Callback result");
        resultTitle.setTextColor(Color.rgb(32, 33, 36));
        resultTitle.setTextSize(18);
        resultTitle.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        resultTitle.setPadding(0, dp(22), 0, dp(8));
        root.addView(resultTitle, matchWrap());

        resultView = new TextView(this);
        resultView.setText("{}");
        resultView.setTextColor(Color.rgb(32, 33, 36));
        resultView.setTextSize(13);
        resultView.setBackgroundColor(Color.WHITE);
        resultView.setPadding(dp(14), dp(14), dp(14), dp(14));
        resultView.setMinLines(10);
        resultView.setMovementMethod(new ScrollingMovementMethod());
        root.addView(resultView, matchWrap());

        return scrollView;
    }

    private Button button(String label, final String paymentMethod) {
        Button button = new Button(this);
        button.setText(label);
        button.setAllCaps(false);
        button.setTextSize(16);
        button.setTextColor(Color.WHITE);
        button.setBackgroundColor(Color.rgb(255, 90, 0));
        LinearLayout.LayoutParams lp = matchWrap();
        lp.topMargin = dp(10);
        button.setLayoutParams(lp);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startQuickStartPayment(paymentMethod);
            }
        });
        return button;
    }

    private void startQuickStartPayment(String paymentMethod) {
        try {
            String now = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(new Date());
            String requestId = "REQ-" + System.currentTimeMillis();
            JSONObject payload = new JSONObject();
            payload.put("requestId", requestId);
            payload.put("amount", "5.85");
            payload.put("currency", "USD");
            payload.put("orderNo", "DEMO-" + new SimpleDateFormat("HHmmss", Locale.US).format(new Date()));
            payload.put("orderTime", now);
            payload.put("orderInfo", "Demo coffee x1");
            payload.put("paymentMethod", paymentMethod);
            payload.put("callbackPackage", getPackageName());
            payload.put("callbackAction", CALLBACK_ACTION);
            payload.put("returnReceiptData", true);

            Intent intent = new Intent(QUICKSTART_ACTION_PAY);
            intent.setPackage(QUICKSTART_PACKAGE);
            intent.putExtra(EXTRA_PAYMENT_JSON, payload.toString());
            startActivity(intent);
            resultView.setText("Waiting for callback...\n\n" + payload.toString());
        } catch (JSONException e) {
            Toast.makeText(this, "Build JSON failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, "Open QuickStartKS failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }
}
