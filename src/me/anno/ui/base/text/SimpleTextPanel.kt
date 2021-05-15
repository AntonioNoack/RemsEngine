package me.anno.ui.base.text

import me.anno.config.DefaultStyle
import me.anno.gpu.GFX
import me.anno.gpu.GFX.windowStack
import me.anno.gpu.GFXx2D
import me.anno.gpu.Window
import me.anno.input.MouseButton
import me.anno.language.translation.Dict
import me.anno.studio.Logging
import me.anno.ui.base.Panel
import me.anno.ui.base.buttons.TextButton
import me.anno.ui.base.constraints.AxisAlignment
import me.anno.ui.base.groups.PanelList
import me.anno.ui.debug.console.COLine
import me.anno.ui.debug.console.ConsoleLogFullscreen
import me.anno.ui.style.Style
import me.anno.utils.Maths.mixARGB
import java.util.logging.Level
import kotlin.math.max
import kotlin.math.min

open class SimpleTextPanel(style: Style) : Panel(style) {

    var text = ""
        set(value) {
            field = value.trim()
            invalidateDrawing()
        }

    var alignment = AxisAlignment.MIN

    var textColor = style.getColor("textColor", DefaultStyle.iconGray)
    var focusTextColor = style.getColor("textColorFocused", -1)

    override fun calculateSize(w: Int, h: Int) {
        val font = GFXx2D.monospaceFont.value
        val text = if (text.isEmpty()) "." else text
        super.calculateSize(w, h)
        val w2 = font.sampleWidth * text.length + 4
        val h2 = font.sampleHeight + 4
        minW = max(1, w2)
        minH = max(1, h2)
    }

    override fun onDraw(x0: Int, y0: Int, x1: Int, y1: Int) {
        GFX.loadTexturesSync.push(true)
        super.onDraw(x0, y0, x1, y1)
        val offset = when(alignment){
            AxisAlignment.MIN -> 1
            AxisAlignment.CENTER -> w/2
            AxisAlignment.MAX -> w-1
        }
        GFXx2D.drawSimpleTextCharByChar(
            x + offset, y + 2, 2, // idk...
            text, textColor, backgroundColor, alignment
        )
        GFX.loadTexturesSync.pop()
    }

}