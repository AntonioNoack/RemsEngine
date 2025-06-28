package me.anno.bullet.bodies

import me.anno.ecs.components.collider.CollisionFilters.ANY_DYNAMIC_MASK
import me.anno.ecs.components.collider.CollisionFilters.GHOST_GROUP_ID
import com.bulletphysics.collision.dispatch.GhostObject
import me.anno.ecs.annotations.DebugProperty
import me.anno.ecs.annotations.Docs
import me.anno.utils.structures.lists.SimpleList

/**
 * todo
 *  - handle this just like Entities with Rigidbodies,
 *  - just create a GhostObject instead of a vehicle or Rigidbody.
 *  - when there is an overlap (starting/ending), call the respective events
 *  - respect the collision mask
 *
 * todo test scene, where a gate opens when a sphere reaches a certain area
 * */
class GhostBody : PhysicsBody<GhostObject>() {

    init {
        collisionGroup = GHOST_GROUP_ID
        collisionMask = ANY_DYNAMIC_MASK
    }

    @DebugProperty
    val numOverlaps: Int
        get() = overlappingBodies.size

    @Docs("Overlapping Dynamic/KinematicBodies; only safe to access onPhysicsUpdate")
    val overlappingBodies: List<PhysicalBody> = OverlappingBodiesList(this)

    private class OverlappingBodiesList(val ghostBody: GhostBody) : SimpleList<PhysicalBody>() {
        override fun get(index: Int): PhysicalBody {
            val ghostObject = ghostBody.nativeInstance!!
            val rigidbody = ghostObject.overlappingPairs[index]
            return rigidbody.userData as PhysicalBody
        }

        override val size: Int
            get() {
                val ghostObject = ghostBody.nativeInstance ?: return 0
                return ghostObject.overlappingPairs.size
            }
    }
}