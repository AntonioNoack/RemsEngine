package me.anno.ecs.components.player

import me.anno.ecs.Component
import me.anno.ecs.systems.OnUpdate
import me.anno.input.Input
import org.joml.Quaternionf
import org.joml.Vector3d

@Suppress("unused")
class ControllerTransform : Component(), OnUpdate {

    var controllerIndex = 0

    // these could be done with a sub-entity ofc, too
    var localOffset = Vector3d() // local
    var extraRotation = Quaternionf()

    override fun onUpdate() {
        val entity = entity ?: return
        val transform = entity.transform
        val controller = Input.controllers.getOrNull(controllerIndex) ?: return
        transform.localRotation = transform.localRotation
            .set(controller.rotation).mul(extraRotation)
        transform.localPosition = transform.localPosition
            .set(localOffset).rotate(transform.localRotation)
            .add(controller.position)
        entity.invalidateAABBsCompletely()
    }
}