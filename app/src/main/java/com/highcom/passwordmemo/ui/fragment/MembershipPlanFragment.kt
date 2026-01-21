package com.highcom.passwordmemo.ui.fragment

import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.highcom.passwordmemo.PasswordMemoDrawerActivity
import com.highcom.passwordmemo.R
import com.highcom.passwordmemo.databinding.FragmentMembershipPlanBinding
import com.highcom.passwordmemo.domain.billing.BillingManager
import com.highcom.passwordmemo.domain.billing.PurchaseManager
import com.highcom.passwordmemo.domain.login.LoginDataManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * 有料会員プラン画面フラグメント
 *
 */
@AndroidEntryPoint
class MembershipPlanFragment : Fragment(), BillingManager.PurchaseListener {
    /** 有料会員プラン画面のbinding */
    private lateinit var binding: FragmentMembershipPlanBinding
    /** ログインデータ管理 */
    @Inject
    lateinit var loginDataManager: LoginDataManager
    /** 課金マネージャー */
    private lateinit var billingManager: BillingManager
    /** 購入状態管理マネージャー */
    @Inject
    lateinit var purchaseManager: PurchaseManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Fragmentのメニューを有効にする
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentMembershipPlanBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        requireActivity().title = getString(R.string.membership_plan_title)

        // ActionBarに戻るボタンを設定
        val activity = requireActivity()
        if (activity is PasswordMemoDrawerActivity) {
            activity.drawerMenuDisabled()
            activity.toggle.setToolbarNavigationClickListener {
                findNavController().navigate(MembershipPlanFragmentDirections.actionMembershipPlanFragmentToSettingFragment())
            }
        }

        // 背景色を設定する
        binding.membershipPlanView.setBackgroundColor(
            loginDataManager.backgroundColor
        )

        // BillingManagerの初期化
        billingManager = BillingManager(requireContext(), this)

        // 月額プランボタン処理
        binding.monthlySubscriptionButton.setOnClickListener {
            if (!purchaseManager.hasActiveSubscription()) {
                billingManager.purchaseProduct(requireActivity(), BillingManager.PRODUCT_MONTHLY_SUBSCRIPTION)
            } else {
                showToast(getString(R.string.already_purchased))
            }
        }

        // 半年プランボタン処理
        binding.halfYearSubscriptionButton.setOnClickListener {
            if (!purchaseManager.hasActiveSubscription()) {
                billingManager.purchaseProduct(requireActivity(), BillingManager.PRODUCT_HALF_YEAR_SUBSCRIPTION)
            } else {
                showToast(getString(R.string.already_purchased))
            }
        }

        // 年額プランボタン処理
        binding.yearlySubscriptionButton.setOnClickListener {
            if (!purchaseManager.hasActiveSubscription()) {
                billingManager.purchaseProduct(requireActivity(), BillingManager.PRODUCT_YEARLY_SUBSCRIPTION)
            } else {
                showToast(getString(R.string.already_purchased))
            }
        }

        // 広告非表示買い切りボタン処理
        binding.removeAdsButton.setOnClickListener {
            if (!purchaseManager.isProductPurchased(BillingManager.PRODUCT_REMOVE_ADS)) {
                billingManager.purchaseProduct(requireActivity(), BillingManager.PRODUCT_REMOVE_ADS)
            } else {
                showToast(getString(R.string.already_purchased))
            }
        }

        // 購入復元ボタン処理
        binding.restorePurchaseButton.setOnClickListener {
            billingManager.restorePurchases()
        }

        // ボタンの初期状態を設定
        updateButtonStates()

        // テキストサイズを設定する
        setTextSize(loginDataManager.textSize)
    }

    override fun onDestroy() {
        super.onDestroy()
        billingManager.destroy()
    }

    /**
     * ボタンの状態を更新
     */
    private fun updateButtonStates() {
        val hasSubscription = purchaseManager.hasActiveSubscription()
        val hasRemoveAds = purchaseManager.isProductPurchased(BillingManager.PRODUCT_REMOVE_ADS)

        // サブスクリプションボタンはサブスクリプションがない場合のみ有効
        binding.monthlySubscriptionButton.isEnabled = !hasSubscription
        binding.halfYearSubscriptionButton.isEnabled = !hasSubscription
        binding.yearlySubscriptionButton.isEnabled = !hasSubscription

        // 広告非表示ボタンは未購入の場合のみ有効
        binding.removeAdsButton.isEnabled = !hasRemoveAds

        // 購入済みのボタンのテキストを変更
        if (hasSubscription) {
            binding.monthlySubscriptionButton.text = getString(R.string.already_purchased)
            binding.halfYearSubscriptionButton.text = getString(R.string.already_purchased)
            binding.yearlySubscriptionButton.text = getString(R.string.already_purchased)
        }

        if (hasRemoveAds) {
            binding.removeAdsButton.text = getString(R.string.already_purchased)
        }
    }

    /**
     * Toastを表示
     */
    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    /**
     * テキストサイズ設定処理
     *
     * @param size 指定テキストサイズ
     */
    private fun setTextSize(size: Float) {
        // 月額プラン
        binding.textMonthlySubscriptionTitle.setTextSize(TypedValue.COMPLEX_UNIT_DIP, size)
        binding.monthlySubscriptionButton.setTextSize(TypedValue.COMPLEX_UNIT_DIP, size - 3)

        // 半年プラン
        binding.textHalfYearSubscriptionTitle.setTextSize(TypedValue.COMPLEX_UNIT_DIP, size)
        binding.halfYearSubscriptionButton.setTextSize(TypedValue.COMPLEX_UNIT_DIP, size - 3)

        // 年額プラン
        binding.textYearlySubscriptionTitle.setTextSize(TypedValue.COMPLEX_UNIT_DIP, size)
        binding.yearlySubscriptionButton.setTextSize(TypedValue.COMPLEX_UNIT_DIP, size - 3)

        // 広告非表示買い切り
        binding.textRemoveAdsTitle.setTextSize(TypedValue.COMPLEX_UNIT_DIP, size)
        binding.removeAdsButton.setTextSize(TypedValue.COMPLEX_UNIT_DIP, size - 3)

        // 購入復元
        binding.textRestorePurchaseTitle.setTextSize(TypedValue.COMPLEX_UNIT_DIP, size)
        binding.restorePurchaseButton.setTextSize(TypedValue.COMPLEX_UNIT_DIP, size - 3)
    }

    // BillingManager.PurchaseListener implementation

    override fun onPurchaseSuccess(productId: String) {
        purchaseManager.setProductPurchased(productId, true)
        updateButtonStates()
        showToast(getString(R.string.purchase_success))

        // 広告非表示状態になった場合はActivityを再作成して広告を非表示にする
        if (purchaseManager.isAdsRemoved()) {
            restartActivity()
        }
    }

    override fun onPurchaseFailed(errorMessage: String) {
        showToast(getString(R.string.purchase_failed))
    }

    override fun onRestoreSuccess(productIds: List<String>) {
        productIds.forEach { productId ->
            purchaseManager.setProductPurchased(productId, true)
        }
        updateButtonStates()
        showToast(getString(R.string.restore_success))

        // 広告非表示状態になった場合はActivityを再作成して広告を非表示にする
        if (purchaseManager.isAdsRemoved()) {
            restartActivity()
        }
    }

    override fun onRestoreFailed(errorMessage: String) {
        showToast(getString(R.string.restore_failed))
    }

    override fun onBillingUnavailable() {
        showToast(getString(R.string.billing_unavailable))
    }

    /**
     * アプリ再起動処理
     */
    private fun restartActivity() {
        // 購入状態が変更された場合、Activityを再作成して広告の状態を更新する
        val intent = requireActivity().intent
        requireActivity().finish()
        requireActivity().startActivity(intent)
    }
}