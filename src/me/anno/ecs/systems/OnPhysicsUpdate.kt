package me.anno.ecs.systems

/**
 * Called on components, which are siblings to rigidbodies, when the physics engine does a simulation step; may be on a separate thread
 * */
interface OnPhysicsUpdate {
    fun onPhysicsUpdate(dt: Double)
}