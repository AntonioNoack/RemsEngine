package me.anno.ui.base.text

import me.anno.config.DefaultStyle
import me.anno.gpu.GFX
import me.anno.gpu.drawing.DrawTexts
import me.anno.ui.base.Panel
import me.anno.ui.base.constraints.AxisAlignment
import me.anno.ui.style.Style
import kotlin.math.max

open class SimpleTextPanel(style: Style) : Panel(style) {

    var text = ""
        set(value) {
            val value2 = value.trim()
            if(field != value2){
                field = value2
                invalidateDrawing()
            }
        }

    var alignment = AxisAlignment.MIN

    var textColor = style.getColor("textColor", DefaultStyle.iconGray)
    var focusTextColor = style.getColor("textColorFocused", -1)

    override fun calculateSize(w: Int, h: Int) {
        val font = DrawTexts.monospaceFont.value
        val text = text.ifEmpty { "." }
        super.calculateSize(w, h)
        val w2 = font.sampleWidth * text.length + 4
        val h2 = font.sampleHeight + 4
        minW = max(1, w2)
        minH = max(1, h2)
    }

    override fun onDraw(x0: Int, y0: Int, x1: Int, y1: Int) {
        drawBackground()
        GFX.loadTexturesSync.push(true)
        val offset = when(alignment){
            AxisAlignment.MIN -> 1
            AxisAlignment.CENTER -> w/2
            AxisAlignment.MAX -> w-1
        }
        DrawTexts.drawSimpleTextCharByChar(
            x + offset, y + 2, 2, // idk...
            text, textColor, backgroundColor, alignment
        )
        GFX.loadTexturesSync.pop()
    }

}