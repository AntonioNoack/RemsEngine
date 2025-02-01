package me.anno.engine.raycast

import me.anno.ecs.Entity
import me.anno.ecs.EntityQuery.anyComponent
import me.anno.ecs.EntityQuery.forAllChildren
import me.anno.ecs.EntityQuery.forAllComponents
import me.anno.ecs.components.collider.CollidingComponent
import me.anno.utils.structures.Recursion

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
    fun raycast(scene: Entity, query: RayQuery): Boolean {
        scene.validateMasks()
        scene.validateTransform()
        val originalDistance = query.result.distance
        Recursion.processRecursive(scene) { entity, remaining ->
            raycast1(entity, query)
            raycastAddChildren(entity, query, remaining)
        }
        return query.result.distance < originalDistance
    }

    private fun raycastAddChildren(entity: Entity, query: RayQuery, remaining: ArrayList<Entity>) {
        entity.forAllChildren(query.includeDisabled) { child ->
            if (child.canCollide(query.collisionMask) &&
                child !in query.ignored &&
                child.getBounds().testLine(query.start, query.direction, query.result.distance)
            ) remaining.add(child)
        }
    }

    private fun raycast1(entity: Entity, query: RayQuery) {
        entity.forAllComponents(query.includeDisabled) { component ->
            if (component is CollidingComponent &&
                mayHit(component, query) &&
                component.raycast(query)
            ) {
                query.result.component = component
            }
        }
    }

    private fun mayHit(component: CollidingComponent, query: RayQuery): Boolean {
        return component !in query.ignored &&
                component.hasRaycastType(query.typeMask) &&
                component.canCollide(query.collisionMask)
    }
}