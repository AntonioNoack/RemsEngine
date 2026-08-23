package me.anno.ui.dragging

import me.anno.gpu.GFX.loadTexturesSync
import me.anno.ui.Canvas
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

    override fun draw(canvas: Canvas) {
        ui.setPosSize(canvas.x0, canvas.y0, canvas.x1 - canvas.x0, canvas.y1 - canvas.y0)
        ui.draw(canvas)
    }

    override fun getContent(): String = content
    override fun getContentType(): String = contentType
    override fun getOriginal(): Any? = original
}