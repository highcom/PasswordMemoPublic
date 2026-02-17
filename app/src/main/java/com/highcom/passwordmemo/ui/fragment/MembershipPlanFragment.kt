package com.highcom.passwordmemo.ui.fragment

import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.snackbar.Snackbar
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.highcom.passwordmemo.PasswordMemoDrawerActivity
import com.highcom.passwordmemo.R
import com.highcom.passwordmemo.databinding.FragmentMembershipPlanBinding
import com.highcom.passwordmemo.domain.DarkModeUtil
import com.highcom.passwordmemo.domain.billing.BillingManager
import com.highcom.passwordmemo.domain.billing.PurchaseManager
import com.highcom.passwordmemo.domain.login.LoginDataManager
import com.highcom.passwordmemo.ui.viewmodel.BillingViewModel
import com.highcom.passwordmemo.ui.viewmodel.PurchaseEvent
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject
import android.widget.TextView
import androidx.core.content.ContextCompat

/**
 * 有料会員プラン画面フラグメント
 *
 */
@AndroidEntryPoint
class MembershipPlanFragment : Fragment() {
    /** 有料会員プラン画面のbinding */
    private lateinit var binding: FragmentMembershipPlanBinding
    /** ログインデータ管理 */
    @Inject
    lateinit var loginDataManager: LoginDataManager
    /** 課金ビューモデル */
    private val billingViewModel: BillingViewModel by activityViewModels()
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

        // 背景色を設定する（ダークモード時はテーマの色を優先）
        if (!DarkModeUtil.isDarkModeEnabled(requireContext(), loginDataManager.darkMode)) {
            binding.membershipPlanView.setBackgroundColor(loginDataManager.backgroundColor)
        }

        // BillingViewModelの初期化
        billingViewModel.initializeBillingManager()

        // 広告非表示ボタンの個別状態を監視
        lifecycleScope.launch {
            purchaseManager.removeAdsPurchasedFlow.collect { purchased ->
                updateRemoveAdsButton(purchased)
            }
        }

        // 購入イベントを監視
        lifecycleScope.launch {
            billingViewModel.purchaseEventFlow.collect { event ->
                handlePurchaseEvent(event)
            }
        }

        // 月額プランボタン処理
        binding.monthlySubscriptionButton.setOnClickListener {
            if (!billingViewModel.hasActiveSubscription()) {
                billingViewModel.purchaseProduct(requireActivity(), BillingManager.PRODUCT_MONTHLY_SUBSCRIPTION)
            } else {
                showSnackBar(getString(R.string.already_purchased))
            }
        }

        // 半年プランボタン処理
        binding.halfYearSubscriptionButton.setOnClickListener {
            if (!billingViewModel.hasActiveSubscription()) {
                billingViewModel.purchaseProduct(requireActivity(), BillingManager.PRODUCT_HALF_YEAR_SUBSCRIPTION)
            } else {
                showSnackBar(getString(R.string.already_purchased))
            }
        }

        // 年額プランボタン処理
        binding.yearlySubscriptionButton.setOnClickListener {
            if (!billingViewModel.hasActiveSubscription()) {
                billingViewModel.purchaseProduct(requireActivity(), BillingManager.PRODUCT_YEARLY_SUBSCRIPTION)
            } else {
                showSnackBar(getString(R.string.already_purchased))
            }
        }

        // 広告非表示買い切りボタン処理
        binding.removeAdsButton.setOnClickListener {
            if (!billingViewModel.isProductPurchased(BillingManager.PRODUCT_REMOVE_ADS)) {
                billingViewModel.purchaseProduct(requireActivity(), BillingManager.PRODUCT_REMOVE_ADS)
            } else {
                showSnackBar(getString(R.string.already_purchased))
            }
        }

        // 購入復元ボタン処理
        binding.restorePurchaseButton.setOnClickListener {
            billingViewModel.restorePurchases()
        }

        // 有料会員についてボタン処理
        binding.aboutMembershipButton.setOnClickListener {
            showAboutMembershipDialog()
        }

        // ボタンの初期状態を設定
        updateButtonStates()

        // テキストサイズを設定する
        setTextSize(loginDataManager.textSize)
    }


    /**
     * 月額プラン購入ボタンの状態を更新
     */
    private fun updateMonthlySubscriptionButton(purchased: Boolean) {
        val hasActiveSubscription = billingViewModel.hasActiveSubscription()
        binding.monthlySubscriptionButton.apply {
            isEnabled = !hasActiveSubscription
            text = if (hasActiveSubscription) getString(R.string.already_purchased) else getString(R.string.purchase)
        }
    }

    /**
     * 半年プラン購入ボタンの状態を更新
     */
    private fun updateHalfYearSubscriptionButton(purchased: Boolean) {
        val hasActiveSubscription = billingViewModel.hasActiveSubscription()
        binding.halfYearSubscriptionButton.apply {
            isEnabled = !hasActiveSubscription
            text = if (hasActiveSubscription) getString(R.string.already_purchased) else getString(R.string.purchase)
        }
    }

    /**
     * 年額プラン購入ボタンの状態を更新
     */
    private fun updateYearlySubscriptionButton(purchased: Boolean) {
        val hasActiveSubscription = billingViewModel.hasActiveSubscription()
        binding.yearlySubscriptionButton.apply {
            isEnabled = !hasActiveSubscription
            text = if (hasActiveSubscription) getString(R.string.already_purchased) else getString(R.string.purchase)
        }
    }

    /**
     * 広告非表示ボタンの状態を更新
     */
    private fun updateRemoveAdsButton(purchased: Boolean) {
        binding.removeAdsButton.apply {
            isEnabled = !purchased
            text = if (purchased) getString(R.string.already_purchased) else getString(R.string.purchase)
        }
    }

    /**
     * ボタンの状態を更新（全体）
     */
    private fun updateButtonStates() {
        val hasActiveSubscription = billingViewModel.hasActiveSubscription()
        updateMonthlySubscriptionButton(hasActiveSubscription)
        updateHalfYearSubscriptionButton(hasActiveSubscription)
        updateYearlySubscriptionButton(hasActiveSubscription)
        updateRemoveAdsButton(purchaseManager.isProductPurchased(BillingManager.PRODUCT_REMOVE_ADS))
    }

    /**
     * SnackBarを表示
     */
    private fun showSnackBar(message: String) {
        val snack = Snackbar.make(requireView(), message, Snackbar.LENGTH_SHORT)
        // Snackbar のメッセージテキストの TextView を取得して色を強制
        val textView = snack.view.findViewById<TextView>(R.id.snackbar_text)
        textView?.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary_dark))
        // アクションテキストも同じ色にしておく
        snack.setActionTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary_dark))
        snack.show()
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

        // 有料会員について
        binding.textAboutMembershipTitle.setTextSize(TypedValue.COMPLEX_UNIT_DIP, size)
        binding.aboutMembershipButton.setTextSize(TypedValue.COMPLEX_UNIT_DIP, size - 3)
    }

    /**
     * 購入イベントを処理
     */
    private fun handlePurchaseEvent(event: PurchaseEvent) {
        when (event) {
            is PurchaseEvent.PurchaseSuccess -> {
                updateButtonStates()
                showSnackBar(getString(R.string.purchase_success))
            }
            is PurchaseEvent.PurchaseFailed -> {
                showSnackBar(getString(R.string.purchase_failed))
            }
            is PurchaseEvent.RestoreSuccess -> {
                updateButtonStates()
                showSnackBar(getString(R.string.restore_success))
            }
            is PurchaseEvent.RestoreFailed -> {
                showSnackBar(getString(R.string.restore_failed))
            }
            is PurchaseEvent.BillingUnavailable -> {
                showSnackBar(getString(R.string.billing_unavailable))
            }
        }
    }

    /**
     * 有料会員プランについての説明
     */
    private fun showAboutMembershipDialog() {
        val dialogView = layoutInflater.inflate(R.layout.alert_about_membership, null)
        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.membership_plan_dialog_title))
            .setView(dialogView)
            .setPositiveButton(R.string.ok, null)
            .show()
    }
}
