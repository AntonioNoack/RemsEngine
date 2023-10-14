package me.anno.tests.geometry

import me.anno.gpu.drawing.DrawRectangles
import me.anno.ui.debug.TestDrawPanel.Companion.testDrawingWithControls
import org.joml.AABBf
import org.joml.Vector3f
import kotlin.math.max

fun main() {
    // looks correct
    /*testSceneWithUI("AABBHitTest", flatCube.front) {
        val renderer = SimpleRenderer(
            "AABBHit",
            ShaderStage(
                "AABBHit", listOf(
                    Variable(GLSLType.V3F, "cameraPosition"),
                    Variable(GLSLType.V1F, "worldScale"),
                    Variable(GLSLType.V3F, "finalPosition"),
                    Variable(GLSLType.V3F, "finalColor", VariableMode.OUT),
                ), "" +
                        "vec3 dir = normalize(finalPosition);\n" +
                        "finalColor = intersectAABB(cameraPosition, 1.0/dir, vec3(-1.0,0.0,-1.0), vec3(1.0,0.0,1.0), 1e9) ? vec3(1.0) : vec3(0.0);\n"
            ).add(intersectAABB)
        )
        it.renderer.renderMode = RenderMode(renderer.name, renderer)
    }*/
    // now the same on CPU side
    // looks correct, too
    val bounds = AABBf(-1f, 0f, -1f, +1f, 0f, +1f)
    testDrawingWithControls("AABBHitTest") { it, cameraPosition, cameraRotation ->
        it.clear()
        val b = DrawRectangles.startBatch()
        val tmp = Vector3f()
        val x0 = it.width / 2f
        val y0 = it.height / 2f
        val z = max(x0, y0)
        for (y in 0 until it.height) {
            for (x in 0 until it.width) {
                tmp.set(x - x0, y0 - y, z).rotate(cameraRotation).normalize()
                tmp.set(1f / tmp.x, 1f / tmp.y, 1f / tmp.z)
                if (bounds.isRayIntersecting(cameraPosition, tmp, 1e9f)) {
                    DrawRectangles.drawRect(x + it.x, y + it.y, 1, 1, -1)
                }
            }
        }
        DrawRectangles.finishBatch(b)
    }
}