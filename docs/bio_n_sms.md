# Biometrics & SMS Verification

- Bookmarks
  - [UserState](#userstate)
  - [Setup](#biometrics--sms-verification-setup)
  - [Functions](#biometrics--sms-verification-for-transaction-and-sign-operation)

## UserState

- Biometrics verification is controlled in the Security Enhancement section on the admin panel.  

  <img src="images/sdk_guideline/screenshot_security_enhancement.png" alt="drawing" width="400"/>

- Although biometrics verification is applied globally, in User Management, the administrator still can set a single user to skip SMS / biometrics verification.

  <img src="images/sdk_guideline/screenshot_skip_sms_1.png" alt="drawing" width="400"/>

  <img src="images/sdk_guideline/screenshot_skip_sms_2.png" alt="drawing" width="800"/> 

    ```java
    public final class UserState {

        public boolean enableBiometrics; // Is enable biometric authentication

        public boolean skipSmsVerify; // Is skip SMS/Biometrics verify

        public boolean accountSkipSmsVerify; // Is skip SMS for specific case, ex. Apple account

        ...
    }
    ```

- `if (enableBiometrics && !skipSmsVerify)` ➜ need biometrics / SMS verification for transaction and sign operation

- `if (accountSkipSmsVerify == true)` ➜ cannot use SMS for verification, use biometrics verification instead.

    e.g. Only biometrics verification available for Apple Sign-In account.

## Biometrics / SMS Verification Setup

![img](images/sdk_guideline/biometric_verification.jpg)

- Must be set up before performing APIs which are mentioned in the next section.
- Steps:
    1. Check if the user needs biometrics / SMS verification
    2. Call `updateDeviceInfo`, pass nil Wallet SDK will decide the value for you.
        - Pass `BiometricsType.NONE` means you are registering for SMS verification
    3. Call `getBiometricsType` ➜ supported biometric type
    4. `if (BiometryType != BiometricsType.NONE)` ➜ call `registerPubkey`
    5. `if (BiometryType == BiometricsType.NONE)` && `accountSkipSmsVerify` ➜ prompt error. ex. The device not supporting biometrics, please contact the system admin.

```java
/// Get device's biometrics type
/// - Returns: BiometryType { NONE / FACE / FINGER }
public abstract BiometricsType getBiometricsType(Context context);

/// Update device's biometrics type, detect type by sdk
/// - Parameters:
///   - type: BiometricsType's raw value, pass nil WalletSDK will decide the value for you
///   - callback: asynchronous callback
public abstract void updateDeviceInfo(Context context, Callback<UpdateDeviceInfoResult> callback);

/// register public key for biometrics authentication
public abstract void registerPubkey(Callback<RegisterPubkeyResult> callback);
```

## Biometrics / SMS Verification for transaction and sign operation

- There are two versions (biometrics and SMS) for following transaction  / sign APIs:
  - createTransaction
  - requestSecureToken
  - signRawTx
  - increaseTransactionFee
  - callAbiFunction
  - cancelTransaction
  - callAbiFunctionTransaction
  - signMessage
  - walletConnectSignTypedData
  - walletConnectSignTransaction
  - walletConnectSignMessage
  - cancelWalletConnectTransaction

- SMS version has the suffix 'Sms', ex. createTransactionSms
- Biometrics version has the suffix 'Bio', ex. createTransactionBio

### SMS version

- call `getTransactionSmsCode` to send a SMS to the user

    ```java
    /// get SMS code for transaction
    /// - Parameters:
    ///   - duration: SMS expire duration (second), ex. 60
    ///   - callback: asynchronous callback
    public abstract void getTransactionSmsCode(long duration, Callback<GetActionTokenResult> callback);
    ```

- `actionToken` + `OTP code` + `PinSecret / PinCode` ➜ call SMS version function

### Biometrics version

- `promptMessage` + `PinSecret / PinCode` ➜ call biometrics version function
