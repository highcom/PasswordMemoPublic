package com.highcom.passwordmemo.ui.list;

public class BackgroundColorItem {
    private String mColorName;
    private Integer mColorCode;

    public BackgroundColorItem() {

    }

    public BackgroundColorItem(String colorName, Integer colorCode) {
        mColorName = colorName;
        mColorCode = colorCode;
    }

    public String getColorName() {
        return mColorName;
    }

    public Integer getColorCode() {
        return mColorCode;
    }
}
