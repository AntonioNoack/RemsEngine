package me.anno.ecs

import org.joml.Matrix4x3d
import org.joml.Vector3d

class Transform {

    val relativePosition = Vector3d()
    val relativeRotation = Vector3d()
    val relativeScale = Vector3d()

    val worldTransform = Matrix4x3d()
    val localTransform = Matrix4x3d()

    // todo only update if changed to save resources
    fun update(parent: Transform) {

        localTransform.identity()

        localTransform.translate(relativePosition)

        localTransform.rotateY(relativeRotation.y)
        localTransform.rotateX(relativeRotation.x)
        localTransform.rotateZ(relativeRotation.z)

        localTransform.scale(relativeScale)

        worldTransform.set(parent.worldTransform)
        worldTransform.mul(localTransform)

    }

}