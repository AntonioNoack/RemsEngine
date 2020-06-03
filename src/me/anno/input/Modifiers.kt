package me.anno.input

object Modifiers {

    private fun modifierString(ctrl: Boolean, shift: Boolean, alt: Boolean): String {
        return "${if(ctrl) "c" else ""}${if(shift) "s" else ""}${if(alt) "a" else ""}"
    }

    private val modifiers = Array(8){ modifierString((it and 4) > 0, (it and 2) > 0, (it and 1) > 0) }

    operator fun get(ctrl: Boolean, shift: Boolean, alt: Boolean) =
        modifiers[(if(ctrl) 4 else 0) + (if(shift) 2 else 0) + (if(alt) 1 else 0)]
    operator fun get(ctrl: Boolean, shift: Boolean) = get(ctrl, shift, false)

}