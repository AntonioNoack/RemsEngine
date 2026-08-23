package me.anno.ui.dragging

import me.anno.ui.canvas.Canvas
import org.joml.Vector2i

/**
 * Something that can be dragged and pasted into some things.
 * */
interface IDraggable {
    fun draw(canvas: Canvas)
    fun getSize(w: Int, h: Int): Vector2i
    fun getContent(): String
    fun getContentType(): String
    fun getOriginal(): Any?
}