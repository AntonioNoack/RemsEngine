package me.anno.tests.game

import me.anno.config.DefaultConfig.style
import me.anno.gpu.drawing.DrawTextures.drawTexture
import me.anno.gpu.texture.Texture2D
import me.anno.maths.Maths.min
import me.anno.maths.Maths.mixARGB
import me.anno.ui.Panel
import me.anno.ui.debug.TestStudio.Companion.testUI
import me.anno.ui.debug.TestStudio.Companion.testUI3
import me.anno.utils.Color.a01
import me.anno.utils.Color.b01
import me.anno.utils.Color.g01
import me.anno.utils.Color.r01
import me.anno.utils.Color.rgba
import org.joml.Matrix4f
import org.joml.Vector2f
import org.joml.Vector3f
import java.nio.IntBuffer
import kotlin.math.max

// todo create a doom like rendering engine
// todo walls:
class Wall(
    val pos0: Vector2f,
    val pos1: Vector2f,
    val top: Float,
    val bottom: Float,
    var color: Int, // todo textures
) {

    var x0 = 0
    var y0b = 0
    var y0t = 0
    var x1 = 0
    var y1b = 0
    var y1t = 0

    var depth = 0f
    var isFront = false
    var hidden = false
    val tmp = Vector3f()

    fun calculate(camera: Matrix4f, w: Int, h: Int) {
        // todo calculate projected position
        tmp.set(pos0.x, bottom, pos0.y)
        camera.transformProject(tmp)
        val x0 = tmp.x
        val y0 = tmp.y
        val z0 = tmp.z
        tmp.set(pos1.x, bottom, pos1.y)
        camera.transformProject(tmp)
        val z1 = tmp.z
        hidden = z0 < 0f && z1 < 0f
        if (hidden) return
        val x1 = tmp.x
        val y1 = tmp.y
        if (z0 < 0f) {
            // todo clipping
            val f = z1

        } else if (z1 < 0f) {
            // todo clipping

        }
        // todo calculate clipping
        isFront = x0 > x1
    }

    fun draw(buffer: IntBuffer, w: Int, h: Int, color: Int) {
        val dx = x1 - x0
        val dyb = (y1b - y0b)
        val dyt = (y1t - y0t)
        var x = x0
        for (i in 0 until dx) {
            val dy0 = max(y0b + dyb * i / dx, 0)
            val dy1 = min(y0t + dyt * i / dx, h)
            for (y in dy0 until dy1) {
                buffer.put(x + y * w, color)
            }
            x++
        }
    }
}

class Section(
    val positions: Array<Vector2f>, val top: Float, val bottom: Float,
    val color: Int,
) {

    constructor(positions: FloatArray, top: Float, bottom: Float, color: Int) :
            this(Array(positions.size / 2) {
                Vector2f(positions[it * 2], positions[it * 2 + 1])
            }, top, bottom, color)

    fun mulBrightness(brightness: Float) =
        rgba(color.r01() * brightness, color.g01() * brightness, color.b01() * brightness, color.a01())

    val topColor = mixARGB(color, -1, 0.5f)
    val bottomColor = mulBrightness(0.5f)

    val walls = Array(positions.size) {
        val pos0 = positions[it]
        var j = it + 1; if (j == positions.size) j = 0
        val pos1 = positions[j]
        // calculate color with shading based on normal
        val normalX = (pos1.y - pos0.y) / pos1.distance(pos0)
        val light = max(0f, normalX) * .5f + .5f
        Wall(pos0, pos1, bottom, top, mulBrightness(light))
    }

    var depth = 0f

    fun calculate(camera: Matrix4f, w: Int, h: Int) {
        depth = 0f
        for (wall in walls) {
            wall.calculate(camera, w, h)
            depth += wall.depth
        }
        depth /= walls.size
    }

    fun draw(camera: Vector3f, buffer: IntBuffer, w: Int, h: Int) {
        // draw insides
        if (camera.y < bottom) {
            for (wall in walls) {
                if (!wall.isFront && !wall.hidden) {
                    wall.draw(buffer, w, h, bottomColor)
                }
            }
        } else if (camera.y > top) {
            for (wall in walls) {
                if (!wall.isFront && !wall.hidden) {
                    wall.draw(buffer, w, h, topColor)
                }
            }
        }
        // draw front side
        for (wall in walls) {
            if (wall.isFront && !wall.hidden) {
                wall.draw(buffer, w, h, wall.color)
            }
        }
    }

}

val sections = arrayOf(
    Section(floatArrayOf(3f, 5f, 3f, 6f, 2f, 6f, 2f, 5f), 2f, 0f, -1)
)

fun main() {
    testUI3 {
        val texture = Texture2D("doom", 1, 1, 1)
        object : Panel(style) {
            val camera = Matrix4f()
            val cameraPosition = Vector3f()
            val cameraRotation = 0f
            override fun onDraw(x0: Int, y0: Int, x1: Int, y1: Int) {
                val w = w
                val h = h
                val buffer0 = Texture2D.bufferPool[4 * w * h, false, false]
                val buffer1 = buffer0.asIntBuffer()
                for (i in 0 until w * h) buffer1.put(i, 0)

                camera.identity()
                camera.setPerspective(1.57f, 1f, 0.01f, 100f) // aspect ratio is 1, because we work in pixel space
                camera.translateLocal(w / 2f, h / 2f, 0f)
                camera.translate(cameraPosition)
                camera.rotateY(cameraRotation)

                // calculate all positions
                for (section in sections) {
                    section.calculate(camera, w, h)
                }

                sections.sortBy { it.depth }

                // then draw all walls
                for (section in sections) {
                    section.draw(cameraPosition, buffer1, w, h)
                }

                texture.createRGBA(buffer0, false)
                drawTexture(x, y, w, h, texture, true)
            }
            // todo controls
        }
    }
}