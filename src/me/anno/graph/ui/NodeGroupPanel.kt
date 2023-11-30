package me.anno.graph.ui

import me.anno.fonts.FontManager
import me.anno.gpu.drawing.DrawTexts
import me.anno.gpu.drawing.GFXx2D
import me.anno.graph.render.NodeGroup
import me.anno.ui.Panel
import me.anno.ui.Style
import me.anno.ui.base.constraints.AxisAlignment
import me.anno.utils.Color.a
import me.anno.utils.Color.mixARGB
import me.anno.utils.Color.withAlpha

class NodeGroupPanel(val group: NodeGroup, val gp: GraphPanel, style: Style) : Panel(style) {

    // todo - draws in the background
    // todo - has a user-customizable size
    // todo - when moved, moves all children

    val lineCount get() = 1 // title
    val baseTextSize get() = gp.baseTextSize

    var lineSpacing = 0.5
    var bgAlpha = 0.7f

    override fun calculateSize(w: Int, h: Int) {
        val baseTextSize = baseTextSize
        val baseTextSizeI4 = (baseTextSize * 4).toInt()
        val font = gp.font
        // enough width for title
        minW = baseTextSizeI4 + GFXx2D.getSizeX(FontManager.getSize(font, group.name, -1, -1))
        // enough height for all lines
        minH = ((lineCount * (1.0 + lineSpacing) + lineSpacing) * baseTextSize).toInt()
    }

    var focusOutlineColor = -1
    var focusOutlineThickness = 2f

    var textColor = -1

    fun drawBackground(outline: Boolean, inner: Boolean, x0: Int, y0: Int, x1: Int, y1: Int) {
        if (!outline && !inner) return
        // draw whether the node is in focus
        if (outline) {
            backgroundOutlineThickness = focusOutlineThickness
            backgroundOutlineColor = focusOutlineColor
            backgroundColor = backgroundColor.withAlpha(if (inner) bgAlpha else 0f)
        } else {
            backgroundOutlineThickness = 0f
            backgroundColor = backgroundColor.withAlpha(bgAlpha)
        }
        drawBackground(x0, y0, x1, y1)
    }

    var titleWidth = 0
    var titleY0 = 0
    var titleY1 = 0


    override fun onDraw(x0: Int, y0: Int, x1: Int, y1: Int) {
        // if gp is zooming, take a screenshot of this panel, and redraw it as such (because that's cheaper)
        // it allows us to render really smooth zooming :)

        if (group.color != 0) backgroundColor = group.color

        val inFocus = isInFocus || (gp is GraphEditor && gp.overlapsSelection(this))
        drawBackground(inFocus, true, x0, y0, x1, y1)

        val backgroundColor = mixARGB(gp.backgroundColor, backgroundColor, backgroundColor.a()) and 0xffffff
        val font = gp.font
        val textSize = font.sampleHeight

        // node title
        titleY0 = y + textSize / 2
        titleY1 = titleY0 + textSize

        titleWidth = DrawTexts.drawText(
            x + width.shr(1), titleY0, font, group.name, textColor,
            backgroundColor, (width * 3).shr(2), -1, AxisAlignment.CENTER
        )
    }
}