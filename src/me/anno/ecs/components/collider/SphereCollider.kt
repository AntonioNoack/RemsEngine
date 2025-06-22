package me.anno.ecs.components.collider

import me.anno.ecs.prefab.PrefabSaveable
import me.anno.engine.raycast.RayQuery
import me.anno.engine.raycast.RayQueryLocal
import me.anno.engine.serialization.SerializedProperty
import me.anno.engine.ui.LineShapes.drawSphere
import me.anno.gpu.pipeline.Pipeline
import org.joml.AABBd
import org.joml.Matrix4x3
import org.joml.Vector3d
import org.joml.Vector3f
import kotlin.math.sqrt

class SphereCollider : Collider() {

    @SerializedProperty
    var radius = 1f

    override fun copyInto(dst: PrefabSaveable) {
        super.copyInto(dst)
        if (dst !is SphereCollider) return
        dst.radius = radius
    }

    override fun union(globalTransform: Matrix4x3, dstUnion: AABBd, tmp: Vector3d) {
        val r = radius.toDouble()
        unionCube(globalTransform, dstUnion, tmp, r, r, r)
    }

    override fun getSignedDistance(deltaPos: Vector3f, outNormal: Vector3f): Float {
        outNormal.set(deltaPos).safeNormalize()
        return deltaPos.length() - radius
    }

    override fun getSignedDistance(deltaPos: Vector3f): Float {
        return deltaPos.length() - radius
    }

    override fun raycast(query: RayQuery): Boolean {
        val radius = radius
        val a = query.direction.lengthSquared()
        val b = 2f * query.start.dot(query.direction)
        val c = query.start.lengthSquared() - radius * radius
        val disc = b * b - 4 * a * c
        return disc >= 0f
    }

    override fun raycast(query: RayQueryLocal, surfaceNormal: Vector3f?): Float {
        val radius = radius
        val a = query.direction.lengthSquared()
        val b = 2f * query.start.dot(query.direction)
        val c = query.start.lengthSquared() - radius * radius
        val disc = b * b - 4 * a * c
        return if (disc < 0f) Float.POSITIVE_INFINITY
        else (-b - sqrt(disc)) / (2f * a)
    }

    override fun drawShape(pipeline: Pipeline) {
        drawSphere(entity, radius.toDouble(), null, getLineColor(hasPhysics))
    }
}