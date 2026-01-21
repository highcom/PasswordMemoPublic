package com.highcom.passwordmemo.domain.billing

import android.content.Context
import android.content.SharedPreferences
import com.highcom.passwordmemo.domain.billing.BillingManager.Companion.ALL_PRODUCT_IDS
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 購入状態管理マネージャー
 *
 * @property context アプリケーションコンテキスト
 */
@Singleton
class PurchaseManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val PREFS_NAME = "purchase_prefs"
        private const val KEY_PURCHASE_PREFIX = "purchase_"
        private const val KEY_ADS_REMOVED = "ads_removed"
    }

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * 製品の購入状態を設定
     */
    fun setProductPurchased(productId: String, purchased: Boolean) {
        prefs.edit().putBoolean("$KEY_PURCHASE_PREFIX$productId", purchased).apply()

        // 広告非表示状態の更新
        if (purchased && (BillingManager.SUBSCRIPTION_PRODUCT_IDS.contains(productId) ||
                          productId == BillingManager.PRODUCT_REMOVE_ADS)) {
            setAdsRemoved(true)
        }
    }

    /**
     * 製品が購入済みかどうかを確認
     */
    fun isProductPurchased(productId: String): Boolean {
        return prefs.getBoolean("$KEY_PURCHASE_PREFIX$productId", false)
    }

    /**
     * 広告が非表示状態かどうかを確認
     */
    fun isAdsRemoved(): Boolean {
        // サブスクリプションが有効か、買い切りで広告非表示を購入している場合
        val hasActiveSubscription = BillingManager.SUBSCRIPTION_PRODUCT_IDS.any { isProductPurchased(it) }
        val hasRemoveAdsPurchase = isProductPurchased(BillingManager.PRODUCT_REMOVE_ADS)

        return hasActiveSubscription || hasRemoveAdsPurchase
    }

    /**
     * 広告非表示状態を設定
     */
    private fun setAdsRemoved(removed: Boolean) {
        prefs.edit().putBoolean(KEY_ADS_REMOVED, removed).apply()
    }

    /**
     * サブスクリプションが有効かどうかを確認
     */
    fun hasActiveSubscription(): Boolean {
        return BillingManager.SUBSCRIPTION_PRODUCT_IDS.any { isProductPurchased(it) }
    }

    /**
     * 全ての購入状態をクリア（テスト用）
     */
    fun clearAllPurchases() {
        val editor = prefs.edit()
        ALL_PRODUCT_IDS.forEach { productId ->
            editor.remove("$KEY_PURCHASE_PREFIX$productId")
        }
        editor.remove(KEY_ADS_REMOVED)
        editor.apply()
    }
}