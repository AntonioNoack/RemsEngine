package com.bulletphysics.collision.shapes

import com.bulletphysics.collision.broadphase.BroadphaseNativeType
import org.joml.Vector3d

/**
 * implements feature based and implicit simplex of up to 4 vertices
 * (tetrahedron, triangle, line, vertex).
 *
 * @author jezek2
 */
class Simplex1to4 : PolyhedralConvexShape {

    override var numVertices: Int = 0
    var vertices: Array<Vector3d?> = arrayOfNulls(4)

    constructor()

    @Suppress("unused")
    constructor(pt0: Vector3d) {
        addVertex(pt0)
    }

    @Suppress("unused")
    constructor(pt0: Vector3d, pt1: Vector3d) {
        addVertex(pt0)
        addVertex(pt1)
    }

    @Suppress("unused")
    constructor(pt0: Vector3d, pt1: Vector3d, pt2: Vector3d) {
        addVertex(pt0)
        addVertex(pt1)
        addVertex(pt2)
    }

    @Suppress("unused")
    constructor(pt0: Vector3d, pt1: Vector3d, pt2: Vector3d, pt3: Vector3d) {
        addVertex(pt0)
        addVertex(pt1)
        addVertex(pt2)
        addVertex(pt3)
    }

    fun reset() {
        numVertices = 0
    }

    override val shapeType: BroadphaseNativeType
        get() = BroadphaseNativeType.TETRAHEDRAL_SHAPE_PROXYTYPE

    fun addVertex(pt: Vector3d) {
        vertices[numVertices++] = pt
        recalculateLocalAabb()
    }


    override val numEdges: Int
        get() {
            // euler formula, F-E+V = 2, so E = F+V-2
            when (numVertices) {
                2 -> return 1
                3 -> return 3
                4 -> return 6
            }
            return 0
        }

    override fun getEdge(i: Int, pa: Vector3d, pb: Vector3d) {
        @Suppress("UNCHECKED_CAST")
        val vertices = vertices as Array<Vector3d>
        when (numVertices) {
            2 -> {
                pa.set(vertices[0])
                pb.set(vertices[1])
            }
            3 -> when (i) {
                0 -> {
                    pa.set(vertices[0])
                    pb.set(vertices[1])
                }
                1 -> {
                    pa.set(vertices[1])
                    pb.set(vertices[2])
                }
                2 -> {
                    pa.set(vertices[2])
                    pb.set(vertices[0])
                }
            }
            4 -> when (i) {
                0 -> {
                    pa.set(vertices[0])
                    pb.set(vertices[1])
                }
                1 -> {
                    pa.set(vertices[1])
                    pb.set(vertices[2])
                }
                2 -> {
                    pa.set(vertices[2])
                    pb.set(vertices[0])
                }
                3 -> {
                    pa.set(vertices[0])
                    pb.set(vertices[3])
                }
                4 -> {
                    pa.set(vertices[1])
                    pb.set(vertices[3])
                }
                5 -> {
                    pa.set(vertices[2])
                    pb.set(vertices[3])
                }
            }
        }
    }

    override fun getVertex(i: Int, vtx: Vector3d) {
        vtx.set(vertices[i]!!)
    }

    override val numPlanes: Int
        get() {
            return when (numVertices) {
                3 -> 2
                4 -> 4
                else -> 0
            }
        }

    override fun getPlane(planeNormal: Vector3d, planeSupport: Vector3d, i: Int) {
    }

    override fun isInside(pt: Vector3d, tolerance: Double): Boolean {
        return false
    }
}
