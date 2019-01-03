package com.cybavo.example.wallet.detail;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;
import android.util.Pair;
import android.widget.Toast;

import com.cybavo.example.wallet.R;

import java.io.IOException;
import java.io.OutputStream;

class SaveImageTask extends AsyncTask<Pair<Bitmap, Uri>, Void, Boolean> {

    private static final String TAG = SaveImageTask.class.getSimpleName();

    @SuppressLint("StaticFieldLeak")
    private final Context mAppContext;
    SaveImageTask(Context context) {
        mAppContext = context.getApplicationContext();
    }

    @Override
    protected Boolean doInBackground(Pair<Bitmap, Uri>... params) {
        final Pair<Bitmap, Uri> param = params[0];
        OutputStream os = null;
        try {
            os = mAppContext.getContentResolver().openOutputStream(param.second);
            param.first.compress(Bitmap.CompressFormat.PNG, 100, os);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Save QR code failed", e);
            return false;
        } finally {
            if (os != null) {
                try { os.close(); } catch (IOException e) { e.printStackTrace(); }
            }
        }
    }

    @Override
    protected void onPostExecute(Boolean success) {
        super.onPostExecute(success);
        if (success) {
            Toast.makeText(mAppContext, R.string.message_qr_code_saved, Toast.LENGTH_SHORT).show();
        }
    }
}
