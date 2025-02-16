package me.anno.ecs.components.collider

import me.anno.ecs.prefab.PrefabSaveable
import me.anno.engine.raycast.RayQuery
import me.anno.engine.raycast.RayQueryLocal
import me.anno.engine.serialization.SerializedProperty
import me.anno.engine.ui.LineShapes.drawSphere
import me.anno.gpu.pipeline.Pipeline
import org.joml.AABBd
import org.joml.Matrix4x3d
import org.joml.Matrix4x3m
import org.joml.Vector3d
import org.joml.Vector3f
import kotlin.math.sqrt

class SphereCollider : Collider() {

    @SerializedProperty
    var radius = 1.0

    override fun copyInto(dst: PrefabSaveable) {
        super.copyInto(dst)
        if (dst !is SphereCollider) return
        dst.radius = radius
    }

    override fun union(globalTransform: Matrix4x3m, aabb: AABBd, tmp: Vector3d, preferExact: Boolean) {
        // otherwise just use a cube and its 8 sides
        val r = radius
        if (preferExact) {
            // if prefer exact, then use multiple outer-sphere points
            // outer ring != outer sphere, increase the radius once more!
            // val rEstimate = 1.13174 * r // estimated / visualized in Blender, approximated,
            // and then accurately calculated
            val rExact = OUTER_SPHERE_RADIUS_X8 * COSINE_22_5 * r // 1.1315167192268571
            unionRing(globalTransform, aabb, tmp, Axis.X, rExact, 0.0, true)
            unionRing(globalTransform, aabb, tmp, Axis.Y, rExact, 0.0, true)
            unionRing(globalTransform, aabb, tmp, Axis.Z, rExact, 0.0, true)
            // we could create a better approximation by picking points ourselves, and calculating their outer radius
        } else {
            unionCube(globalTransform, aabb, tmp, r, r, r)
        }
    }

    override fun getSignedDistance(deltaPos: Vector3f, outNormal: Vector3f): Float {
        outNormal.set(deltaPos).safeNormalize()
        return deltaPos.length() - radius.toFloat()
    }

    override fun getSignedDistance(deltaPos: Vector3f): Float {
        return deltaPos.length() - radius.toFloat()
    }

    override fun raycast(query: RayQuery): Boolean {
        val radius = radius.toFloat()
        val a = query.direction.lengthSquared()
        val b = 2f * query.start.dot(query.direction)
        val c = query.start.lengthSquared() - radius * radius
        val disc = b * b - 4 * a * c
        return disc >= 0f
    }

    override fun raycast(query: RayQueryLocal, surfaceNormal: Vector3f?): Float {
        val radius = radius.toFloat()
        val a = query.direction.lengthSquared()
        val b = 2f * query.start.dot(query.direction)
        val c = query.start.lengthSquared() - radius * radius
        val disc = b * b - 4 * a * c
        return if (disc < 0f) Float.POSITIVE_INFINITY
        else (-b - sqrt(disc)) / (2f * a)
    }

    override fun drawShape(pipeline: Pipeline) {
        drawSphere(entity, radius, null, getLineColor(hasPhysics))
    }
}