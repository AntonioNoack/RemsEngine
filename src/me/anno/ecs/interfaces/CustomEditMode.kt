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

    /**
     * returns whether event was consumed
     * */
    fun onEditClick(button: Key, long: Boolean): Boolean = false

    /**
     * returns whether event was consumed
     * */
    fun onEditMove(x: Float, y: Float, dx: Float, dy: Float) = false

    /**
     * returns whether event was consumed
     * */
    fun onEditDown(button: Key) = false

    /**
     * returns whether event was consumed
     * */
    fun onEditUp(button: Key) = false

    /**
     * returns whether event was consumed
     * */
    fun onEditTyped(char: Int) = false
}