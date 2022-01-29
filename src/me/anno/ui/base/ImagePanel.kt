package me.anno.ui.base

import me.anno.gpu.drawing.DrawTextures.drawTexture
import me.anno.gpu.texture.Texture2D
import me.anno.ui.Panel
import me.anno.ui.style.Style

/**
 * Panel that draws a gpu.Texture2D, e.g. for icons, showing images, ...
 * todo there should be different modes on how the image is aligned:
 * todo stretch x/y?, keep aspect ratio?, show inner part only?, show all?
 * */
abstract class ImagePanel(style: Style): Panel(style){

    override fun getVisualState(): Any? = getTexture()

    abstract fun getTexture(): Texture2D?

    override fun onDraw(x0: Int, y0: Int, x1: Int, y1: Int) {
        super.onDraw(x0, y0, x1, y1)
        drawTexture(x, y, w, h, getTexture() ?: return, -1, null)
    }

}