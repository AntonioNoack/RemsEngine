package me.anno.ui.base.text

import me.anno.config.DefaultStyle
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.gpu.GFX
import me.anno.gpu.drawing.DrawTexts
import me.anno.gpu.drawing.DrawTexts.monospaceFont
import me.anno.ui.Panel
import me.anno.ui.Style
import me.anno.utils.Color.withAlpha
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

    override val canDrawOverBorders: Boolean
        get() = true

    override fun calculateSize(w: Int, h: Int) {
        // calculate max line length & line count
        var lineCount = 0
        var maxLineLength = 0
        forEachLine { i0, i1 ->
            lineCount++
            maxLineLength = max(maxLineLength, i1 - i0)
        }
        val font = monospaceFont
        minW = font.sampleWidth * maxLineLength + 4
        minH = font.sampleHeight * lineCount + 4
    }

    private var loadTextSync = false
    override fun onDraw(x0: Int, y0: Int, x1: Int, y1: Int) {
        drawBackground(x0, y0, x1, y1)
        GFX.loadTexturesSync.push(loadTextSync)
        val offset = alignmentX.getOffset(width, 0)
        val text = text
        val x = x + offset
        var y = y + 2
        val font = monospaceFont
        val dy = font.sampleHeight
        forEachLine { i0, i1 ->
            drawLine(text.subSequence(i0, i1), x, y)
            y += dy
        }
        GFX.loadTexturesSync.pop()
    }

    open fun forEachLine(callback: (i0: Int, i1: Int) -> Unit) {
        val text = text
        var i = 0
        while (true) {
            val ni = text.indexOf('\n', i)
            var ni1 = if (ni < 0) text.length else ni
            if (ni1 > i && text[ni1 - 1] == '\r') ni1--
            callback(i, ni1)
            if (ni > i) {
                i = ni + 1
            } else break
        }
    }

    open fun drawLine(text: CharSequence, x: Int, y: Int) {
        DrawTexts.drawSimpleTextCharByChar(
            x, y, 2,
            text, textColor, backgroundColor.withAlpha(0),
            alignmentX
        )
    }

    override fun onCopyRequested(x: Float, y: Float) = text

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