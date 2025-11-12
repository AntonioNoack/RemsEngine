package com.bulletphysics.collision.shapes

import com.bulletphysics.collision.broadphase.BroadphaseNativeType
import org.joml.Vector3d
import org.joml.Vector3f

/**
 * BUSimplex1to4 implements feature based and implicit simplex of up to 4 vertices
 * (tetrahedron, triangle, line, vertex).
 *
 * @author jezek2
 */
open class BUSimplex1to4 : PolyhedralConvexShape {

    override var numVertices: Int = 0
    var vertices: Array<Vector3d?> = arrayOfNulls(4)

    constructor()

    constructor(pt0: Vector3d?) {
        addVertex(pt0)
    }

    constructor(pt0: Vector3d?, pt1: Vector3d?) {
        addVertex(pt0)
        addVertex(pt1)
    }

    constructor(pt0: Vector3d?, pt1: Vector3d?, pt2: Vector3d?) {
        addVertex(pt0)
        addVertex(pt1)
        addVertex(pt2)
    }

    constructor(pt0: Vector3d?, pt1: Vector3d?, pt2: Vector3d?, pt3: Vector3d?) {
        addVertex(pt0)
        addVertex(pt1)
        addVertex(pt2)
        addVertex(pt3)
    }

    fun reset() {
        numVertices = 0
    }

    override val shapeType: BroadphaseNativeType
        get() = BroadphaseNativeType.TETRAHEDRAL

    fun addVertex(pt: Vector3d?) {
        if (vertices[numVertices] == null) {
            vertices[numVertices] = Vector3d()
        }

        vertices[numVertices++] = pt

        recalculateLocalAabb()
    }

    /**
     * euler formula, F-E+V = 2, so E = F+V-2
     * */
    override val numEdges
        get(): Int = when (numVertices) {
            2 -> 1
            3 -> 3
            4 -> 6
            else -> 0
        }

    override fun getEdge(i: Int, pa: Vector3f, pb: Vector3f) {
        @Suppress("UNCHECKED_CAST") // any accessed elements are guaranteed to nto be null
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

    override fun getVertex(i: Int, vtx: Vector3f) {
        vtx.set(vertices[i]!!)
    }

    fun getIndex(i: Int): Int {
        return 0
    }

    override fun isInside(pt: Vector3d, tolerance: Double): Boolean {
        return false
    }
}
