package com.kozen.quickstartks;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Typeface;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.kozen.quickstartks.emvconfig.EMVConfig;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Random;

/**
 * Builds a standard English acquiring receipt (merchant copy) entirely in code
 * and renders it to a print-ready bitmap. Replaces the static receipt_content.xml
 * template for the print path.
 */
public class ReceiptBuilder {

    public static final int RECEIPT_WIDTH_PX = 384;

    public static class ReceiptData {
        public String merchantName = "";
        public String mid = "";
        public String tid = "";
        public String operatorNo = "";
        public String cardNo = "";
        public String cardHolder = "";
        public String cardBrand = "";
        public String expiry = "";
        public String batchNo = "";
        public String voucherNo = "";
        public String refNo = "";
        public String authCode = "";
        public String transType = "SALE";
        public String amount = "";
        public String dateTime = "";
        public String orderNo = "";
        public String orderInfo = "";
        public boolean qrPayment = false;
    }

    /**
     * Builds receipt data from the real values available at the result screen and
     * generates placeholder values for fields this app does not track
     * (batch / voucher / reference / auth code / operator).
     */
    public static ReceiptData fromTransaction(String cardNo, String cardHolder, String cardBrand,
                                              String expiry, String amountWithSymbol, String transType) {
        EMVConfig cfg = EMVConfig.getDefault();
        ReceiptData d = new ReceiptData();
        d.merchantName = safe(cfg.merchantNameAndLocation);
        d.mid = safe(cfg.merchantIdentifier);
        d.tid = safe(cfg.terminalIdentification);
        d.operatorNo = "01";
        d.cardNo = safe(cardNo);
        d.cardHolder = safe(cardHolder);
        d.cardBrand = safe(cardBrand);
        d.expiry = safe(expiry);
        d.transType = TextUtils.isEmpty(transType) ? "SALE" : transType;
        d.amount = safe(amountWithSymbol);

        Date now = new Date();
        d.dateTime = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(now);

        // Placeholder / mock values (this app does not carry real acquiring fields).
        Random r = new Random();
        d.batchNo = "000001";
        d.voucherNo = String.format(Locale.US, "%06d", r.nextInt(1000000));
        d.refNo = new SimpleDateFormat("yyMMddHHmmss", Locale.getDefault()).format(now);
        d.authCode = String.format(Locale.US, "%06d", r.nextInt(1000000));
        return d;
    }

    public static ReceiptData fromTransaction(String cardNo, String cardHolder, String cardBrand,
                                              String expiry, String amountWithSymbol, String transType,
                                              PaymentOrder order, boolean qrPayment) {
        ReceiptData d = fromTransaction(cardNo, cardHolder, cardBrand, expiry, amountWithSymbol, transType);
        d.qrPayment = qrPayment;
        if (order != null) {
            d.orderNo = safe(order.orderNo);
            d.orderInfo = safe(order.orderInfo);
            if (!TextUtils.isEmpty(order.orderTime)) {
                d.dateTime = order.orderTime;
            }
        }
        return d;
    }

    public static View build(Context ctx, ReceiptData d) {
        LinearLayout root = new LinearLayout(ctx);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setLayoutParams(new ViewGroup.LayoutParams(RECEIPT_WIDTH_PX, ViewGroup.LayoutParams.WRAP_CONTENT));
        root.setBackgroundColor(Color.WHITE);
        root.setPadding(12, 16, 12, 16);

        root.addView(center(ctx, d.merchantName, 28, true));
        root.addView(center(ctx, "MERCHANT COPY", 22, false));
        root.addView(center(ctx, d.transType, 26, true));
        root.addView(divider(ctx));

        addRow(root, ctx, "MERCHAN T NO.", d.mid);
        addRow(root, ctx, "TERMINAL NO.", d.tid);
        addRow(root, ctx, "OPERATOR NO.", d.operatorNo);
        addRow(root, ctx, "ORDER NO.", d.orderNo);
        addRow(root, ctx, "ORDER INFO", d.orderInfo);
        if (!d.qrPayment) {
            addRow(root, ctx, "CARD NO.", d.cardNo);
            addRow(root, ctx, "CARD TYPE", d.cardBrand);
            addRow(root, ctx, "EXPIRY DATE", d.expiry);
        }
        addRow(root, ctx, "BATCH NO.", d.batchNo);
        addRow(root, ctx, "VOUCHER NO.", d.voucherNo);
        addRow(root, ctx, "REF NO.", d.refNo);
        addRow(root, ctx, "AUTH CODE", d.authCode);
        addRow(root, ctx, "DATE/TIME", d.dateTime);

        root.addView(divider(ctx));
        root.addView(amountRow(ctx, "AMOUNT", d.amount));
        root.addView(divider(ctx));

        if (!d.qrPayment) {
            TextView sign = left(ctx, "CARDHOLDER SIGNATURE", 20, false);
            ((LinearLayout.LayoutParams) sign.getLayoutParams()).topMargin = 56;
            root.addView(sign);
            root.addView(divider(ctx));

            TextView disclaimer = left(ctx,
                    "I ACKNOWLEDGE SATISFACTORY RECEIPT OF RELATIVE GOODS / SERVICES", 18, false);
            root.addView(disclaimer);
        }

        TextView thanks = center(ctx, "THANK YOU", 20, true);
        ((LinearLayout.LayoutParams) thanks.getLayoutParams()).topMargin = 24;
        root.addView(thanks);

        return root;
    }

    public static Bitmap toBitmap(Context ctx, ReceiptData d) {
        View view = build(ctx, d);
        view.measure(
                View.MeasureSpec.makeMeasureSpec(RECEIPT_WIDTH_PX, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(Integer.MAX_VALUE >> 2, View.MeasureSpec.AT_MOST));
        view.layout(0, 0, view.getMeasuredWidth(), view.getMeasuredHeight());

        Bitmap bitmap = Bitmap.createBitmap(RECEIPT_WIDTH_PX, view.getMeasuredHeight() + 200, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        canvas.drawColor(Color.WHITE);
        view.draw(canvas);
        return bitmap;
    }

    // --- helpers ---------------------------------------------------------

    private static void addRow(LinearLayout root, Context ctx, String label, String value) {
        if (TextUtils.isEmpty(value)) {
            return;
        }
        root.addView(row(ctx, label, value));
    }

    private static LinearLayout row(Context ctx, String label, String value) {
        LinearLayout row = new LinearLayout(ctx);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView left = new TextView(ctx);
        left.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        left.setTextColor(Color.BLACK);
        left.setTextSize(TypedValue.COMPLEX_UNIT_PX, 20);
        left.setGravity(Gravity.START);
        left.setPadding(0, 3, 0, 3);
        left.setText(label);

        TextView right = new TextView(ctx);
        right.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        right.setTextColor(Color.BLACK);
        right.setTextSize(TypedValue.COMPLEX_UNIT_PX, 20);
        right.setGravity(Gravity.END);
        right.setPadding(0, 3, 0, 3);
        right.setText(value);

        row.addView(left);
        row.addView(right);
        return row;
    }

    private static LinearLayout amountRow(Context ctx, String label, String value) {
        LinearLayout row = new LinearLayout(ctx);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView left = new TextView(ctx);
        left.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        left.setTextColor(Color.BLACK);
        left.setTextSize(TypedValue.COMPLEX_UNIT_PX, 30);
        left.setTypeface(Typeface.DEFAULT_BOLD);
        left.setGravity(Gravity.START);
        left.setText(label);

        TextView right = new TextView(ctx);
        right.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        right.setTextColor(Color.BLACK);
        right.setTextSize(TypedValue.COMPLEX_UNIT_PX, 30);
        right.setTypeface(Typeface.DEFAULT_BOLD);
        right.setGravity(Gravity.END);
        right.setText(value);

        row.addView(left);
        row.addView(right);
        return row;
    }

    private static TextView center(Context ctx, String text, int sizePx, boolean bold) {
        TextView tv = new TextView(ctx);
        tv.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        tv.setTextColor(Color.BLACK);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_PX, sizePx);
        tv.setGravity(Gravity.CENTER);
        tv.setPadding(0, 3, 0, 3);
        if (bold) {
            tv.setTypeface(Typeface.DEFAULT_BOLD);
        }
        tv.setText(text);
        return tv;
    }

    private static TextView left(Context ctx, String text, int sizePx, boolean bold) {
        TextView tv = new TextView(ctx);
        tv.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        tv.setTextColor(Color.BLACK);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_PX, sizePx);
        tv.setGravity(Gravity.START);
        tv.setPadding(0, 3, 0, 3);
        if (bold) {
            tv.setTypeface(Typeface.DEFAULT_BOLD);
        }
        tv.setText(text);
        return tv;
    }

    private static View divider(Context ctx) {
        View v = new View(ctx);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 2);
        lp.topMargin = 10;
        lp.bottomMargin = 10;
        v.setLayoutParams(lp);
        v.setBackgroundColor(Color.BLACK);
        return v;
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }
}
