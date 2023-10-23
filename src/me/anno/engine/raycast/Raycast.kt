package me.anno.engine.raycast

import me.anno.ecs.Entity
import me.anno.ecs.Transform
import me.anno.ecs.components.collider.CollidingComponent
import me.anno.ecs.components.mesh.Mesh
import me.anno.maths.Maths.SQRT3
import me.anno.maths.Maths.hasFlag
import me.anno.maths.bvh.BVHBuilder
import me.anno.maths.bvh.SplitMethod
import me.anno.utils.pooling.JomlPools
import me.anno.utils.types.Triangles.rayTriangleIntersection
import org.joml.Matrix4x3d
import org.joml.Vector3d
import org.joml.Vector3f

/**
 * Casts a ray into the scene, reports the closest hit.
 *
 * Unity: Physics.SphereCast
 * Unreal: LineTraceByChannel
 * Godot: RayCast
 * */
object Raycast {

    val TRIANGLE_FRONT = 1
    val TRIANGLE_BACK = 2
    val TRIANGLES = TRIANGLE_FRONT or TRIANGLE_BACK // 3
    val COLLIDERS = 4
    val SDFS = 8
    val SKY = 16

    // todo option for smoothed collision surfaces by their normal

    /**
     * finds the minimum distance hit;
     * returns whether something was hit
     * */
    fun raycastClosestHit(entity: Entity, query: RayQuery): Boolean {
        entity.validateMasks()
        entity.validateAABBs()
        entity.validateTransform()
        val originalDistance = query.result.distance
        val components = entity.components
        for (i in components.indices) {
            val component = components[i]
            if ((query.includeDisabled || component.isEnabled) && component !in query.ignored) {
                if (component is CollidingComponent &&
                    component.hasRaycastType(query.typeMask) &&
                    component.canCollide(query.collisionMask)
                ) {
                    if (component.raycastClosestHit(query)) {
                        query.result.component = component
                    }
                }
            }
        }
        val children = entity.children
        for (i in children.indices) {
            val child = children[i]
            if ((query.includeDisabled || child.isEnabled) &&
                child.canCollide(query.collisionMask) &&
                child !in query.ignored
            ) {
                if (child.aabb.testLine(query.start, query.direction, query.result.distance)) {
                    raycastClosestHit(child, query)
                }
            }
        }
        return query.result.distance < originalDistance
    }

    /**
     * finds any hit;
     * returns whether something was hit
     * */
    @Suppress("unused")
    fun raycastAnyHit(entity: Entity, query: RayQuery): Boolean {
        entity.validateMasks()
        entity.validateAABBs()
        entity.validateTransform()
        val components = entity.components
        for (i in components.indices) {
            val component = components[i]
            if ((query.includeDisabled || component.isEnabled) && component !in query.ignored) {
                if (component is CollidingComponent &&
                    component.hasRaycastType(query.typeMask) &&
                    component.canCollide(query.collisionMask)
                ) {
                    if (component.raycastAnyHit(query)) {
                        query.result.component = component
                        return true
                    }
                }
            }
        }
        val children = entity.children
        for (i in children.indices) {
            val child = children[i]
            if ((query.includeDisabled || child.isEnabled) &&
                child.canCollide(query.collisionMask) &&
                child !in query.ignored
            ) {
                if (child.aabb.testLine(query.start, query.direction, query.result.distance)) {
                    if (raycastAnyHit(child, query)) return true
                }
            }
        }
        return false
    }

}