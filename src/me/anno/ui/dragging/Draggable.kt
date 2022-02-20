package me.anno.ui.dragging

import me.anno.gpu.GFX.loadTexturesSync
import me.anno.ui.Panel
import me.anno.ui.base.constraints.WrapAlign
import me.anno.ui.base.text.TextPanel
import me.anno.ui.style.Style

class Draggable(
    private val content: String,
    private val contentType: String,
    private val original: Any?,
    val ui: Panel
) : IDraggable {

    constructor(
        content: String, contentType: String, original: Any?,
        title: String, style: Style
    ) : this(content, contentType, original, TextPanel(title, style))

    constructor(
        content: String, contentType: String, original: Any?,
        style: Style
    ) : this(content, contentType, original, content, style)

    init {
        ui += WrapAlign.LeftTop
        loadTexturesSync.push(true)
        ui.calculateSize(300, 300)
        ui.setSize(300, 300)
        loadTexturesSync.pop()
    }

    override fun draw(x: Int, y: Int) {
        ui.setPosition(x, y)
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