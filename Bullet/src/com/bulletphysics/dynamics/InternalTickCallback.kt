package com.bulletphysics.dynamics

/**
 * Callback called for each internal tick.
 *
 * @author jezek2
 */
interface InternalTickCallback {
    fun internalTick(world: DynamicsWorld, timeStep: Double)
}
