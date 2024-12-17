package me.anno.ui.editor.files

import me.anno.gpu.drawing.DrawTexts.drawTextOrFail
import me.anno.gpu.texture.Filtering
import me.anno.gpu.texture.ITexture2D
import me.anno.gpu.texture.TextureCache
import me.anno.image.thumbs.Thumbs
import me.anno.input.Key
import me.anno.io.files.FileReference
import me.anno.maths.Maths.max
import me.anno.ui.Style
import me.anno.ui.base.components.AxisAlignment
import me.anno.ui.base.image.ImagePanel

class ImageViewer(val files: List<FileReference>, style: Style) : ImagePanel(style) {

    var index = 0
    val file get() = files[index]

    init {
        showAlpha = true
    }

    override fun getTexture(): ITexture2D? {
        return TextureCache[file, true]?.createdOrNull()
            ?: Thumbs[file, max(width, height), true]
    }

    override fun onUpdate() {
        super.onUpdate()
        getTexture() // keep texture loaded
    }

    val font = style.getFont("text")
    override fun onDraw(x0: Int, y0: Int, x1: Int, y1: Int) {
        super.onDraw(x0, y0, x1, y1)
        // todo draw controls into the background
        // todo show image statistics in a corner?
        // todo switch sources on the parent, so folders with images and videos can be (dis)played properly?
        // todo if texture still loads, show loading circle

        // todo bug: background isn't drawn... why???
        val failed = drawTextOrFail( // draw file name at bottom center
            x + width / 2, y + height, font, file.name,
            -1, backgroundColor, width, -1,
            AxisAlignment.CENTER, AxisAlignment.MAX
        )
        if (failed) invalidateDrawing()
    }

    fun step(di: Int) {
        index = (index + di) % files.size
        invalidateDrawing()
    }

    fun prev() = step(files.size - 1)
    fun next() = step(1)
    fun reset() {
        zoom = 1f
        offsetX = 0f
        offsetY = 0f
        invalidateDrawing()
    }

    override fun onMouseClicked(x: Float, y: Float, button: Key, long: Boolean) {
        if (button == Key.BUTTON_LEFT && !long) next()
        else prev()
    }

    override fun onDoubleClick(x: Float, y: Float, button: Key) {
        onMouseClicked(x, y, button, false)
    }

    override fun onKeyTyped(x: Float, y: Float, key: Key) {
        when (key) {
            Key.KEY_ARROW_LEFT, Key.KEY_ARROW_UP, Key.KEY_PAGE_UP -> prev()
            Key.KEY_ARROW_RIGHT, Key.KEY_ARROW_DOWN, Key.KEY_PAGE_DOWN -> next()
            Key.KEY_0, Key.KEY_KP_0 -> reset()
            Key.KEY_SPACE -> toggleFiltering()
            else -> super.onKeyTyped(x, y, key)
        }
    }

    fun toggleFiltering() {
        filtering =
            if (filtering == Filtering.NEAREST) Filtering.LINEAR
            else Filtering.NEAREST
        invalidateDrawing()
    }
}