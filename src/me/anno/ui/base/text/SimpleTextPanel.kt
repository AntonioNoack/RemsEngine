package me.anno.ui.base.text

import me.anno.config.DefaultStyle
import me.anno.ecs.prefab.PrefabSaveable
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
            if (field != value2) {
                field = value2
                invalidateDrawing()
            }
        }

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
        val offset = when (alignmentX) {
            AxisAlignment.MIN -> 1
            AxisAlignment.CENTER -> w / 2
            AxisAlignment.MAX -> w - 1
            else -> 1
        }
        DrawTexts.drawSimpleTextCharByChar(
            x + offset, y + 2, 2, // idk...
            text, textColor, backgroundColor, alignmentX
        )
        GFX.loadTexturesSync.pop()
    }

    override fun clone(): SimpleTextPanel {
        val clone = SimpleTextPanel(style)
        copy(clone)
        return clone
    }

    override fun copy(clone: PrefabSaveable) {
        super.copy(clone)
        clone as SimpleTextPanel
        clone.text = text
        clone.textColor = textColor
        clone.focusTextColor = focusTextColor
    }

    override val className: String = "SimpleTextPanel"

}