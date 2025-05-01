package me.anno.ui.editor.color

import me.anno.gpu.drawing.DrawGradients.drawRectGradient
import me.anno.gpu.drawing.DrawTextures.drawTexture
import me.anno.gpu.texture.Clamping
import me.anno.gpu.texture.Filtering
import me.anno.gpu.texture.TextureLib
import me.anno.input.Key
import me.anno.ui.Style
import me.anno.ui.base.components.AxisAlignment
import me.anno.utils.Color.toVecRGBA
import me.anno.utils.types.Floats.roundToIntOr
import org.joml.Vector3f
import org.joml.Vector4f
import kotlin.math.min

class HSVBoxMain(chooser: ColorChooser, v0: Vector3f, du: Vector3f, dv: Vector3f, style: Style) :
    HSVBox(chooser, v0, du, dv, 1f, style, 5f, { rx, ry ->
        chooser.visualisation.setColorByClick(rx, ry, chooser)
    }) {

    override fun calculateSize(w: Int, h: Int) {
        super.calculateSize(w, h)
        val size = min(w, h)
        minW = size
        minH = (size * chooser.visualisation.ratio).roundToIntOr()
    }

    override fun onKeyDown(x: Float, y: Float, key: Key) {
        if (key == Key.BUTTON_LEFT) {
            val rx = (x - this.x) / this.width
            val ry = (y - this.y) / this.height
            val visualisation = chooser.visualisation
            chooser.isDownInRing = visualisation.isDownInRing(rx, ry)
            visualisation.setColorByClick(rx, ry, chooser)
        } else super.onKeyDown(x, y, key)
    }

    override fun draw(x0: Int, y0: Int, x1: Int, y1: Int) {
        drawBackground(x0, y0, x1, y1)
        chooser.drawColorBox(this, v0, du, dv, dh, true)
        chooser.visualisation.drawColorBoxDecoration(x, y, width, height, backgroundColor, chooser)
    }

    init {
        alignmentX = AxisAlignment.FILL
    }

    companion object {
        fun drawColoredAlpha(
            x: Int, y: Int, w: Int, h: Int, chooser: ColorChooser,
            sx: Float, sy: Float, withGradient: Boolean
        ) {
            if (withGradient) {
                drawRectGradient(x, y, w, h, chooser.backgroundColor.toVecRGBA(), chooser.rgba)
            } else {
                chooser.drawColorBox(
                    x, y, w, h,
                    Vector3f(chooser.hue, chooser.saturation, chooser.lightness), Vector3f(), Vector3f(), 0f,
                    false
                )
            }
            if (chooser.withAlpha && chooser.opacity < 1f) {
                TextureLib.colorShowTexture.bind(0, Filtering.TRULY_NEAREST, Clamping.REPEAT)
                val color =
                    (chooser.backgroundColor and 0xffffff) or ((1f - chooser.opacity) * 255).roundToIntOr().shl(24)
                drawTexture(x, y, w, h, TextureLib.colorShowTexture, color, Vector4f(sx, sy, sx / 2, sy / 2))
            }
        }
    }
}