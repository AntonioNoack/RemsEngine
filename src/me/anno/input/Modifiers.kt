package me.anno.input

import me.anno.utils.types.Booleans.toInt

@Suppress("unused")
object Modifiers {

    @JvmStatic
    private fun modifierString(ctrl: Boolean, shift: Boolean, alt: Boolean): String {
        return "${if (ctrl) "c" else ""}${if (shift) "s" else ""}${if (alt) "a" else ""}"
    }

    @JvmStatic
    private val modifiers = Array(8) { modifierString((it and 4) > 0, (it and 2) > 0, (it and 1) > 0) }

    @JvmStatic
    operator fun get(ctrl: Boolean, shift: Boolean, alt: Boolean) =
        modifiers[ctrl.toInt(4) + shift.toInt(2) + alt.toInt(1)]

    @JvmStatic
    operator fun get(ctrl: Boolean, shift: Boolean) = get(ctrl, shift, false)

    @JvmField
    val shift = get(ctrl = false, shift = true, alt = false)

    @JvmField
    val control = get(ctrl = true, shift = false, alt = false)

    @JvmField
    val alt = get(ctrl = false, shift = false, alt = true)

}