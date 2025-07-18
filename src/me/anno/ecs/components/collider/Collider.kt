package me.anno.ecs.components.collider

import me.anno.ecs.Entity
import me.anno.ecs.EntityPhysics.invalidatePhysics
import me.anno.ecs.annotations.Docs
import me.anno.ecs.annotations.Range
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.ecs.systems.OnChangeStructure
import me.anno.ecs.systems.OnDrawGUI
import me.anno.engine.raycast.RayQuery
import me.anno.engine.raycast.RayQueryLocal
import me.anno.engine.raycast.Raycast
import me.anno.engine.raycast.RaycastCollider
import me.anno.engine.serialization.SerializedProperty
import me.anno.gpu.pipeline.Pipeline
import me.anno.maths.bvh.HitType
import me.anno.utils.Color.black
import me.anno.utils.pooling.JomlPools
import org.joml.AABBd
import org.joml.Matrix4x3
import org.joml.Vector3d
import org.joml.Vector3f
import kotlin.math.abs

abstract class Collider : CollidingComponent(), OnChangeStructure, OnDrawGUI {

    @Range(0.0, 1e38)
    @SerializedProperty
    var roundness = 0.04f

    @Docs("Whether the collider is convex; in Bullet, only convex-convex and convex-concave interactions are defined")
    open val isConvex: Boolean = true

    override fun hasRaycastType(typeMask: Int): Boolean {
        return typeMask.and(Raycast.COLLIDERS) != 0
    }

    override fun raycast(query: RayQuery): Boolean {
        return RaycastCollider.raycastGlobalCollider(query, entity ?: sampleEntity, this)
    }

    override fun onChangeStructure(entity: Entity) {
        entity.invalidatePhysics()
        entity.invalidateMasks()
    }

    override fun fillSpace(globalTransform: Matrix4x3, dstUnion: AABBd) {
        val tmp = JomlPools.vec3d.create()
        union(globalTransform, dstUnion, tmp)
        JomlPools.vec3d.sub(1)
    }

    override fun copyInto(dst: PrefabSaveable) {
        super.copyInto(dst)
        if (dst !is Collider) return
        dst.roundness = roundness
    }

    open fun union(globalTransform: Matrix4x3, dstUnion: AABBd, tmp: Vector3d) {
        cubeAABB.transformUnion(globalTransform, dstUnion)
    }

    /**
     * gets the signed distance to the surface of the collider, in local coordinates
     * the coordinates will be lost
     * outNormal returns the local normal
     *
     * if the distance field is warped, the returned distance should be a little smaller, so we don't overstep
     * */
    open fun getSignedDistance(deltaPos: Vector3f, outNormal: Vector3f): Float {
        return getSignedDistance(deltaPos, outNormal, 0.002f)
    }

    fun getSignedDistance(deltaPos: Vector3f, outNormal: Vector3f, e: Float): Float {

        // save position
        val px = deltaPos.x
        val py = deltaPos.y
        val pz = deltaPos.z

        // from https://github.com/glslify/glsl-sdf-normal/blob/master/index.glsl
        // from https://www.shadertoy.com/view/ldfSWs
        val dx = getSignedDistance(deltaPos.set(px + e, py - e, pz - e))
        val dy = getSignedDistance(deltaPos.set(px - e, py + e, pz - e))
        val dz = getSignedDistance(deltaPos.set(px - e, py - e, pz + e))
        val dw = getSignedDistance(deltaPos.set(px + e, py + e, pz + e))

        outNormal.set(+dx, -dx, -dx)
        outNormal.add(-dy, +dy, -dy)
        outNormal.add(-dz, -dz, +dz)
        outNormal.add(+dw, +dw, +dw)

        deltaPos.set(px, py, pz) // store position

        return (dx + dz + dy + dw) * 0.25f
    }

    /**
     * gets the signed distance to the surface of the collider, in local coordinates
     * the coordinates will be lost
     *
     * if the distance field is warped, the returned distance should be a little smaller, so we don't overstep
     * */
    open fun getSignedDistance(deltaPos: Vector3f): Float {
        return Float.POSITIVE_INFINITY
    }

    /**
     * returns +Inf, if not
     * returns the distance, if it hit
     *
     * also sets the surface normal
     * */
    open fun raycast(query: RayQueryLocal, surfaceNormal: Vector3f?): Float {
        // todo check if the ray is inside the bounding box:
        // todo I get many false-positives behind the object... why?
        // default implementation is slow, and not perfect:
        // ray casting
        var distance = 0f
        val pos = Vector3f(query.start)
        var isDone = 0
        var allowedStepDistance = 0f
        val anyHit = query.hitType == HitType.ANY
        for (i in 0 until 16) { // max steps
            allowedStepDistance = getSignedDistance(pos)
            if (allowedStepDistance < 0 && anyHit) {
                return distance // we found a hit :3
            }
            distance += allowedStepDistance
            // we have gone too far -> the ray does not intersect the collider
            if (distance >= query.maxDistance) {
                return Float.POSITIVE_INFINITY
            } else {
                // we are still in the sector
                pos.set(query.direction).mul(distance).add(query.start)
                if (abs(allowedStepDistance) < 1e-3f) {
                    // we found the surface :)
                    isDone++ // we want at least one extra iteration
                    if (isDone > 1) {
                        break
                    }
                }
            }
        }
        if (abs(allowedStepDistance) > 1f) return Float.POSITIVE_INFINITY
        // the normal calculation can be 4x more expensive than the normal evaluation -> only do it once
        if (surfaceNormal != null) getSignedDistance(pos, surfaceNormal)
        return distance
    }

    // a collider needs to be drawn
    override fun onDrawGUI(pipeline: Pipeline, all: Boolean) {
        drawShape(pipeline)
        // todo draw transformation gizmos for easy collider manipulation
        //  e.g., for changing the radius of a sphere
    }

    abstract fun drawShape(pipeline: Pipeline)

    companion object {
        private val sampleEntity = Entity()
        private val cubeAABB = AABBd(-1.0, 1.0)

        var colliderLineColor = 0x77ffff or black
    }
}