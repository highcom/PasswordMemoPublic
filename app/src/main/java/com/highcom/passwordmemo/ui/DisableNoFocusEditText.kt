package com.highcom.passwordmemo.ui

import android.content.Context
import android.util.AttributeSet
import android.view.KeyEvent
import androidx.appcompat.widget.AppCompatEditText

/**
 * テキスト編集ビューのカスタムクラス
 * * フォーカスされていない時のテキスト編集を無効にする
 *
 */
class DisableNoFocusEditText : AppCompatEditText {
    constructor(context: Context?) : super(context!!)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(
        context: Context, attrs: AttributeSet?, defStyleAttr: Int
    ) : super(context, attrs, defStyleAttr)

    /**
     * キーイベントクラスのオーバーライド
     *
     * @param keyCode キーコード
     * @param event キーイベント
     * @return キーイベント
     */
    override fun onKeyPreIme(keyCode: Int, event: KeyEvent): Boolean {
        /** フォーカスを無効にする */
        if (keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP) {
            isFocusable = false
            isFocusableInTouchMode = false
        }
        return super.onKeyPreIme(keyCode, event)
    }
}