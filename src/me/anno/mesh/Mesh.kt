package me.anno.mesh

import me.anno.gpu.buffer.Attribute
import me.anno.gpu.buffer.StaticBuffer
import me.anno.gpu.shader.Shader
import me.anno.input.Input.isControlDown
import me.anno.input.Input.isKeyDown
import me.anno.input.Input.keysDown
import me.anno.utils.Color.toVecRGBA
import me.anno.utils.ColorParsing.parseColor
import me.anno.utils.Vectors
import org.joml.AABBf
import org.joml.Vector3f
import org.joml.Vector4f
import org.lwjgl.opengl.GL11.GL_LINES

class Mesh(val material: String, val points: ArrayList<Point>, val lines: ArrayList<Line>) {

    fun flipV() = points.forEach { it.flipV() }
    fun scale(scale: Float) = points.forEach { it.scale(scale) }

    fun switchYZ() {
        points.forEach { it.switchYZ() }
    }

    fun switchXZ() {
        points.forEach { it.switchXZ() }
    }

    fun translate(delta: Vector3f) {
        points.forEach { it.translate(delta) }
    }

    val tint = (try {
        parseColor(material)
    } catch (e: Exception) {
        null
    })?.toVecRGBA() ?: Vector4f(1f)

    private val bounds = AABBf()
    fun getBounds(): AABBf {
        if (bounds.maxX < bounds.minX) {
            // needs recalculation
            points.forEach { pt ->
                bounds.union(pt.position)
            }
        }
        return bounds
    }

    val hasUV get() = points[0].uv != null
    var buffer: StaticBuffer? = null

    fun draw(shader: Shader, alpha: Float, materialOverride: Map<String, Vector4f>) {
        if (points.isNotEmpty()) {
            if (buffer == null) fillBuffer()
            val tint = materialOverride[material] ?: tint
            val finalAlpha = alpha * tint.w
            if (finalAlpha > 0.5 / 255f) {
                shader.v4("tint", tint.x, tint.y, tint.z, tint.w * alpha)
                if (isControlDown && 'L'.toInt() in keysDown) {
                    buffer?.draw(shader, GL_LINES)
                } else {
                    buffer?.draw(shader)
                }
            }
        }
    }

    fun draw(shader: Shader, tint: Vector4f) {
        if (points.isNotEmpty()) {
            if (buffer == null) fillBuffer()
            shader.v4("tint", tint.x, tint.y, tint.z, tint.w)
            if (isControlDown && isKeyDown('l')) {

                buffer?.draw(shader, GL_LINES)
            } else buffer?.draw(shader)
        }
    }

    fun fillBuffer() {
        val withUV = hasUV
        val attr = if (withUV) attrUV else attrNoUV
        val buffer = StaticBuffer(attr, points.size)
        if (withUV) {
            for (point in points) {
                buffer.put(point.position)
                buffer.put(point.normal)
                buffer.put(point.uv!!)
            }
        } else {
            for (point in points) {
                buffer.put(point.position)
                buffer.put(point.normal)
            }
        }
        this.buffer = buffer
    }

    fun contains(v: Vector3f): Boolean {
        // todo direct lookup using rough 3d array of the mesh inside?
        val randomDir = Vector3f(0f, 1f, 0f)
        val p = points
        var ctr = 0
        for (i in p.indices step 3) {
            val a = p[i].position
            val b = p[i + 1].position
            val c = p[i + 2].position
            if (Vectors.rayTriangleIntersection(v, randomDir, a, b, c, 1e10f) != null) {
                ctr++
            }
        }
        return (ctr % 2) == 1
    }

    fun contains(v: Vector3f, bbx: AABBf): Boolean {
        return if (bbx.testPoint(v)) {
            contains(v)
        } else false
    }

    override fun toString() = "$material x ${points.size}"

    companion object {
        val attrNoUV = listOf(
            Attribute("coords", 3),
            Attribute("normals", 3)
        )
        val attrUV = attrNoUV + Attribute("uvs", 2)
    }

}