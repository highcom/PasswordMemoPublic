package com.highcom.passwordmemo.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.autofill.AutofillManager
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.highcom.passwordmemo.data.PasswordEntity
import com.highcom.passwordmemo.databinding.ActivityAutofillInputBinding
import com.highcom.passwordmemo.ui.viewmodel.AutofillInputViewModel
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.Date

@AndroidEntryPoint
class AutofillInputActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAutofillInputBinding
    private val viewModel: AutofillInputViewModel by viewModels()

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAutofillInputBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val extras = intent.extras
        if (extras != null) {
            val url = extras.getString("url")
            val account = extras.getString("account")
            val password = extras.getString("password")
            binding.editTitle.setText(url)
            binding.editUrl.setText(url)
            binding.editAccount.setText(account)
            binding.editPassword.setText(password)
        }

        binding.okButton.setOnClickListener {
            val title = binding.editTitle.text.toString()
            val account = binding.editAccount.text.toString()
            val password = binding.editPassword.text.toString()
            val url = binding.editUrl.text.toString()
            val memo = binding.editMemo.text.toString()
            val entity = PasswordEntity(
                id = 0,
                title = title,
                account = account,
                password = password,
                url = url,
                groupId = 1,
                memo = memo,
                inputDate = nowDate,
                color = 0
            )

            viewModel.insert(entity)

            val result = Intent()
            setResult(RESULT_OK, result)
            val autofillManager = getSystemService(AutofillManager::class.java)
            autofillManager?.commit()
            finish()
        }

        binding.cancelButton.setOnClickListener {
            setResult(RESULT_CANCELED)
            finish()
        }
    }

    /** 現在日付 */
    private val nowDate: String
        // 現在の日付取得処理
        @SuppressLint("SimpleDateFormat")
        get() {
            val date = Date()
            val sdf = SimpleDateFormat("yyyy/MM/dd")
            return sdf.format(date)
        }

}
