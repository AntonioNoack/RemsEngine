package me.anno.box2d

import me.anno.ecs.annotations.DebugProperty
import me.anno.ecs.annotations.Docs
import me.anno.ecs.components.collider.CollisionFilters.ANY_DYNAMIC_MASK
import me.anno.ecs.components.collider.CollisionFilters.GHOST_GROUP_ID

class GhostBody2d : PhysicsBody2d() {

    init {
        collisionGroup = GHOST_GROUP_ID
        collisionMask = ANY_DYNAMIC_MASK
    }

    @DebugProperty
    val numOverlaps: Int
        get() = overlappingBodies.size

    @Docs("Overlapping Dynamic/KinematicBodies; only safe to access onPhysicsUpdate")
    val overlappingBodies: List<PhysicalBody2d> = TODO()

}