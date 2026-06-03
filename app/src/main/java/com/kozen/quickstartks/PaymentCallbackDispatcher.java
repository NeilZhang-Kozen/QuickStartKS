package com.kozen.quickstartks;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

public class PaymentCallbackDispatcher {
    public static final String EXTRA_PAYMENT_RESULT_JSON = "payment_result_json";

    private static final String TAG = "PaymentCallback";

    public static void dispatch(Activity activity, PaymentOrder order, String status,
                                String paymentMethod, ReceiptBuilder.ReceiptData receiptData,
                                String errorMessage) {
        if (activity == null || order == null) {
            return;
        }
        String resultJson = buildResultJson(order, status, paymentMethod, receiptData, errorMessage);

        Intent resultIntent = new Intent();
        resultIntent.putExtra(EXTRA_PAYMENT_RESULT_JSON, resultJson);
        activity.setResult("SUCCESS".equals(status) ? Activity.RESULT_OK : Activity.RESULT_CANCELED, resultIntent);

        if (!TextUtils.isEmpty(order.callbackAction)) {
            Intent callback = new Intent(order.callbackAction);
            if (!TextUtils.isEmpty(order.callbackPackage)) {
                callback.setPackage(order.callbackPackage);
            }
            callback.putExtra(EXTRA_PAYMENT_RESULT_JSON, resultJson);
            activity.sendBroadcast(callback);
        }

        if (!TextUtils.isEmpty(order.callbackUri)) {
            try {
                Uri uri = Uri.parse(order.callbackUri)
                        .buildUpon()
                        .appendQueryParameter(EXTRA_PAYMENT_RESULT_JSON, resultJson)
                        .build();
                Intent callback = new Intent(Intent.ACTION_VIEW, uri);
                activity.startActivity(callback);
            } catch (Exception e) {
                Log.e(TAG, "callback uri failed", e);
            }
        }
    }

    private static String buildResultJson(PaymentOrder order, String status, String paymentMethod,
                                          ReceiptBuilder.ReceiptData receiptData, String errorMessage) {
        try {
            JSONObject root = new JSONObject();
            root.put("requestId", safe(order.requestId));
            root.put("status", safe(status));
            root.put("paymentMethod", safe(paymentMethod));
            root.put("amount", safe(order.amount));
            root.put("currency", safe(order.currency));
            root.put("orderNo", safe(order.orderNo));
            root.put("orderTime", safe(order.orderTime));
            root.put("orderInfo", safe(order.orderInfo));
            if (!TextUtils.isEmpty(errorMessage)) {
                root.put("errorMessage", errorMessage);
            }
            if (order.returnReceiptData && receiptData != null) {
                JSONObject receipt = new JSONObject();
                receipt.put("merchantName", safe(receiptData.merchantName));
                receipt.put("mid", safe(receiptData.mid));
                receipt.put("tid", safe(receiptData.tid));
                receipt.put("operatorNo", safe(receiptData.operatorNo));
                receipt.put("transType", safe(receiptData.transType));
                receipt.put("orderNo", safe(receiptData.orderNo));
                receipt.put("orderInfo", safe(receiptData.orderInfo));
                receipt.put("batchNo", safe(receiptData.batchNo));
                receipt.put("voucherNo", safe(receiptData.voucherNo));
                receipt.put("refNo", safe(receiptData.refNo));
                receipt.put("authCode", safe(receiptData.authCode));
                receipt.put("dateTime", safe(receiptData.dateTime));
                receipt.put("amount", safe(receiptData.amount));
                if (!receiptData.qrPayment) {
                    receipt.put("cardNo", safe(receiptData.cardNo));
                    receipt.put("cardBrand", safe(receiptData.cardBrand));
                    receipt.put("expiry", safe(receiptData.expiry));
                    receipt.put("cardHolder", safe(receiptData.cardHolder));
                }
                root.put("receipt", receipt);
            }
            return root.toString();
        } catch (JSONException e) {
            Log.e(TAG, "build callback json failed", e);
            return "{}";
        }
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
