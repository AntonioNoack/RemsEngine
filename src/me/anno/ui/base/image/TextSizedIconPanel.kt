package me.anno.ui.base.image

import me.anno.fonts.FontStats
import me.anno.io.files.FileReference
import me.anno.io.files.InvalidRef
import me.anno.ui.Style

open class TextSizedIconPanel(source: FileReference, style: Style) : IconPanel(source, style) {
    constructor(style: Style) : this(InvalidRef, style)

    var size = style.getSize("text.fontSize", FontStats.getDefaultFontSize())

    override fun calculateSize(w: Int, h: Int) {
        minW = size
        minH = size
    }
}