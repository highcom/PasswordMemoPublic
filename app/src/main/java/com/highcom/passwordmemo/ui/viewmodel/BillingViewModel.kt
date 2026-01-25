package com.highcom.passwordmemo.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.highcom.passwordmemo.domain.billing.BillingManager
import com.highcom.passwordmemo.domain.billing.PurchaseManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 購入イベントの種類
 */
sealed class PurchaseEvent {
    data class PurchaseSuccess(val productId: String) : PurchaseEvent()
    data class PurchaseFailed(val errorMessage: String) : PurchaseEvent()
    data class RestoreSuccess(val productIds: List<String>) : PurchaseEvent()
    data class RestoreFailed(val errorMessage: String) : PurchaseEvent()
    object BillingUnavailable : PurchaseEvent()
}

/**
 * 課金関連のビューモデル
 */
@HiltViewModel
class BillingViewModel @Inject constructor(
    application: Application,
    private val purchaseManager: PurchaseManager
) : AndroidViewModel(application) {

    // BillingManager
    private lateinit var billingManager: BillingManager

    // 広告表示状態のFlow
    private val _adsRemovedFlow = MutableSharedFlow<Boolean>(replay = 1)
    val adsRemovedFlow: SharedFlow<Boolean> = _adsRemovedFlow.asSharedFlow()

    // 購入イベントのFlow
    private val _purchaseEventFlow = MutableSharedFlow<PurchaseEvent>()
    val purchaseEventFlow: SharedFlow<PurchaseEvent> = _purchaseEventFlow.asSharedFlow()

    // BillingManagerの初期化
    fun initializeBillingManager() {
        if (!::billingManager.isInitialized) {
            // 初期値をemit
            _adsRemovedFlow.tryEmit(purchaseManager.isAdsRemoved())
            billingManager = BillingManager(getApplication(), object : BillingManager.PurchaseListener {
                override fun onPurchaseSuccess(productId: String) {
                    purchaseManager.setProductPurchased(productId, true)
                    updateAdsRemovedFlow()
                    viewModelScope.launch {
                        _purchaseEventFlow.emit(PurchaseEvent.PurchaseSuccess(productId))
                    }
                }

                override fun onPurchaseFailed(errorMessage: String) {
                    viewModelScope.launch {
                        _purchaseEventFlow.emit(PurchaseEvent.PurchaseFailed(errorMessage))
                    }
                }

                override fun onRestoreSuccess(productIds: List<String>) {
                    productIds.forEach { productId ->
                        purchaseManager.setProductPurchased(productId, true)
                    }
                    updateAdsRemovedFlow()
                    viewModelScope.launch {
                        _purchaseEventFlow.emit(PurchaseEvent.RestoreSuccess(productIds))
                    }
                }

                override fun onRestoreFailed(errorMessage: String) {
                    viewModelScope.launch {
                        _purchaseEventFlow.emit(PurchaseEvent.RestoreFailed(errorMessage))
                    }
                }

                override fun onBillingUnavailable() {
                    viewModelScope.launch {
                        _purchaseEventFlow.emit(PurchaseEvent.BillingUnavailable)
                    }
                }
            })

            // PurchaseManagerを設定
            billingManager.setPurchaseManager(purchaseManager)
        }
    }

    /**
     * 定期購入の状態を確認
     */
    private fun checkSubscriptionStatus() {
        billingManager.checkSubscriptionStatus(purchaseManager)
    }

    /**
     * 製品の購入を開始
     */
    fun purchaseProduct(activity: android.app.Activity, productId: String) {
        billingManager.purchaseProduct(activity, productId)
    }

    /**
     * 購入の復元
     */
    fun restorePurchases() {
        billingManager.restorePurchases()
    }

    /**
     * BillingManagerを取得
     */
    fun getBillingManager(): BillingManager {
        return billingManager
    }

    /**
     * 広告表示状態のFlowを更新
     */
    private fun updateAdsRemovedFlow() {
        _adsRemovedFlow.tryEmit(purchaseManager.isAdsRemoved())
    }

    /**
     * 製品が購入済みかどうかを確認
     */
    fun isProductPurchased(productId: String): Boolean {
        return purchaseManager.isProductPurchased(productId)
    }

    /**
     * アクティブなサブスクリプションがあるかどうかを確認
     */
    fun hasActiveSubscription(): Boolean {
        return purchaseManager.hasActiveSubscription()
    }

    override fun onCleared() {
        super.onCleared()
        if (::billingManager.isInitialized) {
            billingManager.destroy()
        }
    }
}