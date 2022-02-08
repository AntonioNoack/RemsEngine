package me.anno.ui.base

import me.anno.gpu.drawing.DrawTextures
import me.anno.gpu.drawing.DrawTextures.drawTexture
import me.anno.gpu.texture.Texture2D
import me.anno.image.ImageScale
import me.anno.ui.Panel
import me.anno.ui.style.Style

/**
 * Panel that draws a gpu.Texture2D, e.g. for icons, showing images, ...
 * */
abstract class ImagePanel(style: Style) : Panel(style) {

    enum class StretchModes {
        OVERFLOW,
        PADDING,
        STRETCH
    }

    var stretchMode = StretchModes.STRETCH

    var showAlpha = false
    var flipX = false
    var flipY = false

    override val canDrawOverBorders: Boolean = true

    override fun getVisualState(): Any? = getTexture()

    abstract fun getTexture(): Texture2D?

    // last image stats
    var lix = 0
    var liy = 0
    var liw = 1
    var lih = 1

    override fun onDraw(x0: Int, y0: Int, x1: Int, y1: Int) {
        // todo alignment (left/right/...) should be customizable
        super.onDraw(x0, y0, x1, y1)
        val texture = getTexture() ?: return
        liw = w
        lih = h
        when (stretchMode) {
            StretchModes.OVERFLOW -> {
                val size = ImageScale.scaleMin(texture.w, texture.h, w, h)
                liw = size.first
                lih = size.second
            }
            StretchModes.PADDING -> {
                val size = ImageScale.scaleMax(texture.w, texture.h, w, h)
                liw = size.first
                lih = size.second
            }
            else -> {}
        }
        if (flipX) liw = -liw
        if (flipY) lih = -lih
        lix = x + (w - liw) / 2
        liy = y + (h - lih) / 2
        if (showAlpha) DrawTextures.drawTransparentBackground(lix, liy, liw, lih)
        drawTexture(lix, liy, liw, lih, texture, -1, null)
    }

}