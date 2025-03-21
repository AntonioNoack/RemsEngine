package me.anno.input

import me.anno.utils.structures.maps.BiMap
import me.anno.utils.types.Booleans.hasFlag
import me.anno.utils.types.Strings.distance
import org.apache.logging.log4j.LogManager
import org.lwjgl.glfw.GLFW

class KeyCombination(val key: Key, val modifiers: Int, val type: Type) {

    val hash = key.ordinal.shl(8) + modifiers * 6 + type.id

    val isControl: Boolean get() = modifiers.hasFlag(GLFW.GLFW_MOD_CONTROL)
    val isShift: Boolean get() = modifiers.hasFlag(GLFW.GLFW_MOD_SHIFT)
    val isAlt: Boolean get() = modifiers.hasFlag(GLFW.GLFW_MOD_ALT)
    val isSuper: Boolean get() = modifiers.hasFlag(GLFW.GLFW_MOD_SUPER)

    enum class Type(val id: Int) {
        /** once when down; "down", "d" */
        DOWN(0),

        /** while pressing, but only after a delay of 0.5s, or when moving around (not a click); "dragging" */
        DRAGGING(1),

        /** while pressing; "press-unsafe", "pressing", "p" */
        PRESSING(2),

        /** once when up; "up", "u" */
        UP(3),

        /** once when down, then repeatedly after a delay; "typed", "t" */
        TYPED(4),

        /** double click required, "double", "double-click", "2" */
        DOUBLE(5)
    }

    override fun hashCode() = hash
    override fun equals(other: Any?) =
        other is KeyCombination && other.key == key && other.modifiers == modifiers && other.type == type

    override fun toString() =
        "${get(key) ?: key}:${if (isControl) "c" else ""}${if (isShift) "s" else ""}${if (isAlt) "a" else ""}:$type"

    companion object {

        private val LOGGER = LogManager.getLogger(KeyCombination::class)

        operator fun get(name: String): Key? = keyMapping[name]
        operator fun get(key: Key): String? = keyMapping.reverse[key]

        val keyMapping = BiMap<String, Key>(200)
        fun put(key: Key, button: String) {
            keyMapping[button] = key
            keyMapping[button.lowercase()] = key
        }

        // not really necessary, just saving a few allocations
        fun put(key: Key, button1: String, button2: String) {
            put(key, button1)
            put(key, button2)
        }

        // not really necessary, just saving a few allocations
        fun put(key: Key, button1: String, button2: String, button3: String) {
            put(key, button1)
            put(key, button2)
            put(key, button3)
        }

        fun put(key: Key, vararg buttons: String) {
            for (button in buttons) {
                put(key, button)
            }
        }

        fun put(key: Int, vararg buttons: String) {
            put(Key.byId(key), *buttons)
        }

        init {
            for (c in 'a'..'z') put(GLFW.GLFW_KEY_A + (c.code - 'a'.code), "$c")
            for (c in '0'..'9') put(GLFW.GLFW_KEY_0 + (c.code - '0'.code), "$c")
            put(Key.KEY_SPACE, " ", "space")
            put(Key.KEY_ENTER, "\n", "enter")
            put(Key.KEY_BACKSPACE, "<--", "backspace")
            put(Key.KEY_BACKSLASH, "\\", "backslash")
            put(Key.KEY_SLASH, "/", "slash")
            put(Key.KEY_SEMICOLON, ";", "semicolon")
            put(Key.KEY_EQUAL, "=", "equal", "equals")
            put(Key.KEY_WORLD_1, "world-1")
            put(Key.KEY_WORLD_2, "world-2")
            put(Key.KEY_TAB, "\t", "tab")
            put(Key.KEY_INSERT, "insert")
            put(Key.KEY_DELETE, "delete")
            put(Key.KEY_ARROW_LEFT, "<-", "leftArrow", "arrowLeft")
            put(Key.KEY_ARROW_RIGHT, "->", "rightArrow", "arrowRight")
            put(Key.KEY_ARROW_UP, "topArrow", "arrowTop", "upArrow", "arrowUp")
            put(Key.KEY_ARROW_DOWN, "bottomArrow", "arrowBottom", "downArrow", "arrowDown")
            put(Key.KEY_PAGE_UP, "pageUp")
            put(Key.KEY_PAGE_DOWN, "pageDown")
            put(Key.KEY_HOME, "pos1", "home")
            put(Key.KEY_END, "end")
            put(Key.KEY_CAPS_LOCK, "capsLock")
            put(Key.KEY_SCROLL_LOCK, "scrollLock")
            put(Key.KEY_NUM_LOCK, "numLock")
            put(Key.KEY_MINUS, "?", "-")
            put(Key.KEY_RIGHT_BRACKET, "+", "rightBracket")
            put(Key.KEY_PAUSE, "pause")
            put(Key.KEY_GRAVE_ACCENT, "degrees", "^", "grave")
            for (i in 1..25) {
                put(GLFW.GLFW_KEY_F1 - 1 + i, "f$i")
            }
            put(Key.KEY_KP_ADD, "kp+", "+")
            put(Key.KEY_KP_SUBTRACT, "kp-", "-")
            put(Key.KEY_KP_MULTIPLY, "kp*", "*")
            put(Key.KEY_KP_DIVIDE, "kp/", "/")
            put(Key.KEY_KP_DECIMAL, "kp,", "kp.", ".")
            put(Key.KEY_KP_ENTER, "kpEnter", "kp\n", "kp\\n", "r-enter", "kp-enter")
            put(Key.KEY_KP_EQUAL, "kp=", "=")
            put(Key.BUTTON_LEFT, "left")
            put(Key.BUTTON_RIGHT, "right")
            put(Key.BUTTON_MIDDLE, "middle")
            put(Key.BUTTON_FORWARD, "mouseForward")
            put(Key.BUTTON_BACK, "mouseBackward")
            for (i in 0..9) {
                @Suppress("SpellCheckingInspection")
                put(GLFW.GLFW_KEY_KP_0 + i, "kp$i", "num$i", "numpad$i", "numblock$i")
            }
            put(Key.KEY_PRINT_SCREEN, "print", "printScreen")
            put(Key.KEY_MENU, "menu", "printMenu")
            put(Key.KEY_LEFT_CONTROL, "l-control", "l-ctrl", "control", "ctrl")
            put(Key.KEY_RIGHT_CONTROL, "r-control", "r-ctrl")
            put(Key.KEY_LEFT_SHIFT, "l-shift", "shift")
            put(Key.KEY_RIGHT_SHIFT, "r-shift")
            put(Key.KEY_LEFT_SUPER, "l-windows", "l-super", "windows", "super")
            put(Key.KEY_RIGHT_SUPER, "r-windows", "r-shift")
            put(Key.KEY_LEFT_ALT, "l-alt", "alt")
            put(Key.KEY_RIGHT_ALT, "r-alt")
            put(Key.KEY_COMMA, ",", "comma")
            put(Key.KEY_PERIOD, ".", "period", "dot")
            put(Key.KEY_ESCAPE, "esc", "escape")
        }

        fun getButton(button: String): Key {
            val asKey = keyMapping[button] ?: keyMapping[button.lowercase()]
            if (asKey != null) return asKey
            val asInt = button.toIntOrNull()
            if (asInt != null) return Key.byId(asInt)
            val bestMatch = keyMapping.keys.minByOrNull { button.distance(it, true) }
            LOGGER.warn("Button unknown: '$button', did you mean '$bestMatch'?")
            return Key.KEY_UNKNOWN
        }

        private fun getType(event: String): Type? {
            return when (event.lowercase()) {
                "down", "d" -> Type.DOWN
                "press", "dragging", "drag" -> Type.DRAGGING
                "typed", "t" -> Type.TYPED
                "up", "u" -> Type.UP
                "pressing", "press-unsafe", "p" -> Type.PRESSING
                "double", "double-click", "2" -> Type.DOUBLE
                else -> null
            }
        }

        private fun getModifiers(modifiers: String): Int {
            var mods = 0
            for (c in modifiers) {
                mods = mods or getModifier(c)
            }
            return mods
        }

        private fun getModifier(c: Char): Int {
            return when (c.lowercaseChar()) {
                'c' -> GLFW.GLFW_MOD_CONTROL
                's' -> GLFW.GLFW_MOD_SHIFT
                'a' -> GLFW.GLFW_MOD_ALT
                'x' -> GLFW.GLFW_MOD_SUPER
                'n' -> GLFW.GLFW_MOD_NUM_LOCK
                'l' -> GLFW.GLFW_MOD_CAPS_LOCK
                ' ' -> 0 // ignore spaces
                else -> {
                    LOGGER.warn("Unknown action modifier '$c'")
                    0
                }
            }
        }

        fun parse(button: String, event: String, modifiers: String?): KeyCombination? {
            val key = getButton(button)
            if (key == Key.KEY_UNKNOWN) return null
            val type = getType(event) ?: return null
            val mods = if (modifiers != null) getModifiers(modifiers) else 0
            return KeyCombination(key, mods, type)
        }
    }
}