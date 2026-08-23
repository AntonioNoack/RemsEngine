package me.anno.ui.base.image

import me.anno.gpu.drawing.DrawTextures
import me.anno.gpu.texture.Clamping
import me.anno.gpu.texture.ITexture2D
import me.anno.ui.Canvas
import me.anno.ui.Style
import kotlin.math.log2
import kotlin.math.max

/**
 * Panel that draws an ITexture2D, e.g., for icons, showing images, ...
 * */
@Suppress("MemberVisibilityCanBePrivate")
abstract class ImagePanel(style: Style) : ImagePanelBase(style) {

    abstract fun getTexture(): ITexture2D?

    override fun draw(canvas: Canvas) {
        super.draw(canvas)
        drawImage(canvas)
    }

    fun drawImage(canvas: Canvas) {
        val texture = getTexture() ?: return
        calculateSizes(texture.width, texture.height)
        drawTexture(canvas, texture)
    }

    open fun drawTexture(canvas: Canvas, texture: ITexture2D) {
        if (showAlpha && texture.numChannels == 4) {
            DrawTextures.drawTransparentBackground(
                lix, liy, liw, lih,
                (5f * (1 shl log2(max(1f, zoom)).toInt()))
            )
        }
        texture.bind(0, filtering, Clamping.CLAMP)
        canvas.drawTexture(lix, liy, liw, lih, texture)
    }
}