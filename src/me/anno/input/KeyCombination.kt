package me.anno.input

import me.anno.utils.BiMap
import org.lwjgl.glfw.GLFW.*

class KeyCombination(val key: Int, val modifiers: Int, val type: Type){

    val hash = key.shl(8) + modifiers * 3 + type.hash

    enum class Type(val hash: Int){
        DOWN(0),
        PRESS(1),
        PRESS_UNSAFE(2),
        UP(3),
        TYPED(4),
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

        operator fun get(name: String) = keyMapping[name]
        operator fun get(key: Int) = keyMapping.reverse[key]

        val keyMapping = BiMap<String, Int>(200)
        fun put(key: Int, vararg buttons: String){
            buttons.forEach { keyMapping[it] = key; keyMapping[it.toLowerCase()] = key }
        }
        init {
            for(c in 'a' .. 'z') keyMapping["$c"] = GLFW_KEY_A + (c.toInt() - 'a'.toInt())
            for(c in '0' .. '9') keyMapping["$c"] = GLFW_KEY_0 + (c.toInt() - '0'.toInt())
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
            put(GLFW_KEY_UP, "topArrow", "arrowUp")
            put(GLFW_KEY_DOWN, "bottomArrow", "arrowDown")
            put(GLFW_KEY_PAGE_UP, "pageUp")
            put(GLFW_KEY_PAGE_DOWN, "pageDown")
            for(i in 1 .. 25) put(GLFW_KEY_F1 - 1 + i, "f$i")
            put(GLFW_MOUSE_BUTTON_LEFT, "left")
            put(GLFW_MOUSE_BUTTON_RIGHT, "right")
            put(GLFW_MOUSE_BUTTON_MIDDLE, "middle")
            for(i in 0 .. 9) put(GLFW_KEY_KP_0 + i, "kp$i", "num$i", "numpad$i", "numblock$i")
        }

        fun getButton(button: String): Int {
            val asKey = keyMapping[button] ?: keyMapping[button.toLowerCase()]
            if(asKey != null) return asKey
            return when(button.toLowerCase()){
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
                    if(asInt != null) return asInt
                    println("Button unknown: $button")
                    -1
                }
            }
        }

        fun parse(button: String, event: String, modifiers: String): KeyCombination? {
            val key = getButton(button)
            if(key < 0) return null
            val type = when(event.toLowerCase()){
                "down" -> Type.DOWN
                "press" -> Type.PRESS
                "typed" -> Type.TYPED
                "up" -> Type.UP
                "press-unsafe" -> Type.PRESS_UNSAFE
                else -> return null
            }
            var mods = 0
            for(c in modifiers){
                when(c.toLowerCase()){
                    'c' -> mods = mods or GLFW_MOD_CONTROL
                    's' -> mods = mods or GLFW_MOD_SHIFT
                    'a' -> mods = mods or GLFW_MOD_ALT
                    'x' -> mods = mods or GLFW_MOD_SUPER
                    'n' -> mods = mods or GLFW_MOD_NUM_LOCK
                    'l' -> mods = mods or GLFW_MOD_CAPS_LOCK
                    ' ' -> {}
                    else -> println("Unknown action modifier '$c'")
                }
            }
            return KeyCombination(key, mods, type)
        }

    }

}