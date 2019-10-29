package com.cybavo.example.wallet.notification;

import android.app.PendingIntent;
import android.content.Intent;
import android.util.Log;

import com.cybavo.example.wallet.MainActivity;
import com.cybavo.example.wallet.R;
import com.cybavo.wallet.service.api.Callback;
import com.cybavo.wallet.service.auth.Auth;
import com.cybavo.wallet.service.auth.results.SetPushDeviceTokenResult;
import com.cybavo.wallet.service.notification.PushNotification;
import com.cybavo.wallet.service.notification.TransactionNotification;
import com.cybavo.wallet.service.wallet.Transaction;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Map;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

public class PushNotificationService extends FirebaseMessagingService {

    private final static String TAG = "PushNotificationService";
    private final static int NOTIFICATION_ID = 99;
    private final static String NOTIFICATION_CHANNEL = "WalletService";

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {

        // Not getting messages here? See why this may be: https://goo.gl/39bRNJ
        Log.d(TAG, "From: " + remoteMessage.getFrom());

        // Check if message contains a data payload.
        if (remoteMessage.getData().size() > 0) {
            Map<String, String> data = remoteMessage.getData();
            final String body = data.get("pinpoint.jsonBody");
            if (body == null) {
                Log.d(TAG, "Empty body: " + data);
                return;
            }
            PushNotification notification = PushNotification.parse(body);
            switch (notification.type) {
                case PushNotification.Type.TRANSACTION:
                    TransactionNotification txNotification = (TransactionNotification) notification;
                    onTransaction(txNotification);
                    break;
                default:
                    Log.w(TAG, "Unknown notification: " + body);
            }
        }

    }

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);

        Log.d(TAG, "FCM onNewToken: " + token);
        Auth.getInstance().setPushDeviceToken(token, new Callback<SetPushDeviceTokenResult>() {
            @Override public void onResult(SetPushDeviceTokenResult result) {}
            @Override public void onError(Throwable error) {
                Log.e(TAG, "setPushDeviceToken failed", error);
            }
        });
    }

    private void onTransaction(TransactionNotification notification) {
        Intent intent = new Intent(this, MainActivity.class).
                setAction(Intent.ACTION_MAIN).
                addCategory(Intent.CATEGORY_LAUNCHER).
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);

        boolean in = notification.direction == Transaction.Direction.IN;
        NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext(), NOTIFICATION_CHANNEL).
                setContentIntent(pendingIntent).
                setSmallIcon(R.drawable.ic_launcher).
                setPriority(NotificationCompat.PRIORITY_DEFAULT);
        if (in) {
            builder = builder.setContentTitle("Transaction Received").
                    setContentText(String.format("Amount %s from %s", notification.amount, notification.fromAddress));
        } else {
            builder = builder.setContentTitle("Transaction Sent").
                    setContentText(String.format("Amount %s to %s", notification.amount, notification.toAddress));
        }
        NotificationManagerCompat.from(getApplicationContext()).notify(NOTIFICATION_ID, builder.build());
    }
}
