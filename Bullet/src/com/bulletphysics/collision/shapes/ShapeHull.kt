package com.bulletphysics.collision.shapes

import com.bulletphysics.linearmath.MiscUtil
import com.bulletphysics.linearmath.convexhull.HullDesc
import com.bulletphysics.linearmath.convexhull.HullFlags
import com.bulletphysics.linearmath.convexhull.HullLibrary
import com.bulletphysics.linearmath.convexhull.HullResult
import com.bulletphysics.util.IntArrayList
import cz.advel.stack.Stack
import org.joml.Vector3d

/**
 * ShapeHull takes a [ConvexShape], builds the convex hull using [HullLibrary]
 * and provides triangle indices and vertices.
 *
 * @author jezek2
 */
class ShapeHull(var shape: ConvexShape) {

    var numIndices: Int
    val vertexPointer = ArrayList<Vector3d>()
    val indexPointer = IntArrayList()
    val unitSpherePoints = ArrayList<Vector3d>()

    fun buildHull(margin: Double): Boolean {
        val norm = Stack.newVec()

        var numSampleDirections = NUM_UNIT_SPHERE_POINTS
        for (i in 0 until shape.numPreferredPenetrationDirections) {
            shape.getPreferredPenetrationDirection(i, norm)
            unitSpherePoints[numSampleDirections++].set(norm)
        }

        val supportPoints = ArrayList<Vector3d>()
        MiscUtil.resize(
            supportPoints,
            NUM_UNIT_SPHERE_POINTS + ConvexShape.MAX_PREFERRED_PENETRATION_DIRECTIONS * 2,
            Vector3d::class.java
        )

        for (i in 0 until numSampleDirections) {
            shape.localGetSupportingVertex(unitSpherePoints[i], supportPoints[i])
        }

        val hullDesc = HullDesc()
        hullDesc.flags = HullFlags.TRIANGLES
        hullDesc.vcount = numSampleDirections

        //#ifdef BT_USE_DOUBLE_PRECISION
        //hd.mVertices = &supportPoints[0];
        //hd.mVertexStride = sizeof(btVector3);
        //#else
        hullDesc.vertices = supportPoints

        //hd.vertexStride = 3 * 4;
        //#endif
        val hullLibrary = HullLibrary()
        val hullResult = HullResult()
        if (!hullLibrary.createConvexHull(hullDesc, hullResult)) {
            return false
        }

        MiscUtil.resize(this.vertexPointer, hullResult.numOutputVertices, Vector3d::class.java)

        for (i in 0 until hullResult.numOutputVertices) {
            vertexPointer[i].set(hullResult.outputVertices[i])
        }
        numIndices = hullResult.numIndices
        MiscUtil.resize(this.indexPointer, numIndices, 0)
        for (i in 0 until numIndices) {
            indexPointer.set(i, hullResult.indices.get(i))
        }

        // free temporary hull result that we just copied
        hullLibrary.releaseResult(hullResult)

        return true
    }

    fun numTriangles(): Int {
        return numIndices / 3
    }

    fun numVertices(): Int {
        return vertexPointer.size
    }

    fun numIndices(): Int {
        return numIndices
    }

    init {
        this.vertexPointer.clear()
        this.indexPointer.clear()
        this.numIndices = 0

        MiscUtil.resize(
            unitSpherePoints,
            NUM_UNIT_SPHERE_POINTS + ConvexShape.MAX_PREFERRED_PENETRATION_DIRECTIONS * 2,
            Vector3d::class.java
        )
        for (i in constUnitSpherePoints.indices) {
            unitSpherePoints[i].set(constUnitSpherePoints[i])
        }
    }

    companion object {
        /** ///////////////////////////////////////////////////////////////////////// */
        private const val NUM_UNIT_SPHERE_POINTS = 42

        private val constUnitSpherePoints = ArrayList<Vector3d>(NUM_UNIT_SPHERE_POINTS)

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
