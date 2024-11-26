package com.highcom.passwordmemo.ui.fragment

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import com.highcom.passwordmemo.R
import com.highcom.passwordmemo.databinding.AlertEditMasterPasswordBinding
import com.highcom.passwordmemo.domain.login.LoginDataManager
import java.lang.ClassCastException

/**
 * マスターパスワード編集ダイアログフラグメント
 *
 * @property loginDataManager
 */
class EditMasterPasswordDialogFragment(private val loginDataManager: LoginDataManager) : DialogFragment() {
    /** マスターパスワード編集ダイアログbinding */
    private lateinit var binding: AlertEditMasterPasswordBinding

    /**
     * マスターパスワード編集結果リスナー
     *
     */
    interface EditMasterPasswordListener {
        /**
         * パスワード変更完了時の通知処理
         *
         */
        fun passwordChangeComplete()
    }

    private lateinit var listener: EditMasterPasswordListener

    override fun onAttach(context: Context) {
        super.onAttach(context)
        listener = try {
            parentFragment as EditMasterPasswordListener
        } catch (e: ClassCastException) {
            throw ClassCastException("$context must implement DialogListener")
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        binding = AlertEditMasterPasswordBinding.inflate(layoutInflater)
        // マスターパスワード編集ダイアログの生成
        val builder = AlertDialog.Builder(requireContext())
            .setTitle(R.string.change_master_password)
            .setView(binding.root)
            .setPositiveButton(R.string.execute, null)
            .setNegativeButton(R.string.discard, null)

        val dialog = builder.create()
        dialog.setOnShowListener {
            val positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            positiveButton.setOnClickListener {
                val orgPassword = loginDataManager.masterPassword
                val newPassword = binding.editNewMasterPassword.text.toString()
                val newPassword2 = binding.editNewMasterPassword2.text.toString()
                if (newPassword != newPassword2) {
                    // 入力内容が異なっていたらエラー
                    binding.inputNavigateText.setText(R.string.input_different_message)
                } else if (newPassword == "") {
                    // 入力内容が空ならエラー
                    binding.inputNavigateText.setText(R.string.nothing_entered_message)
                } else if (newPassword == orgPassword) {
                    // マスターパスワードと同じならエラー
                    binding.inputNavigateText.setText(R.string.password_same_message)
                } else {
                    binding.inputNavigateText.text = " "
                    loginDataManager.setMasterPassword(newPassword)
                    listener.passwordChangeComplete()
                    dismiss()
                }
            }
        }

        return dialog
    }
}