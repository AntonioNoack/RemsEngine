package me.anno.ui.base.text

interface TextStyleable {

    var textSize: Float
    var textColor: Int
    var isBold: Boolean
    var isItalic: Boolean

    fun toggleBold() {
        isBold = !isBold
    }

    fun toggleItalic() {
        isItalic = !isItalic
    }

}