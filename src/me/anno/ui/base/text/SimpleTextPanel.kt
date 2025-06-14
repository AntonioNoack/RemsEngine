package me.anno.ui.base.text

import me.anno.config.DefaultStyle
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.gpu.GFX
import me.anno.gpu.drawing.DrawTexts
import me.anno.gpu.drawing.DrawTexts.monospaceFont
import me.anno.gpu.drawing.GFXx2D.getSize
import me.anno.gpu.drawing.GFXx2D.getSizeX
import me.anno.gpu.drawing.GFXx2D.getSizeY
import me.anno.ui.Panel
import me.anno.ui.Style
import me.anno.ui.base.components.AxisAlignment
import me.anno.utils.Color.withAlpha
import me.anno.utils.callbacks.I2U
import me.anno.utils.types.Floats.toIntOr
import kotlin.math.max

open class SimpleTextPanel(style: Style) : Panel(style) {

    var text = ""
    var textColor = style.getColor("textColor", DefaultStyle.iconGray)
    var focusTextColor = style.getColor("textColorFocused", -1)

    var textAlignmentX = AxisAlignment.MIN
    var textAlignmentY = AxisAlignment.CENTER

    override val canDrawOverBorders: Boolean get() = true

    override fun calculateSize(w: Int, h: Int) {
        // calculate max line length & line count
        val lineStats = getLineStats()
        val font = monospaceFont
        minW = font.sampleWidth * getSizeX(lineStats) + 4
        minH = font.sampleHeight * getSizeY(lineStats) + 4
    }

    private var loadTextSync = false
    override fun draw(x0: Int, y0: Int, x1: Int, y1: Int) {
        drawBackground(x0, y0, x1, y1)
        GFX.loadTexturesSync.push(loadTextSync)

        val font = monospaceFont
        val lineStats = getLineStats()
        val usedWidth = font.sampleWidth * getSizeX(lineStats)
        val usedHeight = font.sampleHeight * getSizeY(lineStats)

        val x = x + textAlignmentX.getOffset(width - 4, usedWidth) + 2
        var y = y + textAlignmentY.getOffset(height - 4, usedHeight) + 2

        y += (font.sampleHeight * 0.69f).toIntOr() // baseline offset

        val text = text
        forEachLine { i0, i1 ->
            drawLine(text.subSequence(i0, i1), x, y)
            y += font.sampleHeight
        }
        GFX.loadTexturesSync.pop()
    }

    open fun forEachLine(callback: I2U) {
        val text = text
        var i = 0
        while (true) {
            val ni = text.indexOf('\n', i)
            var ni1 = if (ni < 0) text.length else ni
            if (ni1 > i && text[ni1 - 1] == '\r') ni1--
            callback.call(i, ni1)
            if (ni > i) {
                i = ni + 1
            } else break
        }
    }

    fun getLineStats(): Int {
        var lineCount = 0
        var maxLineLength = 0
        forEachLine { i0, i1 ->
            lineCount++
            maxLineLength = max(maxLineLength, i1 - i0)
        }
        return getSize(maxLineLength, lineCount)
    }

    open fun drawLine(text: CharSequence, x: Int, y: Int) {
        DrawTexts.drawSimpleTextCharByChar(
            x, y, 2,
            text, textColor, backgroundColor.withAlpha(0),
            textAlignmentX, textAlignmentY
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
        if (dst !is SimpleTextPanel) return
        dst.text = text
        dst.textColor = textColor
        dst.focusTextColor = focusTextColor
    }
}