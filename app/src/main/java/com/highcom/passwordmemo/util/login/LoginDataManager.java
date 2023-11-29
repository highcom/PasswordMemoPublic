package com.highcom.passwordmemo.util.login;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.ColorInt;

import com.highcom.passwordmemo.util.BackgroundColorUtil;
import com.highcom.passwordmemo.R;
import com.highcom.passwordmemo.util.TextSizeUtil;

public class LoginDataManager {
    private static LoginDataManager manager;

    private static final String TAG = "LoginDataManager";
    private static final String PREF_FILE_NAME ="com.highcom.LoginActivity.MasterPass";
    private SharedPreferences sharedPref;
    private MasterPasswordUtil passUtil;
    private String masterPassword = null;
    private String sortKey;
    private boolean deleteSwitchEnable;
    private boolean biometricLoginSwitchEnable;
    private boolean displayBackgroundSwitchEnable;
    private boolean memoVisibleSwitchEnable;
    private int backgroundColor;
    private float textSize;
    private int copyClipboard;
    private long selectGroup;

    private LoginDataManager(final Activity activity) {
        sharedPref = activity.getSharedPreferences(PREF_FILE_NAME, Context.MODE_PRIVATE);
        passUtil = new MasterPasswordUtil(sharedPref, activity);
        checkBackgroundColor(activity.getApplicationContext());
        updateSetting();
    }

    public static LoginDataManager getInstance(final Activity activity) {
        if (manager == null) {
            manager = new LoginDataManager(activity);
        }
        return manager;
    }

    public void updateSetting() {
        sortKey = sharedPref.getString("sortKey", "id");
        deleteSwitchEnable = sharedPref.getBoolean("deleteSwitchEnable", false);
        biometricLoginSwitchEnable = sharedPref.getBoolean("biometricLoginSwitchEnable", true);
        displayBackgroundSwitchEnable = sharedPref.getBoolean("displayBackgroundSwitchEnable", false);
        memoVisibleSwitchEnable = sharedPref.getBoolean("memoVisibleSwitchEnable", false);
        backgroundColor = sharedPref.getInt("backgroundColor", 0);
        textSize = sharedPref.getFloat("textSize", TextSizeUtil.TEXT_SIZE_MEDIUM);
        copyClipboard = sharedPref.getInt("copyClipboard", 0);
        selectGroup = sharedPref.getLong("selectGroup", 1);
        try {
            masterPassword = passUtil.getMasterPasswordString("masterPassword");
        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
        }
    }

    public String getSortKey() {
        return sortKey;
    }

    public void setSortKey(String key) {
        sharedPref.edit().putString("sortKey", key).apply();
        sortKey = key;
    }

    public boolean isDeleteSwitchEnable() {
        return deleteSwitchEnable;
    }

    public void setDeleteSwitchEnable(boolean b) {
        sharedPref.edit().putBoolean("deleteSwitchEnable", b).apply();
        updateSetting();
    }

    public boolean isBiometricLoginSwitchEnable() {
        return biometricLoginSwitchEnable;
    }

    public void setBiometricLoginSwitchEnable(boolean b) {
        sharedPref.edit().putBoolean("biometricLoginSwitchEnable", b).apply();
        updateSetting();
    }

    public boolean isDisplayBackgroundSwitchEnable() {
        return displayBackgroundSwitchEnable;
    }

    public void setDisplayBackgroundSwitchEnable(boolean b) {
        sharedPref.edit().putBoolean("displayBackgroundSwitchEnable", b).apply();
        updateSetting();
    }

    public boolean isMemoVisibleSwitchEnable() {
        return memoVisibleSwitchEnable;
    }

    public void setMemoVisibleSwitchEnable(boolean b) {
        sharedPref.edit().putBoolean("memoVisibleSwitchEnable", b).apply();
        updateSetting();
    }

    @ColorInt
    public int getBackgroundColor() {
        return backgroundColor;
    }

    public void setBackgroundColor(@ColorInt int color) {
        sharedPref.edit().putInt("backgroundColor", color).apply();
        backgroundColor = color;
    }

    public float getTextSize() {
        return textSize;
    }

    public void setTextSize(float size) {
        sharedPref.edit().putFloat("textSize", size).apply();
        textSize = size;
    }

    public int getCopyClipboard() {
        return copyClipboard;
    }

    public void setCopyClipboard(int operation) {
        sharedPref.edit().putInt("copyClipboard", operation).apply();
        copyClipboard = operation;
    }

    public long getSelectGroup() {
        return selectGroup;
    }

    public void setSelectGroup(long select) {
        sharedPref.edit().putLong("selectGroup", select).apply();
        selectGroup = select;
    }

    public boolean isMasterPasswordCreated() {
        if (masterPassword != null) return true;
        return false;
    }

    public String getMasterPassword() {
        return masterPassword;
    }

    public void setMasterPassword(String password) {
        try {
            passUtil.saveMasterPasswordString("masterPassword", password);
        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
        }
        updateSetting();
    }

    public void clearAllData() {
        sharedPref.edit().clear().apply();
        updateSetting();
    }

    private void checkBackgroundColor(Context context) {
        BackgroundColorUtil backgroundColorUtil = new BackgroundColorUtil(context, null);
        if (!backgroundColorUtil.isColorExists(sharedPref.getInt("backgroundColor", 0))) {
            sharedPref.edit().putInt("backgroundColor", context.getResources().getColor(R.color.white)).commit();
        }
    }
}
