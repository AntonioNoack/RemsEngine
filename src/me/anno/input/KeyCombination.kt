package me.anno.input

import me.anno.utils.structures.maps.BiMap
import org.apache.logging.log4j.LogManager
import org.lwjgl.glfw.GLFW.*
import java.util.*

class KeyCombination(val key: Int, val modifiers: Int, val type: Type){

    val hash = key.shl(8) + modifiers * 6 + type.hash

    val isControl = (modifiers and GLFW_MOD_CONTROL) != 0
    val isShift = (modifiers and GLFW_MOD_SHIFT) != 0
    val isAlt = (modifiers and GLFW_MOD_ALT) != 0

    val isWritingKey = !isControl && !isAlt && !(key == GLFW_KEY_SPACE && isShift)

    enum class Type(val hash: Int){
        DOWN(0),
        PRESS(1),
        PRESS_UNSAFE(2),
        UP(3),
        TYPED(4),
        DOUBLE(5)
    }

    override fun hashCode() = hash
    override fun equals(other: Any?): Boolean {
        return other is KeyCombination &&
                other.hash == hash &&
                other.key == key &&
                other.modifiers == modifiers &&
                other.type == type
    }

    override fun toString() = "$key:$modifiers:$type"

    companion object {

        private val LOGGER = LogManager.getLogger(KeyCombination::class.java)

        operator fun get(name: String) = keyMapping[name]
        operator fun get(key: Int) = keyMapping.reverse[key]

        val keyMapping = BiMap<String, Int>(200)
        fun put(key: Int, vararg buttons: String){
            buttons.forEach {
                keyMapping[it] = key
                keyMapping[it.lowercase(Locale.getDefault())] = key
                keyMapping[it.uppercase(Locale.getDefault())] = key
            }
        }

        init {
            for(c in 'a' .. 'z') keyMapping["$c"] = GLFW_KEY_A + (c.code - 'a'.code)
            for(c in '0' .. '9') keyMapping["$c"] = GLFW_KEY_0 + (c.code - '0'.code)
            put(GLFW_KEY_SPACE, " ", "space")
            put(GLFW_KEY_ENTER, "\n", "enter")
            put(GLFW_KEY_BACKSPACE, "<--", "backspace")
            put(GLFW_KEY_BACKSLASH, "\\", "backslash")
            put(GLFW_KEY_SLASH, "/", "slash")
            put(GLFW_KEY_SEMICOLON, ";", "semicolon")
            put(GLFW_KEY_EQUAL, "=", "equal", "equals")
            put(GLFW_KEY_WORLD_1, "world-1")
            put(GLFW_KEY_WORLD_2, "world-2")
            put(GLFW_KEY_TAB, "\t", "tab")
            put(GLFW_KEY_INSERT, "insert")
            put(GLFW_KEY_DELETE, "delete")
            put(GLFW_KEY_LEFT, "<-", "leftArrow", "arrowLeft")
            put(GLFW_KEY_RIGHT, "->", "rightArrow", "arrowRight")
            put(GLFW_KEY_UP, "topArrow", "arrowTop", "upArrow", "arrowUp")
            put(GLFW_KEY_DOWN, "bottomArrow", "arrowBottom", "downArrow", "arrowDown")
            put(GLFW_KEY_PAGE_UP, "pageUp")
            put(GLFW_KEY_PAGE_DOWN, "pageDown")
            for(i in 1 .. 25) put(GLFW_KEY_F1 - 1 + i, "f$i")
            put(GLFW_MOUSE_BUTTON_LEFT, "left")
            put(GLFW_MOUSE_BUTTON_RIGHT, "right")
            put(GLFW_MOUSE_BUTTON_MIDDLE, "middle")
            put(GLFW_MOUSE_BUTTON_5, "mouseForward")
            put(GLFW_MOUSE_BUTTON_4, "mouseBackward")
            for(i in 0 .. 9) put(GLFW_KEY_KP_0 + i, "kp$i", "num$i", "numpad$i", "numblock$i")
            put(GLFW_KEY_PRINT_SCREEN, "print", "printScreen")
            put(GLFW_KEY_MENU, "menu", "printMenu")
            put(GLFW_KEY_LEFT_CONTROL, "control", "ctrl")
            put(GLFW_KEY_RIGHT_CONTROL, "r-control", "r-ctrl")
            put(GLFW_KEY_LEFT_SHIFT, "shift")
            put(GLFW_KEY_RIGHT_SHIFT, "r-shift")
            put(GLFW_KEY_LEFT_SUPER, "super")
            put(GLFW_KEY_RIGHT_SUPER, "r-shift")
            put(GLFW_KEY_LEFT_ALT, "alt")
            put(GLFW_KEY_RIGHT_ALT, "r-alt")
            put(GLFW_KEY_COMMA, ",", "comma")
            put(GLFW_KEY_PERIOD, ".", "period", "dot")
            put(GLFW_KEY_ESCAPE, "esc", "escape")
        }

        fun getButton(button: String): Int {
            val asKey = keyMapping[button] ?: keyMapping[button.lowercase(Locale.getDefault())]
            if (asKey != null) return asKey
            return when (button.lowercase(Locale.getDefault())) {
                // kp = key pad = num pad probably
                "kp," -> GLFW_KEY_KP_DECIMAL
                "kp/" -> GLFW_KEY_KP_DIVIDE
                "kp*" -> GLFW_KEY_KP_MULTIPLY
                "kp-" -> GLFW_KEY_KP_SUBTRACT
                "kp+" -> GLFW_KEY_KP_ADD
                "kpenter", "kp\n" -> GLFW_KEY_KP_ENTER
                "kp=" -> GLFW_KEY_KP_EQUAL
                else -> {
                    val asInt = button.toIntOrNull()
                    if (asInt != null) return asInt
                    LOGGER.warn("Button unknown: $button")
                    -1
                }
            }
        }

        fun parse(button: String, event: String, modifiers: String): KeyCombination? {
            val key = getButton(button)
            if (key < 0) return null
            val type = when (event.lowercase(Locale.getDefault())) {
                "down", "d" -> Type.DOWN
                "press" -> Type.PRESS
                "typed", "t" -> Type.TYPED
                "up", "u" -> Type.UP
                "press-unsafe", "p" -> Type.PRESS_UNSAFE
                "double", "double-click", "2" -> Type.DOUBLE
                else -> return null
            }
            var mods = 0
            for (c in modifiers) {
                when (c.lowercaseChar()) {
                    'c' -> mods = mods or GLFW_MOD_CONTROL
                    's' -> mods = mods or GLFW_MOD_SHIFT
                    'a' -> mods = mods or GLFW_MOD_ALT
                    'x' -> mods = mods or GLFW_MOD_SUPER
                    'n' -> mods = mods or GLFW_MOD_NUM_LOCK
                    'l' -> mods = mods or GLFW_MOD_CAPS_LOCK
                    ' ' -> {
                    }
                    else -> LOGGER.warn("Unknown action modifier '$c'")
                }
            }
            return KeyCombination(key, mods, type)
        }

    }

}