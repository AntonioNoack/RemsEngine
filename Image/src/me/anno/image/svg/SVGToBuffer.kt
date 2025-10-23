package me.anno.image.svg

import me.anno.gpu.buffer.Attribute
import me.anno.gpu.buffer.CompactAttributeLayout.Companion.bind
import me.anno.gpu.buffer.StaticBuffer
import me.anno.image.svg.gradient.Formula
import org.joml.Vector4f
import kotlin.math.max

object SVGToBuffer {

    val attr = bind(
        Attribute("aLocalPosition", 3),
        Attribute("aLocalPos2", 2),
        Attribute("aFormula0", 4),
        Attribute("aFormula1", 1),
        Attribute("aColor0", 4),
        Attribute("aColor1", 4),
        Attribute("aColor2", 4),
        Attribute("aColor3", 4),
        Attribute("aStops", 4),
        Attribute("aPadding", 1)
    )

    /**
     * Creates a buffer for rendering with the dimensions
     * [-w/h, -1, minZ] - [+w/h, +1, maxZ]
     * */
    fun SVGMesh.createBuffer(): StaticBuffer? {
        val cx = x0 + w0 * 0.5f
        val cy = y0 + h0 * 0.5f
        val scale = 2f / h0

        val totalPointCount = totalPointCount
        if (totalPointCount <= 0) return null

        val buffer = StaticBuffer("SVG", attr, totalPointCount)
        val formula = Formula()
        val c0 = Vector4f()
        val c1 = Vector4f()
        val c2 = Vector4f()
        val c3 = Vector4f()
        val stops = Vector4f()
        for (curve in curves) {
            val triangleIndices = curve.trianglesIndices
            if (triangleIndices.isEmpty()) continue
            val triangleVertices = curve.triangleVertices
            val minX = triangleVertices.minOf { it.x }
            val maxX = triangleVertices.maxOf { it.x }
            val minY = triangleVertices.minOf { it.y }
            val maxY = triangleVertices.maxOf { it.y }
            val scaleX = 1f / max(1e-7f, maxX - minX)
            val scaleY = 1f / max(1e-7f, maxY - minY)
            // upload all shapes
            val gradient = curve.gradient
            gradient.fill(formula, c0, c1, c2, c3, stops)
            // if (gradient.colors.size > 1) LOGGER.info("$gradient -> $formula")
            val padding = gradient.spreadMethod.id.toFloat()
            val z = curve.depth
            val circle = if (formula.isCircle) 1f else 0f
            for (vi in 0 until triangleIndices.size) {
                val v = triangleVertices[triangleIndices[vi]]
                val vx = v.x
                val vy = v.y
                // position, v3
                val x = ((vx - cx) * scale)
                val y = ((vy - cy) * scale)
                buffer.put(x, y, z)
                // local pos 2
                buffer.put(((vx - minX) * scaleX), ((vy - minY) * scaleY))
                // formula 0
                buffer.put(formula.position)
                // formula 1
                buffer.put(formula.directionOrRadius)
                buffer.put(circle)
                // color 0-3, v4 each
                buffer.put(c0)
                buffer.put(c1)
                buffer.put(c2)
                buffer.put(c3)
                // stops, v4
                buffer.put(stops)
                // padding, v1
                buffer.put(padding)
            }
        }
        return buffer
    }

}