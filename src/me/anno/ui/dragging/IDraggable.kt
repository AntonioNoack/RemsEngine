package me.anno.ui.dragging

import me.anno.utils.structures.tuples.IntPair

interface IDraggable {
    fun draw(x: Int, y: Int)
    fun getSize(w: Int, h: Int): IntPair
    fun getContent(): String
    fun getContentType(): String
    fun getOriginal(): Any?
}