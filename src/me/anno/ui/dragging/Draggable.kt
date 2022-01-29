package me.anno.ui.dragging

import me.anno.gpu.GFX.loadTexturesSync
import me.anno.ui.Panel
import me.anno.ui.base.constraints.WrapAlign

class Draggable(
    private val content: String,
    private val contentType: String,
    private val original: Any?,
    val ui: Panel
): IDraggable {

    init {
        ui += WrapAlign.LeftTop
        loadTexturesSync.push(true)
        ui.calculateSize(300, 300)
        ui.applyPlacement(300, 300)
        loadTexturesSync.pop()
    }

    override fun draw(x: Int, y: Int) {
        ui.placeInParent(x, y)
        ui.draw(x, y, x + ui.w, y + ui.h)
    }

    override fun getSize(w: Int, h: Int): Pair<Int, Int> {
        // ui.applyConstraints()
        return ui.w to ui.h
    }

    override fun getContent(): String = content
    override fun getContentType(): String = contentType
    override fun getOriginal(): Any? = original

}