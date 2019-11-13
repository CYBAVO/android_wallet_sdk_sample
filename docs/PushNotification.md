# Setup push notification
## Requirements
- Android wallet sdk `com.cybavo.wallet:wallet-sdk-lib:1.2.1403`
## Installation and configuration
1. Install and configure AWS Amplify push notification, please refer to [this](https://aws-amplify.github.io/docs/js/push-notifications).
2. Setup AWS Mobile Hub.
    1. Create an app in [AWS Mobile Hub](https://console.aws.amazon.com/mobilehub/home#/). 
    2. Click on the app in AWS Mobile Hub. You can get app id from URL `https://console.aws.amazon.com/mobilehub/home#/xxxx_appid_xxxx/build`

    3. Create an IAM user with following policy applied. (Replace *xxxx_appid_xxxx* with your app id)
    ```
    {
        "Version": "2012-10-17",
        "Statement": [
            {
                "Sid": "VisualEditor0",
                "Effect": "Allow",
                "Action": "mobiletargeting:SendMessages",
                "Resource": [
                    "arn:aws:mobiletargeting:*:*:apps/xxxx_appid_xxxx/campaigns/*",
                    "arn:aws:mobiletargeting:*:*:apps/xxxx_appid_xxxx/segments/*",
                    "arn:aws:mobiletargeting:*:*:apps/xxxx_appid_xxxx/messages"
                ]
            }
        ]
    }
    ```
    4. Go to `https://console.aws.amazon.com/pinpoint/home#/apps/xxxx_appid_xxxx/settings/push/edit` to config your creditial for android/ios
    ![image](images/aws_pinpoint_push.png)

    5. Follow steps in [Setting Up Push Notifications for Amazon Pinpoint](https://docs.aws.amazon.com/pinpoint/latest/developerguide/mobile-push.html) to setup your pinpoint projects

    6. Generates access key of IAM user and setup in **CYBAVO VAULT** -> **System Settings** -> **App Settings** -> 
    ![image](images/pinpoint_settings.png)

    7. Ready to test!
## Working with the API
1. Please remember to call `Auth.getInstance().setPushDeviceToken(token)`after signin. Otherwise the device won't be able to receive notification successfully. 
    ```java
    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        
        // Make sure call setPushDeviceToken after signin. 
        Log.d(TAG, "FCM onNewToken: " + token);
        Auth.getInstance().setPushDeviceToken(token, new Callback<SetPushDeviceTokenResult>() {
            @Override public void onResult(SetPushDeviceTokenResult result) {}
            @Override public void onError(Throwable error) {
                Log.e(TAG, "setPushDeviceToken failed", error);
            }
        });
    }
    ```
3. Receive the notification and we provided `PushNotification.parse(json)` to help parsing the json string.
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
4. The properties of `PushNotification.parse(json)` result are listed below

    Property    | Description  | Type  
    :------------|:------------|:-------
    walletID    | Wallet ID    |  long 
    currency    | Currency     |  int 
    tokenAddress  | Token address | String
    direction  | Transaction direction | Wallets.Transaction.Direction<br>(IN:0, OUT:1)
    amount  | Transaction amount | String
    fee  | Transaction fee | String
    fromAddress  | Transaction from address | String
    toAddress  | Transaction to address | String
    timestamp  | Transaction timestamp | long
    txid  | Transaction TXID | String
    description  | Transaction description | String
