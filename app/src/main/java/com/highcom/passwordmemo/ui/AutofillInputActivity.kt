package com.highcom.passwordmemo.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.autofill.AutofillManager
import android.view.autofill.AutofillValue
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import com.highcom.passwordmemo.R

class AutofillInputActivity : AppCompatActivity() {
    private lateinit var accountEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var urlEditText: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_autofill_input)

        accountEditText = findViewById(R.id.edit_account)
        passwordEditText = findViewById(R.id.edit_password)
        urlEditText = findViewById(R.id.edit_url)

        val extras = intent.extras
        if (extras != null) {
            val autofillValue = extras.getParcelable<AutofillValue>(AutofillManager.EXTRA_AUTHENTICATION_RESULT)
            if (autofillValue != null && autofillValue.isText) {
                val text = autofillValue.textValue.toString()
                if (accountEditText.text.isEmpty()) {
                    accountEditText.setText(text)
                } else if (passwordEditText.text.isEmpty()) {
                    passwordEditText.setText(text)
                }
            }
            val url = extras.getString("url")
            urlEditText.setText(url)
        }

        findViewById<Button>(R.id.ok_button).setOnClickListener {
            val result = Intent()
            setResult(Activity.RESULT_OK, result)
            val autofillManager = getSystemService(AutofillManager::class.java)
            autofillManager?.commit()
            finish()
        }
    }
}
