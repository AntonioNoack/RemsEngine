package me.anno.ui.base

import me.anno.config.DefaultStyle.black
import me.anno.gpu.GFX
import me.anno.input.MouseButton
import me.anno.ui.base.components.Padding
import me.anno.ui.base.constraints.WrapAlign
import me.anno.ui.style.Style
import me.anno.utils.mixARGB

open class ButtonPanel(text: String, style: Style): TextPanel(text, style.getChild("button")){

    val leftColor = style.getColor("borderColorLeft", black or 0x999999)
    val rightColor = style.getColor("borderColorRight", black or 0x111111)
    val topColor = style.getColor("borderColorTop", black or 0x999999)
    val bottomColor = style.getColor("borderColorBottom", black or 0x111111)

    val borderSize = style.getPadding("borderSize", 2)

    val normalBackground = backgroundColor
    val hoveredBackground = mixARGB(bottomColor, normalBackground, 0.7f)

    init {
        padding += borderSize
        this += WrapAlign.LeftTop
    }

    var mouseDown = false
    override fun draw(x0: Int, y0: Int, x1: Int, y1: Int) {

        val isHovered = isHovered
        val mouseDown = mouseDown

        backgroundColor = if(isHovered && !mouseDown) hoveredBackground else normalBackground
        drawBackground()

        GFX.drawRect(x+w-borderSize.right, y, borderSize.right, h, getColor(isHovered, mouseDown, rightColor, leftColor)) // right
        GFX.drawRect(x, y+h-borderSize.bottom, w, borderSize.bottom, getColor(isHovered, mouseDown, bottomColor, topColor)) // bottom
        GFX.drawRect(x, y, borderSize.left, h, getColor(isHovered, mouseDown, leftColor, rightColor)) // left
        GFX.drawRect(x, y, w, borderSize.top, getColor(isHovered, mouseDown, topColor, bottomColor)) // top

        drawText(0, 0, text, textColor)

    }

    fun getColor(isHovered: Boolean, mouseDown: Boolean, base: Int, alternative: Int): Int {
        val alpha = if(isHovered && !mouseDown) 0xaa000000.toInt() else black
        return alpha or ((if(mouseDown) alternative else base) and 0xffffff)
    }

    override fun onMouseDown(x: Float, y: Float, button: MouseButton) {
        super.onMouseDown(x, y, button)
        mouseDown = true
    }

    override fun onMouseUp(x: Float, y: Float, button: MouseButton) {
        super.onMouseUp(x, y, button)
        mouseDown = false
    }

}