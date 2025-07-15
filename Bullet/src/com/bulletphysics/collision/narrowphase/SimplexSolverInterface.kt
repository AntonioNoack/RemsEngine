package com.bulletphysics.collision.narrowphase

import org.joml.Vector3d

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
    fun addVertex(w: Vector3d, p: Vector3d, q: Vector3d)

    /**
     * Return/calculate the closest vertex.
     * Returns false if no vertex was ever added.
     */
    fun closest(dst: Vector3d): Boolean

    fun fullSimplex(): Boolean

    fun inSimplex(w: Vector3d): Boolean

    fun isSimplex4Full(): Boolean = numVertices() == 4

    fun backupClosest(v: Vector3d)

    fun computePoints(p1: Vector3d, p2: Vector3d)

    fun numVertices(): Int
}
