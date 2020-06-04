package me.anno.ui.base

import me.anno.config.DefaultStyle.black
import me.anno.ui.style.Style

open class ButtonPanel(text: String, style: Style): TextPanel(text, style.getChild("button")){

    val normalBackground = backgroundColor
    val hoveredBackground = style.getColor("background.hover", black)

    var mouseDown = false
    override fun draw(x0: Int, y0: Int, x1: Int, y1: Int) {
        backgroundColor = if(isHovered && !mouseDown) hoveredBackground else normalBackground
        super.draw(x0, y0, x1, y1)
    }

    override fun onMouseDown(x: Float, y: Float, button: Int) {
        super.onMouseDown(x, y, button)
        mouseDown = true
    }

    override fun onMouseUp(x: Float, y: Float, button: Int) {
        super.onMouseUp(x, y, button)
        mouseDown = false
    }

}