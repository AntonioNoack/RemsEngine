package me.anno.engine.ui.input

import me.anno.gpu.drawing.DrawTextures.drawTexture
import me.anno.gpu.texture.TextureCache
import me.anno.input.Key
import me.anno.ui.Style
import me.anno.ui.base.components.AxisAlignment
import me.anno.ui.base.text.TextPanel
import me.anno.utils.Color
import me.anno.utils.OS.res
import me.anno.utils.types.Floats.toIntOr

class TagPanel(name: String, style: Style) : TextPanel(name, style) {

    private val texture get() = TextureCache[res.getChild("textures/Cross.png"), true]
    private val tagsPanel get() = parent?.parent as? TagsPanel

    override fun calculateSize(w: Int, h: Int) {
        super.calculateSize(w, h)
        minW += textSize.toIntOr()
    }
    override fun onUpdate() {
        super.onUpdate()
        if (texture == null || wasHovered != isHovered) {
            invalidateDrawing()
        }
    }

    override fun draw(x0: Int, y0: Int, x1: Int, y1: Int) {
        super.draw(x0, y0, x1, y1)
        val textSize = (textSize * 0.85f).toIntOr()
        val color = if (isHovered) Color.white else Color.black
        drawTexture(
            x + width - textSize - (backgroundRadius * 0.5f).toIntOr(),
            y + AxisAlignment.CENTER.getOffset(height, textSize) + 2,
            textSize, textSize, texture ?: return, color
        )
    }

    override fun onMouseClicked(x: Float, y: Float, button: Key, long: Boolean) {
        val tagsPanel = tagsPanel
        if (button == Key.BUTTON_LEFT && tagsPanel?.isInputAllowed == true) {
            tagsPanel.removeTag(text, true)
        } else super.onMouseClicked(x, y, button, long)
    }
}