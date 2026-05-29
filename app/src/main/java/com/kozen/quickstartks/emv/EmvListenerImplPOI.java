package com.kozen.quickstartks.emv;

import static com.kozen.quickstartks.utils.Utils.CURRENCY_TAG;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import com.kozen.financial.constant.ConstantEmv;
import com.kozen.quickstartks.pinpad.PinPadDialogPOI;
import com.kozen.quickstartks.pinpad.PinPadPOI;
import com.kozen.quickstartks.utils.SelectKernelUtils;
import com.pos.sdk.emvcore.POIEmvCoreManager;
import com.pos.sdk.emvcore.POIEmvCoreManager.EmvCardInfoConstraints;
import com.pos.sdk.security.POIHsmManage;
import com.kozen.quickstartks.R;
import com.kozen.quickstartks.TransActivity;
import com.kozen.quickstartks.TransResultActivity;
import com.kozen.quickstartks.emvconfig.BerTag;
import com.kozen.quickstartks.emvconfig.BerTlv;
import com.kozen.quickstartks.emvconfig.BerTlvBuilder;
import com.kozen.quickstartks.emvconfig.BerTlvParser;
import com.kozen.quickstartks.emvconfig.BerTlvs;
import com.kozen.quickstartks.utils.HexUtil;
import com.kozen.quickstartks.utils.AppExecutors;
import com.kozen.quickstartks.utils.BundleUtil;
import com.kozen.quickstartks.utils.DialogUtils;
import com.kozen.quickstartks.utils.LogPrintUtil;
import com.kozen.quickstartks.utils.ParameterInit;
import com.kozen.quickstartks.utils.PosUtils;
import com.kozen.quickstartks.utils.Utils;
import com.pos.sdk.accessory.POIGeneralAPI;
import com.pos.sdk.emvcore.IPosEmvCoreListener;
import com.pos.sdk.emvcore.PosEmvErrorCode;

import java.util.List;

public class EmvListenerImplPOI extends IPosEmvCoreListener.Stub {

    private Context context;
    private int chnaleType = 0;
    private int mTransResult = -1;
    private int Card_Type;

    private TransActivity instance;

    private static String TAG = "EmvListenerImplPOI";

    public EmvListenerImplPOI(Context context) {
        this.context = context;
    }

    @Override
    public void onEmvProcess(int type, Bundle bundle) throws RemoteException {
        Log.d(TAG, "onEmvProcess type:" + type);
        chnaleType = type;
        AppExecutors.getInstance().mainThread().execute(new Runnable() {
            @Override
            public void run() {
                TransActivity.getInstance().scrollingLight(type);
            }
        });
    }

    @Override
    public void onSelectApplication(List<String> list, boolean isFirstSelect) throws RemoteException {
        AppExecutors.getInstance().mainThread().execute(new Runnable() {
            @Override
            public void run() {
                TransActivity.getInstance().selectApplication(list);
            }
        });
    }

    @Override
    public void onConfirmCardInfo(int mode, Bundle bundle) throws RemoteException {
        Bundle outBundle = new Bundle();
        LogPrintUtil.printBundle(bundle, "onConfirmCardInfo");
        Log.e(TAG, "onConfirmCardInfo mode:" + mode);
        if (mode == POIEmvCoreManager.CMD_AMOUNT_CONFIG) {
            outBundle.putString(EmvCardInfoConstraints.OUT_AMOUNT, "11");
            outBundle.putString(EmvCardInfoConstraints.OUT_AMOUNT_OTHER, "22");
        } else if (mode == POIEmvCoreManager.CMD_TRY_OTHER_APPLICATION) {
            outBundle.putBoolean(EmvCardInfoConstraints.OUT_CONFIRM, true);
        } else if (mode == POIEmvCoreManager.CMD_ISSUER_REFERRAL) {
            outBundle.putBoolean(EmvCardInfoConstraints.OUT_CONFIRM, true);
        } else if (mode == POIEmvCoreManager.CMD_SELECT_APPLICATION) {
            outBundle.putByteArray(POIEmvCoreManager.EmvCardInfoConstraints.OUT_TLV, new byte[0]);
//                emvCoreManager.onSetCardInfoResponse(outBundle);
        } else if (mode == POIEmvCoreManager.CMD_READ_RECORD) {
            outBundle.putByteArray(POIEmvCoreManager.EmvCardInfoConstraints.OUT_TLV, new byte[0]);
//                emvCoreManager.onSetCardInfoResponse(outBundle);
        } else if (mode == POIEmvCoreManager.CMD_SELECT_KERNEL) {
            Log.d(TAG, "onConfirmCardInfo: CMD_SELECT_KERNEL");
            SelectKernelUtils.doSelectKernel(bundle.getByteArray(POIEmvCoreManager.EmvCardInfoConstraints.DATA));
        }  else if (mode == POIEmvCoreManager.CMD_CARD_READ_SUCCESS) {
            Log.d(TAG, "onConfirmCardInfo: CMD_CARD_READ_SUCCESS");
            if (chnaleType == POIEmvCoreManager.DEVICE_CONTACTLESS) {
                POIGeneralAPI.getDefault().setBeep(true, 200, 1);
            }
        }
        TransActivity.getInstance().emvPoiManager.onSetCardInfoResponse(outBundle);
    }

    @Override
    public void onKernelType(int type) throws RemoteException {
        Card_Type = type;
    }

    @Override
    public void onSecondTapCard() throws RemoteException {
        TransActivity.tvMessage1.setText(R.string.trans_second_tap);
    }

    @Override
    public void onRequestInputPin(Bundle bundle) throws RemoteException {
        AppExecutors.getInstance().mainThread().execute(new Runnable() {
            @Override
            public void run() {

                //这里PIN格式写死了，后面可能需要更改
                int keyMode = POIHsmManage.PED_PINBLOCK_FETCH_MODE_TPK;
                int keyIndex = ParameterInit.KeyIndexConstants.SESSION_PIN_KEY_INDEX;
                Log.d("onRequestInputPin", "PinPadDialog bundle:" + BundleUtil.showKeyTypesInBundle(bundle));
                if ("P13".equals(Build.MODEL) || "P3".equals(Build.MODEL) || "P17".equals(Build.MODEL) || "D300".equals(Build.MODEL)) {
                    PinPadPOI dialog = new PinPadPOI(TransActivity.getInstance(), bundle, keyMode, keyIndex, new PinPadPOI.PinInputFinish() {
                        @Override
                        public void onSuccess(byte[] pinBlock, byte[] pinKsn) {
                        }

                        @Override
                        public void onError(int verifyResult, int pinTryCntOut) {

                        }
                    });
                } else {
                    PinPadDialogPOI dialog = new PinPadDialogPOI(TransActivity.getInstance(), bundle, keyMode, keyIndex, new PinPadDialogPOI.PinInputFinish() {
                        @Override
                        public void onSuccess(byte[] pinBlock, byte[] pinKsn) {
                        }

                        @Override
                        public void onError(int verifyResult, int pinTryCntOut) {

                        }
                    });
                    dialog.showDialog();
                }
            }
        });
    }

    private Bundle processOnlineResult(String data) {
        Bundle bundle = new Bundle();
        BerTlvBuilder tlvBuilder = new BerTlvBuilder();
        String authRespCode = null;
        String authCode = null;
        String script = null;
        BerTlvParser tlvParser = new BerTlvParser();
        List<BerTlv> tlvs = tlvParser.parse(PosUtils.hexStringToBytes(data)).getList();
        for (BerTlv tlv : tlvs) {
            switch (tlv.getTag().getBerTagHex()) {
                case "8A":
                    authRespCode = tlv.getHexValue();
                    break;
                case "91":
                    authCode = tlv.getHexValue();
                    break;
                case "71":
                case "72":
                    tlvBuilder.addBerTlv(tlv);
                    break;
                default:
                    break;
            }
        }
        if (tlvBuilder.build() != 0) {
            script = PosUtils.bytesToHexString(tlvBuilder.buildArray());
        }

//            EmvOnlineConstraints.EMV_ONLINE_FAIL;
        if (authRespCode != null) {
            switch (authRespCode) {
                case "3030":
                    bundle.putInt(POIEmvCoreManager.EmvOnlineConstraints.OUT_AUTH_RESP_CODE, POIEmvCoreManager.EmvOnlineConstraints.EMV_ONLINE_APPROVE);
                    bundle.putByteArray(POIEmvCoreManager.EmvOnlineConstraints.OUT_SPECIAL_AUTH_RESP_CODE,
                            PosUtils.hexStringToBytes("3030"));
                    break;
                case "3031":
                    bundle.putInt(POIEmvCoreManager.EmvOnlineConstraints.OUT_AUTH_RESP_CODE, POIEmvCoreManager.EmvOnlineConstraints.EMV_ONLINE_REFER_TO_CARD_ISSUER);
                    bundle.putByteArray(POIEmvCoreManager.EmvOnlineConstraints.OUT_SPECIAL_AUTH_RESP_CODE,
                            PosUtils.hexStringToBytes("3031"));
                    break;
                case "3032":
                    bundle.putInt(POIEmvCoreManager.EmvOnlineConstraints.OUT_AUTH_RESP_CODE, POIEmvCoreManager.EmvOnlineConstraints.EMV_ONLINE_DENIAL);
                    bundle.putByteArray(POIEmvCoreManager.EmvOnlineConstraints.OUT_SPECIAL_AUTH_RESP_CODE,
                            PosUtils.hexStringToBytes("3032"));
                    break;
                case "3535":
                    bundle.putInt(POIEmvCoreManager.EmvOnlineConstraints.OUT_AUTH_RESP_CODE, POIEmvCoreManager.EmvOnlineConstraints.EMV_ONLINE_FAIL);
                    bundle.putByteArray(POIEmvCoreManager.EmvOnlineConstraints.OUT_SPECIAL_AUTH_RESP_CODE,
                            PosUtils.hexStringToBytes("3535"));
                    break;
                case "3035":
                    bundle.putInt(POIEmvCoreManager.EmvOnlineConstraints.OUT_AUTH_RESP_CODE, POIEmvCoreManager.EmvOnlineConstraints.EMV_ONLINE_FAIL);
                    bundle.putByteArray(POIEmvCoreManager.EmvOnlineConstraints.OUT_SPECIAL_AUTH_RESP_CODE,
                            PosUtils.hexStringToBytes("3531"));
                    break;
                default:
                    bundle.putInt(POIEmvCoreManager.EmvOnlineConstraints.OUT_AUTH_RESP_CODE, POIEmvCoreManager.EmvOnlineConstraints.EMV_ONLINE_FAIL);
                    break;
            }
        }
        if (authCode != null) {
            bundle.putByteArray(POIEmvCoreManager.EmvOnlineConstraints.OUT_AUTH_DATA, PosUtils.hexStringToBytes(authCode));
        }
        if (script != null) {
            bundle.putByteArray(POIEmvCoreManager.EmvOnlineConstraints.OUT_ISSUER_SCRIPT, PosUtils.hexStringToBytes(script));
        }

        return bundle;
    }

    @Override
    public void onRequestOnlineProcess(Bundle bundle) throws RemoteException {
        AppExecutors.getInstance().mainThread().execute(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "here is the emv data return from the SDK:");
                byte[] data = bundle.getByteArray(POIEmvCoreManager.EmvOnlineConstraints.EMV_DATA);
                if (data != null) {
                    Log.d(TAG, "Trans Data : " + HexUtil.toHexString(data));
                    StringBuffer sb = new StringBuffer();
                    BerTlvParser tlvParser = new BerTlvParser();
                    BerTlvs tlvs = tlvParser.parse(data);
                    for (BerTlv tlv : tlvs.getList()) {

                        Log.d(TAG, String.format("%1$-4s", tlv.getTag().getBerTagHex())
                                + " : " + tlv.getHexValue());
                    }
                }
                Log.d(TAG, "app can pack the iso8583 DE55 data base on the EMV_DATA");

                DialogUtils.showProgressDialog(String.valueOf(R.string.trans_online_auth), TransActivity.getInstance());
            }
        });

        try {
            Thread.sleep(1500);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        AppExecutors.getInstance().mainThread().execute(new Runnable() {
            @Override
            public void run() {
                DialogUtils.dismissProgressDialog();
                Bundle outBundle = new Bundle();
                //Todo
                Log.d(TAG, "will feedback the kernel with the host response");
                Log.d(TAG, "the following is the fix data, and need to modify base on the host response");

                //Whether the connection is successful, if successful isOnlineSuccess = true, otherwise isOnlineSuccess = false
                boolean isOnlineSuccess = true;
                if (isOnlineSuccess) {
                    Log.d(TAG, "please fill in the DE55 data in ISO8583 message ");
                    Log.d(TAG, "here is the hardcode sample data ");
                    outBundle = processOnlineResult("8A023030");
                } else {
                    outBundle.putInt(POIEmvCoreManager.EmvOnlineConstraints.OUT_AUTH_RESP_CODE,
                            POIEmvCoreManager.EmvOnlineConstraints.EMV_ONLINE_FAIL);
                }
                Log.d("onRequestOnlineProcess", BundleUtil.showKeyTypesInBundle(outBundle));
                TransActivity.getInstance().emvPoiManager.onSetOnlineResponse(outBundle);
            }
        });
    }

    @Override
    public void onTransactionResult(int result, Bundle bundle) throws RemoteException {
        Log.d(TAG, "onTransactionResult " + result);
        AppExecutors.getInstance().mainThread().execute(new Runnable() {
            @Override
            public void run() {
                switch (result) {
                    case PosEmvErrorCode.EMV_MULTI_CONTACTLESS:
                    case PosEmvErrorCode.EMV_OTHER_ICC_INTERFACE:
                        TransActivity.getInstance().onTransStart();
                        return;
                    case PosEmvErrorCode.EMV_FALLBACK:
                    case PosEmvErrorCode.EMV_APP_EMPTY:
                        TransActivity.getInstance().isFallBack = true;
                        TransActivity.getInstance().onTransStart();
                        return;
                    default:
                        break;
                }

                switch (result) {
                    case PosEmvErrorCode.EMV_APPROVED:
                    case PosEmvErrorCode.EMV_APPROVED_ONLINE:
                    case PosEmvErrorCode.EMV_FORCE_APPROVED:
                    case PosEmvErrorCode.EMV_DELAYED_APPROVED:
                    case PosEmvErrorCode.APPLE_VAS_APPROVED:
                        mTransResult = 0;

                        break;

                    case PosEmvErrorCode.EMV_TIMEOUT:
                        mTransResult = 1;
                        //Todo
                        break;
                    case PosEmvErrorCode.EMV_CANCEL:
                        mTransResult = 2;
                        //Todo
                        break;
                    default:
                        mTransResult = -1;
                        break;
                }

                if (mTransResult != 2) {
                    TransActivity.getInstance().transData = bundle.getByteArray(POIEmvCoreManager.EmvResultConstraints.EMV_DATA);
                    if (TransActivity.getInstance().transData != null) {
                        updateCardType(TransActivity.getInstance().transData);
                    }

                    Intent intent = new Intent(TransActivity.getInstance(), TransResultActivity.class);
                    intent.putExtra(TransActivity.TransResult_Code, mTransResult);
                    intent.putExtra(TransActivity.TransResult_Amount, TransActivity.getInstance().eAmount);
                    intent.putExtra(TransActivity.TransResult_Data, TransActivity.getInstance().transData);
                    intent.putExtra(TransActivity.TransResult_Card_Type, Card_Type);
                    intent.putExtra(CURRENCY_TAG, TransActivity.getInstance().currency);
                    TransActivity.getInstance().startActivity(intent);
                    TransActivity.getInstance().finish();
                } else {
                    Utils.setToast(TransActivity.getInstance(), String.valueOf(R.string.trans_user_cancel));
                    TransActivity.getInstance().finish();
                }

            }
        });
    }

    private void updateCardType(byte[] data) {
        if (Card_Type != POIEmvCoreManager.EMV_CARD_VISA) {
            return;
        }

        BerTlvParser tlvParser = new BerTlvParser();
        BerTlvs tlvs = tlvParser.parse(data);
        BerTlv tlv = tlvs.find(new BerTag("9F06"));
        if (tlv != null && tlv.getHexValue().contains("A000000333")) {
            Card_Type = POIEmvCoreManager.EMV_CARD_UNIONPAY;
        }
    }
}
