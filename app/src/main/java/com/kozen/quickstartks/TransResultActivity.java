
package com.kozen.quickstartks;

import static com.kozen.quickstartks.utils.Utils.USD_TAG;

import android.app.Activity;
import android.graphics.Bitmap;
import android.media.AudioAttributes;
import android.media.SoundPool;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.view.animation.TranslateAnimation;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.kozen.component.constant.KeyboardConstant;
import com.kozen.component.keyboard.InputCallback;
import com.kozen.component_client.ComponentEngine;
import com.kozen.quickstartks.R;

import com.kozen.financial.constant.errorcode.PrinterError;
import com.kozen.quickstartks.emv.utils.EmvCard;
import com.kozen.quickstartks.emv.utils.Utility;
import com.kozen.quickstartks.utils.DialogUtils;
import com.kozen.quickstartks.utils.ScreenUtils;
import com.kozen.quickstartks.utils.SecondScreenUtils;
import com.kozen.quickstartks.utils.Utils;
import com.pos.sdk.printer.POIPrinterManager;

import java.text.SimpleDateFormat;
import java.util.Locale;

public class TransResultActivity extends BaseActivity {

    private ScrollView sl_receipt;
    private TextView tv_result_confirm;
    private View reslut_line;
    private SoundPool mSoundPool;

    private ReceiptBuilder.ReceiptData receiptData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.trans_result);
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }
        int code = getIntent().getIntExtra(TransActivity.TransResult_Code, -1);
        int Card_Type = getIntent().getIntExtra(TransActivity.TransResult_Card_Type, 0);
        String amount = getIntent().getStringExtra(TransActivity.TransResult_Amount);
        String currency = getIntent().getStringExtra(Utils.CURRENCY_TAG);
        byte[] data = getIntent().getByteArrayExtra(TransActivity.TransResult_Data);
        ImageView imageView = findViewById(R.id.iv_result_image);
        reslut_line = findViewById(R.id.reslut_line);
        TextView tv_result = findViewById(R.id.tv_result);
        TextView tv_card_number = findViewById(R.id.tv_card_number);
        TextView tv_card_user = findViewById(R.id.tv_card_user);
        TextView tv_amount = findViewById(R.id.tv_amount);
        tv_result_confirm = findViewById(R.id.tv_result_confirm);
        sl_receipt = findViewById(R.id.sl_receipt);
        LinearLayout content = findViewById(R.id.content);
        LinearLayout result_data_ll = findViewById(R.id.result_data_ll);
        tv_result_confirm.setVisibility(View.GONE);
        tv_amount.setText((USD_TAG.equals(currency) ? "$" : "€") + amount);
        String cardNo = "";
        String cardHolder = "";
        String cardBrand = "";
        String expiry = "";
        if (data != null) {
            EmvCard emvCard = new EmvCard(data);

            if (emvCard.getCardNumber() != null) {
                cardNo = Utility.formatCard(emvCard.getCardNumber(), true);
                tv_card_number.setText(cardNo);
            } else {
                tv_card_number.setText("");
            }

//            tv_card_user.setText(EmvCardType.getCardType(Card_Type));

            if (emvCard.getCardHolderName() != null) {
                cardHolder = emvCard.getCardHolderName();
                tv_card_user.setText(cardHolder);
            }

            if (emvCard.getAppLabel() != null) {
                cardBrand = emvCard.getAppLabel();
            }

            if (emvCard.getCardExpireDate() != null) {
                expiry = new SimpleDateFormat("MM/yy", Locale.getDefault()).format(emvCard.getCardExpireDate());
            }
        }

        amount = (USD_TAG.equals(currency) ? "$" : "€") + amount;
        receiptData = ReceiptBuilder.fromTransaction(cardNo, cardHolder, cardBrand, expiry, amount, "SALE");
        bindReceiptViews(findViewById(android.R.id.content), receiptData);
        if (code == 0) {
            playSound();
            if (TransInitActivity.isExistSecScreen) {
                SecondScreenUtils.showView(TransResultActivity.this, R.layout.second_trans_result_success, amount);
            }
            sl_receipt.setVisibility(View.VISIBLE);
            tv_result_confirm.setVisibility(View.VISIBLE);
            imageView.clearAnimation();
            tv_result.setText(getString(R.string.trans_result_success));
            tv_result_confirm.setText(getString(R.string.trans_result_print));
            imageView.setImageResource(R.drawable.result_success);
            tv_result_confirm.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    sl_receipt.scrollTo(0, 0);
                    slideUp(content);
//                String data = "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAaa";
//                Printer.printImage(TransResultActivity.this, Printer.generateBitmap(TransResultActivity.this,data,"fonts/SBSansCondMonoRegular.ttf"));
//                    Printer.printImage(TransResultActivity.this, layoutToBitmap(content), new Printer.PrintFinish() {
//                    View viewBitmap = LayoutInflater.from(TransResultActivity.this).inflate(R.layout.receipt_content, null);
                    if ("K1362".equals(Build.MODEL)){
                        return;
                    }
                    if (TransInitActivity.isPOISdk) {
                        PrinterPOI.printImage(TransResultActivity.this, layoutToBitmap(TransResultActivity.this), new PrinterPOI.PrintFinish() {
                            @Override
                            public void onSuccess(String data) {
                            }

                            @Override
                            public void onError(int errorCode) {
                                content.clearAnimation();
                                if (POIPrinterManager.ERROR_NO_PAPER == errorCode) {
                                    DialogUtils.showAlertDialogCenter(TransResultActivity.this, new DialogUtils.DialogCallback() {
                                        @Override
                                        public void onConfirm() {

                                        }

                                        @Override
                                        public void onCancel() {

                                        }
                                    });
                                }

                            }
                        });
                    } else {
                        Printer.printImage(TransResultActivity.this, layoutToBitmap(TransResultActivity.this), new Printer.PrintFinish() {
                            @Override
                            public void onSuccess(String data) {
                            }

                            @Override
                            public void onError(int errorCode) {
                                content.clearAnimation();
                                if (PrinterError.PRINTER_ERROR_NO_PAPER == errorCode) {
                                    DialogUtils.showAlertDialogCenter(TransResultActivity.this, new DialogUtils.DialogCallback() {
                                        @Override
                                        public void onConfirm() {

                                        }

                                        @Override
                                        public void onCancel() {

                                        }
                                    });
                                }

                            }
                        });
                    }

                }
            });

        } else if (code == -1) {
            if (TransInitActivity.isExistSecScreen) {
                SecondScreenUtils.showView(TransResultActivity.this, R.layout.second_trans_result_failed, amount);
            }
            sl_receipt.setVisibility(View.GONE);
            tv_result_confirm.setVisibility(View.GONE);
            reslut_line.setVisibility(View.GONE);
            imageView.clearAnimation();
            tv_result.setText(getString(R.string.trans_result_failed));
            imageView.setImageResource(R.drawable.result_fail);
        } else if (code == 1) {
            if (TransInitActivity.isExistSecScreen) {
                SecondScreenUtils.showView(TransResultActivity.this, R.layout.second_trans_result_timeout, amount);
            }
            sl_receipt.setVisibility(View.GONE);
            tv_result_confirm.setVisibility(View.GONE);
            result_data_ll.setVisibility(View.GONE);
            reslut_line.setVisibility(View.GONE);
            imageView.clearAnimation();
            tv_result.setText(getString(R.string.trans_result_timeout));
            imageView.setImageResource(R.drawable.result_timeout);
        } else {
            sl_receipt.setVisibility(View.GONE);
            tv_result_confirm.setVisibility(View.GONE);
            result_data_ll.setVisibility(View.GONE);
            reslut_line.setVisibility(View.GONE);
            startLoadingAnimation(imageView);
            tv_result.setText(getString(R.string.trans_result_wait));
            imageView.setImageResource(R.drawable.result_waiting);
        }

//        btnInit.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                //init key
//                tvMessage1.setText("Initing...");
//                boolean EraseAllKey = true;
//                btnInit.setEnabled(false);
//
//
//            }
//        });

//        edtAmount.setShowSoftInputOnFocus(false);
//        edtAmount.requestFocus();
//        edtAmount.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
//            @Override
//            public void onGlobalLayout() {
//
//                InputMethodManager imm = (InputMethodManager) getSystemService(getApplicationContext().INPUT_METHOD_SERVICE);
//                imm.hideSoftInputFromWindow(edtAmount.getWindowToken(), 0);
////                AppExecutors.getInstance().mainThread().execute(new Runnable() {
////                    @Override
////                    public void run() {
////
////                    }
////                });
//
//
//            }
//        });
//        edtAmount.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                ValueAnimator animator= ValueAnimator.ofFloat(1f,0f,1f);
//                animator.setDuration(500);
//                animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
//                    @Override
//                    public void onAnimationUpdate(ValueAnimator animation) {
//                        tableLayout.setAlpha(animation.getAnimatedFraction());
//                    }
//                });
//                animator.start();
//            }
//        });
//        btnTrans.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                onTransStart();
//            }
//        });
//
//
//        transType = 0;
//
//        emvCoreManager = POIEmvCoreManager.getDefault();
//        emvCoreListener = new POIEmvCoreListener();
        if ("K1211".equals(Build.MODEL)) {
            ComponentEngine.INSTANCE.getKeyboardManager().startPhysicalKeyboard(new InputCallback() {
                @Override
                public void onKey(KeyboardConstant.KeyCode keyCode, KeyboardConstant.KeyAction keyAction) {

                    if ((keyAction.getAction() != 0)) {
                        TransResultActivity.this.finish();
                    }

                }
            });
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (Build.MODEL != null) {
            if ("L200".equals(Build.MODEL) || "P17".equals(Build.MODEL) || Build.MODEL.startsWith("P12") || "K1211".equals(Build.MODEL)) {
                sl_receipt.setVisibility(View.GONE);
                reslut_line.setVisibility(View.GONE);
                tv_result_confirm.setVisibility(View.GONE);
            }
        }

        if ("K1211".equals(Build.MODEL)) {
            try {
                int code = ComponentEngine.INSTANCE.getKeyboardManager().startPhysicalKeyboard(new InputCallback() {
                    @Override
                    public void onKey(KeyboardConstant.KeyCode keyCode, KeyboardConstant.KeyAction keyAction) {
//                                    String text = keyCode.getValue()+"=="+((keyAction.getAction() == 0)? "按下":"起");
//                                    Toast.makeText(TransInitActivity.this,text,Toast.LENGTH_SHORT).show();

                        if ((keyAction.getAction() != 0)) {
                            if (TransResultActivity.this.getWindow().getDecorView().getVisibility() == View.VISIBLE) {
                                if (KeyboardConstant.KeyCode.BUTTON_ENTER == keyCode) {
                                    TransResultActivity.this.finish();

                                }
                            }
                        }

                    }
                });

            } catch (Exception e) {
                finish();
            }
        }
    }

    public void slideUp(View view) {
        view.setVisibility(View.VISIBLE);
        TranslateAnimation animate = new TranslateAnimation(
                0,                 // fromXDelta
                0, 0,                // toXDelta
                0 - view.getHeight() - 20  // fromYDelta
        );
        animate.setDuration(3500);
        animate.setFillBefore(true);
        view.startAnimation(animate);
    }

    private void playSound() {

// 1. 初始化SoundPool（Android 21+）
        SoundPool.Builder builder = new SoundPool.Builder();
        builder.setMaxStreams(10); // 最大同时播放数
        builder.setAudioAttributes(new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build());
        mSoundPool = builder.build();

// 2. 加载音效（从Resources加载）
        int soundId = mSoundPool.load(this, R.raw.pay_successful, 0); // 优先级1

// 3. 播放音效
        mSoundPool.setOnLoadCompleteListener(new SoundPool.OnLoadCompleteListener() {
            @Override
            public void onLoadComplete(SoundPool soundPool, int sampleId, int status) {
                if (status == 0) { // 加载成功
                    soundPool.play(soundId,  // 音效ID
                            1.0f,  // 左声道音量（0.0-1.0）
                            1.0f,  // 右声道音量
                            0,     // 优先级（0为最低）
                            0,     // 循环次数（-1为无限循环）
                            1.0f); // 播放速率（1.0为正常）
                }
            }
        });

//
    }

    public Bitmap layoutToBitmap(Activity activity) {
        int width = ReceiptBuilder.RECEIPT_WIDTH_PX;
        LinearLayout view = (LinearLayout) android.view.LayoutInflater.from(activity)
                .inflate(R.layout.receipt_content, null, false);
        bindReceiptViews(view, receiptData);

        view.measure(
                View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        view.layout(0, 0, view.getMeasuredWidth(), view.getMeasuredHeight());
        Bitmap bitmap = Bitmap.createBitmap(width, view.getMeasuredHeight() + 80, Bitmap.Config.ARGB_8888);
        android.graphics.Canvas canvas = new android.graphics.Canvas(bitmap);
        canvas.drawColor(android.graphics.Color.WHITE);
        view.draw(canvas);
        return bitmap;
    }

    private static void bindReceiptViews(View root, ReceiptBuilder.ReceiptData d) {
        if (root == null || d == null) {
            return;
        }
        setText(root, R.id.tv_receipt_merchant, d.merchantName);
        setText(root, R.id.tv_receipt_transtype, d.transType);
        setText(root, R.id.tv_receipt_mid, d.mid);
        setText(root, R.id.tv_receipt_tid, d.tid);
        setText(root, R.id.tv_receipt_operator, d.operatorNo);
        setText(root, R.id.tv_receipt_card, d.cardNo);
        setText(root, R.id.tv_receipt_brand, d.cardBrand);
        setText(root, R.id.tv_receipt_expiry, d.expiry);
        setText(root, R.id.tv_receipt_batch, d.batchNo);
        setText(root, R.id.tv_receipt_voucher, d.voucherNo);
        setText(root, R.id.tv_receipt_ref, d.refNo);
        setText(root, R.id.tv_receipt_auth, d.authCode);
        setText(root, R.id.tv_receipt_customer, d.cardHolder);
        setText(root, R.id.tv_receipt_datetime, d.dateTime);
        setText(root, R.id.tv_receipt_amount, d.amount);
        setText(root, R.id.tv_receipt_total, d.amount);
    }

    private static void setText(View root, int id, String text) {
        View v = root.findViewById(id);
        if (v instanceof TextView) {
            ((TextView) v).setText(text == null ? "" : text);
        }
    }

    private void startLoadingAnimation(ImageView iv) {
        Animation animation = new RotateAnimation(0, 359, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        animation.setDuration(1000);
        animation.setRepeatCount(1000);
        animation.setFillAfter(true);//设置为true，动画转化结束后被应用
        iv.startAnimation(animation);//开始动画
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mSoundPool != null) {
            mSoundPool.release();
        }
    }
}
