package me.anno.ui.editor.color

import me.anno.gpu.drawing.DrawRectangles.drawRect
import me.anno.maths.Maths.clamp
import me.anno.ui.Style
import me.anno.utils.Color.black
import me.anno.utils.types.Floats.roundToIntOr
import org.joml.Vector3f

class AlphaBar(chooser: ColorChooser, style: Style) : HSVBox(
    chooser,
    Vector3f(0f, 0f, 0f),
    Vector3f(0f, 0f, 1f),
    Vector3f(0f, 0f, 0f), 0f, style, 1f,
    { opacity, _ ->
        chooser.setHSL(
            chooser.hue,
            chooser.saturation,
            chooser.lightness,
            clamp(opacity, 0f, 1f),
            chooser.colorSpace,
            8, true
        )
    }) {
    override fun draw(x0: Int, y0: Int, x1: Int, y1: Int) {
        val dragX = clamp(x0 + ((x1 - x0) * chooser.opacity).roundToIntOr(), x0, x1 - 1)
        // drawRectGradient(x, y, w, h, backgroundColor.toVecRGBA(), Vector4f(1f))
        // colorShowTexture.bind(0, NearestMode.TRULY_NEAREST, ClampMode.REPEAT)
        // drawTexture(x, y, w, h, colorShowTexture, -1, Vector4f(w.toFloat() / h, 1f, 0f, 0f))
        HSVBoxMain.drawColoredAlpha(x, y, width, height, chooser, width.toFloat() / height, 1f, true)
        drawRect(dragX, y0, 1, y1 - y0, black)
    }
}