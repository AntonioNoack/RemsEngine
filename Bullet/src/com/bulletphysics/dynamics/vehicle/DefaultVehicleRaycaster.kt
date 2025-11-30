package com.bulletphysics.dynamics.vehicle

import com.bulletphysics.collision.dispatch.CollisionWorld.ClosestRayResultCallback
import com.bulletphysics.dynamics.DynamicsWorld
import com.bulletphysics.dynamics.RigidBody
import org.joml.Vector3d

/**
 * Default implementation of [VehicleRaycaster].
 *
 * @author jezek2
 */
@Suppress("unused")
class DefaultVehicleRaycaster(var dynamicsWorld: DynamicsWorld) : VehicleRaycaster() {
    override fun castRay(from: Vector3d, to: Vector3d, result: VehicleRaycasterResult): RigidBody? {
        val rayCallback = ClosestRayResultCallback(from, to)
        dynamicsWorld.rayTest(from, to, rayCallback)

        if (rayCallback.hasHit()) {
            val body = rayCallback.collisionObject
            if (body is RigidBody && body.hasContactResponse()) {
                result.hitPointInWorld.set(rayCallback.hitPointWorld)
                result.hitNormalInWorld.set(rayCallback.hitNormalWorld)
                result.hitNormalInWorld.normalize()
                result.distFraction = rayCallback.closestHitFraction
                return body
            }
        }
        return null
    }
}
