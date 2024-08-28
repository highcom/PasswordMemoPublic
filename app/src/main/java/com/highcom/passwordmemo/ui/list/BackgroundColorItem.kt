package com.highcom.passwordmemo.ui.list

/**
 * 背景色設定用アイテム
 *
 * @constructor
 * 背景色設定用アイテムコンストラクタ
 *
 * @param colorName
 * @param colorCode
 */
class BackgroundColorItem(colorName: String?, colorCode: Int?) {
    /** 背景色名 */
    var colorName: String? = colorName
        private set
    /** 背景色コード */
    var colorCode: Int? = colorCode
        private set

}