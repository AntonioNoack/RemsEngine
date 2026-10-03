package me.anno.ui.base.buttons

import me.anno.fonts.Font
import me.anno.fonts.FontManager
import me.anno.gpu.GFX
import me.anno.gpu.buffer.SimpleBuffer.Companion.flat01
import me.anno.gpu.drawing.GFXx2D
import me.anno.gpu.drawing.GFXx2D.posSize
import me.anno.gpu.shader.ShaderLib.subpixelCorrectTextGraphicsShader
import me.anno.gpu.texture.ITexture2D
import me.anno.language.translation.NameDesc
import me.anno.maths.Maths.PIf
import me.anno.ui.Style
import me.anno.ui.canvas.Canvas
import me.anno.utils.Color.withAlpha

open class VerticalTextButton(nameDesc: NameDesc, style: Style) : TextButton(nameDesc, style) {
    @Suppress("unused")
    constructor(style: Style) : this(NameDesc.EMPTY, style)

    var rotatedLeft = true

    override fun calculateSize(w: Int, h: Int) {
        super.calculateSize(w, h)
        val tmp = minH
        minH = minW
        minW = tmp
    }

    override fun drawText(canvas: Canvas, color: Int) {
        drawRotatedText(
            canvas, font, text, x, y, width, height,
            rotatedLeft, color, backgroundColor
        )
    }

    companion object {
        fun drawRotatedText(
            canvas: Canvas, font: Font, text: String,
            x: Int, y: Int, width: Int, height: Int,
            rotatedLeft: Boolean, color: Int, backgroundColor: Int,
        ) {
            val texture = FontManager.getTexture(font, text, -1, -1)
                .value?.createdOrNull() ?: return

            canvas.finish()

            val transform = GFXx2D.transform
            transform.pushMatrix()

            val cx = x + width.shr(1)
            val cy = y + height.shr(1)

            val angle = if (rotatedLeft) -PIf * 0.5f else PIf * 0.5f
            rotateAround(angle, cx, cy)

            val tx = cx - texture.width.shr(1)
            val ty = cy - texture.height.shr(1)
            drawTransformedText(tx, ty, texture, color, backgroundColor)

            transform.popMatrix()
        }

        fun drawTransformedText(
            x: Int, y: Int, texture: ITexture2D,
            color: Int, backgroundColor: Int,
        ) {
            val shader = subpixelCorrectTextGraphicsShader[0].value
            shader.use()

            posSize(shader, x, y, texture.width, texture.height)
            shader.v3f("instData", 0f, 0f, 0f) // offset, unused
            shader.v4f("textColor", color)
            shader.v4f("backgroundColor", backgroundColor.withAlpha(0))
            shader.v2f("windowSize", GFX.viewportWidth.toFloat(), GFX.viewportHeight.toFloat()) // correct?
            shader.v1b("disableSubpixelRendering", true)
            shader.v1b("enableTrueBlending", true)
            shader.m4x4("transform", GFXx2D.transform)
            texture.bindTrulyNearest(0)

            flat01.draw(shader)
        }

        fun rotateAround(angle: Float, cx: Int, cy: Int) {

            val wx = GFX.viewportX
            val wy = GFX.viewportY
            val w = GFX.viewportWidth
            val h = GFX.viewportHeight

            val transform = GFXx2D.transform
            val px = (cx - wx) / w.toFloat() * 2f - 1f
            val py = (cy - wy) / h.toFloat() * 2f - 1f

            transform.translate(+px, -py, 0f)
            transform.rotateZ(angle)
            val aspect = w.toFloat() / h.toFloat()
            transform.scale(-aspect, 1f / aspect, 1f)
            transform.translate(-px, +py, 0f)

        }
    }
}