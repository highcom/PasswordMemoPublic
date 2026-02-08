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
 * バナー広告を管理するクラス
 *
 * @property purchaseManager 購入状態を管理するマネージャー
 */
class AdBanner @Inject constructor(
    private val purchaseManager: PurchaseManager
) {
    /**
     * バナー広告を読み込んで表示する。
     * ライフサイクルを監視し、課金状態に応じて広告の表示・非表示を自動で切り替える。
     *
     * @param lifecycleOwner 広告のライフサイクルを監視するためのLifecycleOwner
     * @param context Context
     * @param adContainerView 広告を表示するFrameLayout
     * @param unitId 広告ユニットID
     */
    fun loadBanner(
        lifecycleOwner: LifecycleOwner,
        context: Context,
        adContainerView: FrameLayout,
        unitId: String
    ) {
        var adView: AdView? = null

        lifecycleOwner.lifecycleScope.launch {
            lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                purchaseManager.adsRemovedFlow.collect { adsRemoved ->
                    if (adsRemoved) {
                        adView?.destroy()
                        adContainerView.removeAllViews()
                        adView = null
                    } else {
                        if (adView == null) {
                            adView = AdView(context).apply {
                                adUnitId = unitId
                                setAdSize(adSize(context, adContainerView))
                                adContainerView.addView(this)
                                loadAd(AdRequest.Builder().build())
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * デバイスの画面幅に基づいて、アダプティブバナーの広告サイズを計算する。
     *
     * @param context Context
     * @param adContainerView 広告コンテナビュー
     * @return 計算されたAdSize
     */
    @Suppress("DEPRECATION")
    private fun adSize(context: Context, adContainerView: FrameLayout): AdSize {
        val displayMetrics = context.resources.displayMetrics
        val density = displayMetrics.density
        var adWidthPixels = adContainerView.width.toFloat()
        if (adWidthPixels == 0f) {
            adWidthPixels = displayMetrics.widthPixels.toFloat()
        }
        val adWidth = (adWidthPixels / density).toInt()
        return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, adWidth)
    }

    /**
     * 指定されたコンテナ内の広告ビューを破棄し、ビューをすべて削除する。
     *
     * @param adContainerView 広告が配置されているFrameLayout
     */
    fun destroyAd(adContainerView: FrameLayout?) {
        adContainerView?.let {
            val adView = it.getChildAt(0) as? AdView
            adView?.destroy()
            it.removeAllViews()
        }
    }
}
