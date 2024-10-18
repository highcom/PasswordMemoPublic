package com.highcom.passwordmemo.ui.fragment

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import com.highcom.passwordmemo.R
import com.highcom.passwordmemo.databinding.AlertGeneratePasswordBinding
import org.apache.commons.lang3.RandomStringUtils
import java.lang.ClassCastException
import java.util.Locale

/**
 * パスワード自動生成ダイアログフラグメント
 *
 */
class GeneratePasswordDialogFragment : DialogFragment() {
    /** パスワード自動生成ダイアログbinding */
    private lateinit var binding: AlertGeneratePasswordBinding
    /** パスワード自動生成種別 */
    private var passwordKind = 0
    /** パスワード自動生成文字数 */
    private var passwordCount = 0
    /** パスワード自動生が成小文字のみかどうか */
    private var isLowerCaseOnly = false
    /** 自動生成パスワード */
    private var generatePassword: String? = null

    /**
     * パスワード自動生成結果リスナー
     *
     */
    interface GeneratePasswordDialogListener {
        /**
         * パスワード自動生成結果通知
         *
         * @param result 自動生成パスワード
         */
        fun onDialogResult(result: String)
    }

    private lateinit var listener: GeneratePasswordDialogListener

    override fun onAttach(context: Context) {
        super.onAttach(context)
        listener = try {
            parentFragment as GeneratePasswordDialogListener
        } catch (e: ClassCastException) {
            throw ClassCastException("$context must implement DialogListener")
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        binding = AlertGeneratePasswordBinding.inflate(layoutInflater)
        // 文字種別ラジオボタンの初期値を設定
        val passwordRadio = binding.passwordKindMenu
        passwordRadio.check(R.id.radio_letters_numbers)
        passwordKind = passwordRadio.checkedRadioButtonId
        passwordRadio.setOnCheckedChangeListener { _, checkedId ->
            passwordKind = checkedId
            generatePasswordString()
        }
        // 小文字のみチェクボックスを設定
        val lowerCaseOnlyCheckBox = binding.lowerCaseOnly
        lowerCaseOnlyCheckBox.isChecked = false
        isLowerCaseOnly = false
        lowerCaseOnlyCheckBox.setOnClickListener {
            isLowerCaseOnly = lowerCaseOnlyCheckBox.isChecked
            if (isLowerCaseOnly) {
                binding.generatePasswordText.setText(generatePassword!!.lowercase(Locale.getDefault()))
            } else {
                binding.generatePasswordText.setText(generatePassword)
            }
        }

        // 文字数ピッカーの初期値を設定
        val passwordPicker = binding.passwordNumberPicker
        passwordPicker.descendantFocusability = ViewGroup.FOCUS_BLOCK_DESCENDANTS
        passwordPicker.maxValue = 32
        passwordPicker.minValue = 4
        passwordPicker.value = 8
        passwordCount = passwordPicker.value
        passwordPicker.setOnValueChangedListener { _, _, newVal ->
            passwordCount = newVal
            generatePasswordString()
        }
        // ボタンのカラーフィルターとイベントを設定
        val generateButton = binding.generateButton
        generateButton.setColorFilter(ContextCompat.getColor(requireContext(), R.color.blue))
        generateButton.setOnClickListener { generatePasswordString() }

        // ダイアログの生成
        val builder = AlertDialog.Builder(requireContext())
            .setTitle(R.string.generate_password_title)
            .setView(binding.root)
            .setPositiveButton(R.string.apply) { _, _ ->
                if (isLowerCaseOnly) {
                    listener.onDialogResult(generatePassword!!.lowercase(Locale.getDefault()))
                } else {
                    listener.onDialogResult(generatePassword!!)
                }
                dismiss()
            }
            .setNegativeButton(R.string.discard) { _, _ ->
                dismiss()
            }
        generatePasswordString()
        return builder.create()
    }

    /**
     * パスワード文字列自動生成処理
     *
     */
    private fun generatePasswordString() {
        generatePassword = when (passwordKind) {
            R.id.radio_numbers -> {
                // 数字のみ
                RandomStringUtils.randomNumeric(passwordCount)
            }
            R.id.radio_letters_numbers -> {
                // 数字＋文字
                RandomStringUtils.randomAlphanumeric(passwordCount)
            }
            else -> {
                // 数字＋文字＋記号
                RandomStringUtils.randomGraph(passwordCount)
            }
        }
        // 小文字のみでの生成かどうか
        if (isLowerCaseOnly) {
            binding.generatePasswordText.setText(generatePassword?.lowercase(Locale.getDefault()))
        } else {
            binding.generatePasswordText.setText(generatePassword)
        }
    }
}