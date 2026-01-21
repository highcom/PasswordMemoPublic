package com.highcom.passwordmemo.domain

import android.util.DisplayMetrics
import android.widget.FrameLayout
import androidx.fragment.app.Fragment
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.highcom.passwordmemo.domain.billing.PurchaseManager
import javax.inject.Inject

/**
 * バナー広告
 *
 * @property purchaseManager 購入状態管理マネージャー
 */
class AdBanner @Inject constructor(
    private val purchaseManager: PurchaseManager
) {
    /** 広告ビュー */
    private var mAdView: AdView? = null

    /**
     * バナー広告ロード処理
     *
     * @param fragment 広告表示対象フラグメント
     * @param adContainerView 広告用コンテナ
     * @param unitId 広告ユニットID
     */
    fun loadBanner(fragment: Fragment, adContainerView: FrameLayout?, unitId: String) {
        // 広告が非表示設定の場合は広告を表示しない
        if (purchaseManager.isAdsRemoved()) {
            adContainerView?.removeAllViews()
            return
        }

        // Create an ad request.
        mAdView = AdView(fragment.requireContext())
        mAdView?.adUnitId = unitId
        adContainerView?.removeAllViews()
        adContainerView?.addView(mAdView)
        val adSize = adSize(fragment, adContainerView)
        mAdView?.setAdSize(adSize)
        val adRequest = AdRequest.Builder().build()

        // Start loading the ad in the background.
        mAdView?.loadAd(adRequest)
    }

    /** 広告サイズ設定 */
    @Suppress("DEPRECATION")
    private fun adSize(fragment: Fragment, adContainerView: FrameLayout?): AdSize {
        // Determine the screen width (less decorations) to use for the ad width.
        val display = fragment.requireActivity().windowManager.defaultDisplay
        val outMetrics = DisplayMetrics()
        display.getMetrics(outMetrics)
        val density = outMetrics.density
        var adWidthPixels = adContainerView?.width?.toFloat() ?: 0f

        // If the ad hasn't been laid out, default to the full screen width.
        if (adWidthPixels == 0f) {
            adWidthPixels = outMetrics.widthPixels.toFloat()
        }
        val adWidth = (adWidthPixels / density).toInt()
        return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(fragment.requireContext(), adWidth)
    }

    /**
     * 広告終了処理
     *
     */
    fun destroy() {
        mAdView?.destroy()
    }
}