package me.anno.ecs.components.collider

import me.anno.ecs.Component
import me.anno.ecs.Entity
import me.anno.ecs.annotations.DebugTitle
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.engine.raycast.RayHit
import me.anno.engine.raycast.RayQuery
import org.joml.AABBd
import org.joml.Matrix4x3d
import org.joml.Vector3d

abstract class CollidingComponent : Component() {

    @DebugTitle("What objects it should collide with, bit mask")
    var collisionMask: Int = 1

    fun canCollide(collisionMask: Int) = this.collisionMask.and(collisionMask) != 0

    /** whether the typeMask includes this type, see Raycast.kt */
    open fun hasRaycastType(typeMask: Int) = true

    /**
     * returns whether the object was hit; closest hit is reported
     * */
    open fun raycastClosestHit(query: RayQuery): Boolean = false

    /**
     * returns whether the object was hit; any hit is reported
     * */
    open fun raycastAnyHit(query: RayQuery): Boolean {
        // by default just use closestHit
        return raycastClosestHit(query)
    }

    abstract override fun fillSpace(globalTransform: Matrix4x3d, aabb: AABBd): Boolean

    override fun copyInto(dst: PrefabSaveable) {
        super.copyInto(dst)
        dst as CollidingComponent
        dst.collisionMask = collisionMask
    }
}