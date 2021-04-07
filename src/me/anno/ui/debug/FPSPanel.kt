package me.anno.ui.debug

import me.anno.gpu.GFX
import me.anno.gpu.GFXx2D
import me.anno.gpu.GFXx2D.drawSimpleTextCharByChar
import me.anno.gpu.GFXx2D.monospaceFont
import me.anno.utils.types.Floats.f1
import kotlin.math.max

object FPSPanel {

    fun showFPS() {

        val x0 = max(0, GFX.width - FrameTimes.width)
        val y0 = max(0, GFX.height - FrameTimes.height)

        FrameTimes.place(x0, y0, FrameTimes.width, FrameTimes.height)
        FrameTimes.draw()

        GFX.loadTexturesSync.push(true)

        val text = "${GFX.currentEditorFPS.f1()}, min: ${(1f / FrameTimes.maxValue).f1()}"
        drawSimpleTextCharByChar(x0, y0, 2, text)

        // keep these chars loaded at all times
        for (char in "0123456789.") {
            GFXx2D.getTextSize(monospaceFont, "$char", -1)
        }

        GFX.loadTexturesSync.pop()

    }

}