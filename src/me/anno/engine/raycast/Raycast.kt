package me.anno.engine.raycast

import me.anno.ecs.Entity
import me.anno.ecs.EntityQuery.forAllChildren
import me.anno.ecs.EntityQuery.forAllComponents
import me.anno.ecs.components.collider.CollidingComponent

/**
 * Casts a ray into the scene, reports the closest hit.
 *
 * Unity: Physics.SphereCast
 * Unreal: LineTraceByChannel
 * Godot: RayCast
 * */
object Raycast {

    const val TRIANGLE_FRONT = 1
    const val TRIANGLE_BACK = 2
    const val TRIANGLES = TRIANGLE_FRONT or TRIANGLE_BACK // 3
    const val COLLIDERS = 4
    const val SDFS = 8
    const val SKY = 16

    // todo option for smoothed collision surfaces by their normal

    /**
     * finds the minimum distance hit;
     * returns whether something was hit
     * */
    fun raycastClosestHit(entity: Entity, query: RayQuery): Boolean {
        entity.validateMasks()
        entity.getBounds()
        entity.validateTransform()
        val originalDistance = query.result.distance
        entity.forAllComponents(query.includeDisabled) { component ->
            if (component !in query.ignored) {
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
        entity.forAllChildren(query.includeDisabled) { child ->
            if (child.canCollide(query.collisionMask) && child !in query.ignored) {
                if (child.getBounds().testLine(query.start, query.direction, query.result.distance)) {
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
        entity.getBounds()
        entity.validateTransform()
        entity.forAllComponents(query.includeDisabled) { component ->
            if (component !in query.ignored) {
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
        entity.forAllChildren(query.includeDisabled) { child ->
            if (child.canCollide(query.collisionMask) && child !in query.ignored) {
                if (child.getBounds().testLine(query.start, query.direction, query.result.distance)) {
                    if (raycastAnyHit(child, query)) return true
                }
            }
        }
        return false
    }
}