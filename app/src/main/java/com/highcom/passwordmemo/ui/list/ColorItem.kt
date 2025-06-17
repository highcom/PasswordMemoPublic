package com.highcom.passwordmemo.ui.list

/**
 * 色設定用アイテム
 *
 * @constructor
 * 色設定用アイテムコンストラクタ
 *
 * @param colorName
 * @param colorCode
 */
class ColorItem(colorName: String?, colorCode: Int?) {
    /** 色名 */
    var colorName: String? = colorName
        private set
    /** 色コード */
    var colorCode: Int? = colorCode
        private set

}