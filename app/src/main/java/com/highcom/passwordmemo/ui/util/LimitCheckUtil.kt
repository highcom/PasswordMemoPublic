package com.highcom.passwordmemo.ui.util

import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.highcom.passwordmemo.R
import com.highcom.passwordmemo.ui.viewmodel.BillingViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 登録上限チェックと上限到達時のダイアログ表示ユーティリティ
 */
object LimitCheckUtil {
    private const val LIMIT = 100

    /**
     * 上限チェックを行い、超過していなければ proceed を実行する。
     * 定期購入会員であれば制限を無視して proceed を実行する。
     * 上限を超過しており定期購入でない場合は、有料会員プラン画面へ遷移するボタンと閉じるボタンを持つダイアログを表示する。
     *
     * @param fragment 呼び出し元フラグメント
     * @param billingViewModel 課金関連のビューモデル
     * @param proceed 呼び出し元から指定した画面遷移処理
     */
    fun checkAndNavigate(fragment: Fragment, billingViewModel: BillingViewModel, proceed: () -> Unit) {
        fragment.lifecycleScope.launch {
            // 全件リストではなく件数のみを問い合わせる（パフォーマンス向上）
            val count = withContext(Dispatchers.IO) { billingViewModel.getPasswordCount() }
            // 定期購入が有効なら制限を無視
            if (billingViewModel.hasActiveSubscription() || count < LIMIT) {
                proceed()
                return@launch
            }

            // 上限に達しているかつ未加入の場合はダイアログを表示
            val ctx = fragment.requireContext()
            val dialog = AlertDialog.Builder(ctx)
                .setTitle(R.string.membership_plan)
                .setMessage(getLimitMessage(ctx, count))
                .setPositiveButton(R.string.membership_plan) { _, _ ->
                    // どのフラグメントからでも遷移できるよう、宛先 ID で直接遷移する
                    val navOptions = NavOptions.Builder()
                        .setEnterAnim(R.anim.slide_in_left)
                        .setExitAnim(R.anim.slide_out_left)
                        .setPopEnterAnim(R.anim.slide_in_right)
                        .setPopExitAnim(R.anim.slide_out_right)
                        .build()
                    fragment.findNavController().navigate(R.id.membershipPlanFragment, null, navOptions)
                }
                .setNegativeButton(R.string.close, null)
                .create()
            dialog.show()
        }
    }

    private fun getLimitMessage(fragmentContext: android.content.Context, count: Int): String {
        // シンプルなメッセージ
        return fragmentContext.getString(R.string.membership_plan_message) + "\n\n" + "${count}/$LIMIT"
    }
}
