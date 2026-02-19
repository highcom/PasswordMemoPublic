package com.highcom.passwordmemo.ui

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.autofill.AutofillManager
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.highcom.passwordmemo.databinding.ActivityAutofillInputBinding

class AutofillInputActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAutofillInputBinding

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
}
