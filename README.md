# CYBAVO Wallet APP SDK for Android - Sample

> Sample app for integrating Cybavo Wallet App SDK, <https://www.cybavo.com/wallet-app-sdk/>

## Institutional-grade security for your customers

Protect your customers' wallets with the same robust technology we use to protect the most important cryptocurrency exchanges. CYBAVO Wallet App SDK allows you to develop your own cryptocurrency wallet, backed by CYBAVO private key protection technology.

- Mobile SDK

    Use CYBAVO Wallet App SDK to easily develop secure wallets for your users without having to code any cryptography on your side. Our SDK allows you to perform the most common operations, such as creating a wallet, querying balances and executing cryptocurrency payments.

- Secure key management system

    Key management is the most critical part of cryptocurrency storage. CYBAVO Wallet App SDK makes our robust private key storage system available to all of your users. Our unique encryption scheme and a shared responsibility model offers top notch protection for your customer's keys.

- CYBAVO Security Cloud

    Cryptocurrency transactions performed by wallets developed with CYBAVO Wallet App SDK will be shielded by our Security Cloud, ensuring their integrity.

## Complete solution for cryptocurrency wallets

- Cost saving

    Leverage your in-house developing team and develop mobile cryptocurrency apps without compromising on security.

- Fast development

    Quickly and easily develop cryptocurrency applications using mobile native languages, without having to worry about cryptographic code.

- Full Node maintenance

    Leverage CYBAVO Wallet App SDK infrastructure and avoid maintaining a full node for your application.

---

# CYBAVO

A group of cybersecurity experts making crypto-currency wallets secure and usable for your daily business operation.

We provide VAULT, wallet, ledger service for cryptocurrency. Trusted by many exchanges and stable-coin ico teams, please feel free to contact us when your company or business needs any help in cryptocurrency operation.

# SDK Features

- Sign in / Sign up with 3rd-party account services
- Wallet Creation / Editing
- Wallet Deposit / Withdrawal
- Transaction History query
- PIN Code configuration: Setup / Change / Recovery
- Secure PIN code input view - NumericPinCodeInputView
- Push Notification - receive push notification of deposit / withdrawal
- Private chain, NFT and WalletConnect supported

# Run the demo app

1. Edit `local.properties` to config Maven repository URL / credentials provided by CYBAVO

   ```
   walletsdk.maven.url=$MAVEN_REPO_URL
   walletsdk.maven.username=$MAVEN_REPO_USRENAME
   walletsdk.maven.password=$MAVEN_REPO_PASSWORD
   ```

2. Edit `values/config.xml` ➜ `google_sign_in_web_cli_id` to your Google sign-in client ID.  
Please refer to "Google Login - Setup with Firebase" section in CYBAVO Wallet SDK Admin Panel User Manual.
3. Edit `values/config.xml` ➜ `wechat_sign_in_app_id` to your WeChat app id.  
Please refer to "Wechat Login Setup" section in CYBAVO Wallet SDK Admin Panel User Manual.
4. Edit `values/config.xml` ➜ `default_endpoint` to point to your Wallet Service endpoint.
    - Test environment:
        - On-Premises: set `default_endpoint` = https://mvault.sandbox.cybavo.com/v1/mw/
        - SaaS: set `default_endpoint` = https://mvault.sandbox.cybavo.com/v1/mw/
    - Production environment:
        - On-Premises: set `default_endpoint` = https://`<Your management portal URL>`/v1/mw/
        - SaaS: set `default_endpoint` = https://appvault.cybavo.com/v1/mw/
5. Register your app on CYBAVO WALLET MANAGEMENT system web > Administration > System settings, input `package name` and `Signature keystore SHA1 fingerprint`, follow the instruction to retrieve an `API Code`.  
Please refer to "Setup in Android" section in CYBAVO Wallet SDK Admin Panel User Manual.
6. Edit `values/config.xml` ➜ `default_api_code` to fill in your `API Code`
7. If you want to provide push notification features, setup project to integrate [Firebase Cloud Messaging](https://firebase.google.com/docs/cloud-messaging) (FCM) service, refer to [official document](https://firebase.google.com/docs/cloud-messaging/android/client) for details.  
Please refer to "Google Firebase" section in CYBAVO Wallet SDK Admin Panel User Manual.
9. Now you can run it on your device!

# More Details

see this : [**SDK Guideline**](docs/sdk_guideline.md)