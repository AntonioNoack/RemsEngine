package me.anno.tests.geometry

import me.anno.gpu.drawing.DrawTextures.drawTexture
import me.anno.gpu.texture.Texture2D
import me.anno.ui.debug.TestDrawPanel.Companion.testDrawingWithControls
import me.anno.utils.hpc.HeavyProcessing
import org.apache.logging.log4j.LogManager
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
    val texture = Texture2D("render", 1, 1, 1)
    LogManager.disableLogger("WorkSplitter")
    testDrawingWithControls("AABBHitTest") { it, cameraPosition, cameraRotation ->
        it.clear()
        val x0f = it.width / 2f
        val y0f = it.height / 2f
        val z = max(x0f, y0f)
        val w = it.width
        val h = it.height
        val data = ByteArray(w * h)
        HeavyProcessing.processBalanced2d(
            0, 0, w, h, 32, 8
        ) { x0, y0, x1, y1 ->
            val tmp = Vector3f()
            for (y in y0 until y1) {
                for (x in x0 until x1) {
                    tmp.set(x - x0f, y0f - y, z).rotate(cameraRotation).normalize()
                    tmp.set(1f / tmp.x, 1f / tmp.y, 1f / tmp.z)
                    if (bounds.isRayIntersecting(cameraPosition, tmp, 1e9f)) {
                        data[x + y * w] = -1
                    }
                }
            }
        }
        texture.width = w
        texture.height = h
        texture.createMonochrome(data, false)
        drawTexture(it.x, it.y, w, h, texture)
    }
}