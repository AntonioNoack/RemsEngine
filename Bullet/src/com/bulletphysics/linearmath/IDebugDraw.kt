package com.bulletphysics.linearmath

import org.joml.Vector3d

/**
 * IDebugDraw interface class allows hooking up a debug renderer to visually debug
 * simulations.
 *
 * Typical use case: create a debug drawer object, and assign it to a [com.bulletphysics.collision.dispatch.CollisionWorld]
 * or [com.bulletphysics.dynamics.DynamicsWorld] using setDebugDrawer and call debugDrawWorld.
 *
 * A class that implements the IDebugDraw interface has to implement the drawLine
 * method at a minimum.
 *
 * @author jezek2
 */
interface IDebugDraw {

    fun drawLine(from: Vector3d, to: Vector3d, color: Vector3d)
    fun drawContactPoint(position: Vector3d, normal: Vector3d, distance: Double, lifeTime: Int, color: Vector3d)
    fun draw3dText(location: Vector3d, textString: String)

    fun reportErrorWarning(warningString: String)

    /**
     * combination of values from DebugDrawModes
     * */
    val debugMode: Int
}
