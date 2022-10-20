# CYBAVO Wallet APP SDK (for Andorid) - Guideline

> Welcome to CYBAVO Wallet APP SDK (for Android) - Guideline

The **CYBAVO Wallet APP SDK** provides a thorough solution for building Institutional-grade security wallets.  
It provides both high-level and low-level APIs for nearly all **CYBAVO Wallet APP** features, backed by **CYBAVO** private key protection technology.

- Category
  - [SDK Guideline](#sdk-guideline)
  - [Auth](#auth)
  - [PIN Code](#pin-code)
  - Wallets ➜ [wallets.md](wallets.md)
  - Transaction ➜ [transaction.md](transaction.md)
  - Security Enhancement ➜ [bio_n_sms.md](bio_n_sms.md)
  - [Push Notification](#push-notification)
  - [Others](#others)
  - Advanced
    - NFT ➜ [NFT.md](NFT.md)
    - WalletConnect ➜ [wallet_connect.md](wallet_connect.md)
    - CYBAVO Private Chain ➜ [private_chain.md](private_chain.md)
    - KYC with Sumsub ➜ [kyc_sumsub.md](kyc_sumsub.md)

## SDK Guideline

### Prerequisite

Please contact **CYBAVO** to get your `endPoint` and `apiCode`.

### Installation

- Add the CYBAVO maven repository to the repositories section in your project level `build.gradle` file: 
    ```gradle
    repositories {
        maven {
            Properties properties = new Properties()
            properties.load(project.rootProject.file('local.properties').newDataInputStream()) // load local.properties

            url properties.getProperty('walletsdk.maven.url')
            credentials {
                username = properties.getProperty('walletsdk.maven.username')
                password = properties.getProperty('walletsdk.maven.password')
            }
        }
    }
    ```
- Add `local.properties` to config Maven repository URL / credentials provided by CYBAVO
   ```properties
   walletsdk.maven.url=$MAVEN_REPO_URL
   walletsdk.maven.username=$MAVEN_REPO_USRENAME
   walletsdk.maven.password=$MAVEN_REPO_PASSWORD
   ```
- Add the following dependencies to your app level `build.gradle` file:
   ```gradle
   dependencies {
      implementation 'com.cybavo.wallet:wallet-sdk-lib:1.2.+'
  }
   ```


### Initialization

- Initialize Wallet SDK before using it.

  ```java
  WalletSdk.init(getApplicationContext(),  new WalletSdk.Configuration(endpoint, apiCode));
  ```

- See this : [Sandbox Environment](#sandbox-environment)

### APP Flowchart

![ref](images/sdk_guideline/app_flowchart.jpg)

### First-time Login Tasks

![ref](images/sdk_guideline/first_time_login.jpg)

[↑ go to the top ↑](#cybavo-wallet-app-sdk-for-andorid---guideline)

---

# Auth

## Sign-in / Sign-up Flowchart

![ref](images/sdk_guideline/signin_signup.jpg)

## Third-Party Login

  Supported services : Google / Facebook / LINE / Twitter / WeChat

## Sign-in Flow

- 3rd party login ➡️ `Auth.getInstance().signIn` ➡️ get success ➡️ wait for `onSignInStateChanged` update
  
- 3rd party login ➡️ `Auth.getInstance().signIn` ➡️ get `Error.Code.ErrRegistrationRequired` ➡️ Sign-up flow

```java
/// Sign in with Wallet SDK Auth 
///
/// - Parameters:
///   - token: Token String from different 3rd party SDK
///     1. Google - googleSignInAccount.getIdToken()
///     2. Facebook - loginResult.getAccessToken().getToken()
///     3. LINE - lineLoginResult.getLineCredential().getAccessToken().getTokenString()
///     4. Twitter - identity token
///     5. WeChat - identity token
///
///   - identityProvider: String of provider
///     1. Google - "Google"
///     2. Facebook - "Facebook"
///     3. LINE - "LINE"
///     4. Twitter - "Twitter"
///     5. WeChat - "WeChat"
///
///   - extraAttributes: Extra attributes for specific provider, pass null if unspecified.
///     1. id_token_secret (String) - Secret for Twitter
///
///   - callback: Callback<SignInResult>
///     onResult: ➡️ Ready to getUserState()
///     onError: if (error.getCode() == Error.Code.ErrRegistrationRequired) ➡️ go to the Sign-up flow
///
public abstract void signIn(String token, String identityProvider, Map<String, String> extraAttributes, Callback<SignInResult> callback);
```

## Sign-up Flow

- `Auth.getInstance().signUp` ➡️ get success ➡️ `Auth.getInstance().signIn`

```java
/// sign-up with Wallet SDK Auth
/// - Parameters:
///   - token: Refer to signIn()
///   - identityProvider: Refer to signIn()
///   - extraAttributes: Refer to signIn()
///
///   - callback: Callback<SignUpResult>
///   onResult: ➡️ Ready to signIn()
///   onError: Handle ApiError
///
public abstract void signUp(String token, String identityProvider, Map<String, String> extraAttributes, Callback<SignUpResult> callback);
```

## Sign-out

```java
public abstract void signOut();
```

## Model : SignInState

```java
public enum SignInState {

    SIGNED_IN, // User signed in

    SIGNED_OUT, // User signed out

    ...

    NEED_VERIFY_OTP, // User has signed in but need verify otp(sms code)

    NEED_REGISTER_PHONE // User has signed in but need register phone
}
```

- Listen `SignInState`

  1. Implement `SignInStateListener` to handle `onUserStateChanged()` callback. 
  2. Add the listener through `addSignInStateListener()`. 
  3. Remove the listener through `removeSignInStateListener()` if you don’t need monitor anymore.

  ```java
  public abstract void addSignInStateListener(SignInStateListener listener);

  public abstract void removeSignInStateListener(SignInStateListener listener);

  public interface SignInStateListener {
    void onSignInStateChanged(SignInState state);
  }
  ```
- For Security Enhancement in the [flowchart](#sign-in--sign-up-flowchart), `NEED_VERIFY_OTP` and `NEED_REGISTER_PHONE` SignInState, please see [Security Enhancement](bio_n_sms.md)


- Call `getSignInState()` whenever you need current `SignInState`.

  ```java
  public abstract SignInState getSignInState();
  ```

## Model : UserState

```java
public final class UserState {

    public String realName; /* Real name of user */

    public String email; /* Email of user */

    public boolean setPin; /* User has finished PIN setup */

    public boolean setSecurityQuestions; /* User has setup BackupChallenges */

    ...
}
```

- Once you signed in, you should get the current `UserState` to check the variable `setPin`.

  `if (setPin == false)` ➡️ go to **_Setup PIN Code_** in the next section

- Call `getUserState` to get the current `UserState`.

  ```java
  public abstract void getUserState(Callback<GetUserStateResult> callback);
  ```

## Account deletion
For account deletion, Wallet SDK provides `revokeUser()` API and the detailed flow is described as below.
1. Check `UserState.setPin` 

    - If it's true, ask user to input PIN and call `revokeUser(pinSecret, callback)`.
    - If it's false, just call `revokeUser(callback)`.

2. (Suggest) Lead user back to sign in page without calling `signOut()` and sign out 3rd party SSO.  
⚠️ After `revokeUser()`, `signOut()` will trigger `onSignInStateChanged` with state `SESSION_EXPIRED`.  

3. On the admin panel, the user will be mark as disabled with extra info: unregistered by user, then the administrator can remove PII (real name, email and phone) of the user.  

4. This account still can be enabled by administrator if needed. Before being enabled, if the user trying to sign in with revoked account, `signIn()` API will return `ErrUserRevoked` error. 

[↑ go to the top ↑](#cybavo-wallet-app-sdk-for-andorid---guideline)

---

# PIN Code

PIN code is one of the most important components for user security.  
Ensure your users setPin right after sign-in success.

## NumericPinCodeInputView

- Use `NumericPinCodeInputView` to input PIN code, see [this](NumericPinCodeInputView.md)
- Feel free to customize your own input view.

## Setup PIN Code / Change PIN Code

- Setup PIN code is mandatory for further API calls. Make sure your user setup PIN code successfully before creating wallets.

``` java
/// setup PIN code
/// - Parameters:
///   - pinSecret: PIN secret retrieved via PinCodeInputView
///   - callback: asynchronous callback
public abstract void setupPinCode(PinSecret pinSecret,
                                      Callback<SetupPinCodeResult> callback);

public abstract void changePinCode(PinSecret newPinSecret,
                                       PinSecret currentPinSecret,
                                       Callback<ChangePinCodeResult> callback);
```

## Reset PIN code - with Security Question
- There are 2 ways to reset PIN code, one is by answering security questions

  1. Before that, the user has to set the answers of security questions.  
  ⚠️ Please note that the account must have at least a wallet, otherwise, the API will return `ErrNoWalletToBackup` error.
  ```java
  public abstract void setupBackupChallenge(PinSecret pinSecret,
                                                BackupChallenge challenge1, BackupChallenge challenge2, BackupChallenge challenge3,
                                                Callback<SetupBackupChallengeResult> callback);
  ```
  2. Get the security question for user to answer
  ```java
  public abstract void getRestoreQuestions(Callback<GetRestoreQuestionsResult> callback);
  ```
  3. Verify user input answer (just check if the answers are correct)
  ```java
  public abstract void verifyRestoreQuestions(BackupChallenge challenge1, BackupChallenge challenge2, BackupChallenge challenge3, Callback<VerifyRestoreQuestionsResult> callback);
  ```
  4. Reset PIN code by security questions and answers
  ```java
  public abstract void restorePinCode(PinSecret newPinSecret,
                                          BackupChallenge challenge1, BackupChallenge challenge2, BackupChallenge challenge3,
                                          Callback<RestorePinCodeResult> callback);
  ```

## Reset PIN code - with Admin System

- If the user forgot both the PIN code and the answers, there's another way to reset the PIN code.

  1. First, call API `forgotPinCode` to get the **_Handle Number_**.
  ```java
  public abstract void forgotPinCode(Callback<ForgotPinCodeResult> callback);
  ```

  2. Second, contact the system administrator and get an 8 digits **_Recovery Code_**.
  3. Verify the recovery code  (just check if the recovery code is correct)
  ```java
  public abstract void verifyRecoveryCode(String recoveryCode,
                                          Callback<VerifyRecoveryCodeResult> callback);
  ```
  4. Reset PIN code by the recovery code.

  ```java
  public abstract void recoverPinCode(PinSecret newPinSecret,
                                          String recoveryCode,
                                          Callback<RecoverPinCodeResult> callback);
  ```

## Notice

- Old version `String pinCode` is deprecated, use `PinSecret` instead.

  `PinSecret` advantages:
    1. Much more secure
    2. Compatible with NumericPinCodeInputView
    3. Certainly release the PIN code with API  

- `PinSecret` will be cleared after Wallet and Auth APIs are executed. If you intendly want to keep the `PinSecret`, call `PinSecret.retain()` everytime before APIs are called.

> **⚠️ WARNING** : When creating multiple wallets for the user. If you call APIs constantly.  
> You will receive the error `Error.Code.ErrInvalidPinSecret` caused by `PinSecret` being cleared.

[↑ go to the top ↑](#cybavo-wallet-app-sdk-for-android---guideline)

---

# Push Notification
> Wallet SDK support 2 ways to integrate Push Notification: Google Firebase and Amazon Pinpoint
## Amazon Pinpoint
- For admin panel configuration, please refre to "Amazon Pinpoint" section in CYBAVO Wallet SDK Admin Panel User Manual.
- See [PushNotification.md](PushNotification.md) for more details.
## Google Firebase
> After user signin, register your FCM instance ID via `setPushDeviceToken`, and update your device token via same API when onNewToken() callback invokes.
- For admin panel configuration, please refre to "Google Firebase" section in CYBAVO Wallet SDK Admin Panel User Manual.
- After signin, call `setPushDeviceToken`
  ```java
  FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(new OnCompleteListener<String>() {
                    @Override
                    public void onComplete(@NonNull Task<String> task) {
                        if (!task.isSuccessful()) {
                            Log.w(TAG, "Fetching FCM registration token failed", task.getException());
                            return;
                        }

                        String token = task.getResult();
                        Auth.getInstance().setPushDeviceToken(token, new Callback<SetPushDeviceTokenResult>() {
                            @Override public void onResult(SetPushDeviceTokenResult result) {}
                            @Override public void onError(Throwable error) {}
                        });
                    }
                });
  ```
- Call `setPushDeviceToken` while refresh token
  ```java
  // extends FirebaseMessagingService
  @Override
    public void onNewToken(String token) {
        Log.d(TAG, "Refreshed token: " + token);
        if(WalletSdk.getContext() == null){
            return;
        }
        Auth.getInstance().setPushDeviceToken(token, new Callback<SetPushDeviceTokenResult>() {
            @Override public void onResult(SetPushDeviceTokenResult result) {}
            @Override public void onError(Throwable error) {}
        });
    }
  ```
- Receive and handle the notification 
  ```java
  // extends FirebaseMessagingService
  @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        Map<String, String> data = remoteMessage.getData();
        String type = data.get("type");
        ...
    }
  ```

## Notification Types

There are 2 types of push notification: Transacion and Announcement.

- Transaction
  
  ```javascript
    {
        "currency": "194",
        "token_address": "",
        "timestamp": "1590376175",
        "fee": "",
        "from_address": "eeeeeeeee111",
        "amount": "0.0010",
        "wallet_id": "2795810471",
        "abi_method": "",
        "to_address": "eeeeeeeee111",
        "type": "1", // 1 means type Transaction
        "txid": "c90e839583f0fda14a1e055065f130883e5d2c597907de223f355b115b410da4",
        "out": "true", // true is Withdraw, false is Deposit
        "description": "d", 
        "abi_arguments": ""
    }
    ```

  - The keys of Transaction `remoteMessage` are listed below
    Key    | Description  | Type  
      :------------|:------------|:-------
      type    | notification type    |  String 
      wallet_id    | Wallet ID    |  String 
      currency    | Currency     |  String 
      token_address  | Token address | String
      out  | Transaction direction<br>("true": out, "false": in)| String
      amount  | Transaction amount | String
      fee  | Transaction fee | String
      from_address  | Transaction from address | String
      to_address  | Transaction to address | String
      timestamp  | Transaction timestamp | String
      txid  | Transaction TXID | String
      description  | Transaction description | String

  - Notification display example:

    - Withdraw (currencySymbol was from API getWallets)

      ```
      Transaction Sent: Amount {{amount}} {{currencySymbol}} to {{fromAddress}}
      ```

    - Deposit (NFT wallet, i.e. wallet mapping to a Currency which tokenVersion is 721 or 1155)
    
      ```
      Transaction Received: Token {{amount}}({{currencySymbol}}) received from {{fromAddress}}
      ```

- Announcement

  ```javascript
  {
      "body": "All CYBAVO Wallet users will be charged 0.1% platform fee for BTC transaction during withdraw since 2021/9/10",
      "sound": "default",
      "title": "Important information",
      "category": "myCategory"
  }
  ```

[↑ go to the top ↑](#cybavo-wallet-app-sdk-for-andorid---guideline)

---

# Others

## Error Handling - com.cybavo.wallet.service.api.Error

> **⚠️ WARNING** : Please properly handle the Error we provided in the API response.

```java
public abstract class Error extends Throwable {

    public static final class Code { … } // some error codes we defined
    
    abstract public int getCode();

    public String getMessage()
    ...
}
```
- Usage
```java
Auth.getInstance().signIn(token, identityProvider, new Callback<SignInResult>() {
            @Override
            public void onError(Throwable error) {
                if (error instanceof Error && ((Error)error).getCode() == Error.Code.ErrRegistrationRequired) { 
                    // registration required
                    registerWithToken(token, identityProvider, identity);
                } else { // sign in failed
                    onSignInFailed(error);
                }
            }
        });
```
### Error Code
The error codes are defined in class `com.cybavo.wallet.service.api.Error.Code` as constant int fields.

| Constant Field  | Value | Description |
| ----  | ----  | ---- |
|	ErrUserCancel	|	-7	| 	User cancel operation	| 
|	ErrBiometricUnsupported	|	-6	| 	Biometric Unsupported	| 
|	ErrInvalidPinSecret	|	-5	| 	PIN secret not valid	| 
|	ErrConcurrentExecution	|	-4	| 	Concurrent execution	| 
|	ErrInvalidPinCode	|	-3	| 	Invalid PIN code	| 
|	ErrNotSignedIn	|	-2	| 	Not signed in	| 
|	ErrUnknown	|	-1	| 	Unknown error	| 
|	ErrDisableAdmin	|	101	| 	Admin cannot disable	| 
|	ErrUserOrPasswordIncorrect	|	102	| 	The Email address or password provided is not correct	| 
|	ErrUserExist	|	103	| 	Account already exists	| 
|	ErrUserDisabled	|	104	| 	Account was banned by admin	| 
|	ErrUserEmailEmpty	|	105	| 	Email cannot be empty	| 
|	ErrUserEmailExist	|	106	| 	Email has been used	| 
|	ErrUserEmailFormatError	|	107	| 	Email format is incorrect	| 
|	ErrUserPasswordFormatError	|	108	| 	Password should be 8-18 chars	| 
|	ErrUserAccountFormatError	|	109	| 	Account can only be english char and 3-50 chars	| 
|	ErrUserRoleError	|	110	| 	Permission is incorrect	| 
|	ErrorUserPasswordError	|	111	| 	Old password is incorrect	| 
|	ErrInvalidParameter	|	112	| 	Invalid parameter	| 
|	ErrPermissionDenied	|	113	| 	Permission denied	| 
|	ErrNotAuthByPIN	|	114	| 	Need user setup PIN before do this	| 
|	ErrNotAuthByOTP	|	115	| 	Need user bind OTP before do this	| 
|	ErrUserInvalidPasswordToken	|	116	| 	Token is Invalid	| 
|	ErrUserReject	|	117	| 	Reject by User	| 
|	ErrNotImplemented	|	118	| 	Not Implemented	| 
|	ErrEncryptFailed	|	119	| 	Error Encrypt failed	| 
|	ErrUserAuthMethodInvalid	|	120	| 	Not support this kind of authorization	| 
|	ErrUserUnactivated	|	125	| 	Unactivated user	| 
|	ErrUserInvalid	|	126	| 	invalid user	| 
|	ErrUserAlreadyActivated	|	127	| 	User already activated	| 
|	ErrUserRequireOTPSetup	|	128	| 	Need to setup OTP for auth before operation this action	| 
|	ErrOnlyAdminCanBeAdded	|	129	| 	Only admin can be added from web	| 
|	ErrUserGetInfoFailed	|	130	| 	Get user info failed	| 
|	ErrUserNoPIN	|	131	| 	User does not set pin yet	| 
|	ErrAddUserDuplicate	|	132	| 	User already exist	| 
|	ErrAddUserFailed	|	133	| 	Add User failed	| 
|	ErrUserUnauthorized	|	134	| 	User unauthorized	| 
|	ErrUserIncorrectPIN	|	135	| 	Incorrect user PIN	| 
|	ErrClientVersion	|	136	| 	Unknown client version	| 
|	ErrAppMustUpdate	|	137	| 	Please update you CYBAVO Vault to access to the latest features!	| 
|	ErrInvalidDeviceToken	|	138	| 	Invalid device token	| 
|	ErrUserIncorrectPINMaxRetry	|	139	| 	Incorrect user PIN. Retry limit exceeded	| 
|	ErrShutdownInProgress	|	140	| 	Shutdown in progress	| 
|	ErrUserDeactivated	|	141	| 	User has been deactivated	| 
|	ErrUserNameEmpty	|	142	| 	User name is empty	| 
|	ErrMappedWallet	|	143	| 	Create mapped wallet fail	| 
|	ErrNotTokenWallet	|	144	| 	Only Token can create mapped wallet	| 
|	ErrNotCreateMultiMappedToSameAddr	|	145	| 	Cannot create 2 mapped wallet to 1 wallet address	| 
|	ErrMappedWalletOnly	|	146	| 	Only Mapped Wallet can use Mapped ID	| 
|	ErrConnectCCServer	|	147	| 	Failed to connect CC server	| 
|	ErrUserIsLocked	|	148	| 	User is locked	| 
|	ErrUnlockCodeExpired	|	149	| 	Unlock link expired	| 
|	ErrOperationTooFrequent	|	150	| 	Operation too frequent	| 
|	ErrUpdateKeyInProgress	|	151	| 	Update key in progress	| 
|	ErrInvalidQRCode	|	152	| 	Invalid QR Code	| 
|	ErrForgotPINNotReady	|	153	| 	Not able to restore PIN yet. Please contact admin to initial this process	| 
|	ErrInvalidRestoreCode	|	154	| 	Invalid restore code	| 
|	ErrNoRecoveryCode	|	155	| 	No wallet recovery data for user	| 
|	ErrRegisterFail	|	156	| 	Register fail	| 
|	ErrRegistrationRequired	|	157	| 	Registration required	| 
|	ErrDisableSelf	|	158	| 	Unable to disable yourself	| 
|	ErrForgotPINInvalid	|	159	| 	User did not request to recovery PIN	| 
|	ErrForgotPINExpired	|	160	| 	Forgot PIN expired. Please ask user to submit again	| 
|	ErrUnableChangeRole	|	161	| 	Unable to change role	| 
|	ErrForgotPINHandled	|	162	| 	Already handle by other admin	| 
|	ErrForgotPINNotApprove	|	163	| 	Not approve by admin	| 
|	ErrAdminInfoNotFound	|	164	| 	Admin info not found	| 
|	ErrInvalidAdminPerm	|	165	| 	User permission was changed by others	| 
|	ErrUserIncorrectPINLock	|	166	| 	The account is temporarily blocked because you entered the wrong PIN too many times	| 
|	ErrUserIsUnlocked	|	167	| 	User was unlocked	| 
|	ErrUserIncorrectSQLock	|	168	| 	The account is temporarily blocked because you entered the wrong answer too many times	| 
|	ErrUserPINAlreadySet	|	169	| 	User already set PIN	| 
|	ErrUserSecureTokenNotReady	|	170	| 	Secure token not ready	| 
|	ErrUserSecurityQuestionsNotReady	|	171	| 	User has not setup security questions yet	| 
|	ErrInvalidUnlockToken	|	172	| 	Invalid Unlock token	| 
|	ErrInvalidHashLength	|	173	| 	Hash is required to be exactly 32 bytes	| 
|	ErrInvalidAbiFunction	|	174	| 	Invalid ABI function	| 
|	ErrOperationTooFrequentShortly	|	175	| 	Frequent operation, please try again after 1 sec	| 
|	ErrUserPhoneNumUnverified	|	180	| 	User phone number not verified, need register phone number	| 
|	ErrActionTokenInvalid	|	181	| 	Action token invalid	| 
|	ErrOTPCodeInvalid	|	182	| 	OTP code(SMS code) invalid	| 
|	ErrRequireTooFrequent	|	183	| 	Require too frequent	| 
|	ErrInvalidSignature	|	184	| 	Invalid Signature	| 
|	ErrBiometricsNotFound	|	185	| 	Biometrics setting not found, need updateDeviceInfo	| 
|	ErrDeviceOtpUnverified	|	186	| 	Device otp(SMS code) unverified, need getLoginSms then verifyOtp	| 
|	ErrOverSMSLimit	|	187	| 	Exceed daily SMS limit	| 
|	ErrUserSkipSMSVerify	|	188	| 	Skip user SMS/Biometrics verify	| 
|	ErrUserSMSVerifed	|	189	| 	User phone has been verified	| 
|	ErrUserReferralAlreadySet	|	190	| 	User referral code has been set	| 
|	ErrUserReferralNotSetSelf	|	191	| 	User referral code can not be yourself	| 
|	ErrReferralUserIdGreaterThenSelf	|	192	| 	Referral user should register earlier then yourself	| 
|	ErrDepartmentInvalid	|	193	| 	Invalid department	| 
|	ErrDepartmentTransactionNotFound	|	194	| 	Cannot find any transaction in this department	| 
|	ErrKeywordForSearchTooShort	|	195	| 	The keyword for search user is too short	| 
|	ErrInputStringTooLong	|	196	| 	The input string is too long	| 
|	ErrUserRevoked	|	197	| 	Account is revoked	| 
|	ErrWalletCreateFailed	|	301	| 	Wallet Create Failed	| 
|	ErrWalletAddressInvalid	|	302	| 	Invalid Address, please make sure your address format is correct	| 
|	ErrWalletCurrencyInvalid	|	303	| 	Invalid currency	| 
|	ErrWalletIDInvalid	|	304	| 	Wallet ID invalid	| 
|	ErrWalletPolicyParseFailed	|	305	| 	Parse SetWalletPolicyRequest failed	| 
|	ErrWalletPolicySetFailed	|	306	| 	Set SetWalletPolicyRequest failed	| 
|	ErrWalletPolicyGetFailed	|	307	| 	Get SetWalletPolicyRequest failed	| 
|	ErrPolicySignFail	|	308	| 	Policy Sign fail	| 
|	ErrPolicySignInvalid	|	309	| 	Policy Sign invalid	| 
|	ErrPolicyState	|	310	| 	Policy state error	| 
|	ErrPolicyTransFail	|	311	| 	Policy transfer fail	| 
|	ErrPolicyNotFound	|	312	| 	Policy not found	| 
|	ErrPolicyNotPass	|	313	| 	Not pass policy enforcement	| 
|	ErrPolicyApprover	|	314	| 	Invalid policy approver	| 
|	ErrPolicyWalletNotFound	|	315	| 	Policy: wallet not found	| 
|	ErrPolicyCurrencyNotMatch	|	316	| 	Policy: currency not match	| 
|	ErrPolicyCurrencyNotSupport	|	317	| 	Policy: currency not support	| 
|	ErrPolicyNotWalletCreator	|	318	| 	Policy: not wallet creator	| 
|	ErrPolicyWalletAddressDiff	|	319	| 	Policy: wallet address is incorrect	| 
|	ErrPolicyWalletHeaderTrans	|	320	| 	Policy: the transaction count of wallet should be greater than zero	| 
|	ErrPolicyAmountTransferFail	|	321	| 	Policy: amount trans to value fail	| 
|	ErrPolicyWalletHeaderAmount	|	322	| 	Policy: the transaction amount of wallet should greater than zero	| 
|	ErrPolicyOutgoingAddressNull	|	323	| 	Policy: no outgoing address	| 
|	ErrPolicyOutgoingAddressQFail	|	324	| 	Policy: outgoing address incorrect	| 
|	ErrPolicyOutgoingAddressInconsistent	|	325	| 	Policy: outgoing address inconsistent	| 
|	ErrPolicyAuditorDuplicateOrder	|	326	| 	Policy: auditor duplicate order	| 
|	ErrPolicyAuditorDuplicateUser	|	327	| 	Policy: auditor duplicated user	| 
|	ErrPolicyApproverDuplicateOrder	|	328	| 	Policy: approver duplicate order	| 
|	ErrPolicyApproverDuplicateUser	|	329	| 	Policy: approver duplicate user	| 
|	ErrPolicyOperatorDuplicateOrder	|	330	| 	Policy: operator duplicate order	| 
|	ErrPolicyOperatorDuplicateUser	|	331	| 	Policy: operator duplicate user	| 
|	ErrPolicyOperatorAmountTransFail	|	332	| 	Policy: operator amount trans fail	| 
|	ErrPolicyOperatorAmountUnderZero	|	333	| 	Policy: the amount of operator should be greater than zero	| 
|	ErrPolicyOperatorTransUnderZero	|	334	| 	Policy: the transaction count of operator should be greater than zero	| 
|	ErrPolicyOperatorAmountOverMax	|	335	| 	Policy: the amount of wallet should be greater than the amount of operator	| 
|	ErrPolicyOperatorTransOverMax	|	336	| 	Policy: the transaction count of wallet should be greater than the transaction count of operator	| 
|	ErrPolicyApproverAmountTransFail	|	337	| 	Policy: approver amount trans fail	| 
|	ErrPolicyApproverAmountUnderZero	|	338	| 	Policy: the amount of approver should be greater than zero	| 
|	ErrPolicyMaxAppOverMaxOp	|	339	| 	Policy: the maximum operator amount should be greater than the minimum approver amount	| 
|	ErrPolicyNoOperator	|	340	| 	Policy: No operator	| 
|	ErrPolicyPINNotSet	|	341	| 	Policy: some user has not set pin	| 
|	ErrPolicyUserNotExist	|	342	| 	Policy: some user not exist	| 
|	ErrPolicyApproverNotCosigner	|	343	| 	Policy: approver is not cosigner	| 
|	ErrPolicyAllCosignersNotApprover	|	344	| 	Policy: cosigners not approvers	| 
|	ErrPolicyApproverConfigureIncorrectly	|	345	| 	Policy: approver configure incorrectly	| 
|	ErrPolicyLevelIncorrect	|	346	| 	Policy: level incorrect	| 
|	ErrPolicyOwnerIncorrect	|	347	| 	Policy: Owner count incorrect	| 
|	ErrWalletNotOperation	|	348	| 	Not wallet operator	| 
|	ErrScheduleWithdrawNotFound	|	349	| 	Invalid schedule withdraw	| 
|	ErrScheduleWithdrawNotCreator	|	350	| 	Permission denied. Only creator can delete schedule withdraw	| 
|	ErrNoWalletToBackup	|	351	| 	No wallet to backup	| 
|	ErrInvalidBackupAnswer	|	352	| 	Invalid backup answer	| 
|	ErrWalletMnemonicDuplicate	|	353	| 	Another activated user with same mnemonic	| 
|	ErrScheduleWithdrawExceedPolicyAmount	|	354	| 	Exceed Operator Allow Amount	| 
|	ErrScheduleWithdrawNotOperator	|	355	| 	Not Operator in WalletPolicy	| 
|	ErrScheduleWithdrawNotOutgoing	|	356	| 	Not Outgoing in WalletPolicy	| 
|	ErrScheduleWithdrawCheckBalanceFailed	|	357	| 	Check balance failed	| 
|	ErrScheduleWithdrawInvalid	|	358	| 	Schedule withdraw invalid	| 
|	ErrGetTransactionFeeError	|	359	| 	Fail to get transaction fee	| 
|	ErrNotPendingNow	|	360	| 	Policy changed, unlock again if needed	| 
|	ErrInvalidFeeLevel	|	361	| 	Invalid fee level	| 
|	ErrSignBatchTransactionFailed	|	362	| 	Fail to sign batch transaction	| 
|	ErrWalletKeyNotReady	|	363	| 	Wallet not ready to withdraw	| 
|	ErrNotBatchWallet	|	364	| 	Not batch wallet	| 
|	ErrNotBatchTransaction	|	365	| 	Not batch transaction	| 
|	ErrExceedMaxBatchAmount	|	366	| 	Exceed max batch amount	| 
|	ErrOngoingBatchTransaction	|	367	| 	Doing batch transaction	| 
|	ErrTxIDNotFound	|	368	| 	Transaction id not found	| 
|	ErrNotWalletOwner	|	369	| 	Not wallet owner	| 
|	ErrNotAdmin	|	370	| 	Not Admin	| 
|	ErrInvalidWalletId	|	371	| 	Invalid wallet	| 
|	ErrWalletAlreadyCreated	|	372	| 	Wallet already created	| 
|	ErrTransactionSameAddress	|	373	| 	Cannot send amount to yourself address	| 
|	ErrDestinationNotInOutgoingAddress	|	374	| 	Destination address must in outgoing address	| 
|	ErrApiSecretExpired	|	384	| 	API Secret expired	| 
|	ErrAPISecretNotValid	|	385	| 	API Secret not valid	| 
|	ErrExpiredTx	|	386	| 	Transaction is expired	| 
|	ErrCPUUsageExceeded	|	387	| 	CPU usage exceeded, please delegate CPU for usage	| 
|	ErrNetUsageExceeded	|	388	| 	NET usage exceeded, please delegate NET for usage	| 
|	ErrRAMUsageExceeded	|	389	| 	RAM usage exceeded, please purchase RAM for usage	| 
|	ErrorInsufficientStakedCPU	|	390	| 	Insufficient staked CPU bandwidth, please delegate CPU for usage	| 
|	ErrorInsufficientStakedNet	|	391	| 	Insufficient staked NET bandwidth, please delegate NET for usage	| 
|	ErrorInsufficientQuota	|	392	| 	Insufficient RAM quota, please purchase RAM for usage	| 
|	ErrTransactionNotReplaceable	|	393	| 	Transaction is not replaceable	| 
|	ErrBalanceUpperTransFail	|	401	| 	Balance: upper bound value transfer fail	| 
|	ErrBalanceLowerTransFail	|	402	| 	Balance: lower bound value transfer fail	| 
|	ErrBalanceLOverU	|	403	| 	Balance: lower bound >= upper bound	| 
|	ErrBalanceIntervalIncorrect	|	404	| 	Balance: Interval should between 1800 (30 minutes) ~ 86400 (1 day)	| 
|	ErrBalanceDBNotFound	|	405	| 	Balance: Not found item in DB	| 
|	ErrBalanceItemNotMatch	|	406	| 	Balance: Item not match	| 
|	ErrBalanceIdInvalid	|	407	| 	Invalid ID	| 
|	ErrNoContractCode	|	408	| 	No contract code at given address	| 
|	ErrInvalidLedgerConfig	|	432	| 	Invalid ledger server config	| 
|	ErrExpiredToken	|	500	| 	Expired Token	| 
|	ErrVerifyFail	|	501	| 	Verify fail, incorrect number	| 
|	ErrVerifyExceed	|	502	| 	Exceed maximum retry counts, please resend the verify number again	| 
|	ErrParameterNoToken	|	701	| 	No token present	| 
|	ErrParameterInvalidOperation	|	702	| 	Unknown operation	| 
|	ErrOperationFailed	|	703	| 	Operation failed	| 
|	ErrSKUInfoFailed	|	704	| 	Failed to get SKU info	| 
|	ErrSKUUserLimit	|	705	| 	Exceed max allow user limitation, Upgrade your SKU to get more users	| 
|	ErrSKUWalletLimit	|	706	| 	Exceed max allow wallet limitation, Upgrade your SKU to get more wallets	| 
|	ErrSKUOutgoingLimit	|	707	| 	Exceed max allow outgoing address limitation, Upgrade your SKU to get more outgoing address	| 
|	ErrTransactionInsufficientFund	|	801	| 	Insufficient fund	| 
|	ErrTransactionUTXOInsufficientFund	|	802	| 	UTXO Temporarily Not Available	| 
|	ErrUTXOTemporarilyNotAvailable	|	803	| 	Full nodes' syncing now, wait a few minutes to do the transaction	| 
|	ErrFullNodeSyncing	|	804	| 	Duplicate outgoing address	| 
|	ErrOutgoingAddressDuplicate	|	901	| 	Invalid outgoing address ID	| 
|	ErrOutgoingAddressIdInvalid	|	902	| 	Invalid outgoing address ID	| 
|	ErrKMSNotReady	|	903	| 	KMS out of service. Please try again later.	| 
|	ErrInvalidApiCode	|	904	| 	Invalid Api Code	| 
|	ErrDuplicateApp	|	905	| 	Duplicate entry	| 
|	ErrSDKOutdated	|	906	| 	WalletSDK is outdated. Please upgrade your SDK	| 
|	ErrorFeeLowerBound	|	907	| 	Inner Fee less than lower bound	| 
|	ErrorFeeUpperBound	|	908	| 	Inner Fee higher than upper bound	| 
|	ErrorInnerFeeAmount	|	909	| 	Inner fee configuration incorrect	| 
|	ErrorTransferAmountFail	|	910	| 	Inner fee transfer amount fail	| 
|	ErrFailToGetFee	|	911	| 	Inner fee fail to check fee	| 
|	ErrFeeTooHigh	|	912	| 	Inner fee higher than amount	| 
|	ErrParseTransactionFail	|	913	| 	Parse transaction fail	| 
|	ErrNotSupportInnerFee	|	914	| 	Batch transaction not support inner fee now, please contact CYBAVO for next step	| 
|	ErrorFeeOverUpper	|	915	| 	Transaction fee over upper bound	| 
|	ErrorInnerFeeAddress	|	916	| 	Inner Fee address incorrect	| 
|	ErrNoTRONForEnergy	|	917	| 	Need at least 1 TRX in wallet for energy usage.	| 
|	ErrorInsufficientBandWidth	|	918	| 	Insufficient bandwidth, need to delegate bandwidth or Tron	| 
|	ErrorDestNotExist	|	919	| 	Destination not create. Too little XRP(< 20) sent to create it	| 
|	ErrTransactionInsufficientBTCFee	|	920	| 	BTC is not enough to pay for transaction fee	| 
|	ErrTransactionInsufficientETHFee	|	921	| 	ETH is not enough to pay for transaction fee	| 
|	ErrTransactionInsufficientBTCUTXOFee	|	922	| 	BTC UTXOs is not enough to pay for transaction fee	| 
|	ErrDustFail	|	923	| 	Transfer amount is too small and considered as dust by blockchain	| 
|	ErrUTXOExceedMax	|	924	| 	The number of UTXO set exceed max allow number (2500)	| 
|	ErrUTXONotEnough	|	925	| 	BTC UTXOs are not enough to pay for platform fee	| 
|	ErrInsufficientFundBNB	|	926	| 	Insufficient fund, you must leave 0.000625 BNB for transaction fee	| 
|	ErrorTxNoResult	|	927	| 	No Result from BNB full node	| 
|	ErrorSendTxFail	|	928	| 	Send Tx to BNB full node fail	| 
|	ErrorIllegalFormat	|	929	| 	Illegal transaction format	| 
|	ErrorAccountNotCreate	|	930	| 	Account was not created, please send at least 1 TRX to this address for account creation	| 
|	ErrorIncorrectTag	|	931	| 	Destination Tag must be integer	| 
|	ErrInvalidEOSFormat	|	932	| 	Invalid EOS Account format	| 
|	ErrInvalidAppId	|	933	| 	Invalid App ID	| 
|	ErrInvalidPinpointAppId	|	934	| 	Invalid AWS Pinpoint App ID	| 
|	ErrEOSAccountExisted	|	935	| 	This EOS Account already existed	| 
|	ErrReferralCodeIncorrect	|	936	| 	User account not exist, referral code incorrect	| 
|	ErrGoogleAuthError	|	1001	| 	Google Auth erro	| 
|	ErrWechatAuthError	|	1002	| 	WeChat Auth error	| 
|	ErrFacebookAuthError	|	1003	| 	Facebook Auth error	| 
|	ErrLineAuthError	|	1004	| 	LINE Auth error	| 
|	ErrInvalidPINToken	|	10001	| 	PIN token expired, need to sign in again to recover	| 
|	ErrorRamOperationMinAmount	|	10301	| 	RAM operation bytes smaller than minimal amounts	| 
|	ErrOutgoingAddressNotMatch	|	10302	| 	Outgoing address is not matched with raw transaction	| 
|	ErrWalletInnerFeeIncorrect	|	10303	| 	Wallet inner fee calculate incorrectly	| 
|	ErrWalletCreateInnerTxFail	|	10304	| 	Create Tx with inner fee failed	| 
|	ErrNoTranslation	|	10936	| 	Translation not ready	| 
|	ErrTransactionInsufficientLTCFee	|	10937	| 	LTC is not enough to pay for transaction fee	| 
|	ErrTransactionInsufficientLTCUTXOFee	|	10938	| 	LTC UTXOs is not enough to pay for transaction fee	| 
|	ErrTransactionInsufficientBCHFee	|	10939	| 	BCH is not enough to pay for transaction fee	| 
|	ErrTransactionInsufficientBCHUTXOFee	|	10940	| 	BCH UTXOs is not enough to pay for transaction fee	| 
|	ErrOverPersonalCurrencyLimit	|	10941	| 	Over personal currency limit	| 
|	ErrKycNotCreated	|	10942	| 	Not createKyc before getApplicantStatus	| 
|	ErrKycSettingsNotFound	|	10943	| 	KYC setting not exist when getApplicantStatus	| 
|	ErrSmartChainInvalidMemoFormat	|	12001	| 	Invalid memo format, only allow numbers	| 
|	ErrSmartChainInvalidAmountFormat	|	12002	| 	Invalid amount format	| 
|	ErrSmartChainBalanceCheckFailed	|	12003	| 	Amount or balance format invalid	| 
|	ErrSmartChainDepositMinCheckFailed	|	12004	| 	Deposit amount smaller than the minimum limit	| 
|	ErrSmartChainDepositMaxCheckFailed	|	12005	| 	Deposit amount greater than the maximum limit	| 
|	ErrSmartChainProductIsOutOfStock	|	12006	| 	Smart chain product is out of stock	| 
|	ErrSmartChainOperationFailed	|	12007	| 	Smart chain operation failed	| 
|	ErrSmartchainBalanceSmallerThanMinWithdraw	|	12008	| 	Balance smaller than the minimum withdraw amount and the platform fee	| 
|	ErrSmartchainCallAmountExceedsBalance	|	12101	| 	Smart chain amount exceeds balance	| 
|	ErrSmartchainCallAmountExceedsAllowance	|	12102	| 	Smart chain amount exceeds allowance	| 
|	ErrSmartchainCallZeroAddr	|	12103	| 	Smart chain zero address	| 
|	ErrSmartchainCallDepositStop	|	12104	| 	Smart chain deposit should not stop	| 
|	ErrSmartchainCallWithdrawStop	|	12105	| 	Smart chain withdraw should not stop	| 
|	ErrSmartchainCallRateLimit	|	12106	| 	Smart chain rate limiting	| 
|	ErrSmartchainCallWithdrawLimit	|	12107	| 	Smart chain exceed withdraw limit	| 
|	ErrSmartchainCallMaxUserLimit	|	12108	| 	Smart chain exceed max user limit	| 
|	ErrSmartchainCallMaxUserOrderLimit	|	12109	| 	Smart chain exceed max user orders limit	| 
|	ErrSmartchainCallMaxDepositAmountLimit	|	12110	| 	Smart chain exceed max deposit amount limit	| 
|	ErrSmartchainCallAmountNotMatch	|	12111	| 	Smart chain amount not match	| 
|	ErrSmartchainCallOrderNotExist	|	12112	| 	Smart chain order not exist	| 
|	ErrSmartchainCallOrderExist	|	12113	| 	Smart chain order already exist	| 
|	ErrSmartchainCallNoProfitSharing	|	12114	| 	Smart chain no profit sharing	| 
|	ErrSmartchainCallAmountSmallThenZero	|	12115	| 	Smart chain amount should grater than 0	| 
|	ErrSmartchainCallNotAllowWithdraw	|	12116	| 	Smart chain payee is not allowed to withdraw	| 
|	ErrSmartchainCallNotAllowEarlyWithdraw	|	12117	| 	Smart chain should do withdraw directly	| 
|	ErrSmartchainCallTransferSmallThenZero	|	12118	| 	Smart chain transferAmount is smaller than zero	| 

## Sandbox Environment

- You will only get the `endPoint` & `apiCode` for testing in the beginning.
- We will provide the production `endPoint` & `apiCode` when you are ready.
Feel free to play around with the WalletSDK in the sandbox environment.

[↑ go to the top ↑](#cybavo-wallet-app-sdk-for-andorid---guideline)
