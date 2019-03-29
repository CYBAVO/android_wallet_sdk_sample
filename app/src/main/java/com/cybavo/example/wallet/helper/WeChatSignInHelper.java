package com.cybavo.example.wallet.helper;

import android.content.Context;
import android.content.Intent;

import com.cybavo.example.wallet.R;
import com.tencent.mm.opensdk.modelmsg.SendAuth;
import com.tencent.mm.opensdk.openapi.IWXAPI;
import com.tencent.mm.opensdk.openapi.IWXAPIEventHandler;
import com.tencent.mm.opensdk.openapi.WXAPIFactory;

public class WeChatSignInHelper {

    private static IWXAPI sIWXAPI;
    public static IWXAPI getWeChatAPI(Context context) {
        if (sIWXAPI == null) {
            final String appId = context.getString(R.string.wechat_sign_in_app_id);
            sIWXAPI = WXAPIFactory.createWXAPI(context.getApplicationContext(), appId, true);
            sIWXAPI.registerApp(appId);
        }
        return sIWXAPI;
    }

    public static void signIn(Context context) {
        final SendAuth.Req req = new SendAuth.Req();
        req.scope = "snsapi_userinfo";
        req.state = "wechat_sdk_demo";
        getWeChatAPI(context).sendReq(req);
    }

    public static void handleIntent(Context context, Intent intent, IWXAPIEventHandler handler) {
        getWeChatAPI(context).handleIntent(intent, handler);
    }
}
