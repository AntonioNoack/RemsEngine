package me.anno.tests.fonts

import me.anno.fonts.Font
import me.anno.fonts.msdf.MSDFFont
import me.anno.gpu.drawing.GFXx2D.getSizeX
import me.anno.input.Input
import me.anno.input.Key
import me.anno.ui.base.components.AxisAlignment
import me.anno.ui.debug.TestDrawPanel.Companion.testDrawing
import me.anno.utils.OS.documents

fun main() {

    val folder = documents.getChild("Satiplanner/msdf-atlas-gen-1.4")
    val texName = folder.getChild("VerdanaMSDF.png")

    val fontImpl = MSDFFont(texName)
    folder.getChild("VerdanaMSDF.json")
        .inputStream { input, exception ->
            exception?.printStackTrace()
            input?.use { fontImpl.read(input) }
        }

    var font = Font("Verdana", 20f)
    testDrawing("MultiSDFText") { p, canvas ->
        if (Input.wasKeyPressed(Key.KEY_LEFT_SHIFT)) font = font.withSize(font.size * 0.7f)
        if (Input.wasKeyPressed(Key.KEY_LEFT_CONTROL)) font = font.withSize(font.size / 0.7f)

        val size = canvas.drawText(
            p.x, p.y, 1, font, fontImpl, "Hello Wgrld!",
            -1, p.background.color,
            -1, -1,
            AxisAlignment.MIN, AxisAlignment.MIN, 0
        )
        canvas.drawText(
            p.x + getSizeX(size), p.y, 1, font, "Hello Wgrld!",
            -1, p.background.color,
            -1, -1,
            AxisAlignment.MIN, AxisAlignment.MIN, 0
        )
    }
}