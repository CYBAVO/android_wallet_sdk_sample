# Setup push notification
## Requirements
- Android wallet sdk `com.cybavo.wallet:wallet-sdk-lib:1.2.1403`
## Installation and configuration
1. Install and configure AWS Amplify push notification, please refer to [this](https://aws-amplify.github.io/docs/js/push-notifications).
2. Setup AWS Mobile Hub. Please refer to [this](../docs/PushNotificationAws.md).
## Working with the API
1. After receive the push token, call `Auth.getInstance().setPushDeviceToken(token)`
    ```java
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
    ```
3. Receive the notification and utilize `PushNotification.parse(json)` to parse the json string.
    ```java
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
    ```
