package me.anno.ecs

import me.anno.ecs.EntityQuery.getComponent
import me.anno.ecs.EntityQuery.hasComponent
import me.anno.ecs.components.physics.Physics

/**
 * Helper functions to add Physics onto an Entity;
 * This may be (re)moved when we have proper Systems
 * */
object EntityPhysics {

    fun Entity.invalidatePhysics(force: Boolean) {
        if (force || hasPhysicsInfluence()) {
            physics?.invalidate(this)
        }
    }

    fun Entity.invalidatePhysicsTransform(force: Boolean) {
        if (force || hasPhysicsInfluence()) {
            physics?.invalidateTransform(this)
        }
    }

    fun Entity.invalidateRigidbody() {
        physics?.invalidate(this)
    }

    val Entity.physics get() = getRoot(Entity::class).getComponent(Physics::class)

    fun Entity.rebuildPhysics(physics: Physics<*, *>) {
        if (hasComponent(physics.rigidComponentClass)) {
            physics.invalidate(this)
        } else {
            val children = children
            for (index in children.indices) {
                children[index].rebuildPhysics(physics)
            }
        }
    }

    fun Entity.hasPhysicsInfluence(): Boolean {
        return isPhysicsControlled || parentEntity?.hasPhysicsInfluence() == true
    }

    fun Entity.checkNeedsPhysics() {
        // physics
        if (allInHierarchy { it.isEnabled }) {
            // something can change
            physics?.invalidate(this)
        }
    }
}