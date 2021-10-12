package me.anno.ecs.interfaces

import me.anno.input.MouseButton

interface CustomEditMode {

    // todo right click: on, catchable
    // todo middle click: ok, awkward
    // todo left click: only if in special mode, escape with esc?
    // todo     - show border, that in special edit mode
    // todo left drag: move stuff -> overrideable (?)

    // todo functions return whether they consumed the event (?)

    fun getEditModeBorderColor(): Int = 0x00ff00

    fun onEditClick(button: MouseButton, long: Boolean): Boolean = false
    fun onEditClick(button: Int) = false

    fun onEditMove(x: Float, y: Float, dx: Float, dy: Float) = false

    fun onEditDown(button: MouseButton) = false
    fun onEditDown(button: Int) = false

    fun onEditUp(button: MouseButton) = false
    fun onEditUp(button: Int) = false

    fun onEditTyped(char: Int) = false

}