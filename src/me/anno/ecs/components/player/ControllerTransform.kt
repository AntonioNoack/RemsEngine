package me.anno.ecs.components.player

import me.anno.ecs.Component
import me.anno.ecs.systems.OnUpdate
import me.anno.input.Input

@Suppress("unused")
class ControllerTransform : Component(), OnUpdate {
    var controllerIndex = 0
    override fun onUpdate() {
        val transform = transform
        val controller = Input.controllers.getOrNull(controllerIndex)
        if (transform != null && controller != null) { // todo this isn't visible, why?? :(
            transform.localPosition = transform.localPosition.set(controller.position)
            transform.localRotation = transform.localRotation.set(controller.rotation)
            transform.teleportUpdate()
        }
    }
}