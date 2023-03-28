package me.anno.ecs.components.collider

import com.bulletphysics.collision.shapes.CollisionShape
import com.bulletphysics.linearmath.Transform
import me.anno.utils.Color.black
import me.anno.ecs.Entity
import me.anno.ecs.annotations.Docs
import me.anno.ecs.annotations.Range
import me.anno.ecs.components.physics.BulletPhysics.Companion.mat4x3ToTransform
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.engine.raycast.RayHit
import me.anno.engine.raycast.Raycast
import me.anno.engine.ui.render.RenderMode
import me.anno.engine.ui.render.RenderView
import me.anno.io.serialization.SerializedProperty
import me.anno.maths.Maths
import me.anno.maths.Maths.SQRT1_2
import me.anno.utils.pooling.JomlPools
import org.joml.AABBd
import org.joml.Matrix4x3d
import org.joml.Vector3d
import org.joml.Vector3f
import kotlin.math.abs
import kotlin.math.max

// todo collision-effect mappings:
//  - which listener is used
//  - whether a collision happens
//  - whether a can push b
//  - whether b can push a

// todo collider events

abstract class Collider : CollidingComponent() {

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

    override fun raycast(
        entity: Entity,
        start: Vector3d,
        direction: Vector3d,
        end: Vector3d,
        radiusAtOrigin: Double,
        radiusPerUnit: Double,
        typeMask: Int,
        includeDisabled: Boolean,
        result: RayHit
    ): Boolean {
        return if (Raycast.raycastCollider(
                entity, this, start, direction, end,
                radiusAtOrigin, radiusPerUnit, result
            )
        ) {
            result.collider = this
            true
        } else false
    }

    override fun onChangeStructure(entity: Entity) {
        super.onChangeStructure(entity)
        entity.invalidateRigidbody()
        entity.invalidateCollisionMask()
    }

    override fun fillSpace(globalTransform: Matrix4x3d, aabb: AABBd): Boolean {
        val tmp = JomlPools.vec3d.create()
        union(globalTransform, aabb, tmp, false)
        JomlPools.vec3d.sub(1)
        return true
    }

    override fun copyInto(dst: PrefabSaveable) {
        super.copyInto(dst)
        dst as Collider
        dst.roundness = roundness
        dst.hasPhysics = hasPhysics
    }

    fun unionRing(
        globalTransform: Matrix4x3d, aabb: AABBd, tmp: Vector3d,
        axis: Int, r: Double, h: Double,
        preferExact: Boolean
    ) {
        if (preferExact) {
            // approximate the circle as 8 points, and their outer circle
            val r0 = INV_COSINE_22_5 * r
            val r1 = SQRT1_2 * r0
            when (axis) {
                0 -> {
                    union(globalTransform, aabb, tmp, h, +r0, 0.0)
                    union(globalTransform, aabb, tmp, h, -r0, 0.0)
                    union(globalTransform, aabb, tmp, h, 0.0, +r)
                    union(globalTransform, aabb, tmp, h, 0.0, -r)
                    union(globalTransform, aabb, tmp, h, +r1, +r1)
                    union(globalTransform, aabb, tmp, h, +r1, -r1)
                    union(globalTransform, aabb, tmp, h, -r1, +r1)
                    union(globalTransform, aabb, tmp, h, -r1, -r1)
                }
                1 -> {
                    union(globalTransform, aabb, tmp, +r1, h, +r1)
                    union(globalTransform, aabb, tmp, +r1, h, -r1)
                    union(globalTransform, aabb, tmp, -r1, h, +r1)
                    union(globalTransform, aabb, tmp, -r1, h, -r1)
                }
                2 -> {
                    union(globalTransform, aabb, tmp, +r1, +r1, h)
                    union(globalTransform, aabb, tmp, +r1, -r1, h)
                    union(globalTransform, aabb, tmp, -r1, +r1, h)
                    union(globalTransform, aabb, tmp, -r1, -r1, h)
                }
            }
        } else {
            // approximate the circle as a quad
            when (axis) {
                0 -> {
                    union(globalTransform, aabb, tmp, h, +r, +r)
                    union(globalTransform, aabb, tmp, h, +r, -r)
                    union(globalTransform, aabb, tmp, h, -r, +r)
                    union(globalTransform, aabb, tmp, h, -r, -r)
                }
                1 -> {
                    union(globalTransform, aabb, tmp, +r, h, +r)
                    union(globalTransform, aabb, tmp, +r, h, -r)
                    union(globalTransform, aabb, tmp, -r, h, +r)
                    union(globalTransform, aabb, tmp, -r, h, -r)
                }
                2 -> {
                    union(globalTransform, aabb, tmp, +r, +r, h)
                    union(globalTransform, aabb, tmp, +r, -r, h)
                    union(globalTransform, aabb, tmp, -r, +r, h)
                    union(globalTransform, aabb, tmp, -r, -r, h)
                }
            }
        }
    }

    open fun unionCube(globalTransform: Matrix4x3d, aabb: AABBd, tmp: Vector3d, hx: Double, hy: Double, hz: Double) {
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

    fun union(globalTransform: Matrix4x3d, aabb: AABBd, tmp: Vector3d, x: Double, y: Double, z: Double) {
        aabb.union(globalTransform.transformPosition(tmp.set(x, y, z)))
    }

    open fun union(globalTransform: Matrix4x3d, aabb: AABBd, tmp: Vector3d, preferExact: Boolean) {
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
        val inside = Maths.min(max(dx, dy), 0f)
        return outside + inside - roundness
    }

    fun and3SDFs(deltaPos: Vector3f, roundness: Float = this.roundness.toFloat()): Float {
        val dx = deltaPos.x + roundness
        val dy = deltaPos.y + roundness
        val dz = deltaPos.z + roundness
        val outside = Maths.length(max(dx, 0f), max(dy, 0f), max(dz, 0f))
        val inside = Maths.min(max(dx, max(dy, dz)), 0f)
        return outside + inside - roundness
    }

    fun createBulletCollider(base: Entity, scale: Vector3d): Pair<Transform, CollisionShape> {
        val transform0 = entity!!.fromLocalToOtherLocal(base)
        // there may be extra scale hidden in there
        val extraScale = transform0.getScale(Vector3d())
        val totalScale = Vector3d(scale).mul(extraScale)
        val shape = createBulletShape(totalScale)
        val transform = mat4x3ToTransform(transform0, extraScale)
        return transform to shape
    }

    /**
     * gets the signed distance to the surface of the collider, in local coordinates
     * the coordinates will be lost
     * outNormal returns the local normal
     *
     * if the distance field is warped, the returned distance should be a little smaller, so we don't overstep
     * */
    open fun getSignedDistance(deltaPos: Vector3f, outNormal: Vector3f): Float {

        outNormal.set(deltaPos) // save the position

        val e = 0.002f

        // from https://github.com/glslify/glsl-sdf-normal/blob/master/index.glsl
        // from https://www.shadertoy.com/view/ldfSWs
        val d1 = getSignedDistance(deltaPos.set(outNormal).add(+e, -e, -e))
        val d2 = getSignedDistance(deltaPos.set(outNormal).add(-e, -e, +e))
        val d3 = getSignedDistance(deltaPos.set(outNormal).add(-e, +e, -e))
        val d4 = getSignedDistance(deltaPos.set(outNormal).add(+e, +e, +e))

        outNormal.set(0f)
        outNormal.add(+d1, -d1, -d1)
        outNormal.add(-d2, -d2, +d2)
        outNormal.add(-d3, +d3, -d3)
        outNormal.add(+d4, +d4, +d4)

        return (d1 + d2 + d3 + d4) * 0.25f

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
    open fun raycast(
        start: Vector3f, direction: Vector3f, radiusAtOrigin: Float, radiusPerUnit: Float,
        surfaceNormal: Vector3f?, maxDistance: Float
    ): Float {
        // todo check if the ray is inside the bounding box:
        // todo I get many false-positives behind the object... why?
        // default implementation is slow, and not perfect:
        // ray casting
        var distance = 0f
        val pos = Vector3f(start)
        var isDone = 0
        var allowedStepDistance = 0f
        for (i in 0 until 16) { // max steps
            allowedStepDistance = getSignedDistance(pos)
            distance += allowedStepDistance
            // we have gone too far -> the ray does not intersect the collider
            if (distance >= maxDistance) {
                return Float.POSITIVE_INFINITY
            } else {
                // we are still in the sector
                pos.set(direction).mul(distance).add(start)
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

    abstract fun createBulletShape(scale: Vector3d): CollisionShape

    // a collider needs to be drawn
    override fun onDrawGUI(all: Boolean) {
        if (all ||
            entity?.physics?.showDebug == true ||
            RenderView.currentInstance?.renderMode == RenderMode.PHYSICS
        ) drawShape()
        // todo draw transformation gizmos for easy collider manipulation
    }

    abstract fun drawShape()

    companion object {
        val guiLineColor = 0x77ffff or black
        const val COSINE_22_5 = 1.0 / 1.082392200292394 // 1.0/cos(45*PI/180/2)
        const val INV_COSINE_22_5 = 1.082392200292394 // 1.0/cos(45*PI/180/2)
        const val OUTER_SPHERE_RADIUS_X8 = 1.224744871391589 // sqrt(1.5),
        // what is the inverse of the inner radius of a sphere approximated by 3 rings of 8 segments each
    }

}