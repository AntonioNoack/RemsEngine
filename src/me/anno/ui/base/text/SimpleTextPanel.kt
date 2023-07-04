package me.anno.ui.base.text

import me.anno.config.DefaultStyle
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.gpu.GFX
import me.anno.gpu.drawing.DrawTexts
import me.anno.ui.Panel
import me.anno.ui.base.constraints.AxisAlignment
import me.anno.ui.style.Style
import kotlin.math.max

open class SimpleTextPanel(style: Style) : Panel(style) {

    var text = ""
        set(value) {
            if (field != value) {
                field = value
                invalidateDrawing()
            }
        }

    var textColor = style.getColor("textColor", DefaultStyle.iconGray)
    var focusTextColor = style.getColor("textColorFocused", -1)

    override fun calculateSize(w: Int, h: Int) {
        val font = DrawTexts.monospaceFont
        val text = text.ifEmpty { "." }
        super.calculateSize(w, h)
        val w2 = font.sampleWidth * text.length + 4
        val h2 = font.sampleHeight + 4
        minW = max(1, w2)
        minH = max(1, h2)
    }

    private var loadTextSync = false
    override fun onDraw(x0: Int, y0: Int, x1: Int, y1: Int) {
        drawBackground(x0, y0, x1, y1)
        GFX.loadTexturesSync.push(loadTextSync)
        val offset = when (alignmentX) {
            AxisAlignment.MIN -> 1
            AxisAlignment.CENTER -> width / 2
            AxisAlignment.MAX -> width - 1
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
        copyInto(clone)
        return clone
    }

    override fun copyInto(dst: PrefabSaveable) {
        super.copyInto(dst)
        dst as SimpleTextPanel
        dst.text = text
        dst.textColor = textColor
        dst.focusTextColor = focusTextColor
    }

    override val className: String get() = "SimpleTextPanel"

}