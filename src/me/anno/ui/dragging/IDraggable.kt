package me.anno.ui.dragging

interface IDraggable {
    fun draw(x: Int, y: Int)
    fun getSize(w: Int, h: Int): Pair<Int, Int>
    fun getContent(): String
    fun getContentType(): String
    fun getOriginal(): Any
}