package com.cybavo.example.wallet.wxapi;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.cybavo.example.wallet.helper.WeChatSignInHelper;
import com.tencent.mm.opensdk.modelbase.BaseReq;
import com.tencent.mm.opensdk.modelbase.BaseResp;
import com.tencent.mm.opensdk.modelmsg.SendAuth;
import com.tencent.mm.opensdk.openapi.IWXAPIEventHandler;

import androidx.appcompat.app.AppCompatActivity;

/**
 * For WeChat sign in integration
 */
public class WXEntryActivity extends AppCompatActivity {

    private static final String TAG = WXEntryActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate");
        super.onCreate(savedInstanceState);

        if ("sign_in".equals(getIntent().getAction())) {
            // signIn
            WeChatSignInHelper.signIn(this);
        } else {
            handleWeChatIntent(getIntent());
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        Log.d(TAG, "onNewIntent");
        super.onNewIntent(intent);
        handleWeChatIntent(intent);
    }

    private void handleWeChatIntent(Intent intent) {
        WeChatSignInHelper.handleIntent(this, intent, new IWXAPIEventHandler() {
            @Override public void onReq(BaseReq baseReq) { }

            @Override
            public void onResp(BaseResp baseResp) {
                final SendAuth.Resp resp = (SendAuth.Resp) baseResp;
                if (resp.errCode == BaseResp.ErrCode.ERR_OK) {
                    final String code = String.valueOf(resp.code);
                    setResult(RESULT_OK, new Intent().putExtra("code", code));
                } else {
                    setResult(RESULT_CANCELED, new Intent().putExtra("errCode", resp.errCode));
                }
                finish();
            }
        });
    }

    @Override
    public void onBackPressed() {
        // block back
    }
}
