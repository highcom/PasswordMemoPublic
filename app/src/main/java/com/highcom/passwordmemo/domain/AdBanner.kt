package com.highcom.passwordmemo.domain

import android.util.DisplayMetrics
import android.widget.FrameLayout
import androidx.fragment.app.Fragment
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
    private var currentFragment: Fragment? = null
    private var currentAdContainerView: FrameLayout? = null
    private var currentUnitId: String? = null
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
        // 現在の情報を保存
        currentFragment = fragment
        currentAdContainerView = adContainerView
        currentUnitId = unitId

        // 広告状態の変更を監視
        if (fragment is LifecycleOwner) {
            fragment.lifecycleScope.launch {
                fragment.repeatOnLifecycle(Lifecycle.State.STARTED) {
                    purchaseManager.adsRemovedFlow.collect { adsRemoved ->
                        updateAdVisibility(adsRemoved)
                    }
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
        val fragment = currentFragment ?: return
        val adContainerView = currentAdContainerView ?: return
        val unitId = currentUnitId ?: return

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
                currentAdView = AdView(fragment.requireContext())
                currentAdView?.adUnitId = unitId
                adContainerView.removeAllViews()
                adContainerView.addView(currentAdView)
                val adSize = adSize(fragment, adContainerView)
                currentAdView?.setAdSize(adSize)
                val adRequest = AdRequest.Builder().build()
                currentAdView?.loadAd(adRequest)
            }
        }
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