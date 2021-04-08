package me.anno.input

import me.anno.config.DefaultConfig.defaultFont
import me.anno.config.DefaultConfig.style
import me.anno.gpu.GFX
import me.anno.gpu.GFXx2D.drawRect
import me.anno.gpu.GFXx2D.drawText
import me.anno.gpu.GFXx2D.getSizeX
import me.anno.gpu.GFXx2D.getTextSize
import me.anno.gpu.blending.BlendDepth
import me.anno.gpu.blending.BlendMode
import me.anno.ui.base.text.TextPanel
import me.anno.utils.Maths.mix
import org.lwjgl.glfw.GLFW

object ShowKeys {

    val activeKeys = ArrayList<Key>()
    val activeKeysMap = HashMap<Int, Key>()
    val decaySpeed = 1f

    val font = style.getFont("tutorialText", defaultFont)
    val colors = TextPanel("", style)

    class Key(val keyCode: Int, val isSuperKey: Boolean, var time: Float)

    fun draw(x: Int, y: Int, w: Int, h: Int): Boolean {

        // draw the current keys for a tutorial...
        // fade out keys
        // blink when typed (overexposure to get attention)

        // full strength at start is 1
        val lower = 0.8f // full strength while hold

        fun addKey(keyCode: Int, isSuperKey: Boolean) {
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

        Input.keysDown.keys.forEach { keyCode ->
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

        activeKeys.removeAll(
            activeKeys.filter { key ->
                key.time = if (key.time > 1f) 1f else key.time - GFX.deltaTime * decaySpeed
                val becameOutdated = key.time < 0f
                if (becameOutdated) activeKeysMap.remove(key.keyCode)
                becameOutdated
            }
        )

        activeKeys.sortBy { !it.isSuperKey }

        if (activeKeys.isNotEmpty()) {

            BlendDepth(BlendMode.DEFAULT, false) {

                var x0 = x

                val bgColor = colors.backgroundColor
                val textColor = colors.textColor

                val fontSize = font.sizeInt
                fun show(text: String, alpha: Float) {
                    val alphaMask = (alpha * 255).toInt().shl(24) or 0xffffff
                    val color = textColor and alphaMask
                    val w0 = getSizeX(getTextSize(font, text, -1))
                    drawRect(x0 + 5, h - y - 12 - fontSize, w0 + 10, fontSize + 8, bgColor and alphaMask)
                    drawText(x0 + 10, h - y - 10 - fontSize, font, text,
                        color and alphaMask,
                        bgColor and 0xffffff, -1)
                    x0 += w0 + 16
                }

                activeKeys.forEach { key ->
                    val alpha = key.time
                    val text = KeyCombination.keyMapping.reverse[key.keyCode] ?: key.keyCode.toString()
                    show(text, alpha)
                }

            }
            return true
        }
        return false
    }

}