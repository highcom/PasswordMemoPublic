package com.highcom.passwordmemo.util

import android.util.DisplayMetrics
import android.widget.FrameLayout
import androidx.fragment.app.Fragment
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView

/**
 * バナー広告
 *
 * @property fragment 広告表示対象フラグメント
 * @property adContainerView 広告用コンテナ
 */
class AdBanner(private val fragment: Fragment, private val adContainerView: FrameLayout?) {
    /** 広告ビュー */
    private var mAdView: AdView? = null

    /**
     * バナー広告ロード処理
     *
     */
    fun loadBanner(unitId: String) {
        // Create an ad request.
        mAdView = AdView(fragment.requireContext())
        mAdView?.adUnitId = unitId
        adContainerView?.removeAllViews()
        adContainerView?.addView(mAdView)
        val adSize = adSize
        mAdView?.setAdSize(adSize)
        val adRequest = AdRequest.Builder().build()

        // Start loading the ad in the background.
        mAdView?.loadAd(adRequest)
    }

    /** 広告サイズ設定 */
    @Suppress("DEPRECATION")
    private val adSize: AdSize
        get() {
            // Determine the screen width (less decorations) to use for the ad width.
            val display = fragment.requireActivity().windowManager.defaultDisplay
            val outMetrics = DisplayMetrics()
            display.getMetrics(outMetrics)
            val density = outMetrics.density
            var adWidthPixels = adContainerView!!.width.toFloat()

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