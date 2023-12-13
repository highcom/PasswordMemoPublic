package com.highcom.passwordmemo.ui.list

class BackgroundColorItem {
    var colorName: String? = null
        private set
    var colorCode: Int? = null
        private set

    constructor()
    constructor(colorName: String?, colorCode: Int?) {
        this.colorName = colorName
        this.colorCode = colorCode
    }
}