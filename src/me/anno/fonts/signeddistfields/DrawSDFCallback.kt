package me.anno.fonts.signeddistfields

fun interface DrawSDFCallback {
    /**
     * return true when done
     * */
    fun draw(textSDF: TextSDF, x0: Float, x1: Float, y: Float, lineWidth: Float, glyphIndex: Int): Boolean
}