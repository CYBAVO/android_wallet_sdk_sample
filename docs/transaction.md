# Transaction

- Bookmarks
  - [Deposit](#deposit)
  - [Withdraw](#withdraw)
  - [Transaction Detail](#transaction-detail)
  - [Transaction Replacement](#transaction-replacement)
  - [Interact with Smart Contract](#interact-with-smart-contract)
  - [Specific Usage](#spcific-usage)
    - [Solana SignMessage](#solana-signmessage)
    - [Solana Token, ATA](#solana-token-ata)

## Deposit

- Select a wallet address, create a new one if needed.
- Generate QR code
- Present the QR code for deposit.

## Withdraw

![img](images/sdk_guideline/create-transation.jpg)

### getTransactionFee

- To get transaction fees of the selected currency,  
you will get three levels { high, medium, low } of fees for the user to select.
- `tokenAddress` is for private chain usage. For public chain, `tokenAddress` should always be ""
- For example:
  - ETH transaction use ETH as transaction fee ➜ pass `currency: 60, tokenAddress: ""`
  - ERC20 transaction use ETH as transaction fee ➜ pass `currency: 60, tokenAddress: ""`

```java
/// Get transaction transactionFee of specified currency
/// - Parameters:
///   - currency: Currency to query
///   - tokenAddress: fee of private to public transaction
///   - callback: asynchronous callback
public abstract void getTransactionFee(long currency, String tokenAddress, Callback<GetTransactionFeeResult> callback);
```

### getCurrencyTraits

- To get currency traits when you are ready to withdraw.

```java
/// Get currency traits for withdraw restriction
/// - Parameters:
///   - currency: query currency
///   - tokenAddress: query tokenAddress
///   - tokenVersion: query tokenVersion
///   - walletAddress: query walletAddress
///   - callback: asynchronous callback of GetCurrencyTraitsResult
public abstract void getCurrencyTraits(long currency, String tokenAddress, long tokenVersion, String walletAddress, Callback<GetCurrencyTraitsResult> callback);
```

- Response: `GetCurrencyTraitsResult`

    ```java
    public final class GetCurrencyTraitsResult {
        /* EPI-777: withdraw must be multiples of granularity. */
        public String granularity = "";
        /*The minimum balance after transaction (ALGO, DOT, KSM). */
        public String existentialDeposit = "";
        /* The minimum balance after transaction (XLM, FLOW) */
        public String minimumAccountBalance = "";
    }
    ```

  - about `granularity`, see [EIP-777](https://eips.ethereum.org/EIPS/eip-777) ➜ search for granularity section
  - about `existentialDeposit`, see [this](https://support.polkadot.network/support/solutions/articles/65000168651-what-is-the-existential-deposit-)

  - about `minimumAccountBalance`, see [this](https://developers.stellar.org/docs/glossary/minimum-balance/)

### estimateTransaction

- Estimate the transaction fees to present for the user.

```java
/// Estimate platform fee / chain fee for given transaction information
/// - Parameters:
///   - currency: Currency of desired new wallet
///   - tokenAddress: Token address for tokens, i.e. an ERC-20 token wallet maps to an Ethereum wallet
///   - amount: Amount to transfer
///   - transactionFee: Transaction transactionFee to pay
///   - walletId: Wallet ID to estimated transaction
///   - toAddress: To Address
///   - callback: asynchronous callback
public abstract void estimateTransaction(long currency, String tokenAddress, String amount, String transactionFee, long walletId, String toAddress, Callback<EstimateTransactionResult> callback);
```

- Response: `EstimateTransactionResult`

    ```java
    public final class EstimateTransactionResult {
        /* Estimated total amount to transaction. */
        public String tranasctionAmout; 
        /* Estimated platform fee of transaction. */
        public String platformFee;
        /* Estimated blockchain fee of transaction. */
        public String blockchainFee;
        /* Minimum transfer amount for private chain. */
        public String withdrawMin; 
    }
    ```

  - Administrators can add `platformFee` on admin panel
  ![screenshot](images/sdk_guideline/screenshot_platform_fee_management.png)

### getAddressesTags

- To get an AML tag for the address.
- Be sure to provide warnings for the user if the address is in the blacklist.

```java
/// Get AML tag for address
/// - Parameters:
///   - currency: query currency
///   - addresses: query address
///   - callback: asynchronous callback
public abstract void getAddressesTags(long currency, String[] addresses, Callback<GetAddressesTagsResult> callback);
```

### createTransaction

- This method will create and broadcast a transaction to blockchain.
- Fulfill the requirement of different types of currencies in the extras field.
- Please use the function with `PinSecret` version, the others are planning to deprecate.
- If you are making SMS transaction, refer to `createTransactionSms`
- If you are making Biometrics transaction, refer to `createTransactionBio`

```java
/// Create a transaction from specified wallet to specified address
/// - Parameters:
///   - fromWalletId: ID of wallet to withdraw from
///   - toAddress: Target address to send
///   - amount: Amount to transfer, token ID for ERC-721, BSC-721
///   - transactionFee: Transaction transactionFee to pay
///   - description: Description of the transaction
///   - pinSecret: PIN secret retrieved via {PinCodeInputView}
///   - extraAttributes: Extra attributes for specific currencies, pass null if unspecified.
///      - Supported extras:
///         1. memo (String) - Memo for XRP, XLM, EOS, BNB
///         2. eos_transaction_type (EosResourceTransactionType) - Resource transaction type for EOS, such as buy RAM, delegate CPU
///         3. num_bytes (Long) - Bytes of RAM/NET for EOS RAM delegation/undelegation transactions. The minimal amounts are 1024 bytes
///         4. input_data (String) - Hex string of input data. Must also set gas_limit when have this attributes
///         5. gas_limit (Long) - Must specify this if there were input_data
///         6. skip_email_notification (Boolean) - Determined whether or not to skip sending notification mail after create a transaction
///         7. token_id (String) - token ID for ERC-1155
///         8. kind (String) - kind for private chain, code: private to private; out: private to public
///         9. to_address_tag (String[]) - AML tag, get from getAddressesTags() API
///        10. custom_nonce (Long, Integer) - Specific nonce
///        11. custom_gas_limit (Long, Integer) - Specific gas limit
///        12. sol_token_id (String) - token ID of SOL NFT, if get from getSolNftTokens() API, the token ID would be TokenMeta.tokenAddress
///        13. force_send (Boolean) - For SOL transaction, true means create ATA account for receiver
///      - Note:
///         - When eos_transaction_type is EosResourceTransactionType.SELL_RAM, EosResourceTransactionType.UNDELEGATE_CPU or EosResourceTransactionType.UNDELEGATE_NET, the receiver should be address of Wallet fromWalletId
///   - callback: asynchronous callback
///
public abstract void createTransaction(long fromWalletId, String toAddress, String amount, String transactionFee, String description, PinSecret pinSecret,
                                           Map<String, Object> extraAttributes,
                                           Callback<CreateTransactionResult> callback);
```

## Transaction Detail

- There are two APIs for retriving transaction histories: `getHistory()` and `getUserHistory()`.

### getHistory

- You can use `getHistory()` to get transaction histories of a certern wallet.

```java
/// Get transaction history from
/// - Parameters:
///   - currency: Currency of the address
///   - tokenAddress: Token Contract Address of the address
///   - walletAddress: Wallet address
///   - start: Query start offset
///   - count: Query count returned
///   - crosschain: For private chain transaction history filtering. 0: history for private chain transfer; 1: history for crossing private and public chain
///   - filters: Filter parameters:
///     - direction {Transaction.Direction} - Direction of transaction
///     - pending {Boolean} - Pending state of transactions
///     - success {Boolean} - Success state of transactions
///     - start_time {Long} - Start of time period to query, in Unix timestamp
///     - end_time {Long} - End of time period to query, in Unix timestamp
///       - ex: ["direction": Direction.OUT, "pending": true, "start_time": 1632387959]
///   - callback: asynchronous callback
public abstract void getHistory(long currency, String tokenAddress, String walletAddress, int start, int count, int crosschain, Map<String, Object> filters, Callback<GetHistoryResult> callback);
```

- Paging query: you can utilize `start` and `count` to fulfill paging query.  
  - For example:
    - pass `start: transactions.count, count: 10` to get 10 more records when it reaches your load more condition until there's no more transactions.
    - Has more: `result.start` + `result.transactions.length` < `result.total`
- Response: list of `Transaction`

    ```java
    public final class Transaction {
        /* transaction ID. */
        public String txid = ""; 

        public boolean pending = false;

        public boolean success = false;
        /* Is transaction dropped by the blockchain. */
        public boolean dropped = false; 
        /* Is transaction replaced by another transaction. */
        public boolean replaced; 
    
        ...
    }
    ```

    <img src="images/sdk_guideline/transaction_state.jpg" alt="drawing" width="600"/>

- If the Tx's final state is `Success` or `Pending`, you could call `getTransactionInfo` to check the information about this Tx on the blockchain.

### getUserHistory
- ⚠️ `getUserHistory()` and `Transaction.Type` are only available on `com.cybavo.wallet:wallet-sdk-lib:1.2.4579` and later.
- You can also use `getUserHistory()` to retrive all transaction histories of the user.
```java
/// Get transaction history of the user
/// - Parameters:
///   - start: Query start offset
///   - count: Query count returned
///   - filters: Filter parameters:
///     - type {Transaction.Type}, {Transaction type[]} - Transaction type
///     - pending {Boolean} - Pending state of transactions
///     - success {Boolean} - Success state of transactions
///     - start_time {Long} - Start of time period to query, in Unix timestamp
///     - end_time {Long} - End of time period to query, in Unix timestamp
///     - currency {Long}, {Integer} - Currency of the transaction
///     - token_address {String} - Token Contract Address of the transaction
///   - callback: asynchronous callback
public abstract void getUserHistory(long currency, String tokenAddress, String walletAddress, int start, int count, int crosschain, Map<String, Object> filters, Callback<GetHistoryResult> callback);
```
- Since the result may include transactions from public chain, private chain and different currency. For the returned `Transaction`, there are three fields you can refer to.
```java
  public final class Transaction {
      /* Currency of the transaction. */
      public long currency; 
      /* Token contract address of the transaction. */
      public String tokenAddress;
      /**
          Type of the transaction.
          Only available in the result of getUserHistory()
          Please refer to Transaction.Type for the definition.
      */
      public Type type;
      ...
  }
```
### Enum - Transaction.Type

- Enum Constant Summary

| Enum Constant  | Value | Description |
| ----  | ----  | ---- |
|	Unknown	|	0	| 	Default value when no data available.	|
|	MainDeposit	|	1	| Deposit on public chain.		| 
|	MainWithdraw	|	2	| Withdraw on public chain.		| 
|	PrivDeposit	|	3	| Deposit on private chain, including inner transfer and deposit to private chain (mint).		| 
|	PrivWithdraw	|	4	| Withdraw on private chain, including inner transfer and withdraw to public chain (burn).		| 
|	PrivOuterDeposit	|	5	| When deposit from public chain to private chain, the history of public chain.		| 
|	PrivOuterWithdraw	|	6	| When withdraw from private chain to public chain, the history of private chain.		| 
|	PrivProductDeposit	|	7	| Deposit financial product.		| 
|	PrivProductWithdraw	|	8	| Withdraw, earlyWithdraw financial product.		| 
|	PrivProductReward	|	9	| WithdrawReward financial product.		| 

- Method Summary

| Modifier and Type  | Method | Description |
| ----  | ----  | ---- |
|	static Transaction.Type	|	getType(int value)	| 	Returns the enum constant of this type with the specified value,<br>return `Unknown` if cannot find a matched enum.	|
|	int	|	getValue()	| Get int value of the enum.	|

### getTransactionInfo

- Check the information about the Tx on the blockchain.

```java
/// Get transaction result for given txid.
/// - Parameters:
///   - currency: currency to get transaction result
///   - txid: txid of transaction
///   - callback: asynchronous callback
public abstract void getTransactionInfo(long currency, String txid, Callback<GetTransactionInfoResult> callback);

/// the batch version of getTransactionInfo
public abstract void getTransactionsInfo(long currency, String[] txids, Callback<GetTransactionsInfoResult> callback);
```

## Transaction Replacement

> ⚠️ Warning: Cancel / Accelerate transactions will incur a higher Tx fee for replacing the original Tx.

- If a user wants to Cancel / Accelerate a `Pending` Tx on blockchain.
The user needs to create another Tx with higher Tx fee and the same nonce to replace the original one.
- You can achive Tx replacement by `cancelTransaction` and `increaseTransactionFee` API.
- Condition: `replaceable == true`

  ```java
  public final class Transaction {
    
      public String txid = "";
      /* Is transaction replaceable. */
      public boolean replaceable;
      /* Is transaction replaced by another transaction. */
      public boolean replaced;
      /* TXID of replacement of this transaction if replaced == true */
      public String replaceTxid;
      /* Nonce of transaction, only valid on ETH, same nonce means replacements. */
      public int nonce;
      ...
  }
  ```
  
  - Steps:
    1. Call `getTransactionFee` to get the current Tx fee.
    2. Decide a new Tx fee
        - if (Tx fee > original Tx fee) ➜ use the new Tx fee
        - if (Tx fee <= original Tx fee) ➜ decide a higher Tx fee by your rules
            - Suggestion: In our experience, (original Tx fee) * 1.1 might be a easy way to calculate a new price for doing this operation.
    3. Call `cancelTransaction` for canceling transactions.
    4. Call `increaseTransactionFee` for accelerating transactions.

### Transaction Replacement History

- In the result of `getHistory`, you will need to determine different states for a transaction.
- How to determine a transaction is replaced or not:
    1. filter `platformFee == false` ➜ reduce the transactions which are platform fees.
    2. filter `nonce != 0` ➜ reduce normal transactions
    3. mapping transactions with the same nonce
    4. in a set of transactions:
        - the Tx fee lower one ➜ the original order
        - `if Tx1.amount == Tx2.amount` ➜ is Accelerate transaction operation
        - `if Tx.amount == 0` ➜ is Cancel transaction operation
        - `if Tx1.replaced == false && Tx2.replaced == false` ➜ is operating
        - `if Original-Tx.replaced == true` ➜ Cancel / Accelerate success
        - `if Replacement-Tx.replaced == true` ➜ Cancel / Accelerate failed

## Interact with Smart Contract
Wallet SDK provides APIs to call [ABI](https://docs.soliditylang.org/en/develop/abi-spec.html) functions for general read and write operation.   
- For read operation, like `balanceOf`, use `callAbiFunctionRead()`. The parameter is depends on the ABI function required.  

  For example, here's the json of the ABI function we want to call:
    ```javascript
    //Part of ABI_JSON
    {
        "constant": true,
        "inputs": [
          {
            "name": "_owner",
            "type": "address"
          },
          {
            "name": "_testInt",
            "type": "uint256"
          },
          {
            "name": "_testStr",
            "type": "string"
          }
        ],
        "name": "balanceOfCB",
        "outputs": [
          {
            "name": "balance",
            "type": "uint256"
          }
        ],
        "payable": false,
        "stateMutability": "view",
        "type": "function"
      }
    ``` 
    According to its definition, we would compose an API call like this:
    ```java
    Wallets.getInstance().callAbiFunctionRead(
                        walletId,
                        "balanceOfCB", // name, function name of ABI
                        "0xef3aa4115b071a9a7cd43f1896e3129f296c5a5f", // contractAddress, contract address of ABI
                        ABI_JSON, // abiJson, ABI contract json
                        new Object[]{"0x281F397c5a5a6E9BE42255b01EfDf8b42F0Cd179", 100, "test"}, // args, argument array of ABI function
                        new Callback<CallAbiFunctionResult>() {
                            @Override
                            public void onError(Throwable error) {
                                Log.e(TAG, "callAbiFunctionRead failed", error);
                            }

                            @Override
                            public void onResult(CallAbiFunctionResult result) {
                                Log.d(TAG, String.format("callAbiFunctionRead success:%s", result.output));
                            }
                        });
    ```
    Aside from `walletId` and  `callback`, all the parameters are varied according to the ABI function.  
    
    See [this](https://github.com/CYBAVO/android_wallet_sdk_sample/blob/master/app/src/main/java/com/cybavo/example/wallet/detail/WithdrawFragment.java#L300-L313) for complete example.  
- For write operaion, like `transferFrom`, use `callAbiFunctionTransaction()`. The parameter is also depends on the ABI function required.  

  For example, here's the json of the ABI function we want to call:
    ```javascript
    //Part of ABI_JSON
    {
        "constant": false,
        "inputs": [
          {
            "name": "_to",
            "type": "address"
          },
          {
            "name": "_value",
            "type": "uint256"
          },
          {
            "name": "_testInt",
            "type": "uint256"
          },
          {
            "name": "_testStr",
            "type": "string"
          }
        ],
        "name": "transferCB",
        "outputs": [
          {
            "name": "success",
            "type": "bool"
          }
        ],
        "payable": false,
        "stateMutability": "nonpayable",
        "type": "function"
      }
    ```
    According to its definition, we would compose an API call like this:
    ```java
    Wallets.getInstance().callAbiFunctionTransaction(walletId,
                    "transferCB",// name, the function name of ABI
                    "0xef3aa4115b071a9a7cd43f1896e3129f296c5a5f",// contractAddress, contract address of ABI
                    ABI_JSON, // abiJson, ABI contract json
                    new Object[]{"0x490d510c1A8b74749949cFE5cA06D0C6BD7119E2", 1, 100, "unintest"},// args, argument array of abi function
                    fee.amount, // transactionFee, see getTransactionFee() and amount property of Fee class
                    pinSecret,
                    new Callback<CallAbiFunctionResult>() {
                        @Override
                        public void onError(Throwable error) {
                            Log.e(TAG, "callAbiFunctionTransaction failed", error);
                        }

                        @Override
                        public void onResult(CallAbiFunctionResult result) {
                            Log.d(TAG, String.format("callAbiFunctionTransaction success:%s, %s", result.txid, result.signedTx));
                        }
                    });
    ```
    Different from `callAbiFunctionRead()`, `callAbiFunctionTransaction()` requires 2 more parameters: `transactionFee` and `PinSecret` for transaction.  
    
    The parameter `name`, `contractAddress`, `abiJson` and `args` are varied according to the ABI function.  
    
    See [this](https://github.com/CYBAVO/android_wallet_sdk_sample/blob/master/app/src/main/java/com/cybavo/example/wallet/detail/WithdrawFragment.java#L282-L295) for complete example.  
    
    See [Withdraw to Public Chain](https://github.com/CYBAVO/android_wallet_sdk_sample/blob/master/docs/private_chain.md#perform-withdraw) for another specific usage in private chain.

## Spcific Usage
There are specific API usages for some scenarios which related to transaction, you can find them in this section.
### Solana SignMessage
Since `signMessage()` of Solana can be used to sign a raw transaction, in order to help the caller be more cautious before signing, it required to get an action token then pass to `signMessage()` to verify.
```java
/**
 * 1. Get action token for signMessage, 
 * the "message" of getSignMessageActionToken() and signMessage() should be the same.
 */
Wallets.getInstance().getSignMessageActionToken(message, new Callback<GetActionTokenResult>() {
    @Override
    public void onError(Throwable error) {
        error.printStackTrace();
    }

    @Override
    public void onResult(GetActionTokenResult result) {
      
        // 2. Put it in a map and pass to signMessage().
        Map<String,Object> extraAttributes = new HashMap<>();
        extraAttributes.put("confirmed_action_token", result.actionToken);

        Wallets.getInstance().signMessage(wallet.walletId, message, pinSecret, extraAttributes, new Callback<SignMessageResult>() {
            @Override
            public void onError(Throwable error) {
                error.printStackTrace();
            }

            @Override
            public void onResult(SignMessageResult result) {
                Log.d(TAG, String.format("signedMessage: %s", result.signedMessage));
                
            }
        });
    }
});
```
### Solana Token, ATA
You can create Solana ATA (associated token account) through `createTransaction()` with extras or `setSolTokenAccountTransaction()`.
- Call `createTransaction()` with `force_send` in extras:
```java
Map<String, Object> extras = new HashMap<>();
// For SOL transaction, "force_send" true means create ATA account for receiver.
if(wallet.currency == CurrencyHelper.Coin.SOL && !TextUtils.isEmpty(wallet.tokenAddress)){
    extras.put("force_send", true);
}
Wallets.getInstance().createTransaction(wallet.walletId, toAddress, transactionAmount, fee, desc, pinSecret, extras, callback);
```
- Call `setSolTokenAccountTransaction()` directly:
```java
/**
* Note 1: The SOL token wallet must have SOL for transaction fee, otherwise, the API will return empty TXID.
* Note 2: If the SOL token wallet have created token account, the API will also return empty TXID.
* */
Wallets.getInstance().setSolTokenAccountTransaction(wallet.walletId, pinSecret, new Callback<SetSolTokenAccountTransactionResult>() {
  @Override
  public void onError(Throwable error) {
      error.printStackTrace();
  }

  @Override
  public void onResult(SetSolTokenAccountTransactionResult result) {
      Log.d(TAG, String.format("TXID: %s", result.txid));
  }
});
```


