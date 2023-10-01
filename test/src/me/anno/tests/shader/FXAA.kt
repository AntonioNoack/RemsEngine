package me.anno.tests.shader

import me.anno.Time
import me.anno.ecs.components.mesh.shapes.CubemapModel
import me.anno.gpu.GFX
import me.anno.gpu.GFXState
import me.anno.gpu.framebuffer.DepthBufferType
import me.anno.gpu.framebuffer.FBStack
import me.anno.gpu.shader.ShaderLib
import me.anno.gpu.shader.effects.FXAA
import me.anno.gpu.texture.Clamping
import me.anno.gpu.texture.GPUFiltering
import me.anno.image.ImageWriter
import me.anno.input.Input
import me.anno.maths.Maths
import me.anno.ui.debug.TestDrawPanel.Companion.testDrawing
import org.apache.logging.log4j.LogManager
import org.joml.Matrix2f
import org.joml.Matrix4f
import org.joml.Vector2f
import org.joml.Vector3f
import kotlin.math.abs

fun main() {
    testShader()
    // testEdgeAA()
}

private fun testShader() {
    val transform = Matrix4f()
    var angle = 0f
    testDrawing("FXAA Test") {
        val w = it.width / 8
        val h = it.height / 8
        transform.identity()
        transform.perspective(1f, w.toFloat() / h, 0.01f, 100f)
        transform.translate(0f, 0f, -5f)
        val delta = 0.2f * Time.deltaTime.toFloat()
        if (Input.isLeftDown) angle += delta
        if (Input.isRightDown) angle -= delta
        transform.rotateZ(angle)
        val depth = FBStack["depth", w, h, 4, false, 1, DepthBufferType.NONE]
        val mesh = CubemapModel.cubemapModel
        mesh.ensureBuffer()
        GFXState.useFrame(depth) {
            depth.clearColor(1f, 0.7f, 0f, 1f)
            val shader = ShaderLib.shader3D.value
            shader.use()
            shader.m4x4("transform", transform)
            mesh.draw(shader)
        }
        val result = FBStack["result", w, h, 4, false, 1, DepthBufferType.NONE]
        GFXState.useFrame(result) {
            val shader = FXAA.shader.value
            shader.use()
            shader.v1b("showEdges", Input.isShiftDown)
            shader.v1b("disableEffect", Input.isControlDown)
            shader.v1f("threshold", 1e-5f)
            shader.v2f("rbOffset", 0f, 0f) // red-blue-offset; disabled for testing
            depth.bindTexture0(0, GPUFiltering.NEAREST, Clamping.CLAMP)
            depth.bindTexture0(1, GPUFiltering.NEAREST, Clamping.CLAMP)
            GFX.flat01.draw(shader)
        }
        GFX.copy(result)
    }
}

private fun testEdgeAA() {

    val logger = LogManager.getLogger(FXAA::class)

    // task: to find the correct formula, maybe draw a graph of fraction ~ correct blur
    // result: there is no simple correct formula, it seems... there must be something inheritely wrong...

    val size = 128
    val sm1 = size - 1

    fun sample(f: IntArray, x: Int, y: Int): Int {
        return f[Maths.clamp(x, 0, sm1) + Maths.clamp(y, 0, sm1) * size]
    }

    fun sample(f: FloatArray, x: Int, y: Int): Float {
        return f[Maths.clamp(x, 0, sm1) + Maths.clamp(y, 0, sm1) * size]
    }

    val rot = Matrix2f()
        .rotate(0.5f)

    val planes = Array(4) {
        val dx = it.and(1) * 2 - 1f
        val dy = it.and(2) - 1f
        val normal = rot.transform(Vector2f(dx, dy))
        val position = Vector2f(dx, dy).mul(-size / 4f).add(size / 2f, size / 2f)
        Vector3f(normal, -normal.dot(position))
    }

    fun render(dst: FloatArray, v0: Float = 0f, v1: Float = 1f) {
        for (y in 0 until size) {
            for (x in 0 until size) {
                val xf = x + 0.5f
                val yf = y + 0.5f
                dst[x * size + y] = if (planes.all { it.dot(xf, yf, 1f) >= 0f }) v1 else v0
            }
        }
    }

    fun renderHQ(dst: FloatArray, q: Int = 20, v0: Float = 0f, v1: Float = 1f) {
        for (y in 0 until size) {
            for (x in 0 until size) {
                var sum = 0f
                for (fx in 0 until q) {
                    for (fy in 0 until q) {
                        val xf = x + fx.toFloat() / q
                        val yf = y + fy.toFloat() / q
                        sum += if (planes.all { it.dot(xf, yf, 1f) >= 0f }) v1 else v0
                    }
                }
                dst[x * size + y] = sum / (q * q)
            }
        }
    }

    val f = FloatArray(size * size)
    render(f)

    ImageWriter.writeRGBImageInt(size * 4, size * 4, "raw.png", -1) { x, y, _ ->
        (sample(f, x / 4, y / 4) * 255).toInt() * 0x10101
    }

    /* val q = FloatArray(size * size)
     renderHQ(q, 20)
     ImageWriter.writeRGBImageInt(size * 4, size * 4, "hq.png", -1) { x, y, _ ->
         (sample(q, x / 4, y / 4) * 255).toInt() * 0x10101
     }*/

    // val points = ArrayList<Vector2f>()

    val r = IntArray(size * size)
    val threshold = 0.1f
    val c0 = 0x337733
    val c1 = 0x33ff33
    for (y in 0 until size) {
        for (x in 0 until size) {
            val i = x + y * size
            fun needsBlur(dx: Int, dy: Int): Boolean {
                val rel = (sample(f, x + dx + 1, y + dy) +
                        sample(f, x + dx, y + dy + 1) +
                        sample(f, x + dx - 1, y + dy) +
                        sample(f, x + dx, y + dy - 1)
                        ) / sample(f, x + dx, y + dy)
                return abs(4f - rel) > threshold
            }

            val baseColor = if (sample(f, x, y) > 0f) c0 else c1
            if (needsBlur(0, 0)) {
                var d0 = sample(f, x, y)
                var d1 = sample(f, x + 1, y)
                var d2 = sample(f, x, y + 1)
                var d3 = sample(f, x - 1, y)
                var d4 = sample(f, x, y - 1)
                var dx = abs(d3 - d1)
                var dy = abs(d4 - d2)
                var dirX = dx >= dy
                if (Maths.min(dx, dy) * 1.1f > Maths.max(dx, dy)) {
                    // small corner: go to one of the edges, x/y doesn't matter
                    val ix = if (abs(d1 - d0) > abs(d3 - d0)) -1 else +1
                    d0 = sample(f, x + ix, y)
                    d1 = sample(f, x + ix + 1, y)
                    d2 = sample(f, x + ix, y + 1)
                    d3 = sample(f, x + ix - 1, y)
                    d4 = sample(f, x + ix, y - 1)
                    dx = abs(d3 - d1)
                    dy = abs(d4 - d2)
                    dirX = dx >= dy
                }
                val stepX = if (dirX) 0 else 1
                val stepY = if (dirX) 1 else 0
                var pos = 1
                var neg = 1
                while (pos < 15) {
                    if (!needsBlur(pos * stepX, pos * stepY)) break
                    pos++
                }
                while (neg < 15) {
                    if (!needsBlur(-neg * stepX, -neg * stepY)) break
                    neg++
                }
                pos--
                neg--
                val fraction = (pos + 0.5f) / (1f + pos + neg)
                var blur = Maths.min(fraction, 1f - fraction)
                blur = blur * blur * (3 - 2 * blur) + 0f
                // val blur = min(1f, 2 * abs(fraction - 0.5f))
                val other =
                    if (dirX) abs(d1 - d0) >= abs(d3 - d0)
                    else abs(d2 - d0) >= abs(d4 - d0)
                val other2 = if (other) +1 else -1
                val otherColor = if (sample(
                        f,
                        x + if (dirX) other2 else 0,
                        y + if (dirX) 0 else other2
                    ) > 0f
                ) c0 else c1
                if (y > size / 2 && otherColor == baseColor) {
                    logger.warn("Incorrect border color! $x $y")
                    r[i] = Maths.mix(0xff0000, baseColor, 0.5f)
                } else {
                    r[i] = when (x * 3 / size) {
                        0 -> (fraction * 255).toInt() * 0x10101
                        1 -> Maths.mixARGB(baseColor, otherColor, blur)
                        else -> (blur * 255).toInt() * 0x10101
                    }
                    /*val correctFraction = if (baseColor == c0) q[i] else 1f - q[i]
                    points.add(Vector2f(blur, correctFraction))*/
                    // r[i] = mix(0x777777, baseColor, 0.5f) // mixARGB(baseColor, otherColor, blur)
                }
            } else {
                r[i] = baseColor
            }

        }
    }

    ImageWriter.writeRGBImageInt(size * 4, size * 4, "aa.png", -1) { x, y, _ ->
        sample(r, x / 4, y / 4)
    }

    /*(points.map { it.x }.joinToString("\n"))
    ("----------------------------------------------------------")
    (points.map { it.y }.joinToString("\n"))*/

}
