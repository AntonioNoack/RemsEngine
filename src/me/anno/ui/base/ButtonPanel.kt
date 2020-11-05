package me.anno.ui.base

import me.anno.config.DefaultStyle.black
import me.anno.gpu.GFX
import me.anno.gpu.GFXx2D
import me.anno.gpu.GFXx2D.drawRect
import me.anno.gpu.GFXx2D.getTextSize
import me.anno.input.Input
import me.anno.input.Input.keysDown
import me.anno.input.MouseButton
import me.anno.ui.base.constraints.WrapAlign
import me.anno.ui.style.Style
import me.anno.utils.isClickKey
import me.anno.utils.Maths.mixARGB
import me.anno.utils.one
import org.lwjgl.glfw.GLFW

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

    override fun tickUpdate() {
        super.tickUpdate()
        mouseDown = (isHovered && 0 in Input.mouseKeysDown) ||
                (isInFocus && keysDown.one { it.key.isClickKey() })
        backgroundColor = if(isHovered && !mouseDown) hoveredBackground else normalBackground
    }

    override fun getVisualState(): Any? {
        return Triple(super.getVisualState(), mouseDown, isHovered)
    }

    override fun onDraw(x0: Int, y0: Int, x1: Int, y1: Int) {
        draw(isHovered, mouseDown)
    }

    fun draw(isHovered: Boolean, mouseDown: Boolean){
        drawBackground()

        val limit = if(breaksIntoMultiline) this.w else -1
        val size = getTextSize(fontName, textSize, isBold, isItalic, text, limit)
        GFXx2D.drawText(x + (w - size.first) / 2, y + (h - size.second) / 2, fontName, textSize, isBold, isItalic,
            text, textColor, backgroundColor, limit)

        drawRect(x+w-borderSize.right, y, borderSize.right, h, getColor(isHovered, mouseDown, rightColor, leftColor)) // right
        drawRect(x, y+h-borderSize.bottom, w, borderSize.bottom, getColor(isHovered, mouseDown, bottomColor, topColor)) // bottom
        drawRect(x, y, borderSize.left, h, getColor(isHovered, mouseDown, leftColor, rightColor)) // left
        drawRect(x, y, w, borderSize.top, getColor(isHovered, mouseDown, topColor, bottomColor)) // top

    }

    fun getColor(isHovered: Boolean, mouseDown: Boolean, base: Int, alternative: Int): Int {
        val alpha = if(isHovered && !mouseDown) 0xaa000000.toInt() else black
        return alpha or ((if(mouseDown) alternative else base) and 0xffffff)
    }

    fun click(){
        onMouseClicked(x + w*0.5f, y + h*0.5f, MouseButton.LEFT, false)
    }

    override fun onKeyTyped(x: Float, y: Float, key: Int) {
        if(key.isClickKey()) click()
    }

    override fun acceptsChar(char: Int) = char.isClickKey()
    override fun isKeyInput() = true

}