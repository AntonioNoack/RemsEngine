package me.anno.ecs.components.collider

import me.anno.ecs.Component
import me.anno.ecs.Entity
import me.anno.ecs.annotations.DebugTitle
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.engine.raycast.RayHit
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
     * returns whether the object was hit
     * */
    open fun raycast(
        entity: Entity,
        start: Vector3d,
        direction: Vector3d,
        end: Vector3d,
        radiusAtOrigin: Double,
        radiusPerUnit: Double,
        typeMask: Int,
        includeDisabled: Boolean,
        result: RayHit
    ): Boolean = false

    abstract override fun fillSpace(globalTransform: Matrix4x3d, aabb: AABBd): Boolean

    override fun copyInto(dst: PrefabSaveable) {
        super.copyInto(dst)
        dst as CollidingComponent
        dst.collisionMask = collisionMask
    }

}