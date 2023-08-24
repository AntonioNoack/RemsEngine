package me.anno.input

import me.anno.utils.types.Booleans.toInt

@Suppress("unused")
object Modifiers {

    @JvmStatic
    private fun modifierString(ctrl: Boolean, shift: Boolean, alt: Boolean): String {
        val str = StringBuilder(3)
        if (ctrl) str.append('c')
        if (shift) str.append('s')
        if (alt) str.append('a')
        return str.toString()
    }

    @JvmStatic
    private val modifiers = Array(8) {
        modifierString((it and 4) > 0, (it and 2) > 0, (it and 1) > 0)
    }

    @JvmStatic
    operator fun get(ctrl: Boolean, shift: Boolean, alt: Boolean): String =
        modifiers[ctrl.toInt(4) + shift.toInt(2) + alt.toInt(1)]

    @JvmStatic
    operator fun get(ctrl: Boolean, shift: Boolean): String = get(ctrl, shift, false)

    @JvmField
    val shift = get(ctrl = false, shift = true, alt = false)

    @JvmField
    val control = get(ctrl = true, shift = false, alt = false)

    @JvmField
    val alt = get(ctrl = false, shift = false, alt = true)
}