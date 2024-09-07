package me.anno.tests.physics.fluid

import me.anno.gpu.Clipping
import me.anno.gpu.drawing.DrawTexts
import me.anno.ui.base.components.AxisAlignment
import me.anno.ui.debug.TestDrawPanel

fun main() {

    val w = 1024
    val h = 1024
    val sim = FluidSimulation(w, h, 0)

    val init = lazy {
        // initialize textures
        sim.velocity.read.clearColor(0f, 0f, 0f, 1f) // 2d, so -1 = towards bottom right
        sim.pressure.read.clearColor(0.5f, 0f, 0f, 1f) // 1d, so max level
    }

    val visuals = listOf(
        sim.divergence to "Div",
        null to "",
        sim.pressure.read to "PressureR",
        sim.pressure.write to "PressureW",
        sim.velocity.read to "VelocityR",
        sim.velocity.write to "VelocityW"
    )

    TestDrawPanel.testDrawing("Fluid Sim") {

        it.allowLeft = true
        it.allowRight = false

        val window = it.window!!
        init.value
        step(
            it, (window.mouseX - it.x) / it.width,
            (window.mouseY - it.y) / it.height, 0.2f, sim
        )

        // draw
        for (i in visuals.indices) {
            val (tex, title) = visuals[i]
            tex ?: continue
            val j = i / 2
            val k = i and 1
            val x0 = it.x + j * it.width / 3
            val x1 = it.x + (j + 1) * it.width / 3
            val y0 = it.y + k * it.height / 2
            val y1 = it.y + (k + 1) * it.height / 2
            Clipping.clip(x0, y0, x1 - x0, y1 - y0) {
                if (i < 4) {
                    FluidDebug.displayTextureR(x0, y0, x1 - x0, y1 - y0, tex.getTexture0())
                } else {
                    FluidDebug.displayTextureRG(x0, y0, x1 - x0, y1 - y0, tex.getTexture0())
                }
                DrawTexts.drawSimpleTextCharByChar(
                    (x0 + x1) / 2, (y0 + y1) / 2, 2, title,
                    AxisAlignment.CENTER, AxisAlignment.CENTER
                )
            }
        }

    }
}