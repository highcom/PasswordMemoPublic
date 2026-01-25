package com.highcom.passwordmemo.domain.billing

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.*
import com.android.billingclient.api.BillingClient.BillingResponseCode
import com.android.billingclient.api.BillingClient.ProductType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 課金マネージャー
 *
 * @property context コンテキスト
 * @property purchaseListener 購入リスナー
 */
class BillingManager(
    private val context: Context,
    private val purchaseListener: PurchaseListener
) : PurchasesUpdatedListener, BillingClientStateListener {

    private var purchaseManager: PurchaseManager? = null

    interface PurchaseListener {
        fun onPurchaseSuccess(productId: String)
        fun onPurchaseFailed(errorMessage: String)
        fun onRestoreSuccess(productIds: List<String>)
        fun onRestoreFailed(errorMessage: String)
        fun onBillingUnavailable()
    }

    companion object {
        private const val TAG = "BillingManager"

        // 製品ID
        const val PRODUCT_MONTHLY_SUBSCRIPTION = "monthly_subscription"
        const val PRODUCT_HALF_YEAR_SUBSCRIPTION = "half_year_subscription"
        const val PRODUCT_YEARLY_SUBSCRIPTION = "yearly_subscription"
        const val PRODUCT_REMOVE_ADS = "remove_ads_one_time"

        // 全ての製品IDリスト
        val ALL_PRODUCT_IDS = listOf(
            PRODUCT_MONTHLY_SUBSCRIPTION,
            PRODUCT_HALF_YEAR_SUBSCRIPTION,
            PRODUCT_YEARLY_SUBSCRIPTION,
            PRODUCT_REMOVE_ADS
        )

        // サブスクリプション製品IDリスト
        val SUBSCRIPTION_PRODUCT_IDS = listOf(
            PRODUCT_MONTHLY_SUBSCRIPTION,
            PRODUCT_HALF_YEAR_SUBSCRIPTION,
            PRODUCT_YEARLY_SUBSCRIPTION
        )

        // 買い切り製品IDリスト
        val ONE_TIME_PRODUCT_IDS = listOf(PRODUCT_REMOVE_ADS)
    }

    private lateinit var billingClient: BillingClient
    private var isBillingClientReady = false

    init {
        setupBillingClient()
    }

    /**
     * BillingClientのセットアップ
     */
    private fun setupBillingClient() {
        billingClient = BillingClient.newBuilder(context)
            .setListener(this)
            .enablePendingPurchases()
            .build()

        startConnection()
    }

    /**
     * 課金サービスへの接続を開始
     */
    private fun startConnection() {
        billingClient.startConnection(this)
    }

    /**
     * BillingClient接続完了時の処理
     */
    override fun onBillingSetupFinished(billingResult: BillingResult) {
        when (billingResult.responseCode) {
            BillingResponseCode.OK -> {
                Log.d(TAG, "Billing setup successful")
                isBillingClientReady = true
                queryPurchases()

                // PurchaseManagerが設定されている場合は購入状態を確認
                purchaseManager?.let { pm ->
                    checkSubscriptionStatus(pm)
                }
            }
            BillingResponseCode.BILLING_UNAVAILABLE -> {
                Log.e(TAG, "Billing unavailable")
                purchaseListener.onBillingUnavailable()
            }
            else -> {
                Log.e(TAG, "Billing setup failed: ${billingResult.responseCode}")
            }
        }
    }

    /**
     * BillingClient接続失敗時の処理
     */
    override fun onBillingServiceDisconnected() {
        Log.d(TAG, "Billing service disconnected")
        isBillingClientReady = false
        // 再接続を試行
        startConnection()
    }

    /**
     * 購入更新時の処理
     */
    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: MutableList<Purchase>?) {
        when (billingResult.responseCode) {
            BillingResponseCode.OK -> {
                purchases?.forEach { purchase ->
                    handlePurchase(purchase)
                }
            }
            BillingResponseCode.USER_CANCELED -> {
                Log.d(TAG, "User canceled the purchase")
                purchaseListener.onPurchaseFailed("Purchase canceled by user")
            }
            else -> {
                Log.e(TAG, "Purchase failed: ${billingResult.responseCode}")
                purchaseListener.onPurchaseFailed("Purchase failed: ${billingResult.responseCode}")
            }
        }
    }

    /**
     * 購入の処理
     */
    private fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
            // 購入が完了した場合
            if (!purchase.isAcknowledged) {
                acknowledgePurchase(purchase)
            }
            purchaseListener.onPurchaseSuccess(purchase.products[0])
        }
    }

    /**
     * 購入の確認
     */
    private fun acknowledgePurchase(purchase: Purchase) {
        val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()

        billingClient.acknowledgePurchase(acknowledgePurchaseParams) { billingResult ->
            if (billingResult.responseCode == BillingResponseCode.OK) {
                Log.d(TAG, "Purchase acknowledged")
            } else {
                Log.e(TAG, "Failed to acknowledge purchase: ${billingResult.responseCode}")
            }
        }
    }

    /**
     * 製品の購入を開始
     */
    fun purchaseProduct(activity: Activity, productId: String) {
        if (!isBillingClientReady) {
            purchaseListener.onPurchaseFailed("Billing client not ready")
            return
        }

        val productType = if (SUBSCRIPTION_PRODUCT_IDS.contains(productId)) {
            ProductType.SUBS
        } else {
            ProductType.INAPP
        }

        val productList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(productId)
                .setProductType(productType)
                .build()
        )

        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()

        billingClient.queryProductDetailsAsync(params) { billingResult, productDetailsList ->
            if (billingResult.responseCode == BillingResponseCode.OK) {
                val productDetails = productDetailsList?.firstOrNull()
                productDetails?.let {
                    val productDetailsParamsBuilder = BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(it)

                    // サブスクリプションの場合、offerTokenを設定
                    if (SUBSCRIPTION_PRODUCT_IDS.contains(productId)) {
                        val subscriptionOfferDetails = it.subscriptionOfferDetails
                        if (!subscriptionOfferDetails.isNullOrEmpty()) {
                            val firstOffer = subscriptionOfferDetails[0]
                            val offerToken = firstOffer.offerToken
                            productDetailsParamsBuilder.setOfferToken(offerToken)
                        }
                    }

                    val productDetailsParams = productDetailsParamsBuilder.build()

                    val billingFlowParams = BillingFlowParams.newBuilder()
                        .setProductDetailsParamsList(listOf(productDetailsParams))
                        .build()

                    billingClient.launchBillingFlow(activity, billingFlowParams)
                } ?: run {
                    purchaseListener.onPurchaseFailed("Product details not found")
                }
            } else {
                purchaseListener.onPurchaseFailed("Failed to query product details: ${billingResult.responseCode}")
            }
        }
    }

    /**
     * 購入履歴の照会
     */
    private fun queryPurchases() {
        if (!isBillingClientReady) return

        // サブスクリプションの購入履歴を照会
        billingClient.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder()
                .setProductType(ProductType.SUBS)
                .build()
        ) { billingResult, purchases ->
            if (billingResult.responseCode == BillingResponseCode.OK) {
                purchases.forEach { purchase ->
                    if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                        purchaseListener.onPurchaseSuccess(purchase.products[0])
                    }
                }
            }
        }

        // 買い切りの購入履歴を照会
        billingClient.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder()
                .setProductType(ProductType.INAPP)
                .build()
        ) { billingResult, purchases ->
            if (billingResult.responseCode == BillingResponseCode.OK) {
                purchases.forEach { purchase ->
                    if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                        purchaseListener.onPurchaseSuccess(purchase.products[0])
                    }
                }
            }
        }
    }

    /**
     * 購入の復元
     */
    fun restorePurchases() {
        if (!isBillingClientReady) {
            purchaseListener.onRestoreFailed("Billing client not ready")
            return
        }

        val restoredProductIds = mutableListOf<String>()

        // サブスクリプションの復元
        billingClient.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder()
                .setProductType(ProductType.SUBS)
                .build()
        ) { billingResult, purchases ->
            if (billingResult.responseCode == BillingResponseCode.OK) {
                purchases.forEach { purchase ->
                    if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                        restoredProductIds.add(purchase.products[0])
                    }
                }
                checkRestoreComplete(restoredProductIds)
            } else {
                purchaseListener.onRestoreFailed("Failed to query subscriptions")
            }
        }

        // 買い切りの復元
        billingClient.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder()
                .setProductType(ProductType.INAPP)
                .build()
        ) { billingResult, purchases ->
            if (billingResult.responseCode == BillingResponseCode.OK) {
                purchases.forEach { purchase ->
                    if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                        restoredProductIds.add(purchase.products[0])
                    }
                }
                checkRestoreComplete(restoredProductIds)
            } else {
                purchaseListener.onRestoreFailed("Failed to query in-app purchases")
            }
        }
    }

    /**
     * 復元の完了チェック
     */
    private fun checkRestoreComplete(restoredProductIds: List<String>) {
        if (restoredProductIds.isNotEmpty()) {
            purchaseListener.onRestoreSuccess(restoredProductIds)
        } else {
            purchaseListener.onRestoreFailed("No purchases found to restore")
        }
    }

    /**
     * 製品が購入済みかどうかを確認
     */
    fun isProductPurchased(productId: String): Boolean {
        // TODO: SharedPreferencesなどで購入状態を管理する必要がある
        return false
    }

    /**
     * PurchaseManagerを設定
     */
    fun setPurchaseManager(purchaseManager: PurchaseManager) {
        this.purchaseManager = purchaseManager
    }

    /**
     * 定期購入の状態を確認してPurchaseManagerを更新
     */
    fun checkSubscriptionStatus(purchaseManager: PurchaseManager) {
        if (!isBillingClientReady) return

        // サブスクリプションの購入履歴を照会
        billingClient.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder()
                .setProductType(ProductType.SUBS)
                .build()
        ) { billingResult, purchases ->
            if (billingResult.responseCode == BillingResponseCode.OK) {
                // 現在の有効なサブスクリプションを確認
                val activeSubscriptions = purchases.filter { purchase ->
                    purchase.purchaseState == Purchase.PurchaseState.PURCHASED
                }.map { it.products[0] }

                // PurchaseManagerを更新
                SUBSCRIPTION_PRODUCT_IDS.forEach { productId ->
                    val isPurchased = activeSubscriptions.contains(productId)
                    purchaseManager.setProductPurchased(productId, isPurchased)
                }

                // 買い切り製品も確認
                billingClient.queryPurchasesAsync(
                    QueryPurchasesParams.newBuilder()
                        .setProductType(ProductType.INAPP)
                        .build()
                ) { inAppBillingResult, inAppPurchases ->
                    if (inAppBillingResult.responseCode == BillingResponseCode.OK) {
                        ONE_TIME_PRODUCT_IDS.forEach { productId ->
                            val isPurchased = inAppPurchases.any { purchase ->
                                purchase.purchaseState == Purchase.PurchaseState.PURCHASED &&
                                purchase.products.contains(productId)
                            }
                            purchaseManager.setProductPurchased(productId, isPurchased)
                        }
                    }
                }
            }
        }
    }

    /**
     * リソースの解放
     */
    fun destroy() {
        if (isBillingClientReady) {
            billingClient.endConnection()
        }
    }
}