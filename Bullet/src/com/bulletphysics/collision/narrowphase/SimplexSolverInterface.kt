package com.bulletphysics.collision.narrowphase

import cz.advel.stack.Stack
import org.joml.Vector3d
import org.joml.Vector3f

/**
 * SimplexSolverInterface can incrementally calculate distance between origin and
 * up to 4 vertices. Used by GJK or Linear Casting. Can be implemented by the
 * Johnson-algorithm or alternative approaches based on voronoi regions or barycentric
 * coordinates.
 *
 * @author jezek2
 */
interface SimplexSolverInterface {

    fun reset()

    /**
     * Add vertex to solver;
     * p = vertex in A, q = vertex in B, w = p - q
     * */
    fun addVertex(diff: Vector3f, posInA: Vector3d, posInB: Vector3d)

    fun addVertex(diff: Vector3f, a: Vector3f, b: Vector3f) {
        val tmpA = Stack.newVec3d().set(a)
        val tmpB = Stack.newVec3d().set(b)
        addVertex(diff, tmpA, tmpB)
        Stack.subVec3d(2)
    }

    /**
     * Return/calculate the closest vertex.
     * Returns false if no vertex was ever added.
     */
    fun closest(dst: Vector3f): Boolean

    fun fullSimplex(): Boolean

    fun inSimplex(w: Vector3f): Boolean

    fun isSimplex4Full(): Boolean = numVertices() == 4

    fun backupClosest(v: Vector3f)

    fun computePoints(p1: Vector3f, p2: Vector3f)

    fun numVertices(): Int
}
