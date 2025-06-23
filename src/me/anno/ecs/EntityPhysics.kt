package me.anno.ecs

import me.anno.ecs.components.physics.Physics
import me.anno.ecs.systems.Systems
import kotlin.reflect.KClass

/**
 * Helper functions to add Physics onto an Entity;
 * This may be (re)moved when we have proper Systems
 * */
object EntityPhysics {

    fun Entity.invalidatePhysicsTransform() {
        if (hasPhysicsInfluence()) {
            forAllPhysics { it.invalidateTransform(this) }
        }
    }

    fun Entity.invalidatePhysics() {
        forAllPhysics { physics -> physics.invalidate(this) }
    }

    fun <V : Physics<*, *>> getPhysics(clazz: KClass<V>): V? {
        var result: V? = null
        Systems.forAllSystems(clazz) { physics -> result = physics }
        return result
    }

    inline fun forAllPhysics(crossinline callback: (Physics<*, *>) -> Unit) {
        Systems.forAllSystems(Physics::class) { physics -> callback(physics) }
    }

    fun Entity.hasPhysicsInfluence(): Boolean {
        return isPhysicsControlled || parentEntity?.hasPhysicsInfluence() == true
    }

    fun Entity.invalidatePhysicsIfEnabled() {
        // physics
        if (allInHierarchy { it.isEnabled }) {
            // something can change
            invalidatePhysics()
        }
    }
}