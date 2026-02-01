package com.highcom.passwordmemo.domain

import android.content.Context
import android.util.DisplayMetrics
import android.widget.FrameLayout
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.highcom.passwordmemo.domain.billing.PurchaseManager
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * バナー広告
 *
 * @property purchaseManager 購入状態管理マネージャー
 */
class AdBanner @Inject constructor(
    private val purchaseManager: PurchaseManager
) {
    private var currentAdView: AdView? = null
    private var currentContext: Context? = null
    private var currentAdContainerView: FrameLayout? = null
    private var currentUnitId: String? = null
    /** 広告ビュー */
    private var mAdView: AdView? = null

    /**
     * バナー広告ロード処理
     *
     * @param lifecycleOwner ライフサイクルオーナー
     * @param context コンテキスト
     * @param adContainerView 広告用コンテナ
     * @param unitId 広告ユニットID
     */
    fun loadBanner(lifecycleOwner: LifecycleOwner, context: Context, adContainerView: FrameLayout?, unitId: String) {
        // 現在の情報を保存
        currentContext = context
        currentAdContainerView = adContainerView
        currentUnitId = unitId

        // 広告状態の変更を監視
        lifecycleOwner.lifecycleScope.launch {
            lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                purchaseManager.adsRemovedFlow.collect { adsRemoved ->
                    updateAdVisibility(adsRemoved)
                }
            }
        }

        // 初期表示
        updateAdVisibility(purchaseManager.isAdsRemoved())
    }

    /**
     * 広告の表示・非表示を更新
     */
    private fun updateAdVisibility(adsRemoved: Boolean) {
        val adContainerView = currentAdContainerView ?: return
        val unitId = currentUnitId ?: return
        val context = currentContext ?: return

        if (adsRemoved) {
            // 広告を非表示
            adContainerView.removeAllViews()
            currentAdView?.destroy()
            currentAdView = null
            mAdView?.destroy()
            mAdView = null
        } else {
            // 広告を表示
            if (currentAdView == null) {
                currentAdView = AdView(context)
                currentAdView?.adUnitId = unitId
                adContainerView.removeAllViews()
                adContainerView.addView(currentAdView)
                val adSize = adSize(context, adContainerView) ?: return
                currentAdView?.setAdSize(adSize)
                val adRequest = AdRequest.Builder().build()
                currentAdView?.loadAd(adRequest)
            }
        }
    }

    /** 広告サイズ設定 */
    @Suppress("DEPRECATION")
    private fun adSize(context: Context, adContainerView: FrameLayout?): AdSize? {
        // Determine the screen width (less decorations) to use for the ad width.
        val displayMetrics = context.resources.displayMetrics
        val density = displayMetrics.density
        var adWidthPixels = adContainerView?.width?.toFloat() ?: 0f

        // If the ad hasn't been laid out, default to the full screen width.
        if (adWidthPixels == 0f) {
            adWidthPixels = displayMetrics.widthPixels.toFloat()
        }
        val adWidth = (adWidthPixels / density).toInt()
        return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, adWidth)
    }

    /**
     * 広告終了処理
     *
     */
    fun destroy() {
        mAdView?.destroy()
    }
}
