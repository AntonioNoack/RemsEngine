package me.anno.engine.ui.render

import org.joml.Matrix4f
import org.joml.Quaterniond
import org.joml.Vector3d

/**
 * shared render state;
 * do not expect this to deliver the values inside UI events!, only on ECS Components
 * */
object RenderState {

    var worldScale = 1.0
    var prevWorldScale = 1.0

    val cameraMatrix = Matrix4f()
    val cameraPosition = Vector3d()
    val cameraDirection = Vector3d()
    val cameraRotation = Quaterniond()

    val prevCamMatrix = Matrix4f()
    val prevCameraPosition = Vector3d()

    var isPerspective = true
    var fovYRadians = 1.57f

}