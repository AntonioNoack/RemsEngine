package me.anno.ecs.interfaces

import me.anno.utils.Color.black
import me.anno.input.MouseButton

interface CustomEditMode {

    // functions return whether they consumed the event

    fun getEditModeBorderColor(): Int = 0x00ff00 or black

    fun onEditClick(button: MouseButton, long: Boolean): Boolean = false
    fun onEditClick(button: Int) = false

    fun onEditMove(x: Float, y: Float, dx: Float, dy: Float) = false

    fun onEditDown(button: MouseButton) = false
    fun onEditDown(button: Int) = false

    fun onEditUp(button: MouseButton) = false
    fun onEditUp(button: Int) = false

    fun onEditTyped(char: Int) = false

}