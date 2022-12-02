# NFT

- Bookmarks
  - [NFT Wallet](#nft-wallet-creation)
  - [NFT Balance](#balance)
  - [Deposit](#deposit)
  - [Withdraw](#withdraw)
  - [Transaction Detail](#transaction-detail)
  - [Specific Usage](#specific-usage)
    - [Solana NFT Tokens](#solana-nft-tokens)
    - [Withdrawing Solana NFT Tokens](#withdrawing-solana-nft-tokens)

## NFT Wallet Creation

- Suppose you'd like to receive some NFT tokens with Wallet SDK, but there's no that kind of `Currency` in the currency list, you can add NFT currency by calling `addContractCurrency`.  
If that kind of `Currency` already exists, there's no need to add it again.

```java
/// [NFT] Add a new token & create first wallet
/// - Parameters:
///   - currency: Currency of desired new wallet
///   - contractAddress: Token address for tokens, i.e. an ERC-20 token wallet maps to an Ethereum wallet
///   - pinSecret: PIN secret retrieved via PinCodeInputView
///   - callback: asynchronous callback of WalletID
public abstract void addContractCurrency(long currency, String contractAddress, PinSecret pinSecret, Callback<AddContractCurrenciesResult> callback);

/// Batch version of addContractCurrency
public abstract void addContractCurrencies(long[] currency, String[] contractAddresses, PinSecret pinSecret, Callback<AddContractCurrenciesResult> callback);
```

- How to get a contract address?  
You can find it on blockchain explorer.  
Take CryptoKitties for example, you can find its contract address on Etherscan

  ![img](images/sdk_guideline/nft_etherscan_1.png)

## NFT Wallet List

![img](images/sdk_guideline/nft_wallets.jpg)

- Same way as we mentioned in [Wallet Information](wallets.md#wallet-information)
- Conditions:
  - `Wallet.isPrivate == false` ➜ it is on public chain
  - `TextUtils.isEmpty(tokenAddress) == false` ➜ it is a mapped wallet (NFT wallet is also mapped wallet)
  - `Currency.tokenVersion == 721 || 1155` ➜ it is an NFT wallet

## Balance

Refer to [Balance](wallets.md#getbalances)

```java
public final class Balance {

    public String[] tokens = {}; /** Non-Fungible Token IDs for ERC-721*/

    public TokenIdAmount[] tokenIdAmounts = {}; /** Non-Fungible Token ID and amounts for ERC-1155 */

    ...
}
```

- For ERC-721 (NFT), use `tokens`
- For ERC-1155 (NFT), use `tokenIdAmounts`
- For Solana, see [Solana NFT Tokens](#solana-nft-tokens)

- In order to present images, call `getMultipleTokenUri` to get token urls.
  
  ```java
  /// Get NFT Token URI
  /// - Parameters:
  ///   - currency: Currency of token to query
  ///   - tokenAddresses: Array of token address to query
  ///   - tokenIds: Array of token address to query
  ///   - callback: asynchronous callback of Map<String, TokenUri>
  public abstract void getMultipleTokenUri(long currency, String[] tokenAddresses, String[]  tokenIds, Callback<GetMultipleTokenUriResult> callback);
  ```

### Error Handling

- For ERC-1155

  ```java
  /// If ERC-1155 token didn't show in wallet's balance, register token ID manually make them in track
  /// - Parameters:
  ///   - walletId: walletId Wallet ID
  ///   - tokenIds: ERC-1155 token IDs for register
  ///   - callback: asynchronous callback
  public abstract void registerTokenIds(long walletId, String[] tokenIds, Callback<RegisterTokenIdsResult> callback);
  ```

## Deposit

- Select a wallet address, create a new one if needed.
- Generate QR code
- Present the QR code for deposit.

## Withdraw

- The steps are similar to normal transactions. Refer to [Withdraw](transaction.md#withdraw)
- when `createTransaction()`
  - For [EIP-721](https://eips.ethereum.org/EIPS/eip-721) , set parameter `amount = tokenId`
  - For [EIP-1155](https://eips.ethereum.org/EIPS/eip-1155) , set parameter `amount = tokenIdAmount` and `extras.put("token_id", tokenId)`
  - For Solana, see [Withdrawing Solana NFT Tokens](#withdrawing-solana-nft-tokens)

## Transaction Detail

- The steps are similar to normal transactions. Refer to [getHistory](transaction.md#gethistory)

## Specific Usage
- There are specific API usages for some scenarios which related to NFT, you can find them in this section.

### Solana NFT Tokens
- For retriving Solana NFT tokens, please use `getSolNftTokens()`.
```java
Wallets.getInstance().getSolNftTokens(walletId, new Callback<GetSolNftTokensResult>() {
                @Override
                public void onError(Throwable error) {
                    error.printStackTrace();
                }

                @Override
                public void onResult(GetSolNftTokensResult result) {
                    for(TokenMeta tokenMeta: result.tokens){
                        // ex. tokenAddress: E3LybqvWfLus2KWyrYKYieLVeT6ENpE4znqkMZ9CTrPH, balance: 17, supply: 100, tokenStandard: Unknown
                        Log.d(TAG, String.format("tokenAddress: %s, balance: %s, supply: %s, tokenStandard: %s",
                                tokenMeta.tokenAddress, tokenMeta.balance, tokenMeta.supply, tokenMeta.tokenStandard));
                    }
                }
            });
```
### Withdrawing Solana NFT Tokens
- For withdrawing Solana NFT tokens, put the selected `TokenMeta.tokenAddress` in extras `sol_token_id` then pass to `createTransaction()`.
```java
Map<String, Object> extras = new HashMap<>();
extras.put("sol_token_id", selectedToken.tokenAddress);
Wallets.getInstance().createTransaction(wallet.walletId, toAddress, transactionAmount, fee, desc, pinSecret, extras, callback);
```
