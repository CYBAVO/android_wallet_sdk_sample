# NumericPinCodeInputView

## NumericPinCodeInputView Introduction

1. Create a `NumericPinCodeInputView` simply
- In layout XML
    ```xml
    <com.cybavo.wallet.service.view.NumericPinCodeInputView
     xmlns:app="http://schemas.android.com/apk/res-auto"
     android:id="@+id/pinInputView"
     android:layout_width="wrap_content"
     android:layout_height="wrap_content"
     app:maxLength="6"/>
    ```
2. Set `OnPinInputListener` for `onChanged` callback
    ```java
    PinCodeInputView pinInputView = findViewById(R.id.pinInputView);
    pinInputView.setOnPinCodeInputListener(length -> {
         ...
     });
    ```

3. Get `PinSecret` by `NumericPinCodeInputView.submit()` and pass it to Wallet and Auth API
    ```java
    PinCodeInputView pinInputView = findViewById(R.id.pinInputView);
    pinInputView.setOnPinCodeInputListener(length -> {
         // update placeholders on UI... etc

         // pin code length fulfilled
         if (length == 6) {
             // submit user input PIN code to SDK core and get a PIN secret
             PinSecret pinSecret = pinInputView.submit();
             // pass PIN secret as pinCode parameter
             Wallets.getInstance().createTransaction(..., pinSecret, ...);
         }
     });
    ```

4. PinSecret will be clear after the Wallet and Auth API are executed.
        If you want to use the same `PinSecret` with multiple API calls,
        Please call `pinSecret.retain()` before calling the API.

    ```java
    PinSecret pinSecret = pinInputView.submit();
    pinSecret.retain();
    // retain it to survive setupPinCode()
    Auth.getInstance().setupPinCode(pinSecret, ...);
    // since we've retained it, we can pass it to createWallet()
    Wallets.getInstance().createWallet(..., pinSecret, ...);
    ```

5. You can also use `NumericPinCodeInputView.clear()` to clear current input

    ```java
    pinInputView.clear()
    ```
