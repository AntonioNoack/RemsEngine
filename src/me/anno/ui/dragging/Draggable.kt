package me.anno.ui.dragging

import me.anno.ui.base.Panel
import me.anno.ui.base.constraints.WrapAlign

class Draggable(
    private val content: String,
    private val contentType: String,
    private val original: Any,
    val ui: Panel
): IDraggable {

    init {
        ui += WrapAlign.LeftTop
    }

    override fun draw(x: Int, y: Int) {
        ui.placeInParent(x, y)
        ui.draw(x, y, x + ui.w, y + ui.h)
    }

    override fun getSize(w: Int, h: Int): Pair<Int, Int> {
        ui.calculateSize(w, h)
        ui.applyConstraints()
        return ui.w to ui.h
    }

    override fun getContent(): String = content
    override fun getContentType(): String = contentType
    override fun getOriginal(): Any = original

}