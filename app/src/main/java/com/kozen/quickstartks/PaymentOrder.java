package com.kozen.quickstartks;

import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

public class PaymentOrder implements Serializable {
    public static final String ACTION_PAY = "com.kozen.quickstartks.action.PAY";
    public static final String EXTRA_PAYMENT_JSON = "payment_json";
    public static final String EXTRA_ORDER = "payment_order";

    private static final String TAG = "PaymentOrder";

    public String amount = "";
    public String currency = "";
    public String orderNo = "";
    public String orderTime = "";
    public String orderInfo = "";
    public String paymentMethod = "";
    public String requestId = "";
    public String callbackPackage = "";
    public String callbackAction = "";
    public String callbackUri = "";
    public boolean returnReceiptData = true;
    public String rawJson = "";

    public static PaymentOrder fromIntent(Intent intent) {
        if (intent == null) {
            return null;
        }
        String json = intent.getStringExtra(EXTRA_PAYMENT_JSON);
        if (TextUtils.isEmpty(json)) {
            json = intent.getStringExtra("json");
        }
        if (TextUtils.isEmpty(json)) {
            Uri data = intent.getData();
            if (data != null) {
                json = data.getQueryParameter("payment_json");
                if (TextUtils.isEmpty(json)) {
                    json = data.getQueryParameter("json");
                }
            }
        }
        return fromJson(json);
    }

    public static PaymentOrder fromJson(String json) {
        if (TextUtils.isEmpty(json)) {
            return null;
        }
        try {
            JSONObject obj = new JSONObject(json);
            PaymentOrder order = new PaymentOrder();
            order.rawJson = json;
            order.amount = normalizeAmount(readString(obj, "amount"));
            if (TextUtils.isEmpty(order.amount) && obj.has("amountCents")) {
                order.amount = centsToAmount(obj.optLong("amountCents"));
            }
            order.currency = readString(obj, "currency");
            order.orderNo = firstNonEmpty(
                    readString(obj, "orderNo"),
                    readString(obj, "orderId"),
                    readString(obj, "orderNumber"));
            order.orderTime = firstNonEmpty(
                    readString(obj, "orderTime"),
                    readString(obj, "createdAt"),
                    readString(obj, "time"));
            order.orderInfo = firstNonEmpty(
                    readString(obj, "orderInfo"),
                    readString(obj, "description"),
                    readString(obj, "summary"),
                    itemsSummary(obj.optJSONArray("items")));
            order.paymentMethod = firstNonEmpty(
                    readString(obj, "paymentMethod"),
                    readString(obj, "method"),
                    readString(obj, "payMethod")).toUpperCase(java.util.Locale.US);
            order.requestId = firstNonEmpty(readString(obj, "requestId"), readString(obj, "requestNo"));
            order.callbackPackage = readString(obj, "callbackPackage");
            order.callbackAction = readString(obj, "callbackAction");
            order.callbackUri = readString(obj, "callbackUri");
            if (obj.has("returnReceiptData")) {
                order.returnReceiptData = obj.optBoolean("returnReceiptData", true);
            }
            if (TextUtils.isEmpty(order.amount)) {
                return null;
            }
            return order;
        } catch (JSONException e) {
            Log.e(TAG, "invalid payment json", e);
            return null;
        }
    }

    public static PaymentOrder fromContext(String amount, String currency, String orderNo,
                                           String orderTime, String orderInfo) {
        return fromContext(null, amount, currency, orderNo, orderTime, orderInfo);
    }

    public static PaymentOrder fromContext(PaymentOrder source, String amount, String currency,
                                           String orderNo, String orderTime, String orderInfo) {
        PaymentOrder order = new PaymentOrder();
        order.amount = amount == null ? "" : amount;
        order.currency = currency == null ? "" : currency;
        order.orderNo = orderNo == null ? "" : orderNo;
        order.orderTime = orderTime == null ? "" : orderTime;
        order.orderInfo = orderInfo == null ? "" : orderInfo;
        if (source != null) {
            order.requestId = source.requestId;
            order.paymentMethod = source.paymentMethod;
            order.callbackPackage = source.callbackPackage;
            order.callbackAction = source.callbackAction;
            order.callbackUri = source.callbackUri;
            order.returnReceiptData = source.returnReceiptData;
            order.rawJson = source.rawJson;
        }
        return order;
    }

    public boolean hasOrderInfo() {
        return !TextUtils.isEmpty(orderNo)
                || !TextUtils.isEmpty(orderTime)
                || !TextUtils.isEmpty(orderInfo);
    }

    public static void put(Intent intent, PaymentOrder order) {
        if (intent != null && order != null) {
            intent.putExtra(EXTRA_ORDER, order);
        }
    }

    public static PaymentOrder get(Intent intent) {
        if (intent == null) {
            return null;
        }
        Serializable value = intent.getSerializableExtra(EXTRA_ORDER);
        return value instanceof PaymentOrder ? (PaymentOrder) value : null;
    }

    private static String readString(JSONObject obj, String key) {
        if (obj == null || !obj.has(key) || obj.isNull(key)) {
            return "";
        }
        Object value = obj.opt(key);
        return value == null ? "" : String.valueOf(value).trim();
    }

    private static String normalizeAmount(String amount) {
        if (TextUtils.isEmpty(amount)) {
            return "";
        }
        try {
            return new BigDecimal(amount)
                    .setScale(2, RoundingMode.DOWN)
                    .toPlainString();
        } catch (NumberFormatException e) {
            return "";
        }
    }

    private static String centsToAmount(long cents) {
        return new BigDecimal(cents)
                .divide(new BigDecimal("100"), 2, RoundingMode.DOWN)
                .toPlainString();
    }

    private static String itemsSummary(JSONArray items) {
        if (items == null || items.length() == 0) {
            return "";
        }
        List<String> parts = new ArrayList<>();
        for (int i = 0; i < items.length(); i++) {
            JSONObject item = items.optJSONObject(i);
            if (item == null) {
                continue;
            }
            String name = firstNonEmpty(readString(item, "name"), readString(item, "title"));
            if (TextUtils.isEmpty(name)) {
                continue;
            }
            int qty = item.optInt("qty", item.optInt("quantity", 1));
            parts.add(qty > 1 ? name + " x" + qty : name);
        }
        return TextUtils.join(", ", parts);
    }

    private static String firstNonEmpty(String... values) {
        if (values == null) {
            return "";
        }
        for (String value : values) {
            if (!TextUtils.isEmpty(value)) {
                return value;
            }
        }
        return "";
    }
}
