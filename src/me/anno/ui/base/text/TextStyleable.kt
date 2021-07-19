package me.anno.ui.base.text

interface TextStyleable {

    // either these or the last two need to be implemented
    fun setBold(){
        setBold(true)
    }

    fun unsetBold(){
        setBold(false)
    }

    fun setItalic(){
        setItalic(true)
    }

    fun unsetItalic(){
        setItalic(false)
    }

    fun setBold(bold: Boolean) {
        if (bold) setBold()
        else unsetBold()
    }

    fun setItalic(italic: Boolean) {
        if (italic) setItalic()
        else unsetItalic()
    }

}