package me.anno.input

import me.anno.Time.uiDeltaTime
import me.anno.config.DefaultConfig.style
import me.anno.gpu.GFXState.renderDefault
import me.anno.gpu.drawing.DrawRectangles.drawRect
import me.anno.gpu.drawing.DrawTexts
import me.anno.gpu.drawing.DrawTexts.drawTextOrFail
import me.anno.gpu.drawing.DrawTexts.getTextSizeX
import me.anno.maths.Maths.clamp
import me.anno.maths.MinMax.min
import me.anno.maths.Maths.mix
import me.anno.ui.base.text.TextPanel
import me.anno.utils.types.Strings.joinChars

/**
 * displays pressed keys for tutorials & debugging
 * */
object ShowKeys {

    @JvmField
    val activeKeys = ArrayList<Key1>()

    @JvmField
    val activeKeysMap = HashMap<Key, Key1>()

    @JvmField
    var decaySpeed = 1f

    @JvmField
    val font = style.getFont("tutorialText")

    @JvmField
    val template = TextPanel("", style)

    class Key1(
        val key: Key,
        val isSuperKey: Boolean,
        var time: Float,
        val state: KeyNames.InputState = KeyNames.InputState()
    ) {
        var stateId = KeyNames.stateId
        var name = findName()
        fun findName(): String {
            // this can be incorrect as long as we don't know the correct mapping
            val char = KeyNames.inputMap[state]
            val text2 = if (char != -1 && char != 32 && char != 9 && char != 10) // space, \n, \r
                char.joinChars() else null
            val text0 = KeyCombination.keyMapping.reverse[key]
            return text2 ?: text0 ?: key.toString()
        }
    }

    private const val lower = 0.8f // full strength while hold

    @JvmStatic
    private fun addKey(keyCode: Key, isSuperKey: Boolean) {
        var key = activeKeysMap[keyCode]
        if (key == null) {
            key = Key1(keyCode, isSuperKey, 2f)
            activeKeys += key
            activeKeysMap[keyCode] = key
        } else {
            key.time =
                if (key.time < lower) lower
                else mix(key.time, lower, uiDeltaTime.toFloat() * 5f)
        }
    }

    @JvmStatic
    private fun drawKey(text: String, alpha: Float, x0: Int, hmy: Int): Int {

        val bgColor = template.backgroundColor
        val textColor = template.textColor
        val fontSize = font.sizeInt

        // background
        val rgbMask = 0xffffff
        val alphaMask = clamp(alpha * 255, 0f, 255f).toInt().shl(24) or rgbMask
        val color = textColor and alphaMask
        val w0 = getTextSizeX(font, text)
        drawRect(x0 + 5, hmy - 12 - fontSize, w0 + 10, fontSize + 8, bgColor and alphaMask)

        // text
        val textColor2 = color and alphaMask
        val bgColor2 = bgColor and 0xffffff
        val x = x0 + 10
        val y = hmy - 10 - fontSize
        val pbb = DrawTexts.pushBetterBlending(true)
        drawTextOrFail(x, y, font, text, textColor2, bgColor2, -1, -1)
        DrawTexts.popBetterBlending(pbb)

        return x0 + w0 + 16
    }

    @JvmStatic
    fun draw(x: Int, y: Int, h: Int) {

        // draw the current keys for a tutorial...
        // fade out keys
        // blink when typed (overexposure to get attention)

        // full strength at start is 1

        for ((key, _) in Input.keysDown) {
            when (key) {
                Key.KEY_LEFT_CONTROL,
                Key.KEY_RIGHT_CONTROL -> addKey(Key.KEY_LEFT_CONTROL, true)
                Key.KEY_LEFT_SHIFT,
                Key.KEY_RIGHT_SHIFT -> addKey(Key.KEY_LEFT_SHIFT, true)
                Key.KEY_LEFT_ALT,
                Key.KEY_RIGHT_ALT -> addKey(Key.KEY_LEFT_ALT, true)
                Key.KEY_LEFT_SUPER,
                Key.KEY_RIGHT_SUPER -> addKey(Key.KEY_LEFT_SUPER, true)
                else -> addKey(key, false)
            }
        }

        if (activeKeys.isEmpty()) return
        val dt = uiDeltaTime.toFloat() * decaySpeed
        val iter = activeKeys.iterator()
        for (key in iter) {
            key.time = min(key.time - dt, 1f)
            if (key.time < 0f) {
                activeKeysMap.remove(key.key)
                iter.remove()
            }
        }

        if (activeKeys.isEmpty()) return
        activeKeys.sortBy { !it.isSuperKey }
        renderDefault {
            var x0 = x
            for (index in activeKeys.indices) {
                val key = activeKeys[index]
                val alpha = key.time
                if (key.stateId != KeyNames.stateId) {
                    key.name = key.findName()
                }
                x0 = drawKey(key.name, alpha, x0, h - y)
            }
        }
    }
}