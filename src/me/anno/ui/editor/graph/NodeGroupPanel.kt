package me.anno.ui.editor.graph

import me.anno.fonts.FontManager
import me.anno.gpu.drawing.DrawTexts
import me.anno.gpu.drawing.GFXx2D
import me.anno.graph.visual.render.NodeGroup
import me.anno.ui.Panel
import me.anno.ui.Style
import me.anno.ui.base.components.AxisAlignment
import me.anno.utils.Color
import me.anno.utils.Color.a
import me.anno.utils.Color.withAlpha
import kotlin.math.max

class NodeGroupPanel(val group: NodeGroup, val gp: GraphPanel, style: Style) : Panel(style) {

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
        val ex = group.extents.x
        val ey = group.extents.y
        val size = FontManager.getSize(font, group.name, -1, -1).waitFor() ?: 0
        minW = max(
            // enough width for title
            baseTextSizeI4 + GFXx2D.getSizeX(size),
            gp.coordsToWindowDirX(ex).toInt()
        )
        minH = max(
            // enough height for all lines
            ((lineCount * (1.0 + lineSpacing) + lineSpacing) * baseTextSize).toInt(),
            gp.coordsToWindowDirY(ey).toInt()
        )
    }

    var focusOutlineColor = -1
    var focusOutlineThickness = 2f

    var textColor = -1

    fun drawBackground(outline: Boolean, inner: Boolean, x0: Int, y0: Int, x1: Int, y1: Int) {
        if (!outline && !inner) return
        // draw whether the node is in focus
        if (outline) {
            background.outlineThickness = focusOutlineThickness
            background.outlineColor = focusOutlineColor
            background.color = backgroundColor.withAlpha(if (inner) bgAlpha else 0f)
        } else {
            background.outlineThickness = 0f
            background.color = backgroundColor.withAlpha(bgAlpha)
        }
        drawBackground(x0, y0, x1, y1)
    }

    var titleWidth = 0
    var titleY0 = 0
    var titleY1 = 0

    override fun draw(x0: Int, y0: Int, x1: Int, y1: Int) {
        // if gp is zooming, take a screenshot of this panel, and redraw it as such (because that's cheaper)
        // it allows us to render really smooth zooming :)

        if (group.color != 0) background.color = group.color

        val inFocus = isInFocus || (gp is GraphEditor && gp.overlapsSelection(this))
        drawBackground(inFocus, true, x0, y0, x1, y1)

        val backgroundColor = Color.mixARGB(gp.background.color, background.color, background.color.a()) and 0xffffff
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