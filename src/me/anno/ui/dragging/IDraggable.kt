package me.anno.ui.dragging

import me.anno.utils.structures.tuples.IntPair

/**
 * Something that can be dragged and pasted into some things.
 * */
interface IDraggable {
    fun draw(x0: Int, y0: Int, x1: Int, y1: Int)
    fun getSize(w: Int, h: Int): IntPair
    fun getContent(): String
    fun getContentType(): String
    fun getOriginal(): Any?
}