package com.bulletphysics.collision.shapes

import me.anno.maths.geometry.convexhull.ConvexHulls
import org.apache.logging.log4j.LogManager
import org.joml.Vector3d

/**
 * ShapeHull takes a [ConvexShape], builds the convex hull using [ConvexHulls]
 * and provides triangle indices and vertices.
 *
 * @author jezek2
 */
class ShapeHull(val shape: ConvexShape) {

    private var vertices: List<Vector3d> = emptyList()

    fun verticesToFloatArray(): FloatArray {
        val dst = FloatArray(vertices.size * 3)
        for (i in vertices.indices) {
            vertices[i].get(dst, i * 3)
        }
        return dst
    }

    val isValid = buildHull()

    private fun buildHull(): Boolean {

        val directions = ArrayList<Vector3d>(NUM_UNIT_SPHERE_POINTS + shape.numPreferredPenetrationDirections)
        for (i in constUnitSpherePoints.indices) {
            directions.add(Vector3d(constUnitSpherePoints[i]))
        }

        for (i in 0 until shape.numPreferredPenetrationDirections) {
            val extraDirection = Vector3d()
            shape.getPreferredPenetrationDirection(i, extraDirection)
            directions.add(extraDirection)
        }

        val tmp = Vector3d()
        for (i in directions.indices) {
            val v = directions[i]
            shape.localGetSupportingVertex(tmp.set(v), v)
        }

        val hullResult = ConvexHulls.calculateConvexHull(directions)
        if (hullResult == null) {
            LOGGER.warn("Failed to create convex hull for $directions")
            return false
        }

        vertices = hullResult.vertices
        return true
    }

    companion object {

        private val LOGGER = LogManager.getLogger(ShapeHull::class)

        /** ///////////////////////////////////////////////////////////////////////// */
        const val NUM_UNIT_SPHERE_POINTS = 42

        val constUnitSpherePoints = ArrayList<Vector3d>(NUM_UNIT_SPHERE_POINTS)

        private fun v(x: Double, y: Double, z: Double) {
            constUnitSpherePoints.add(Vector3d(x, y, z))
        }

        init {
            v(+0.000000, -0.000000, -1.000000)
            v(+0.723608, -0.525725, -0.447219)
            v(-0.276388, -0.850649, -0.447219)
            v(-0.894426, -0.000000, -0.447216)
            v(-0.276388, +0.850649, -0.447220)
            v(+0.723608, +0.525725, -0.447219)
            v(+0.276388, -0.850649, +0.447220)
            v(-0.723608, -0.525725, +0.447219)
            v(-0.723608, +0.525725, +0.447219)
            v(+0.276388, +0.850649, +0.447219)
            v(+0.894426, +0.000000, +0.447216)
            v(-0.000000, +0.000000, +1.000000)
            v(+0.425323, -0.309011, -0.850654)
            v(-0.162456, -0.499995, -0.850654)
            v(+0.262869, -0.809012, -0.525738)
            v(+0.425323, +0.309011, -0.850654)
            v(+0.850648, -0.000000, -0.525736)
            v(-0.525730, -0.000000, -0.850652)
            v(-0.688190, -0.499997, -0.525736)
            v(-0.162456, +0.499995, -0.850654)
            v(-0.688190, +0.499997, -0.525736)
            v(+0.262869, +0.809012, -0.525738)
            v(+0.951058, +0.309013, +0.000000)
            v(+0.951058, -0.309013, +0.000000)
            v(+0.587786, -0.809017, +0.000000)
            v(+0.000000, -1.000000, +0.000000)
            v(-0.587786, -0.809017, +0.000000)
            v(-0.951058, -0.309013, -0.000000)
            v(-0.951058, +0.309013, -0.000000)
            v(-0.587786, +0.809017, -0.000000)
            v(-0.000000, +1.000000, -0.000000)
            v(+0.587786, +0.809017, -0.000000)
            v(+0.688190, -0.499997, +0.525736)
            v(-0.262869, -0.809012, +0.525738)
            v(-0.850648, +0.000000, +0.525736)
            v(-0.262869, +0.809012, +0.525738)
            v(+0.688190, +0.499997, +0.525736)
            v(+0.525730, +0.000000, +0.850652)
            v(+0.162456, -0.499995, +0.850654)
            v(-0.425323, -0.309011, +0.850654)
            v(-0.425323, +0.309011, +0.850654)
            v(+0.162456, +0.499995, +0.850654)
        }
    }
}
