package com.bulletphysics.linearmath

import com.bulletphysics.linearmath.VectorUtil.mulCoord
import cz.advel.stack.Stack
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

    fun drawTriangle(
        v0: Vector3d, v1: Vector3d, v2: Vector3d,
        n0: Vector3d, n1: Vector3d, n2: Vector3d,
        color: Vector3d, alpha: Double
    ) {
        drawTriangle(v0, v1, v2, color, alpha)
    }

    fun drawTriangle(v0: Vector3d, v1: Vector3d, v2: Vector3d, color: Vector3d, alpha: Double) {
        drawLine(v0, v1, color)
        drawLine(v1, v2, color)
        drawLine(v2, v0, color)
    }

    fun drawContactPoint(position: Vector3d, normal: Vector3d, distance: Double, lifeTime: Int, color: Vector3d)

    fun reportErrorWarning(warningString: String)

    fun draw3dText(location: Vector3d, textString: String)

    val debugMode: Int

    fun drawAabb(from: Vector3d, to: Vector3d, color: Vector3d) {
        val halfExtents = Stack.newVec(to)
        halfExtents.sub(from)
        halfExtents.mul(0.5)

        val center = Stack.newVec(to)
        center.add(from)
        center.mul(0.5)

        val edgecoord = Stack.newVec()
        edgecoord.set(1.0, 1.0, 1.0)
        val pa = Stack.newVec()
        val pb = Stack.newVec()
        for (i in 0 until 4) {
            for (j in 0 until 3) {
                pa.set(edgecoord.x * halfExtents.x, edgecoord.y * halfExtents.y, edgecoord.z * halfExtents.z)
                pa.add(center)

                val othercoord = j % 3

                mulCoord(edgecoord, othercoord, -1.0)
                pb.set(edgecoord.x * halfExtents.x, edgecoord.y * halfExtents.y, edgecoord.z * halfExtents.z)
                pb.add(center)

                drawLine(pa, pb, color)
            }
            edgecoord.set(-1.0, -1.0, -1.0)
            if (i < 3) {
                mulCoord(edgecoord, i, -1.0)
            }
        }
    }
}
