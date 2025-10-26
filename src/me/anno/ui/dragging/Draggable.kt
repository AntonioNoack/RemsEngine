package me.anno.ui.dragging

import me.anno.gpu.GFX.loadTexturesSync
import me.anno.ui.Panel
import me.anno.ui.Style
import me.anno.ui.base.text.TextPanel
import org.joml.Vector2i

/**
 * Standard implementation for IDraggable
 * */
open class Draggable(
    private val content: String,
    private val contentType: String,
    private val original: Any?,
    val ui: Panel
) : IDraggable {

    constructor(
        content: String, contentType: String, original: Any?,
        title: String, style: Style
    ) : this(content, contentType, original, TextPanel(title, style))

    @Suppress("unused")
    constructor(
        content: String, contentType: String, original: Any?,
        style: Style
    ) : this(content, contentType, original, content, style)

    init {
        loadTexturesSync.push(true)
        ui.calculateSize(300, 300)
        loadTexturesSync.pop()
    }

    override fun getSize(w: Int, h: Int): Vector2i {
        return Vector2i(ui.minW, ui.minH)
    }

    override fun draw(x0: Int, y0: Int, x1: Int, y1: Int) {
        ui.setPosSize(x0, y0, x1 - x0, y1 - y0)
        ui.draw(x0, y0, x1, y1)
    }

    override fun getContent(): String = content
    override fun getContentType(): String = contentType
    override fun getOriginal(): Any? = original
}