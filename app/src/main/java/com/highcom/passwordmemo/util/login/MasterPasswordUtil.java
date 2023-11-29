package com.highcom.passwordmemo.util.login;

import android.content.Context;
import android.content.SharedPreferences;

import com.highcom.passwordmemo.R;
import com.highcom.passwordmemo.util.login.CryptUtil;

public class MasterPasswordUtil {

    SharedPreferences sharedPref;
    // 暗号化/復号キー
    private String secretKey;

    MasterPasswordUtil(SharedPreferences pref, Context context) {
        sharedPref = pref;
        secretKey = context.getString(R.string.master_secret_key);
    }

    // 暗号化してパスワードを保存
    public void saveMasterPasswordString(String key, String value) throws Exception {
        if (key == null || key.length() == 0) {
            throw new Exception("キーが空です。");
        }
        if (value == null) {
            throw new Exception("値が空です。");
        }
        // 暗号化
        CryptUtil cryptUtil = new CryptUtil();
        String encValue = cryptUtil.encrypt(value, secretKey);
        if (encValue == null) {
            throw new Exception("暗号化に失敗しました。");
        }

        // 保存
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(key, encValue).commit();
    }

    // 複合化してパスワードを読み込み
    public String getMasterPasswordString(String key) throws Exception {
        // 値取得
        if (key == null || key.length() == 0) {
            throw new Exception("キーが空です。");
        }
        String value = sharedPref.getString(key, null);
        if (value == null) {
            return null;
        }

        // 復号
        CryptUtil cryptUtil = new CryptUtil();
        String decValue = cryptUtil.decrypt(value, secretKey);
        if (decValue == null) {
            throw new Exception("復号に失敗しました。");
        }
        return decValue;
    }

}
