package me.anno.ui.dragging

import org.joml.Vector2i

/**
 * Something that can be dragged and pasted into some things.
 * */
interface IDraggable {
    fun draw(x0: Int, y0: Int, x1: Int, y1: Int)
    fun getSize(w: Int, h: Int): Vector2i
    fun getContent(): String
    fun getContentType(): String
    fun getOriginal(): Any?
}