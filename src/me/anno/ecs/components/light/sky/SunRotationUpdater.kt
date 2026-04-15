package me.anno.ecs.components.light.sky

import me.anno.ecs.Component
import me.anno.ecs.EntityQuery.getComponent
import me.anno.ecs.systems.OnUpdate

/**
 * copies the rotation in skybox to the entity
 * */
class SunRotationUpdater : Component(), OnUpdate {
    override fun onUpdate() {
        val entity = entity ?: return
        val sky = getComponent(Skybox::class) ?: return
        entity.setRotation(sky.sunRotation)
    }
}