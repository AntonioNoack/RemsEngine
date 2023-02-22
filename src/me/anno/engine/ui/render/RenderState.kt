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
    val cameraMatrixInv = Matrix4f()
    val cameraPosition = Vector3d()
    val cameraRotation = Quaterniond()
    val cameraDirectionRight = Vector3d() // = cameraRotation.transform([1,0,0])
    val cameraDirectionUp = Vector3d() // = cameraRotation.transform([0,1,0])
    val cameraDirection = Vector3d() // = cameraRotation.transform([0,0,-1])

    fun calculateDirections() {
        cameraDirection.set(0.0, 0.0, -1.0)
            .rotate(cameraRotation)
        cameraDirectionRight.set(1.0, 0.0, 0.0)
            .rotate(cameraRotation)
        cameraDirectionUp.set(0.0, 1.0, 0.0)
            .rotate(cameraRotation)
    }

    val prevCamMatrix = Matrix4f()
    val prevCamMatrixInv = Matrix4f()
    val prevCameraPosition = Vector3d()

    var isPerspective = true
    var fovXRadians = 1.57f
    var fovYRadians = 1.57f
    var reverseDepth = true
    var near = 0f
    var far = 0f

}