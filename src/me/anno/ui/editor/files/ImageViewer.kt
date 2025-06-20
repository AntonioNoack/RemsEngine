package me.anno.ui.editor.files

import me.anno.config.DefaultStyle
import me.anno.gpu.drawing.DrawRounded.drawRoundedRect
import me.anno.gpu.drawing.DrawTexts.drawTextOrFail
import me.anno.gpu.drawing.DrawTexts.getTextSize
import me.anno.gpu.drawing.DrawTexts.getTextSizeOr
import me.anno.gpu.drawing.GFXx2D.getSize
import me.anno.gpu.drawing.GFXx2D.getSizeX
import me.anno.gpu.drawing.GFXx2D.getSizeY
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
import me.anno.utils.Color.withAlpha

class ImageViewer(val files: List<FileReference>, style: Style) : ImagePanel(style) {

    var index = 0
    val file get() = files[index]

    init {
        showAlpha = true
    }

    override fun getTexture(): ITexture2D? {
        return TextureCache[file].value?.createdOrNull()
            ?: Thumbs[file, max(width, height), true]
    }

    override fun onUpdate() {
        super.onUpdate()
        getTexture() // keep texture loaded
    }

    val font = style.getFont("text")
    val textColor = style.getColor("textColor", DefaultStyle.iconGray)

    override fun draw(x0: Int, y0: Int, x1: Int, y1: Int) {
        super.draw(x0, y0, x1, y1)

        // todo draw controls into the background
        // todo show image statistics in a corner?
        // todo switch sources on the parent, so folders with images and videos can be (dis)played properly?
        // todo if texture still loads, show loading circle

        val padding = 10
        val widthLimit = width
        val radius = padding.toFloat()
        val title = file.name
        val textSize = getTextSizeOr(font, title, widthLimit, -1)
        val textWidth = getSizeX(textSize) + padding
        val textHeight = getSizeY(textSize) + padding.shr(1)
        drawRoundedRect(
            x + (width - textWidth) / 2, y + height - textHeight - padding.shr(2), textWidth, textHeight,
            radius, radius, radius, radius, 0f, backgroundColor, backgroundColor,
            backgroundColor.withAlpha(0), 1f
        )
        drawTextOrFail( // draw file name at bottom center
            x + width / 2, y + height - padding.shr(1), font, title,
            textColor, backgroundColor, widthLimit, -1,
            AxisAlignment.CENTER, AxisAlignment.MAX
        )
    }

    fun step(di: Int) {
        index = (index + di) % files.size
    }

    fun prev() = step(files.size - 1)
    fun next() = step(1)
    fun reset() {
        zoom = 1f
        offsetX = 0f
        offsetY = 0f
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
    }
}