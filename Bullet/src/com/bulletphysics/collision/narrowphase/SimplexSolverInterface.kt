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

    fun addVertex(w: Vector3d, p: Vector3d, q: Vector3d)

    fun closest(v: Vector3d): Boolean

    fun fullSimplex(): Boolean

    fun inSimplex(w: Vector3d): Boolean

    fun backupClosest(v: Vector3d)

    fun computePoints(p1: Vector3d, p2: Vector3d)

    fun numVertices(): Int
}
