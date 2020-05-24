package me.anno.ui.base

import me.anno.config.DefaultStyle.iconGray
import me.anno.gpu.GFX
import me.anno.ui.base.components.Padding
import me.anno.ui.style.Style

open class TextPanel(open var text: String, style: Style): Panel(style){

    var padding = Padding(style.getSize("textPadding", 2))
    var fontSize = style.getSize("textSize", 12)
    var textColor = style.getColor("textColor", iconGray)
    var focusTextColor = style.getColor("textColorFocused", -1)

    // can be disabled for parents to copy ALL lines, e.g. for a bug report :)
    var disableCopy = false

    override fun calculateSize(w: Int, h: Int) {
        super.calculateSize(w, h)
        val (w2, h2) = GFX.getTextSize("Verdana", fontSize, if(text.isBlank()) "x" else text)
        minW = w2 + padding.width
        minH = h2 + padding.height
    }

    override fun draw(x0: Int, y0: Int, x1: Int, y1: Int) {
        super.draw(x0, y0, x1, y1)
        GFX.drawText(x + padding.left, y + padding.top, fontSize, text, if(isInFocus) focusTextColor else textColor, backgroundColor)
    }

    override fun onCopyRequested(x: Float, y: Float): String? {
        if(disableCopy) return super.onCopyRequested(x, y)
        else return text
    }

}