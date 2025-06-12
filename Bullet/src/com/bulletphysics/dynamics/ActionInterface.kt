package com.bulletphysics.dynamics

import com.bulletphysics.collision.dispatch.CollisionWorld
import com.bulletphysics.linearmath.IDebugDraw

/**
 * Basic interface to allow actions such as vehicles and characters to be updated inside a [DynamicsWorld].
 *
 * @author tomrbryn
 */
interface ActionInterface {
    fun updateAction(collisionWorld: CollisionWorld, deltaTimeStep: Double)
    fun debugDraw(debugDrawer: IDebugDraw)
}
