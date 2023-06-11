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

    testDrawing {

        it.clear()

        val sc = it.h / 10f
        val dx = it.x + it.w / 2
        val dy = it.y + it.h / 2

        val bg = it.backgroundColor
        val window = it.window!!

        chain.solveForTarget((window.mouseX - dx) / sc, (window.mouseY - dy) / sc, 0f)
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