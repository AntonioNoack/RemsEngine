package me.anno.ecs.components.collider

import me.anno.ecs.Entity
import me.anno.ecs.EntityPhysics.invalidateRigidbody
import me.anno.ecs.annotations.Docs
import me.anno.ecs.annotations.Range
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.ecs.systems.OnDrawGUI
import me.anno.engine.raycast.RayQuery
import me.anno.engine.raycast.RayQueryLocal
import me.anno.engine.raycast.Raycast
import me.anno.engine.raycast.RaycastCollider
import me.anno.engine.serialization.SerializedProperty
import me.anno.engine.ui.render.RenderMode
import me.anno.engine.ui.render.RenderView
import me.anno.gpu.pipeline.Pipeline
import me.anno.maths.Maths
import me.anno.maths.Maths.SQRT1_2
import me.anno.maths.bvh.HitType
import me.anno.utils.Color.black
import me.anno.utils.pooling.JomlPools
import org.joml.AABBd
import org.joml.Matrix4x3
import org.joml.Vector3d
import org.joml.Vector3f
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

// todo collision-effect mappings:
//  - which listener is used
//  - whether a collision happens
//  - whether a can push b
//  - whether b can push a

// todo collider events

abstract class Collider : CollidingComponent(), OnDrawGUI {

    @Range(0.0, 1.0)
    @SerializedProperty
    var roundness = 0.0

    @Docs("Whether this collider will collide with stuff, or just detect collisions")
    @SerializedProperty
    var hasPhysics = true
        set(value) {
            if (field != value) {
                field = value
                invalidateRigidbody()
            }
        }

    @Docs("Whether the collider is convex; in Bullet, only convex-convex and convex-concave interactions are defined")
    open val isConvex: Boolean = true

    override fun hasRaycastType(typeMask: Int): Boolean {
        return typeMask.and(Raycast.COLLIDERS) != 0
    }

    override fun raycast(query: RayQuery): Boolean {
        return RaycastCollider.raycastGlobalCollider(query, entity ?: sampleEntity, this)
    }

    override fun onChangeStructure(entity: Entity) {
        super.onChangeStructure(entity)
        entity.invalidateRigidbody()
        entity.invalidateCollisionMask()
    }

    override fun fillSpace(globalTransform: Matrix4x3, dstUnion: AABBd): Boolean {
        val tmp = JomlPools.vec3d.create()
        union(globalTransform, dstUnion, tmp, false)
        JomlPools.vec3d.sub(1)
        return true
    }

    override fun copyInto(dst: PrefabSaveable) {
        super.copyInto(dst)
        if (dst !is Collider) return
        dst.roundness = roundness
        dst.hasPhysics = hasPhysics
    }

    fun unionRing(
        globalTransform: Matrix4x3, aabb: AABBd, tmp: Vector3d,
        axis: Axis, r: Double, h: Double, preferExact: Boolean
    ) {
        if (preferExact) {
            // approximate the circle as 8 points, and their outer circle
            val r1 = SQRT1_2 * r
            when (axis) {
                Axis.X -> {
                    union(globalTransform, aabb, tmp, h, +r, 0.0)
                    union(globalTransform, aabb, tmp, h, -r, 0.0)
                    union(globalTransform, aabb, tmp, h, 0.0, +r)
                    union(globalTransform, aabb, tmp, h, 0.0, -r)
                }
                Axis.Y -> {
                    union(globalTransform, aabb, tmp, +r, h, 0.0)
                    union(globalTransform, aabb, tmp, -r, h, 0.0)
                    union(globalTransform, aabb, tmp, 0.0, h, +r)
                    union(globalTransform, aabb, tmp, 0.0, h, -r)
                }
                Axis.Z -> {
                    union(globalTransform, aabb, tmp, +r, 0.0, h)
                    union(globalTransform, aabb, tmp, -r, 0.0, h)
                    union(globalTransform, aabb, tmp, 0.0, +r, h)
                    union(globalTransform, aabb, tmp, 0.0, -r, h)
                }
            }
            unionRingQuad(globalTransform, aabb, tmp, axis, r1, h)
        } else unionRingQuad(globalTransform, aabb, tmp, axis, r, h)
    }

    private fun unionRingQuad(
        globalTransform: Matrix4x3, aabb: AABBd, tmp: Vector3d,
        axis: Axis, r: Double, h: Double
    ) {
        // approximate the circle as a quad
        when (axis) {
            Axis.X -> {
                union(globalTransform, aabb, tmp, h, +r, +r)
                union(globalTransform, aabb, tmp, h, +r, -r)
                union(globalTransform, aabb, tmp, h, -r, +r)
                union(globalTransform, aabb, tmp, h, -r, -r)
            }
            Axis.Y -> {
                union(globalTransform, aabb, tmp, +r, h, +r)
                union(globalTransform, aabb, tmp, +r, h, -r)
                union(globalTransform, aabb, tmp, -r, h, +r)
                union(globalTransform, aabb, tmp, -r, h, -r)
            }
            Axis.Z -> {
                union(globalTransform, aabb, tmp, +r, +r, h)
                union(globalTransform, aabb, tmp, +r, -r, h)
                union(globalTransform, aabb, tmp, -r, +r, h)
                union(globalTransform, aabb, tmp, -r, -r, h)
            }
        }
    }

    fun unionCube(globalTransform: Matrix4x3, aabb: AABBd, tmp: Vector3d, hx: Double, hy: Double, hz: Double) {
        // union the most typical layout: a sphere
        // 001,010,100,-001,-010,-100
        union(globalTransform, aabb, tmp, +hx, +hy, +hz)
        union(globalTransform, aabb, tmp, +hx, +hy, -hz)
        union(globalTransform, aabb, tmp, +hx, -hy, +hz)
        union(globalTransform, aabb, tmp, +hx, -hy, +hz)
        union(globalTransform, aabb, tmp, -hx, +hy, +hz)
        union(globalTransform, aabb, tmp, -hx, +hy, -hz)
        union(globalTransform, aabb, tmp, -hx, -hy, +hz)
        union(globalTransform, aabb, tmp, -hx, -hy, +hz)
    }

    fun union(globalTransform: Matrix4x3, aabb: AABBd, tmp: Vector3d, x: Double, y: Double, z: Double) {
        aabb.union(globalTransform.transformPosition(tmp.set(x, y, z)))
    }

    open fun union(globalTransform: Matrix4x3, aabb: AABBd, tmp: Vector3d, preferExact: Boolean) {
        // union a sample layout
        // 001,010,100,-001,-010,-100
        union(globalTransform, aabb, tmp, +1.0, 0.0, 0.0)
        union(globalTransform, aabb, tmp, -1.0, 0.0, 0.0)
        union(globalTransform, aabb, tmp, 0.0, +1.0, 0.0)
        union(globalTransform, aabb, tmp, 0.0, -1.0, 0.0)
        union(globalTransform, aabb, tmp, 0.0, 0.0, +1.0)
        union(globalTransform, aabb, tmp, 0.0, 0.0, -1.0)
    }

    fun and2SDFs(deltaPos: Vector3f, roundness: Float = this.roundness.toFloat()): Float {
        val dx = deltaPos.x + roundness
        val dy = deltaPos.y + roundness
        val outside = Maths.length(max(dx, 0f), max(dy, 0f))
        val inside = min(max(dx, dy), 0f)
        return outside + inside - roundness
    }

    fun and3SDFs(deltaPos: Vector3f, roundness: Float = this.roundness.toFloat()): Float {
        val dx = deltaPos.x + roundness
        val dy = deltaPos.y + roundness
        val dz = deltaPos.z + roundness
        val outside = Maths.length(max(dx, 0f), max(dy, 0f), max(dz, 0f))
        val inside = min(max(dx, max(dy, dz)), 0f)
        return outside + inside - roundness
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
        if (all ||
            // entity?.physics?.showDebug == true ||
            RenderView.currentInstance?.renderMode == RenderMode.PHYSICS
        ) drawShape(pipeline)
        // todo color based on physics / trigger (?)
        // todo draw transformation gizmos for easy collider manipulation
    }

    abstract fun drawShape(pipeline: Pipeline)

    companion object {

        private val sampleEntity = Entity()

        fun getLineColor(hasPhysics: Boolean): Int {
            return if (hasPhysics) 0x77ffff or black
            else 0xffff77 or black
        }

        const val COSINE_22_5 = 1.0 / 1.082392200292394 // 1.0/cos(45*PI/180/2)
        const val INV_COSINE_22_5 = 1.082392200292394 // 1.0/cos(45*PI/180/2)
        const val OUTER_SPHERE_RADIUS_X8 = 1.224744871391589 // sqrt(1.5),
        // what is the inverse of the inner radius of a sphere approximated by 3 rings of 8 segments each
    }
}