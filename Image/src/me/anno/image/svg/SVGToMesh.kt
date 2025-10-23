package me.anno.image.svg

import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.MeshAttributes.color0
import me.anno.image.svg.gradient.RadialGradient
import me.anno.utils.algorithms.ForLoop.forLoopSafely
import me.anno.utils.structures.arrays.FloatArrayList
import me.anno.utils.structures.arrays.FloatArrayListUtils.add
import me.anno.utils.structures.arrays.IntArrayList
import org.joml.Vector2f
import org.the3deers.util.EarCut.pointInTriangle
import kotlin.math.max

object SVGToMesh {

    /**
     * Creates a Mesh with the dimensions
     * [-w/h, -1, minZ] - [+w/h, +1, maxZ]
     * */
    fun SVGMesh. createMesh(): Mesh? {
        val cx = x0 + w0 * 0.5f
        val cy = y0 + h0 * 0.5f
        val scale = 2f / h0
        val totalPointCount = totalPointCount
        if (totalPointCount <= 0) return null

        val mesh = Mesh()
        val positions = FloatArrayList(totalPointCount * 3)
        val colors = IntArrayList(totalPointCount)

        for (curve in curves) {
            val triangleVertices = curve.triangleVertices
            val triangleIndices = curve.trianglesIndices
            if (triangleIndices.isEmpty()) continue
            val triangles = triangleIndices.map(triangleVertices)
            val minX = triangleVertices.minOf { it.x }
            val maxX = triangleVertices.maxOf { it.x }
            val minY = triangleVertices.minOf { it.y }
            val maxY = triangleVertices.maxOf { it.y }
            val scaleX = 1f / max(1e-7f, maxX - minX)
            val scaleY = 1f / max(1e-7f, maxY - minY)
            // upload all shapes
            val gradient = curve.gradient
            val z = curve.depth
            if (gradient.colors.size >= 2) {
                gradient.sort()

                fun add(a: Vector2f) {
                    val x = ((a.x - cx) * scale)
                    val y = ((a.y - cy) * scale)
                    positions.add(x, -y, z)
                    val lx = (a.x - minX) * scaleX
                    val ly = (a.y - minY) * scaleY
                    val p = gradient.getProgress(lx, ly)
                    val c = gradient.getColor(p)
                    colors.add(c)
                }

                fun tri(a: Vector2f, b: Vector2f, c: Vector2f) {
                    add(a)
                    add(c)
                    add(b)
                }

                forLoopSafely(triangles.size, 3) { i ->
                    val a = triangles[i]
                    val b = triangles[i + 1]
                    val c = triangles[i + 2]
                    val lxa = (a.x - minX) * scaleX
                    val lya = (a.y - minY) * scaleY
                    val lxb = (b.x - minX) * scaleX
                    val lyb = (b.y - minY) * scaleY
                    val lxc = (c.x - minX) * scaleX
                    val lyc = (c.y - minY) * scaleY
                    // if is circle: check if the point is within this triangle, and if so, split there
                    var done = false
                    if (gradient is RadialGradient) {
                        val p = gradient.position
                        if (pointInTriangle(lxa, lya, lxb, lyb, lxc, lyc, p.x, p.y) ||
                            pointInTriangle(lxa, lya, lxc, lyc, lxb, lyb, p.x, p.y)
                        ) {
                            val p2 = Vector2f(p).div(scaleX, scaleY).add(minX, minY)
                            tri(a, b, p2)
                            tri(b, c, p2)
                            tri(c, a, p2)
                            done = true
                        }
                    }
                    if (!done) tri(a, b, c)
                }
            } else {// no gradient -> fast path
                val color = gradient.getColor(0f)
                for (vi in triangles.indices.reversed()) {
                    val v = triangles[vi]
                    val x = ((v.x - cx) * scale)
                    val y = ((v.y - cy) * scale)
                    positions.add(x, -y, z)
                    colors.add(color)
                }
            }
        }
        mesh.positions = positions.toFloatArray()
        mesh.color0 = colors.toIntArray()
        return mesh
    }

}