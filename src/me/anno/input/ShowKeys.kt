package me.anno.input

import me.anno.config.DefaultConfig.defaultFont
import me.anno.config.DefaultConfig.style
import me.anno.gpu.GFX
import me.anno.gpu.OpenGL.renderDefault
import me.anno.gpu.drawing.DrawRectangles.drawRect
import me.anno.gpu.drawing.DrawTexts.drawText
import me.anno.gpu.drawing.DrawTexts.getTextSize
import me.anno.gpu.drawing.GFXx2D.getSizeX
import me.anno.ui.base.text.TextPanel
import me.anno.maths.Maths.clamp
import me.anno.maths.Maths.mix
import org.lwjgl.glfw.GLFW

object ShowKeys {

    val activeKeys = ArrayList<Key>()
    val activeKeysMap = HashMap<Int, Key>()
    val decaySpeed = 1f

    val font = style.getFont("tutorialText", defaultFont)
    val colors = TextPanel("", style)

    class Key(val keyCode: Int, val isSuperKey: Boolean, var time: Float)

    private const val lower = 0.8f // full strength while hold
    private fun addKey(keyCode: Int, isSuperKey: Boolean) {
        var key = activeKeysMap[keyCode]
        if (key == null) {
            key = Key(keyCode, isSuperKey, 2f)
            activeKeys += key
            activeKeysMap[keyCode] = key
        } else {
            key.time =
                if (key.time < lower) lower
                else mix(key.time, lower, GFX.deltaTime * 5f)
        }
    }

    fun drawKey(text: String, alpha: Float, x0: Int, hmy: Int): Int {

        val bgColor = colors.backgroundColor
        val textColor = colors.textColor
        val fontSize = font.sizeInt

        // background
        val rgbMask = 0xffffff
        val alphaMask = clamp(alpha * 255, 0f, 255f).toInt().shl(24) or rgbMask
        val color = textColor and alphaMask
        val w0 = getSizeX(getTextSize(font, text, -1, -1))
        drawRect(x0 + 5, hmy - 12 - fontSize, w0 + 10, fontSize + 8, bgColor and alphaMask)

        // text
        val textColor2 = color and alphaMask
        val bgColor2 = bgColor and rgbMask
        val x = x0 + 10
        val y = hmy - 10 - fontSize
        drawText(x, y, font, text, textColor2, bgColor2, -1, -1)

        return x0 + w0 + 16

    }

    fun draw(x: Int, y: Int, h: Int): Boolean {

        // draw the current keys for a tutorial...
        // fade out keys
        // blink when typed (overexposure to get attention)

        // full strength at start is 1

        for (keyCode in Input.keysDown.keys) {
            when (keyCode) {
                GLFW.GLFW_KEY_LEFT_CONTROL,
                GLFW.GLFW_KEY_RIGHT_CONTROL -> addKey(GLFW.GLFW_KEY_LEFT_CONTROL, true)
                GLFW.GLFW_KEY_LEFT_SHIFT,
                GLFW.GLFW_KEY_RIGHT_SHIFT -> addKey(GLFW.GLFW_KEY_LEFT_SHIFT, true)
                GLFW.GLFW_KEY_LEFT_ALT,
                GLFW.GLFW_KEY_RIGHT_ALT -> addKey(GLFW.GLFW_KEY_LEFT_ALT, true)
                GLFW.GLFW_KEY_LEFT_SUPER,
                GLFW.GLFW_KEY_RIGHT_SUPER -> addKey(GLFW.GLFW_KEY_LEFT_SUPER, true)
                else -> addKey(keyCode, false)
            }
        }

        activeKeys.removeIf { key ->
            key.time = if (key.time > 1f) 1f else key.time - GFX.deltaTime * decaySpeed
            val becameOutdated = key.time < 0f
            if (becameOutdated) activeKeysMap.remove(key.keyCode)
            becameOutdated
        }

        activeKeys.sortBy { !it.isSuperKey }

        if (activeKeys.isNotEmpty()) {
            renderDefault {
                var x0 = x
                for (index in activeKeys.indices) {
                    val key = activeKeys[index]
                    val alpha = key.time
                    val text0 = KeyCombination.keyMapping.reverse[key.keyCode]
                    val text = text0 ?: key.keyCode.toString()
                    x0 = drawKey(text, alpha, x0, h - y)
                }
            }
        }

        return activeKeys.isNotEmpty()

    }

}