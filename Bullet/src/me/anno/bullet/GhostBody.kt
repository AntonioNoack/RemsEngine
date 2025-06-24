package me.anno.bullet

import com.bulletphysics.collision.dispatch.GhostObject
import me.anno.ecs.annotations.Docs
import me.anno.ecs.annotations.Range

/**
 * todo
 *  - handle this just like Entities with Rigidbodies,
 *  - just create a GhostObject instead of a vehicle or Rigidbody.
 *  - when there is an overlap (starting/ending), call the respective events
 *  - respect the collision mask
 *
 * todo test scene, where a gate opens when a sphere reaches a certain area
 * */
abstract class GhostBody : PhysicsBody<GhostObject>() {

    @Range(0.0, 15.0)
    var collisionMask = 1

    @Docs("Overlapping Rigidbodies/KinematicBodies, updates by physics; use OnPhysicsUpdate")
    val overlappingBodies = HashSet<PhysicalBody>()

}