package me.anno.ui.base.image

import me.anno.gpu.drawing.DrawTextures
import me.anno.gpu.texture.Clamping
import me.anno.gpu.texture.ITexture2D
import me.anno.ui.Style
import kotlin.math.log2
import kotlin.math.max

/**
 * Panel that draws an ITexture2D, e.g., for icons, showing images, ...
 * */
@Suppress("MemberVisibilityCanBePrivate")
abstract class ImagePanel(style: Style) : ImagePanelBase(style) {

    abstract fun getTexture(): ITexture2D?

    override fun draw(x0: Int, y0: Int, x1: Int, y1: Int) {
        super.draw(x0, y0, x1, y1)
        drawImage()
    }

    fun drawImage() {
        val texture = getTexture() ?: return
        calculateSizes(texture.width, texture.height)
        drawTexture(texture)
    }

    open fun drawTexture(texture: ITexture2D) {
        if (showAlpha && texture.channels == 4) {
            DrawTextures.drawTransparentBackground(
                lix, liy, liw, lih,
                (5f * (1 shl log2(max(1f, zoom)).toInt()))
            )
        }
        texture.bind(0, filtering, Clamping.CLAMP)
        DrawTextures.drawTexture(lix, liy, liw, lih, texture, -1, null, flipY)
    }
}