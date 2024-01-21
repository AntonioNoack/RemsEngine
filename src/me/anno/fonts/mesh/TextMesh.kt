package me.anno.fonts.mesh

import me.anno.ecs.components.mesh.Mesh
import me.anno.fonts.Font
import me.anno.fonts.TextDrawable
import me.anno.fonts.signeddistfields.Contour.Companion.calculateContours
import me.anno.fonts.signeddistfields.edges.CubicSegment
import me.anno.fonts.signeddistfields.edges.LinearSegment
import me.anno.fonts.signeddistfields.edges.QuadraticSegment
import me.anno.gpu.buffer.Attribute
import me.anno.mesh.Triangulation
import me.anno.utils.types.Triangles.isInsideTriangle
import me.anno.utils.types.Vectors.avg
import org.joml.AABBf
import org.joml.Vector2f
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

class TextMesh(val font: Font, val text: String) : TextDrawable() {

    val mesh = Mesh()

    init {

        // was 30 before it had O(x²) complexity ;)
        val quadAccuracy = 5f
        val cubicAccuracy = 5f

        val contours = calculateContours(font, text)
        val fragments = ArrayList(contours.map {
            val points = ArrayList<Vector2f>()
            for (s in it.segments) {
                val steps = when (s) {
                    is QuadraticSegment -> {
                        val length = s.p0.distance(s.p1) + s.p1.distance(s.p2)
                        max(2, (quadAccuracy * length).roundToInt())
                    }
                    is CubicSegment -> {
                        val length = s.p0.distance(s.p1) + s.p1.distance(s.p2) + s.p2.distance(s.p3)
                        max(3, (cubicAccuracy * length).roundToInt())
                    }
                    is LinearSegment -> 1
                    else -> throw NotImplementedError()
                }
                for (i in 0 until steps) {
                    points.add(s.point(i.toFloat() / steps, Vector2f()))
                }
            }
            // y is mirrored, because y is up, not down in our 3D coordinate system
            for (point in points) {
                point.y = -point.y
            }
            Fragment(points)
        })

        // ignore the winding? just use our intuition?
        // intuition:
        //  - large areas are outside
        //  - if there is overlap, the smaller one is inside, the larger outside

        fragments.sortByDescending { it.size }

        for (index1 in fragments.indices) {
            val fragment = fragments[index1]
            if (!fragment.isInside) {
                val tri = fragment.triangles
                // find all fragments, which need to be removed from this one
                for (index2 in index1 + 1 until fragments.size) {
                    // inside must only cut one outside
                    val maybeInside = fragments[index2]
                    if (!maybeInside.isInside) {
                        if (fragment.boundsOverlap(maybeInside)) {
                            val insideTriangles = maybeInside.triangles
                            if (insideTriangles.isNotEmpty()) {
                                val probe = avg(insideTriangles[0], insideTriangles[1], insideTriangles[2])
                                if (fragment.boundsContain(probe)) {
                                    isInsideSearch@ for (j in tri.indices step 3) {
                                        if (probe.isInsideTriangle(tri[j], tri[j + 1], tri[j + 2])) {
                                            maybeInside.isInside = true
                                            fragment.needingRemoval.add(maybeInside)
                                            break@isInsideSearch
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        val outerFragments = fragments.filter { !it.isInside }
        // "found ${outerFragments.size} outer rings"

        for (outer in outerFragments) {
            outer.apply {
                if (needingRemoval.isNotEmpty()) {
                    mergeRings2(ring, needingRemoval.map { it.ring })
                    /*needingRemoval.sortedByDescending { it.size }.forEach { inner ->
                        mergeRings(ring, inner.ring)
                    }*/
                    needingRemoval.clear()
                    triangles = Triangulation.ringToTrianglesVec2f(ring)
                }
            }
        }

        val triangles = ArrayList<Vector2f>(outerFragments.sumOf { it.triangles.size })
        for (outer in outerFragments) {
            triangles += outer.triangles
        }

        val numVertices = triangles.size
        val positions = FloatArray(numVertices * 3)
        mesh.positions = positions

        for (it in outerFragments) {
            bounds.union(it.bounds)
        }

        // center the text, ignore the characters themselves
        val baseScale = DEFAULT_LINE_HEIGHT / font.size
        var i = 0
        for (point in triangles) {
            positions[i++] = point.x * baseScale
            positions[i++] = point.y * baseScale
            positions[i++] = 0f
        }

        bounds.minX *= baseScale * 0.5f
        bounds.maxX *= baseScale * 0.5f

        bounds.minX += 0.5f
        bounds.maxX += 0.5f
    }

    class Fragment(val ring: MutableList<Vector2f>) {
        var triangles = Triangulation.ringToTrianglesVec2f(ring)
        val bounds = AABBf()

        init {
            for (v in ring) {
                bounds.union(v)
            }
        }

        val size = triangleSize(triangles)
        var isInside = false
        val needingRemoval = ArrayList<Fragment>()
        fun boundsOverlap(s: Fragment): Boolean {
            return bounds.testAABB(s.bounds)
        }

        fun boundsContain(v: Vector2f) = bounds.testPoint(v.x, v.y, 0f)
    }

    companion object {

        val attributes = listOf(
            Attribute("coords", 2)
        )

        const val DEFAULT_LINE_HEIGHT = 0.2f

        private fun mergeRings2(outer: MutableList<Vector2f>, innerList: List<List<Vector2f>>) {
            for (inner in innerList.sortedBy { it.minOfOrNull { p -> p.x }!! }) {
                mergeRings(outer, inner)
            }
        }

        private fun mergeRings(outer: MutableList<Vector2f>, inner: List<Vector2f>) {

            // find the closest pair
            var bestDistance = Float.POSITIVE_INFINITY
            var bestOuterIndex = 0
            var bestInnerIndex = 0

            for (outerIndex in outer.indices) {
                val o = outer[outerIndex]
                for (innerIndex in inner.indices) {
                    val i = inner[innerIndex]
                    val distance = o.distanceSquared(i)
                    if (distance < bestDistance) {
                        bestOuterIndex = outerIndex
                        bestInnerIndex = innerIndex
                        bestDistance = distance
                    }
                }
            }

            // merge them at the merging points
            val mergingPoint = outer[bestOuterIndex]
            outer.addAll(bestOuterIndex, inner.subList(0, bestInnerIndex + 1))
            outer.addAll(bestOuterIndex, inner.subList(bestInnerIndex, inner.size))
            outer.add(bestOuterIndex, mergingPoint)
        }

        fun triangleSize(triangles: List<Vector2f>): Float {
            var areaSum = 0f
            for (i in triangles.indices step 3) {
                val a = triangles[i]
                val b = triangles[i + 1]
                val c = triangles[i + 2]
                val v1x = a.x - c.x
                val v1y = a.y - c.y
                val v2x = b.x - c.x
                val v2y = b.y - c.y
                val area = (v1x * v2y) - (v1y * v2x)
                areaSum += area
            }
            return abs(areaSum * 0.5f)
        }
    }

    /**
     * start- and endIndex are not supported,
     * as this class is only used for generating the meshes
     * */
    override fun draw(startIndex: Int, endIndex: Int, drawBuffer: DrawBufferCallback) {
        drawBuffer.draw(mesh, null, 0f)
    }

    override fun destroy() {
        mesh.destroy()
    }
}
