package com.bulletphysics.extras.gimpact

import com.bulletphysics.extras.gimpact.ClipPolygon.distancePointPlane
import com.bulletphysics.extras.gimpact.ClipPolygon.planeClipPolygon
import com.bulletphysics.extras.gimpact.ClipPolygon.planeClipTriangle
import com.bulletphysics.extras.gimpact.GeometryOperations.edgePlane
import com.bulletphysics.linearmath.Transform
import cz.advel.stack.Stack
import me.anno.utils.types.Triangles.subCross
import org.joml.Vector3d
import org.joml.Vector4d

/**
 * @author jezek2
 */
class PrimitiveTriangle {

    private val tmpVecList1 = Array(TriangleContact.MAX_TRI_CLIPPING) { Vector3d() }
    private val tmpVecList2 = Array(TriangleContact.MAX_TRI_CLIPPING) { Vector3d() }
    private val tmpVecList3 = Array(TriangleContact.MAX_TRI_CLIPPING) { Vector3d() }

    @JvmField
    val vertices = Array(3) { Vector3d() }
    val plane = Vector4d()
    var margin = 0.01f

    fun buildTriPlane() {
        val normal = Stack.newVec3d()
        subCross(vertices[0], vertices[1], vertices[2], normal)
            .normalize()

        plane.set(normal.x, normal.y, normal.z, vertices[0].dot(normal))
        Stack.subVec3d(1)
    }

    /**
     * Test if triangles could collide.
     */
    fun overlapTestConservative(other: PrimitiveTriangle): Boolean {
        val totalMargin = margin + other.margin
        // classify points on other triangle
        var dis0 = distancePointPlane(plane, other.vertices[0]) - totalMargin
        var dis1 = distancePointPlane(plane, other.vertices[1]) - totalMargin
        var dis2 = distancePointPlane(plane, other.vertices[2]) - totalMargin

        if (dis0 > 0.0 && dis1 > 0.0 && dis2 > 0.0) {
            return false // classify points on this triangle
        }

        dis0 = distancePointPlane(other.plane, vertices[0]) - totalMargin
        dis1 = distancePointPlane(other.plane, vertices[1]) - totalMargin
        dis2 = distancePointPlane(other.plane, vertices[2]) - totalMargin
        return !(dis0 > 0.0 && dis1 > 0.0 && dis2 > 0.0)
    }

    /**
     * Calculates the plane which is parallel to the edge and perpendicular to the triangle plane.
     * This triangle must have its plane calculated.
     */
    fun getEdgePlane(edge_index: Int, plane: Vector4d) {
        val e0 = vertices[edge_index]
        val e1 = vertices[(edge_index + 1) % 3]

        val tmp = Stack.newVec3d()
        tmp.set(this.plane.x, this.plane.y, this.plane.z)

        edgePlane(e0, e1, tmp, plane)
    }

    fun applyTransform(t: Transform) {
        t.transformPosition(vertices[0])
        t.transformPosition(vertices[1])
        t.transformPosition(vertices[2])
    }

    /**
     * Clips the triangle against this.
     *
     * @param clippedPoints must have MAX_TRI_CLIPPING size, and this triangle must have its plane calculated.
     * @return the number of clipped points
     */
    fun clipTriangle(other: PrimitiveTriangle, clippedPoints: Array<Vector3d>): Int {
        // edge 0
        val tmpPoints1 = tmpVecList1
        val edgePlane = Vector4d()

        getEdgePlane(0, edgePlane)

        var clippedCount =
            planeClipTriangle(edgePlane, other.vertices[0], other.vertices[1], other.vertices[2], tmpPoints1)
        if (clippedCount == 0) {
            return 0
        }

        val tmpPoints2 = tmpVecList2

        // edge 1
        getEdgePlane(1, edgePlane)
        clippedCount = planeClipPolygon(edgePlane, tmpPoints1, clippedCount, tmpPoints2)
        if (clippedCount == 0) {
            return 0 // edge 2
        }

        getEdgePlane(2, edgePlane)
        return planeClipPolygon(edgePlane, tmpPoints2, clippedCount, clippedPoints)
    }

    /**
     * Find collision using the clipping method.
     * This triangle and other must have their triangles calculated.
     */
    fun findTriangleCollisionClipMethod(other: PrimitiveTriangle, contacts: TriangleContact): Boolean {
        val margin = (this.margin + other.margin).toDouble()

        val clippedPoints = tmpVecList3

        //create planes
        // plane v vs U points
        val contacts1 = TriangleContact()

        contacts1.separatingNormal.set(plane)

        var clippedCount = clipTriangle(other, clippedPoints)
        if (clippedCount == 0) {
            return false // Reject
        }

        // find most deep interval face1
        contacts1.mergePoints(contacts1.separatingNormal, margin, clippedPoints, clippedCount)
        if (contacts1.numPoints == 0) {
            return false // too far
            // Normal pointing to this triangle
        }
        contacts1.separatingNormal.x *= -1.0
        contacts1.separatingNormal.y *= -1.0
        contacts1.separatingNormal.z *= -1.0

        // Clip tri1 by tri2 edges
        val contacts2 = TriangleContact()
        contacts2.separatingNormal.set(other.plane)

        clippedCount = other.clipTriangle(this, clippedPoints)

        if (clippedCount == 0) {
            return false // Reject
        }

        // find most deep interval face1
        contacts2.mergePoints(contacts2.separatingNormal, margin, clippedPoints, clippedCount)
        if (contacts2.numPoints == 0) {
            return false // too far

            // check most dir for contacts
        }
        if (contacts2.penetrationDepth < contacts1.penetrationDepth) {
            contacts.copyFrom(contacts2)
        } else {
            contacts.copyFrom(contacts1)
        }
        return true
    }
}
