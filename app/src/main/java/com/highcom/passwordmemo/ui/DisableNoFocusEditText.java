package com.highcom.passwordmemo.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.view.KeyEvent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class DisableNoFocusEditText extends androidx.appcompat.widget.AppCompatEditText {

    public DisableNoFocusEditText(Context context) {
        super(context);
    }

    public DisableNoFocusEditText(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public DisableNoFocusEditText(
            @NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public boolean onKeyPreIme(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_UP) {
            setFocusable(false);
            setFocusableInTouchMode(false);
        }
        return super.onKeyPreIme(keyCode, event);
    }
}
