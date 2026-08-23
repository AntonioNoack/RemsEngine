package me.anno.tests.utils

import au.edu.federation.caliko.FabrikBone3D
import au.edu.federation.caliko.FabrikChain3D
import au.edu.federation.utils.Vec3f
import me.anno.gpu.drawing.DrawCurves.drawLine
import me.anno.ui.debug.TestDrawPanel.Companion.testDrawing

fun main() {

    val chain = FabrikChain3D();
    chain.addBone(FabrikBone3D(Vec3f(0f, 0f, 0f), Vec3f(0f, 1f, 0f)))
    chain.addConsecutiveBone(Vec3f(0f, 1f, 0f), 1f)
    chain.addConsecutiveBone(Vec3f(0f, 1f, 0f), 1f)
    chain.addConsecutiveBone(Vec3f(0f, 1f, 0f), 1f)

    testDrawing("FABRIK") { p, canvas ->

        p.clear(canvas)

        val sc = p.height / 10f
        val dx = p.x + p.width / 2
        val dy = p.y + p.height / 2

        val bg = p.backgroundColor
        val window = p.window!!

        chain.solveForTarget((window.mouseX - dx) / sc, (window.mouseY - dy) / sc, 0f)
        canvas.custom {
            for (i in 0 until chain.numBones) {
                val bone = chain.getBone(i)
                val p0 = bone.startLocation
                val p1 = bone.endLocation
                drawLine(
                    p0.x * sc + dx, p0.y * sc + dy, p1.x * sc + dx, p1.y * sc + dy,
                    1f, -1, bg, false
                )
            }
        }
    }

}