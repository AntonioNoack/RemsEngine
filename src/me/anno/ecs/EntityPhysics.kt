package me.anno.ecs

import me.anno.ecs.components.physics.Physics
import me.anno.ecs.systems.Systems
import kotlin.reflect.KClass
import kotlin.reflect.safeCast

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

    fun Entity.invalidateRigidbody() {
        forAllPhysics { it.invalidate(this) }
    }

    fun <V : Physics<*, *>> getPhysics(clazz: KClass<V>): V? {
        var result: V? = null
        forAllPhysics {
            if (result == null) {
                result = clazz.safeCast(it)
            }
        }
        return result
    }

    inline fun forAllPhysics(crossinline callback: (Physics<*, *>) -> Unit) {
        Systems.forAllSystems(Physics::class) { callback(it) }
    }

    fun Entity.hasPhysicsInfluence(): Boolean {
        return isPhysicsControlled || parentEntity?.hasPhysicsInfluence() == true
    }

    fun Entity.checkNeedsPhysics() {
        // physics
        if (allInHierarchy { it.isEnabled }) {
            // something can change
            forAllPhysics { it.invalidate(this) }
        }
    }
}