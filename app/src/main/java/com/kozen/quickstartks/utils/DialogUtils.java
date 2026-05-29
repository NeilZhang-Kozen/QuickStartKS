package com.kozen.quickstartks.utils;

import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import com.kozen.quickstartks.R;

public class DialogUtils {

    private static AlertDialog mProgressDialog;
    private static AlertDialog.Builder builder;
    private static View mDialogView;
    private static ProgressBar progressBar;
    private static AlertDialog loadingDialog;
    private static TextView progressDialogTitle;
    private static TextView progressDialogSubtitle;
    public static void showProgressDialog(String text, Activity context) {
        if (context == null || context.isFinishing()) {
            return;
        }
        dismissProgressDialog();
        AlertDialog.Builder progressBuilder = new AlertDialog.Builder(context);
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_waiting_customer, null);
        progressDialogTitle = dialogView.findViewById(R.id.tv_waiting_title);
        progressDialogSubtitle = dialogView.findViewById(R.id.tv_waiting_subtitle);
        progressDialogTitle.setText(text);
        progressDialogSubtitle.setText(R.string.waiting_customer_subtitle);
        progressBuilder.setView(dialogView);
        mProgressDialog = progressBuilder.create();
        mProgressDialog.setCancelable(false);
        mProgressDialog.show();
        Window window = mProgressDialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawableResource(android.R.color.transparent);
            window.getDecorView().setPadding(0, 0, 0, 0);
            WindowManager.LayoutParams lp = window.getAttributes();
            lp.width = (int) (context.getResources().getDisplayMetrics().widthPixels * 0.82f);
            lp.height = ViewGroup.LayoutParams.WRAP_CONTENT;
            window.setAttributes(lp);
        }
    }
    public static void updateProgressDialog(String text) {
        if (mProgressDialog == null || !mProgressDialog.isShowing() || progressDialogTitle == null) {
            return;
        }
        progressDialogTitle.setText(text);
    }
    public static void dismissProgressDialog() {
        if(mProgressDialog !=null && mProgressDialog.isShowing()){
            mProgressDialog.dismiss();
        }
        mProgressDialog = null;
        progressDialogTitle = null;
        progressDialogSubtitle = null;
    }
    public static void dismissLoadingDialog() {
        if(loadingDialog !=null && loadingDialog.isShowing()){
            loadingDialog.dismiss();
        }
    }
    public interface DialogCallback {
        void onConfirm();
        void onCancel();
    }
    public static void showAlertDialogCenter(Context context,DialogCallback callback) {
        //使用Builder来创建
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        View mDialogView = LayoutInflater.from(context).inflate(R.layout.custom_dialog_center, null);
        builder.setView(mDialogView);
        //使用创建器,生成一个对话框
        AlertDialog dialog = builder.create();
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        dialog.getWindow().getAttributes().windowAnimations = R.style.CustomDialog;
//        TextView tv_content = mDialogView.findViewById(R.id.tv_content);
//        tv_content.setText(content);
        mDialogView.findViewById(R.id.bt_confirm).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.cancel();
                callback.onConfirm();
            }
        });
        dialog.setCancelable(false);
        dialog.show();
    }
    public static void showAlertDialog(Context context, String title, String content,DialogCallback callback) {
        //使用Builder来创建
        builder = new AlertDialog.Builder(context);
        mDialogView = LayoutInflater.from(context).inflate(R.layout.loading_dialog, null);
        builder.setView(mDialogView);
        progressBar = mDialogView.findViewById(R.id.progressBar);
        //使用创建器,生成一个对话框
        loadingDialog = builder.create();
        //  将弹框边距，水平铺满
        Window dialogWindow = loadingDialog.getWindow();
        // 把 DecorView 的默认 padding 取消，同时 DecorView 的默认大小也会取消
        dialogWindow.getDecorView().setPadding(0, 0, 0, 0);
        // 设置宽度
        WindowManager.LayoutParams lp = dialogWindow.getAttributes();
        lp.width = ViewGroup.LayoutParams.MATCH_PARENT;
        lp.height = ViewGroup.LayoutParams.WRAP_CONTENT;
        loadingDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        loadingDialog.getWindow().getAttributes().windowAnimations = R.style.CustomDialog;
//        TextView tv_title = mDialogView.findViewById(R.id.tv_title);
//        TextView tv_content = mDialogView.findViewById(R.id.tv_content);
//        tv_title.setText(title);
//        tv_content.setText(content);
        loadingDialog.setCancelable(false);
        loadingDialog.show();
    }
    public static void setIndeterminateDrawable(Context context, int success, int drawableId){
        if(mDialogView == null){
            return;
        }

        Drawable drawable = context.getResources().getDrawable(drawableId);
        progressBar.setIndeterminateDrawable(drawable);
        progressBar.setProgressDrawable(drawable);
        TextView title = mDialogView.findViewById(R.id.title);
        TextView content = mDialogView.findViewById(R.id.content);
        Button btn_dialog = mDialogView.findViewById(R.id.btn_dialog);
        if(success == 0){
            btn_dialog.setVisibility(View.VISIBLE);
            title.setText("Init Success");
            content.setText("Transaction encryption has been successfully initiated.");
            btn_dialog.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dismissLoadingDialog();
                }
            });
        }else{
            btn_dialog.setVisibility(View.VISIBLE);
            title.setText("Init Failed");
            content.setText("Transaction encryption initiated failed, please retry.");
            btn_dialog.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dismissLoadingDialog();
                }
            });

        }

    }

}
