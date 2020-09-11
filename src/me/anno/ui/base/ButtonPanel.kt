package me.anno.ui.base

import me.anno.config.DefaultStyle.black
import me.anno.gpu.GFX
import me.anno.input.Input
import me.anno.input.Input.keysDown
import me.anno.input.MouseButton
import me.anno.ui.base.constraints.WrapAlign
import me.anno.ui.style.Style
import me.anno.utils.mixARGB
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

    override fun onDraw(x0: Int, y0: Int, x1: Int, y1: Int) {

        val isInFocus = isInFocus
        val isHovered = isHovered
        val mouseDown = (isHovered && 0 in Input.mouseKeysDown) ||
                GLFW.GLFW_KEY_DOWN in keysDown ||
                GLFW.GLFW_KEY_UP in keysDown ||
                GLFW.GLFW_KEY_ENTER in keysDown

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

    fun click(){
        onMouseClicked(x + w*0.5f, y + h*0.5f, MouseButton.LEFT, false)
    }

    override fun onEnterKey(x: Float, y: Float) {
        click()
    }

    override fun onKeyTyped(x: Float, y: Float, key: Int) {
        when(key){
            GLFW.GLFW_KEY_DOWN, GLFW.GLFW_KEY_UP -> click()
        }
    }

    override fun acceptsChar(char: Int) = false // ^^
    override fun isKeyInput() = true

}