package me.anno.input

import me.anno.Engine.deltaTime
import me.anno.config.DefaultConfig.style
import me.anno.gpu.OpenGL.renderDefault
import me.anno.gpu.drawing.DrawRectangles.drawRect
import me.anno.gpu.drawing.DrawTexts.drawText
import me.anno.gpu.drawing.DrawTexts.getTextSize
import me.anno.gpu.drawing.GFXx2D.getSizeX
import me.anno.maths.Maths.clamp
import me.anno.maths.Maths.mix
import me.anno.ui.base.text.TextPanel
import org.lwjgl.glfw.GLFW
import java.util.function.BiConsumer

/**
 * displays pressed keys for tutorials & debugging
 * */
object ShowKeys {

    val activeKeys = ArrayList<Key>()
    val activeKeysMap = HashMap<Int, Key>()
    var decaySpeed = 1f

    val font = style.getFont("tutorialText")
    val template = TextPanel("", style)

    class Key(
        val keyCode: Int,
        val isSuperKey: Boolean,
        var time: Float,
        val state: KeyMap.InputState = KeyMap.InputState()
    ) {
        var stateId = KeyMap.stateId
        var name = findName()
        fun findName(): String {
            // this can be incorrect as long as we don't know the correct mapping
            val text1 = KeyMap.inputMap[state]
            val text2 = if (text1 != null && text1 != 32 && text1 != 9 && text1 != 10) // space, \n, \r
                String(Character.toChars(text1)) else null
            val text0 = KeyCombination.keyMapping.reverse[keyCode]
            return text2 ?: text0 ?: keyCode.toString()
        }
    }

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
                else mix(key.time, lower, deltaTime * 5f)
        }
    }

    private fun drawKey(text: String, alpha: Float, x0: Int, hmy: Int): Int {

        val bgColor = template.backgroundColor
        val textColor = template.textColor
        val fontSize = font.sizeInt

        // background
        val rgbMask = 0xffffff
        val alphaMask = clamp(alpha * 255, 0f, 255f).toInt().shl(24) or rgbMask
        val color = textColor and alphaMask
        val w0 = getSizeX(getTextSize(font, text, -1, -1))
        drawRect(x0 + 5, hmy - 12 - fontSize, w0 + 10, fontSize + 8, bgColor and alphaMask)

        // text
        val textColor2 = color and alphaMask
        val bgColor2 = bgColor and alphaMask
        val x = x0 + 10
        val y = hmy - 10 - fontSize
        drawText(x, y, font, text, textColor2, bgColor2, -1, -1)

        return x0 + w0 + 16

    }

    private val addKeyConsumer = BiConsumer<Int, Long> { keyCode, _ ->
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

    fun draw(x: Int, y: Int, h: Int): Boolean {

        // draw the current keys for a tutorial...
        // fade out keys
        // blink when typed (overexposure to get attention)

        // full strength at start is 1

        Input.keysDown.forEach(addKeyConsumer)

        return if (activeKeys.isNotEmpty()) {
            activeKeys.removeIf { key ->
                key.time = if (key.time > 1f) 1f else key.time - deltaTime * decaySpeed
                val becameOutdated = key.time < 0f
                if (becameOutdated) activeKeysMap.remove(key.keyCode)
                becameOutdated
            }
            if (activeKeys.isNotEmpty()) {
                activeKeys.sortBy { !it.isSuperKey }
                renderDefault {
                    var x0 = x
                    for (index in activeKeys.indices) {
                        val key = activeKeys[index]
                        val alpha = key.time
                        if (key.stateId != KeyMap.stateId) {
                            key.name = key.findName()
                        }
                        x0 = drawKey(key.name, alpha, x0, h - y)
                    }
                }
                true
            } else false
        } else false

    }

}