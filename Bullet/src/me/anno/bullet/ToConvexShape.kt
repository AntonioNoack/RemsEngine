package me.anno.bullet

import com.bulletphysics.collision.shapes.BoxShape
import com.bulletphysics.collision.shapes.CollisionShape
import com.bulletphysics.collision.shapes.CompoundShape
import com.bulletphysics.collision.shapes.ConcaveShape
import com.bulletphysics.collision.shapes.ConvexHullShape
import com.bulletphysics.collision.shapes.ConvexShape
import com.bulletphysics.linearmath.Transform
import me.anno.maths.geometry.FibonacciSphere
import me.anno.maths.geometry.convexhull.ConvexHulls
import me.anno.utils.assertions.assertNull
import me.anno.utils.types.Booleans.hasFlag
import org.apache.logging.log4j.LogManager
import org.joml.Matrix3f
import org.joml.Vector3d
import org.joml.Vector3f

object ToConvexShape {

    private val LOGGER = LogManager.getLogger(ToConvexShape::class)
    private val directions = lazy { FibonacciSphere.create(16) }

    fun convertToConvexShape(shape: CollisionShape): ConvexShape {
        return when {
            shape is ConvexShape -> shape
            shape is CompoundShape && shape.children.size == 1 &&
                    shape.children[0].shape is ConvexShape &&
                    shape.children[0].transform.origin.length() < 1e-9 &&
                    shape.children[0].transform.basis.equals(Matrix3f(), 1e-5) -> {
                shape.children[0].shape as ConvexShape
            }
            else -> {
                LOGGER.warn("Converting $shape to convex shape")
                val boundsMin = Vector3d()
                val boundsMax = Vector3d()
                shape.getBounds(Transform(), boundsMin, boundsMax)

                val vertices = ArrayList<Vector3d>()
                collectHullVertices(null, shape, vertices)

                val hull = ConvexHulls.calculateConvexHull(vertices, 48)
                // if hull result is empty, use bounds as convex hull
                    ?: return BoxShape(Vector3f(boundsMax.sub(boundsMin).mul(0.5)))
                ConvexHullShape(hull.vertices.toFloatArray(), hull.triangles)
            }
        }
    }

    private fun List<Vector3d>.toFloatArray(): FloatArray {
        val dst = FloatArray(size * 3)
        for (i in indices) {
            this[i].get(dst, i * 3)
        }
        return dst
    }

    fun collectHullVertices(transform: Transform?, shape: CollisionShape, dst: ArrayList<Vector3d>) {
        when (shape) {
            is ConvexShape -> {
                val dir = Vector3f()
                val tmp = Vector3f()
                for (i in 0 until shape.numPreferredPenetrationDirections) {
                    shape.getPreferredPenetrationDirection(i, dir)
                    val support = shape.localGetSupportingVertex(dir, tmp)
                    transform?.transformPosition(support)
                    dst.add(Vector3d(support))
                }
                for (dir0 in directions.value) {
                    dir.set(dir0)
                    val support = shape.localGetSupportingVertex(dir, tmp)
                    transform?.transformPosition(support)
                    dst.add(Vector3d(support))
                }
            }
            is ConcaveShape -> {
                val boundsMin = Vector3d()
                val boundsMax = Vector3d()
                shape.getBounds(Transform(), boundsMin, boundsMax)
                shape.processAllTriangles({ a, b, c, _, _ ->
                    dst.add(a)
                    dst.add(b)
                    dst.add(c)
                }, boundsMin, boundsMax)
            }
            is CompoundShape -> {
                assertNull(transform)
                for (child in shape.children) {
                    collectHullVertices(child.transform, child.shape, dst)
                }
            }
            else -> {
                val boundsMin = Vector3d()
                val boundsMax = Vector3d()
                shape.getBounds(Transform(), boundsMin, boundsMax)
                if (transform != null) {
                    transform.transformPosition(boundsMin)
                    transform.transformPosition(boundsMax)
                    for (i in 0 until 8) {
                        val x = if (i.hasFlag(1)) boundsMax.x else boundsMin.x
                        val y = if (i.hasFlag(2)) boundsMax.x else boundsMin.x
                        val z = if (i.hasFlag(4)) boundsMax.x else boundsMin.x
                        dst.add(Vector3d(x, y, z))
                    }
                }
            }
        }
    }
}