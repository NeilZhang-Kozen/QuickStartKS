package com.kozen.quickstartks;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Typeface;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.util.Log;

import com.pos.sdk.printer.POIPrinterManager;
import com.pos.sdk.printer.models.BitmapPrintLine;
import com.pos.sdk.printer.models.PrintLine;
import com.pos.sdk.printer.models.TextPrintLine;

import java.util.ArrayList;
import java.util.List;

public class PrinterPOI {
    private static final String TAG = "Printer";

    public interface PrintFinish {
        void onSuccess(String data);

        void onError(int errorCode);
    }

    /**
     * The string is grouped into 40 and generated the corresponding picture
     *
     * @param context
     * @param content  the string need to print
     * @param fontPath the font path in assets, such as 'fonts/SBSansCondMonoRegular.ttf'
     * @return Bitmap contain the content string
     */
    public static Bitmap generateBitmap(Context context, String content, String fontPath) {
        StringBuilder sb = new StringBuilder(content);
        //Calculate 40 characters and wrapping
        int line = content.length() / 40;
        int extra = content.length() % 40;
        for (int i = 0; i < line; i++) {
            sb.insert(40 + 40 * i + i, "\n");
        }
        //creat bitmap
        Bitmap newBitmap = Bitmap.createBitmap(384, (line + (extra == 0 ? 0 : 1)) * 25, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(newBitmap);
        canvas.drawBitmap(newBitmap, 0, 0, null);
        //Creat textPaint, set the text size and letterSpacing
        TextPaint textPaint = new TextPaint();
        textPaint.setAntiAlias(true);
        textPaint.setFilterBitmap(false);
        textPaint.setDither(true);
        textPaint.setTextSize(18.6f);
        textPaint.setLetterSpacing(0.022f);
        //Set font of SBSansCondMonoRegular
        Typeface typeface = Typeface.createFromAsset(context.getAssets(), fontPath);
        textPaint.setTypeface(typeface);
        //Draw text with staticLayout and textPaint
        StaticLayout staticLayout = new StaticLayout(sb.toString(), textPaint, 384, Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false);
        canvas.save();
        staticLayout.draw(canvas);
        canvas.restore();
        return newBitmap;
    }

    public static void printImage(Context context, Bitmap bitmap, PrintFinish printFinish) {
        POIPrinterManager printerManager;
        printerManager = new POIPrinterManager(context);
//        if(printerManager==null){
//            printerManager = new POIPrinterManager(context);
//        }
        printerManager.open();
        int state = printerManager.getPrinterState();
        Log.d(TAG, "printer state = " + state);
        //printerManager.setPrintFont("/system/fonts/Android-1.ttf");
//        printerManager.setPrintGray(1000);
        printerManager.setPrintGray(1200);
        printerManager.getPrinterLength();
        //printerManager.cleanCache();
        BitmapFactory.Options options = new BitmapFactory.Options();
//或者 inDensity 搭配 inTargetDensity 使用，算法和 inSampleSize 一样
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
//        Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.result_re,options);
        printerManager.addPrintLine(new BitmapPrintLine(bitmap, PrintLine.CENTER));

//        printerManager.addPrintLine(new BitmapPrintLine(bitmap, PrintLine.LEFT));
//        printerManager.addPrintLine(new TextPrintLine(" ", 0, 100));
//        printerManager.addPrintLine(new TextPrintLine("Welcome to use KOZEN products", 0, 30,true));
        POIPrinterManager.IPrinterListener listener = new POIPrinterManager.IPrinterListener() {
            @Override
            public void onStart() {
            }

            @Override
            public void onFinish() {
                //printerManager.cleanCache();
                printerManager.close();
                printFinish.onSuccess("");
            }

            @Override
            public void onError(int code, String msg) {
                Log.e("POIPrinterManager", "code: " + code + "msg: " + msg);
                printerManager.close();
                printFinish.onError(code);
            }
        };
        if (state == 4) {
            printerManager.close();
            printFinish.onError(state);
            return;
        }
        printerManager.beginPrint(listener);
    }

        public static void printImage (Context context, Bitmap bitmap1, Bitmap bitmap2, Bitmap
        bitmap3){
            final POIPrinterManager printerManager = new POIPrinterManager(context);
            printerManager.open();
            int state = printerManager.getPrinterState();
            Log.d(TAG, "printer state = " + state);
            //printerManager.setPrintFont("/system/fonts/Android-1.ttf");
            printerManager.setPrintGray(4800);
            printerManager.setLineSpace(5);
            //printerManager.cleanCache();
            printerManager.addPrintLine(new BitmapPrintLine(bitmap1, PrintLine.LEFT));
            printerManager.addPrintLine(new BitmapPrintLine(bitmap2, PrintLine.LEFT));
            printerManager.addPrintLine(new BitmapPrintLine(bitmap3, PrintLine.LEFT));
            printerManager.addPrintLine(new TextPrintLine(" ", 0, 100));
            POIPrinterManager.IPrinterListener listener = new POIPrinterManager.IPrinterListener() {
                @Override
                public void onStart() {
                }

                @Override
                public void onFinish() {
                    //printerManager.cleanCache();
                }

                @Override
                public void onError(int code, String msg) {
                    Log.e("POIPrinterManager", "code: " + code + "msg: " + msg);
                    printerManager.close();
                }
            };
            if (state == 4) {
                printerManager.close();
                return;
            }
            printerManager.beginPrint(listener);
        }

        private static List<TextPrintLine> printList (String leftStr, String centerStr, String
        rightStr,int size, boolean bold){
            TextPrintLine textPrintLine1 = new TextPrintLine(leftStr, PrintLine.LEFT, size, bold);
            TextPrintLine textPrintLine2 = new TextPrintLine(centerStr, PrintLine.CENTER, size, bold);
            TextPrintLine textPrintLine3 = new TextPrintLine(rightStr, PrintLine.RIGHT, size, bold);
            List<TextPrintLine> list = new ArrayList<>();
            list.add(textPrintLine1);
            list.add(textPrintLine2);
            list.add(textPrintLine3);
            return list;
        }
    }
