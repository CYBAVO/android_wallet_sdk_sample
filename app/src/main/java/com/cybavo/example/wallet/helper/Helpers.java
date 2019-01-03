package com.cybavo.example.wallet.helper;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.util.Log;
import android.widget.Toast;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.common.BitMatrix;

public class Helpers {

    private static final String TAG = Helpers.class.getSimpleName();

    private final static String PIN_CODE_PATTERN = "\\d{8}";
    public static boolean isPinCodeValid(String pinCode) {
        return pinCode.matches(PIN_CODE_PATTERN);
    }

    public static Bitmap createQrCodeBitmap(String content, int size) {
        try {
            final BitMatrix matrix = new MultiFormatWriter()
                    .encode(content, BarcodeFormat.QR_CODE, size, size);
            final int w = matrix.getWidth();
            final int h = matrix.getHeight();
            final int[] pixels = new int[w * h];
            for (int i = 0; i < w; i++) {
                for (int j = 0; j < h; j++) {
                    pixels[i + j * w] = matrix.get(i, j) ? Color.BLACK : Color.WHITE;
                }
            }

            final Bitmap bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.RGB_565);
            bitmap.setPixels(pixels, 0, w, 0, 0, w, h);
            return bitmap;
        } catch (Exception e) {
            Log.e(TAG, "Generate QR code failed", e);
            return null;
        }
    }


    public static void showToast(Context context, String message) {
        if (context != null) {
            Toast.makeText(context, message, Toast.LENGTH_LONG).show();
        }
    }
}
