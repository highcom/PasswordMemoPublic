package com.highcom.passwordmemo.ui.extension

import android.view.View
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.google.android.material.snackbar.Snackbar
import com.highcom.passwordmemo.R

/**
 * Snackbar を表示するヘルパー。メッセージとアクションの文字色を常に text_primary_dark に設定します。
 */
fun View.showThemedSnackBar(message: String, length: Int = Snackbar.LENGTH_SHORT) {
    val snack = Snackbar.make(this, message, length)
    val textView = snack.view.findViewById<TextView>(com.google.android.material.R.id.snackbar_text)
    textView?.setTextColor(ContextCompat.getColor(context, R.color.text_primary_dark))
    snack.setActionTextColor(ContextCompat.getColor(context, R.color.text_primary_dark))
    snack.show()
}
