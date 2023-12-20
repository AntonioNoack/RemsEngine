package me.anno.ecs.interfaces

import me.anno.input.Key
import me.anno.utils.Color.black

/**
 * interface for Components that may want to override the UI controls in the editor,
 * e.g., for terrain painting
 *
 * functions return whether they consumed the event
 * */
interface CustomEditMode {
    fun getEditModeBorderColor(): Int = 0x00ff00 or black
    fun onEditClick(button: Key, long: Boolean): Boolean = false
    fun onEditMove(x: Float, y: Float, dx: Float, dy: Float) = false
    fun onEditDown(button: Key) = false
    fun onEditUp(button: Key) = false
    fun onEditTyped(char: Int) = false
}