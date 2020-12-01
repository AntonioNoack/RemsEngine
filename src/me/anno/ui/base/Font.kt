package me.anno.ui.base

class Font(var name: String, var size: Float, var isBold: Boolean, var isItalic: Boolean) {

    fun withSize(size: Float) = Font(name, size, isBold, isItalic)

}