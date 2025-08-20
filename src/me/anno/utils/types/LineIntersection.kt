package me.anno.utils.types

import me.anno.utils.pooling.JomlPools
import org.joml.Vector2d
import org.joml.Vector2f
import org.joml.Vector3d
import org.joml.Vector3f
import kotlin.math.abs

/**
 * approximate line intersection
 * http://paulbourke.net/geometry/pointlineplane/calclineline.cs
 * */
object LineIntersection {

    /**
     * Finds the closest positions of the lines (pos0a, pos0b) and (pos1a, pos1b).
     * Returns the parameters for these positions, meaning closest0 = mix(pos0a,pos0b,dst.x) and closest1 = mix(pos1a,pos1b,dst.y).
     * Returns whether a result was found. No result can be found if the lines are close to being parallel.
     * */
    @JvmStatic
    fun lineIntersection(
        pos0a: Vector3d, pos0b: Vector3d,
        pos1a: Vector3d, pos1b: Vector3d,
        dst: Vector2d? = null
    ): Boolean {
        val dir0 = pos0b.sub(pos0a, JomlPools.vec3d.create())
        val dir1 = pos1b.sub(pos1a, JomlPools.vec3d.create())
        val result = RayIntersection.rayIntersection(pos0a, dir0, pos1a, dir1, dst)
        JomlPools.vec3d.sub(2)
        return result
    }

    /**
     * Finds the closest positions of the lines (pos0a, pos0b) and (pos1a, pos1b).
     * Returns the parameters for these positions, meaning closest0 = mix(pos0a,pos0b,dst.x) and closest1 = mix(pos1a,pos1b,dst.y).
     * Returns whether a result was found. No result can be found if the lines are close to being parallel.
     * */
    @JvmStatic
    fun lineIntersection(
        pos0a: Vector3f, pos0b: Vector3f,
        pos1a: Vector3f, pos1b: Vector3f,
        dst: Vector2f? = null
    ): Boolean {
        val dir0 = pos0b.sub(pos0a, JomlPools.vec3f.create())
        val dir1 = pos1b.sub(pos1a, JomlPools.vec3f.create())
        val result = RayIntersection.rayIntersection(pos0a, dir0, pos1a, dir1, dst)
        JomlPools.vec3f.sub(2)
        return result
    }

    /**
     * Finds the closest positions of the lines (pos0a, pos0b) and (pos1a, pos1b).
     * Returns the parameters for these positions, meaning closest0 = mix(pos0a,pos0b,dst.x) and closest1 = mix(pos1a,pos1b,dst.y).
     * Returns whether a result was found. No result can be found if the lines are close to being parallel.
     * */
    @JvmStatic
    fun lineIntersection(
        pos0a: Vector2d, pos0b: Vector2d,
        pos1a: Vector2d, pos1b: Vector2d,
        dst: Vector2d? = null
    ): Boolean {
        val dir0 = pos0b.sub(pos0a, JomlPools.vec2d.create())
        val dir1 = pos1b.sub(pos1a, JomlPools.vec2d.create())
        val result = RayIntersection.rayIntersection(pos0a, dir0, pos1a, dir1, dst)
        JomlPools.vec2d.sub(2)
        return result
    }

    /**
     * Finds the closest positions of the lines (pos0a, pos0b) and (pos1a, pos1b).
     * Returns the parameters for these positions, meaning closest0 = mix(pos0a,pos0b,dst.x) and closest1 = mix(pos1a,pos1b,dst.y).
     * Returns whether a result was found. No result can be found if the lines are close to being parallel.
     * */
    @JvmStatic
    fun lineIntersection(
        pos0a: Vector2f, pos0b: Vector2f,
        pos1a: Vector2f, pos1b: Vector2f,
        dst: Vector2f? = null
    ): Boolean {
        val dir0 = pos0b.sub(pos0a, JomlPools.vec2f.create())
        val dir1 = pos1b.sub(pos1a, JomlPools.vec2f.create())
        val result = RayIntersection.rayIntersection(pos0a, dir0, pos1a, dir1, dst)
        JomlPools.vec2f.sub(2)
        return result
    }
}