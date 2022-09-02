# CYBAVO Private Chain (CPC)

- Scenario for:
  - Financial Products
  - Financial Management Services
  
- Advantages of a private chain:
    1. Free; zero transaction fee for inner transfer
    2. Faster; faster than public chain
    3. Community; referral system is possible

- Easy to implement, sharing APIs with the public chain.

- Bookmarks
  - [Model - Wallet](#wallet)
  - [Model - Currency](#currency)
  - [Model - UserState](#userstate)
  - [Transactions - Deposit to Private Chain](#1-deposit-to-private-chain)
  - [Transactions - Withdraw to Public Chain](#2-withdraw-to-public-chain)
  - [Transactions - Inner Transfer](#3-inner-transfer)
  - [Transaction History](#transaction-history)
  - [CPC Financial Product](#cpc-financial-product)
    - [Financial Product](#financial-product)
    - [Financial History](#financial-history)
    - [Financial Order](#financial-order)
    - [Financial Bonus](#financial-bonus)
    - [Transaction Operations](#transaction-operations)

## Models

### Wallet

```java
public final class Wallet {

    public long walletId; // Wallet ID

    public boolean isPrivate; // Is private chain (CPC)

    public long mapToPublicCurrency; // Public chain's currency

    public String mapToPublicTokenAddress; // Public chain's tokenAddress

    public String mapToPublicName; // Public chain's currency_name

    public String walletCode; // Address(referral code) for transaction in private chain

    public DepositAddress[] depositAddresses; // Deposit info, public chain to private chain

    public boolean isPrivateDisabled; // Is disabled private currency
    
    ...
}
```

- `isPrivate` means the wallet is on the private chain
- Thus, it will map to a public currency on the public chain.  
  - related infos: `mapToPublicCurrency`, `mapToPublicTokenAddress`, `mapToPublicName`
- `depositAddresses` provides the addresses on the public chain. When you deposit currencies / tokens to these addresses, you will receive tokens in the related private chain wallet.
  - multiple `depositAddresses` means one private chain wallet might provide multiple addresses for depositing.
  - ex: CPSC-USDT on private chain is mapped to USDT-ERC20 and USDT-TRC20 on the public chains, so you will get multiple addresses in this field.
  - `memo` in model `DepositAddress` is necessary when depositing tokens from public chain to private chain.

### Currency

```java
public final class Currency {

    final public boolean isPrivate; // Is private chain (CPC)

    final public long mapToPublicType; // Public chain's currency type

    final public String mapToPublicTokenAddress; // Public chain's token address

    final public String mapToPublicName; // Public chain's currency name

    final public boolean canCreateFinanceWallet; // Can create financial wallet

    ...
}
```

- `isPrivate` means the currency is on the private chain
- Thus, it will map to a public currency on the public chain.  
  - related infos: `mapToPublicType`, `mapToPublicTokenAddress`, `mapToPublicName`, `canCreateFinanceWallet`

- How to create a private chain wallet with the currency?
  - Basically, it's the same way as we mentioned in [createWallet](wallets.md#createwallet), the only difference is the filtering condition of currency and wallet.
  - In the chart below, `Available Currencies` should be `isPrivate == true && (canCreateFinanceWallet == true || TextUtils.isEmpty(tokenAddress))`
  ![img](images/sdk_guideline/create_wallet.jpg)

### UserState

```java
public final class UserState {

    public String userReferralCode; // User referral code

    public String linkUserReferralCode; // Link user referral code (referral by this code, only one per user)

    ...
}
```

- Referral Code has two use cases:
    1. referral system
    2. substitute readable address for making transactions in the private chain
- `userReferralCode` represent the user's referral code
- `linkUserReferralCode` represent the referrer's referral code
- Call `Auth.getInstance().registerReferralCode()` to register a referrer.
- You can search user by calling `Auth.getInstance().searchUser()`, the keyword can be `realName` (partial match) or `referralCode` (fully match)
  ```java
  Auth.getInstance().searchUser("userX", new Callback<SearchUserResult>() {
                @Override
                public void onError(Throwable error) {
                    //keyword length cannot less then 3, 
                    //otherwise the API will receive ErrKeywordForSearchTooShort
                    error.printStackTrace();
                }

                @Override
                public void onResult(SearchUserResult result) {
                    for(int i = 0; i < result.infos.length; i++){
                        Log.d("searchUser", String.format("#%d, Name: %s, Code: %s", 
                              i, result.infos[i].realName, result.infos[i].referralCode));
                    }
                }
            });

  ```
- You can update `realName` by calling `Auth.getInstance().updateRealName()`
  ```java
  Auth.getInstance().updateRealName("userY" , new Callback<UpdateRealNameResult>() {
                @Override
                public void onError(Throwable error) {
                    //realName length cannot less then 3, 
                    // otherwise the API will receive ErrKeywordForSearchTooShort
                    error.printStackTrace();
                }

                @Override
                public void onResult(UpdateRealNameResult result) {
                    //getUserState after updateRealName
                    Auth.getInstance().getUserState(new Callback<GetUserStateResult>() {
                        @Override
                        public void onError(Throwable error) {
                            error.printStackTrace();
                        }

                        @Override
                        public void onResult(GetUserStateResult result) {
                            Log.d("updateRealName",String.format("newRealName: %s", result.userState.realName));
                        }
                    });
                }
            });
  ```
## Transactions

- There are 3 types of transactions on the private chain.

### 1. Deposit to Private Chain

- Select a private wallet, create a new one if needed.
- Select a deposit address of the private wallet.
- Present the address and memo of the deposit address for deposit.

### 2. Withdraw to Public Chain

#### Get Transaction Fee

- Withdrawing to public chain will be charged a fixed transaction fee.  
i.e. `getTransactionFee()` will return the same amount of { high, medium, low } level for private chain currency.
- Use `wallet.depositAddress` 's `Currency` and `tokenAddress` as parameters to get the transaction fee for withdraw to public chain.
- The { receive amount = transfer amount - transaction fee }
- The receive amount cannot less than `withdrawMin`

```java
public final  class GetTransactionFeeResult {
    
    public String withdrawMin; // Minimum transfer amount for private
    
    ...
}
```

#### Perform Withdraw

- Call `callAbiFunctionTransaction()` to perform the transaction with specific parameters:

```java
DepositAddress depositAddress = wallet.depositAddress[0] //select a deposit address
Object[] args = new Object[]{toAddress,
                   transferAmount, //ex. "123.123456"
                   memo, // optional, ex. "123456" 
                   Long.toString(depositAddress.mapToPublicCurrency), //ex. "60"
                   depositAddress.mapToPublicTokenAddress}; 

Wallets.getInstance().callAbiFunctionTransaction(walletId, 
                    "burn", // name: fixed to "burn"
                    wallet.tokenAddress, 
                    "", // abiJson: fixed to ""
                    args, 
                    "0", //transactionFee: our backend will take care of this 
                    pinSecret, callback);
```

### 3. Inner Transfer

#### Private Chain Platform Fee
- On the **admin panel** ➜ **CYBAVO Smart Chain** ➜ **Chain Settings**, choose a currency which supports platform fee, click **Manage** button ➜ **Chain Wallet info**, you can found **Transfer Fee Rate** and **Transfer Fee Min**.  

  <img src="images/sdk_guideline/private_chain_platform_fee.png" alt="drawing" width="600"/>
- All the transfer operation on private chain will be charged platform fee, including inner transfer and transaction for financial product, not including deposit to private chain and withdraw to public chain. 
- Platform fee calculation:
  1. Platform Fee = Transfer Amount * **Transfer Fee Rate**
  2. If the result of step 1 is less then **Transfer Fee Min**, use **Transfer Fee Min**.
  3. If the currency not supported platform fee, the `platformFee` will be "0".
- You can use `estimateTransaction()` to get the platfom fee:
```java
Wallets.getInstance().estimateTransaction(
                wallet.currency,
                wallet.tokenAddress,
                amount,// ex. "100"
                "0", // transactionFee: fixed to "0"
                wallet.walletId,
                new Callback<EstimateTransactionResult>() {
                    @Override
                    public void onError(Throwable error) {
                        error.printStackTrace();
                    }
        
                    @Override
                    public void onResult(EstimateTransactionResult result) {
                        //check result.platformFee
                    }
        });
```
#### Create Transaction
- Call `createTransaction()` to perform the transaction with specific parameters:

```java
final Map<String, Object> extras = new HashMap<>();
extras.put("kind", "code"); //means it's a inner transfer transaction

Wallets.getInstance().createTransaction(walletId,
                    toAddress, //other user's userReferralCode, ex. "8X372G"
                    transferAmount, //ex. "123.123456"
                    "0", // transactionFee: fixed to "0"
                    description,
                    pinSecret,
                    extras, callback);
```

## Transaction History

- Basically, it's the same way as we mentioned in [transaction.md](transaction.md).  
 The only different thing is the parameter `crosschain` of `getHistory()`:
  - Pass `crosschain: 1`, it returns transactions of [Deposit to Private Chain](#deposit-to-private-chain) and [Withdraw to Public Chain](#withdraw-to-public-chain)
  - Pass `crosschain: 0`, it returns transactions of [Inner Transfer](#inner-transfer).

## CPC Financial Product
- After deposit to CPC, users can further deposit to financial product for a period of time to get interest, the financial product can be setup on the admin panel.  
- In the following part, we will introduce necessary class and retrive data APIs first, then the operation API.

### Financial Product  
- The following image and table shows the mapping of product settings on the admin panel and FinancialProduct fields. 

<img src="images/sdk_guideline/private_chain_product_setting.png" alt="drawing" width="700"/> 


|  Product Setting<br>(Admin Panel)   | FinancialProduct Field  | Note  |
|  ----  | ----  | ----  |
|  Contract Address  | `uuid`  | |
|  StartAt  | `startTimestamp`  | |
|  Title:zh-tw <br>Title:zh-cn<br>Title:zh-en | `title.tw`<br>`title.cn`<br>`title.en`  |- Display one of these as product name according to device locale.|
|  Max Users<br>UserWarningCountPrcent  | `maxUsers`<br>`userPercent`  |- `maxUsers` <= `userCount`, means sold out.<br>- `maxUsers` * `userPercent` >= `userCount`, means available<br>- `maxUsers` * `userPercent` < `userCount`, means about full.|
|  Show Rate  | `rate`  |- Display it as annual interest rate<br>-`ratePercent` is `double` version of annual interest rate.|
|  Campaign  | `GetFinancialProductsResult.campaign`  |- If Campaign is checked, this product will also exist in `GetFinancialProductsResult.campaign`.|
|  MinDeposit<br>MaxDeposit  | `minDeposit`<br>`maxDeposit`  |- Display the deposit amount limit range,<br>ex. Min 0.5 HW-ETH - 1000 HW-ETH. |
|  InverseProfitSharingCurrency  | `kind`  |- enum: `FinancialProduct.Kind`<br>- If InverseProfitSharingCurrency is set to **Disable**, `kind` would be `DemandDeposit`(2) ,<br>otherwise, `kind` would be `FixedDeposit`(1).|

 #### Get Financial Product Lists
- You can get financial product list by `FinancialProduct.ListKind`:
 ```java
/** 
* Refers to FinancialProduct.ListKind:
* All(0), UserDeposit(1), DemandDeposit(2), FixedDeposit(3), Campaign(4)
*/
int kind = FinancialProduct.ListKind.All.getValue();

Wallets.getInstance().getFinancialProducts(
        kind,
        new Callback<GetFinancialProductsResult>() {
        @Override
        public void onError(Throwable error) {
            error.printStackTrace();
        }

        @Override
        public void onResult(GetFinancialProductsResult result) {
            /**
              Financial product lists are categorized as following:
              result.userDeposits, result.demandDeposits,
              result.fixedDeposits, result.campaign
              */

            CharSequence format = DateFormat.getBestDateTimePattern(Locale.getDefault(), "yyyy-MM-dd HH:mm:ss");
            SimpleDateFormat formatter = getCountDownFormat();

            for(FinancialProduct product: result.demandDeposits){
                long msInFuture = getMsInFuture(product.userWaitToWithdraw);
                String tag = getAvailableTag(product);
                //ex. Product: Demand Deposits (Hourly Interest), kind: DemandDeposit,
                // Annualized Rate: 10%, Amount: 0.900000000000000000 HW-ETH,
                // Maturity Interest: 0.064776852000000000 HW-ETH,
                // Allow withdraw after: 00:03:07, Available
                Log.d(TAG, String.format("Product: %s, kind: %s, Annualized Rate: %s%%, " +
                                "Amount: %s %s, Maturity Interest: %s %s, " +
                                "Allow withdraw after: %s, %s",
                        product.title.en,
                        FinancialProduct.Kind.getKind(product.kind),
                        product.rate,
                        product.userDeposit, product.publicName,
                        product.userReward, product.publicName,
                        msInFuture <= 0 || (!product.isCanWithdraw && !Boolean.TRUE.equals(product.isCanWithdrawReward))? "": formatter.format(msInFuture),
                        tag));
            }
            for(FinancialProduct product: result.fixedDeposits){
                //ex. Product: Time deposit (10 days), kind:FixedDeposit,
                // Annualized Rate: 15%, Amount: 1.004613 HW-XRP,
                // Maturity Interest: 0.004128 HW-XRP,
                // Start date: 2022/09/02 12:48:11,
                // Value date: ,
                // Expiry date: 2022/09/12 12:48:11
                Log.d(TAG, String.format("Product: %s, kind:%s, Annualized Rate: %s%%, " +
                                "Amount: %s %s, Maturity Interest: %s %s, " +
                                "Start date: %s, Value date: %s, Expiry date: %s",
                        product.title.en,
                        FinancialProduct.Kind.getKind(product.kind),
                        product.rate,
                        product.userDeposit, product.publicName,
                        product.userReward, product.publicName,
                        product.startTimestamp == 0? "": DateFormat.format(format, product.startTimestamp * 1000),
                        product.rewardTimestamp == 0? "": DateFormat.format(format, product.rewardTimestamp * 1000),
                        product.endTimestamp == 0? "": DateFormat.format(format, product.endTimestamp * 1000)));
            }
        }
});

public static long getMsInFuture(long deadline){
        return deadline == 0 ? 0: (deadline * 1000) - new Date().getTime();
}

public static SimpleDateFormat getCountDownFormat(){
    SimpleDateFormat simpleDateFormat = new SimpleDateFormat("HH:mm:ss");
    simpleDateFormat.setTimeZone(TimeZone.getTimeZone("GMT+00:00"));
    return simpleDateFormat;
}

public static String getAvailableTag(FinancialProduct product){
    long time = new Date().getTime() / 1000;
    if(product.startTimestamp > time){
        return "Not Start";
    }
    if(product.userCount >= product.maxUsers){
        return "Sold Out";
    }
    if(product.maxUsers * product.userPercent >= product.userCount){
        return "Available";
    }else{
        return "About Full";
    }
}
 ```
 ### Financial History
- If users have deposited or withdrawn a financial product, related FinancialHistory will be created / removed.  
- For depositing FinancialHistory, App can perform transaction operations according to the field starting with `isCan`, see [Transaction Operations](#transaction-operations) for detailed usage.   

#### Get Financial History List
|  Transaction Operation   | FinancialProduct.kind  | Changes in GetFinancialHistoryResult  |
|  ----  | ----  | ----  |
|  deposit  | `FixedDeposit`  | - Add one `Depositing` history. |
|  deposit  | `DemandDeposit`  | - Add one `Depositing` history when there's no one for this product.<br>- Or update the existing `Depositing` history.|
|  withdraw<br>earlyWithdraw  | `FixedDeposit`  | - Remove the `Depositing` history.<br>- Add one `Withdraw` history.<br>- Add one `WithdrawReward` history. |
|  withdraw  | `DemandDeposit`  | - Add one `Withdraw` history.<br>- Remove the `Depositing` history if no `userDeposit` and `userReward` left.<br>- Or update the existing `Depositing` history. |
|  withdrawReward  | `DemandDeposit`  | - Add one `WithdrawReward` history.<br>- Remove the `Depositing` history if no `userDeposit` and `userReward` left.<br>- Or update the existing `Depositing` history. |

- You can get financial history list by `FinancialHistory.ListKind` or `FinancialProduct.uuid`. 
 ```java
/**
  * Refers to FinancialHistory.ListKind:
  * Depositing(1), Withdraw(2), WithdrawReward(3)
  */
int kind = FinancialHistory.ListKind.Depositing.getValue();

// Flag for paging: pass null, or nextPage or prevPage of GetFinancialHistoryResult
String page = doRefresh? null: previousResult.nextPage

Wallets.getInstance().getFinancialHistory(
        kind, 
        page,
        new Callback<GetFinancialHistoryResult>() {
          @Override
          public void onError(Throwable error) {
              error.printStackTrace();
          }

          @Override
          public void onResult(GetFinancialHistoryResult result) {
                CharSequence format = DateFormat.getBestDateTimePattern(Locale.getDefault(), "yyyy-MM-dd HH:mm:ss");
                for(FinancialHistory history: result.histories){
                    //Get FinancialProduct for this history in result.products
                    FinancialProduct product = result.products.get(history.productUuid);

                    // Use FinancialHistory.isCan first, only use FinancialProduct's if FinancialHistory's is null
                    boolean isCanWithdraw = history.isCanWithdraw == null? product.isCanWithdraw: history.isCanWithdraw;
                    boolean isCanEarlyWithdraw = history.isCanEarlyWithdraw == null? product.isCanEarlyWithdraw: history.isCanEarlyWithdraw;
                    boolean isCanWithdrawReward = history.isCanWithdrawReward == null? product.isCanWithdrawReward: history.isCanWithdrawReward;
                    
                    //ex. Currency: HW-ETH, Subscribe item: Demand Deposits (Hourly Interest), 
                    // Deposit amount: 0.151400000000000000, Start date: 2021/11/03 23:44:00, Value date: , 
                    // Expiry date: 2022/12/03 23:44:00, Interest amount: 0.000001727474000000, Annual Interest Rate: 10%
                    Log.d(TAG, String.format("Currency: %s, Subscribe item: %s, Deposit amount: %s, " +
                            "Start date: %s, Value date: %s, " +
                                    "Expiry date: %s, " +// if kind is ListKind.Withdraw, should display as "Withdraw date"
                                    "Interest amount: %s, Annual Interest Rate: %s%%",
                            product.publicName, product.title.en, history.userDeposit,
                            product.startTimestamp == 0? "": DateFormat.format(format, product.startTimestamp * 1000),
                            product.rewardTimestamp == 0? "": DateFormat.format(format, product.rewardTimestamp * 1000),
                            product.endTimestamp == 0? "": DateFormat.format(format, product.endTimestamp * 1000),
                            history.userReward, product.rate));
                }
            }
});
 ```
- ⚠️ Get financial history list by `FinancialProduct.uuid` will only return `Depositing` history.
```java
// Flag for paging: pass null, or nextPage or prevPage of GetFinancialHistoryResult
String page = doRefresh? null: previousResult.nextPage

Wallets.getInstance().getFinancialHistory(
                financialProduct.uuid,
                page, // Flag for paging: pass null, or nextPage or prevPage of GetFinancialHistoryResult
                new Callback<GetFinancialHistoryResult>() {
                @Override
                public void onError(Throwable error) {
                    error.printStackTrace();
                }

                @Override
                public void onResult(GetFinancialHistoryResult result) {
                    CharSequence format = DateFormat.getBestDateTimePattern(Locale.getDefault(), "yyyy-MM-dd HH:mm:ss");
                    SimpleDateFormat formatter = getCountDownFormat();
                    for(FinancialHistory history: result.histories){
                        FinancialProduct product = result.products.get(history.productUuid);

                        // Use FinancialHistory.isCan first, only use FinancialProduct's if FinancialHistory's is null
                        boolean isCanWithdraw = history.isCanWithdraw == null? product.isCanWithdraw: history.isCanWithdraw;
                        boolean isCanEarlyWithdraw = history.isCanEarlyWithdraw == null? product.isCanEarlyWithdraw: history.isCanEarlyWithdraw;
                        boolean isCanWithdrawReward = history.isCanWithdrawReward == null? product.isCanWithdrawReward: history.isCanWithdrawReward;

                        long msInFuture = getMsInFuture(history.userWaitToWithdraw);
                        // ex. Currency: HW-XRP, Subscribe item: Time deposit (10 days),
                        // Deposit amount: 225.005000, Start date: 2022/09/02 14:35:32, Value date: ,
                        // Expiry date: 2022/09/12 14:35:32, Interest amount: 0.924678,
                        // Annual Interest Rate: 15%, Allow withdraw after: 00:04:43
                        Log.d(TAG, String.format("Currency: %s, Subscribe item: %s, Deposit amount: %s, " +
                                        "Start date: %s, Value date: %s, Expiry date: %s, " +
                                        "Interest amount: %s, Annual Interest Rate: %s%%, " +
                                        "Allow withdraw after: %s",
                                product.publicName, product.title.en, history.userDeposit,
                                product.startTimestamp == 0? "": DateFormat.format(format, product.startTimestamp * 1000),
                                product.rewardTimestamp == 0? "": DateFormat.format(format, product.rewardTimestamp * 1000),
                                product.endTimestamp == 0? "": DateFormat.format(format, product.endTimestamp * 1000),
                                history.userReward, product.rate,
                                msInFuture <= 0 || (!isCanWithdraw && !isCanEarlyWithdraw)? "": formatter.format(msInFuture))
                        );
                    }
                }
        });
```
### Financial Order
- ⚠️ Financial order is only for `FixedDeposit` product.
- Every deposit will create an order.
- The following image and table shows the mapping of order info on the admin panel and GetFinancialOrderResult fields.  

  <img src="images/sdk_guideline/private_chain_order.png" alt="drawing" width="900"/>  

  |  Order Column <br>(Admin Panel)  | GetFinancialOrderResult Field  | Note |
  |  ----  | ----  | ----  |
  |  OrderID  | `uuid`  | |
  |  Amount  | `userDeposit`| |
  |  Reward  | `userReward`  | |
  |  Penalty  | `earlyReward` | `earlyReward` = Reward - Penalty|

```java
Wallets.getInstance().getFinancialOrder(
                history.productUuid,
                history.orderId,
                new Callback<GetFinancialOrderResult>() {
                    @Override
                    public void onError(Throwable error) {
                        error.printStackTrace();
                    }

                    @Override
                    public void onResult(GetFinancialOrderResult result) {
                        // If the order is not exist, result.kind will be -1
                        
                        // ex. Receivable interest: 0.000000 HW-XRP,
                        // Origin receivable interest: 0.231169 HW-XRP"
                        Log.d(TAG, String.format("Receivable interest: %s %s, Origin receivable interest: %s %s",
                                result.earlyReward, product.publicName,
                                result.userReward, product.publicName));
                    }
        });
```
### Financial Bonus
- CPC financial product also has rebate mechanism, if the user meet the requirement, ex. the user's referrer deposit a finance product, the user will have a `FinancialBonus` in his/her financial list.
- User can perform `withdrawBonus` with `uuid` if `isAlreadyWithdrawn` is false.
```java
Wallets.getInstance().getFinancialBonusList(new Callback<GetFinancialBonusResult>() {
            @Override
            public void onError(Throwable error) {
                error.printStackTrace();
            }

            @Override
            public void onResult(GetFinancialBonusResult result) {
                for (FinancialBonus bonus: result.bonusList){
                    BigDecimal totalPerBonus = BigDecimal.ZERO;
                    if(bonus.rewards == null){
                        continue;
                    }
                    for (FinancialReward reward: bonus.rewards){
                        // If need to display total amount, accumulate reward.amount
                        BigDecimal amountValue = BigDecimal.valueOf(Double.parseDouble(reward.amount));
                        totalPerBonus = totalPerBonus.add(amountValue);
                    }
                    //ex. Bonus: SavingRebate, withdraw:false, total: 5.375417 HW-XRP
                    Log.d(TAG, String.format("Bonus: %s, withdraw:%b, total: %s %s",
                            FinancialBonus.Kind.getKind(bonus.kind),
                            bonus.isAlreadyWithdrawn,
                            totalPerBonus.stripTrailingZeros().toPlainString(),
                            bonus.publicName));
                }
            }
        });
```

### Transaction Operations 
- There are 6 operations for CPC financial product, they can be achieved by `callAbiFunctionTransaction()` with different `args`, the behavior might be different between different `FinancialProduct.kind`.
- ⚠️ After performed `callAbiFunctionTransaction()`, it'll take a while to change data, App may need to display a status for transition to prevent users execute the same operation again (press again the same button).
 
|  ABI Method Name<br>`args[0]`   | `kind` /<br>Perform  to  | Note | `args` |
|  :----:  | :----  | :----  | :---- |
|  [approve](#approve-activate)  | `FixedDeposit`<br>`DemandDeposit` / <br>FinancialProduct | - Approve to activate the product.<br>- Required and cannot perform other operations if `FinancialProduct.isNeedApprove` is true | ["approve", product.uuid] |
|  [deposit](#deposit)  | `FixedDeposit`<br>`DemandDeposit` / <br>FinancialProduct  | - Deposit to the product.<br>- Performable when `FinancialProduct.isCanDeposit` is true| ["deposit",<br>product.uuid,<br>amount, <br>""] |
|  [withdraw](#withdraw---fixeddeposit)  | `FixedDeposit` / <br>Order which linked to FinancialHistory| - Withdraw all principal and interest to given financial wallet.<br>- amount is fixed to "0" for all.<br>- Cannot withdraw if current time is earlier then `FinancialHistory.userWaitToWithdraw`.<br>- Performable when `isCanWithdraw` is true<br>- `isCanWithdraw = history.isCanWithdraw == null? product.isCanWithdraw: history.isCanWithdraw`| ["withdraw", product.uuid,<br>"0",<br>history.orderId] |
|  [withdraw](#withdraw---demanddeposit)  | `DemandDeposit` / <br>FinancialProduct | - Withdraw a certain amount of principal to given financial wallet.<br>- Cannot withdraw if current time is earlier then `FinancialProduct.userWaitToWithdraw`.<br>- Performable when `FinancialProduct.isCanWithdraw` is true| ["withdraw", product.uuid,<br>amount,<br>""] |
|  [earlyWithdraw](#earlywithdraw)  | `FixedDeposit` / <br>Order which linked to FinancialHistory | - Withdraw all principal and interest to given financial wallet.<br>- Withdraw by product / order.<br>- Interest will be deducted, see [Financial Order](#financial-order).<br>- amount is fixed to "0" for all.<br>- Cannot withdraw if current time is earlier then `FinancialHistory.userWaitToWithdraw`.<br>- Performable when `isCanEarlyWithdraw` is true<br>- `isCanEarlyWithdraw = history.isCanEarlyWithdraw == null? product.isCanEarlyWithdraw: history.isCanEarlyWithdraw`| ["earlyWithdraw",<br>product.uuid,<br>"0", <br>history.orderId] |
|  [withdrawReward](#withdrawreward)  | `DemandDeposit` / <br>FinancialProduct | - Withdraw all interest to given financial wallet.<br>- amount is fixed to "0" for all.<br>- Cannot withdraw if current time is earlier then `FinancialProduct.userWaitToWithdraw`.<br>- Performable when `FinancialProduct.isCanWithdrawReward` is true| ["withdrawReward", product.uuid,<br>"0",<br>""] |
|  [withdrawBonus](#withdrawbonus)  | - / FinancialBonus | - Withdraw bonus to given financial wallet.<br>- Performable when `FinancialBonus.isAlreadyWithdrawn` is false| ["withdrawBonus", bonus.uuid,<br>"0"] |

Below code snippet shows a pattern to use `callAbiFunctionTransaction()` for those operations.
 ```java
Wallet wallet = findWallet(privateWallets, product.currency, product.tokenAddress);

Object[] args = new Object[]{
                abiMethodName, // Possible value: "approve", "deposit", "withdraw", "earlyWithdraw", "withdrawReward", "withdrawBonus"
                ...
                };

Wallets.getInstance().callAbiFunctionTransaction(
        wallet.walletId, 
        "financial", //name: fixed to "financial"
        wallet.tokenAddress,
        "", //abiJson: fixed to ""
        args,
        "0", // transactionFee: fixed to "0"
        pinSecret,
        callback);

   // Find wallet by currency and tokenAddress in giving list.
   public Wallet findWallet(ArrayList<Wallet> wallets, long currency, String tokenAddress){
        Wallet targetWallet = null;
        for(Wallet w: wallets){
            if(currency == w.currency && tokenAddress.equals(w.tokenAddress)){
                targetWallet = w;
                break;
            }
        }
        return targetWallet;
    }
 ```
#### Check and Create Wallet
Before performing those operations, you should check if required wallets are created and create for the user if needed.  
Required wallets including:  
1. `currency` is same as `FinancialProduct.currency`, `tokenAddress` is empty.
2. `mapToPublicCurrency` is same as `FinancialProduct.publicCurrency`, `mapToPublicTokenAddress` is empty. 
3. `mapToPublicCurrency` is same as `FinancialProduct.publicCurrency`, `mapToPublicTokenAddress` is same as `FinancialProduct.publicTokenAddress`.  

For example, for a HW-ETH financial product  
(`currency`: 99999999995, `tokenAddress`: "0x123...", `publicCurrency`: 60, `publicTokenAddress`: "")  
required wallets are
1. CPSC wallet (`currency`: 99999999995, `tokenAddress`: "").
2. CPSC-ETH wallet(`mapToPublicCurrency`: 60, `mapToPublicTokenAddress`: "").

For another example, for a HW-USDT financial product  
(`currency`: 99999999995, `tokenAddress`: "0x234...", `publicCurrency`: 60, `publicTokenAddress`: "0x456...")  
required wallets are
1. CPSC wallet (`currency`: 99999999995, `tokenAddress`: "").
2. CPSC-ETH wallet(`mapToPublicCurrency`: 60, `mapToPublicTokenAddress`: "").
3. CPSC-USDT wallet(`mapToPublicCurrency`: 60, `mapToPublicTokenAddress`: "0x456...").

#### Transaction Explain
- Perform those operations may also create [Transaction History](#transaction-history) for inner transfer, those transaction will have `explain` field with additional information, you can use `explain` to make the UI more clearer.
```java
if(item.explain.kind == TransactionExplain.Kind.Unknown.getValue()){
    return;
}
if(!item.explain.isShowAmount){
    //hide amount for 0 amount operation like approve
}
// ex. kind: WithdrawReward, product: Demand Deposits (Hourly Interest)
Log.e("TAG", String.format("kind: %s, product: %s", TransactionExplain.Kind.getKind(item.explain.kind), item.explain.name.en));
```

#### Approve Activate
 ```java
 if(!product.isNeedApprove){
    return;
 }

 // Find wallet by currency and tokenAddress in giving list.
Wallet wallet = findWallet(privateWallets, product.currency, product.tokenAddress);

Object[] args = new Object[]{
                "approve", // ABI method name: fixed to "approve"
                product.uuid};

Wallets.getInstance().callAbiFunctionTransaction(
        wallet.walletId, 
        "financial", //name: fixed to "financial"
        wallet.tokenAddress,
        "", //abiJson: fixed to ""
        args,
        "0", // transactionFee: fixed to "0"
        pinSecret,
        new Callback<CallAbiFunctionResult>() {
        @Override
        public void onError(Throwable error) {
            error.printStackTrace();
        }

        @Override
        public void onResult(CallAbiFunctionResult result) {
            /**
              Keep product.uuid and display activating, because isNeedApprove will not change immediately.
              Call getFinancialProducts() to refresh.
            */
        }
    });
 ```
 [↑ Transaction Operations ↑](#transaction-operations)
#### Deposit
- You can display `minDeposit` and `maxDeposit` as minimum / maximum deposit amount.  
ex.  Min 0.5 HW-ETH - 1000 HW-ETH
- For `FixedDeposit`, you can display estimate reward when editing amount.  
estimate reward = product.ratePercent * amount 
```java
if(!product.isCanDeposit){
  return;
}

// Find wallet by currency and tokenAddress in giving list.
Wallet wallet = findWallet(privateWallets, product.currency, product.tokenAddress);

Object[] args = new Object[]{
                "deposit", // ABI method name: fixed to "deposit"
                product.uuid,
                amount,
                "" // orderId: fixed to ""
              };

Wallets.getInstance().callAbiFunctionTransaction(
        wallet.walletId, 
        "financial", //name: fixed to "financial"
        wallet.tokenAddress,
        "", //abiJson: fixed to ""
        args,
        "0", // transactionFee: fixed to "0"
        pinSecret,
        callback);
```
 [↑ Transaction Operations ↑](#transaction-operations)
#### Withdraw - FixedDeposit
```java
boolean isCanWithdraw = history.isCanWithdraw == null? product.isCanWithdraw: history.isCanWithdraw;
if(!isCanWithdraw){
    return;
}
long msInFuture = getMsInFuture(history.userWaitToWithdraw);
if(msInFuture <= 0){
    return;
}
Object[] args = new Object[]{
        "withdraw", // ABI method name: fixed to "withdraw"
        product.uuid,
        "0", // amount: fixed to "0"
        history.orderId 
};

Wallets.getInstance().callAbiFunctionTransaction(
        wallet.walletId,
        "financial", //name: fixed to "financial"
        wallet.tokenAddress,
        "", //abiJson: fixed to ""
        args,
        "0", // transactionFee: fixed to "0"
        pinSecret,
        callback);
```
 [↑ Transaction Operations ↑](#transaction-operations)
#### Withdraw - DemandDeposit
```java
if(!product.isCanWithdraw){
    return;
}
long msInFuture = getMsInFuture(product.userWaitToWithdraw);
if(msInFuture <= 0){
    return;
}
Object[] args = new Object[]{
        "withdraw", // ABI method name: fixed to "withdraw"
        product.uuid,
        amount,
        "", // orderId: fixed to "" 
};

Wallets.getInstance().callAbiFunctionTransaction(
        wallet.walletId,
        "financial", //name: fixed to "financial"
        wallet.tokenAddress,
        "", //abiJson: fixed to ""
        args,
        "0", // transactionFee: fixed to "0"
        pinSecret,
        callback);
```
 [↑ Transaction Operations ↑](#transaction-operations)
#### earlyWithdraw
```java
boolean isCanEarlyWithdraw = history.isCanEarlyWithdraw == null? product.isCanEarlyWithdraw: history.isCanEarlyWithdraw;
if(!isCanEarlyWithdraw){
    return;
}
long msInFuture = getMsInFuture(history.userWaitToWithdraw);
if(msInFuture <= 0){
    return;
}
Object[] args = new Object[]{
        "earlyWithdraw", // ABI method name: fixed to "earlyWithdraw"
        product.uuid,
        "0", // amount: fixed to "0"
        history.orderId 
};

Wallets.getInstance().callAbiFunctionTransaction(
        wallet.walletId,
        "financial", //name: fixed to "financial"
        wallet.tokenAddress,
        "", //abiJson: fixed to ""
        args,
        "0", // transactionFee: fixed to "0"
        pinSecret,
        callback);
```
 [↑ Transaction Operations ↑](#transaction-operations)
#### withdrawReward
```java
if(!product.isCanWithdrawReward){
    return;
}
long msInFuture = getMsInFuture(product.userWaitToWithdraw);
if(msInFuture <= 0){
    return;
}
Object[] args = new Object[]{
        "withdrawReward", // ABI method name: fixed to "withdrawReward"
        product.uuid,
        "0", // amount: fixed to "0"
        "", // orderId: fixed to "" 
};

Wallets.getInstance().callAbiFunctionTransaction(
        wallet.walletId,
        "financial", //name: fixed to "financial"
        wallet.tokenAddress,
        "", //abiJson: fixed to ""
        args,
        "0", // transactionFee: fixed to "0"
        pinSecret,
        callback);
```
 [↑ Transaction Operations ↑](#transaction-operations)
#### withdrawBonus
```java
if(bonus.isAlreadyWithdrawn){
    return;
}
Object[] args = new Object[]{
        "withdrawBonus", // ABI method name: fixed to "withdrawBonus"
        bouns.uuid,
        "0", // amount: fixed to "0"
};

Wallets.getInstance().callAbiFunctionTransaction(
        wallet.walletId,
        "financial", //name: fixed to "financial"
        wallet.tokenAddress,
        "", //abiJson: fixed to ""
        args,
        "0", // transactionFee: fixed to "0"
        pinSecret,
        callback);
```
 [↑ Transaction Operations ↑](#transaction-operations)

