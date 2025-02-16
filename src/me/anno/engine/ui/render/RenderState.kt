package me.anno.engine.ui.render

import org.joml.Matrix4f
import org.joml.Quaterniond
import org.joml.Quaternionf
import org.joml.Vector3d
import org.joml.Vector3f

/**
 * shared render state;
 * do not expect this to deliver the values inside UI events!, only on ECS Components
 * */
object RenderState {

    var aspectRatio = 1f

    val cameraMatrix = Matrix4f()
    val cameraMatrixInv = Matrix4f()
    val cameraPosition = Vector3d()
    val cameraRotation = Quaternionf()
    val cameraDirectionRight = Vector3f() // = cameraRotation.transform([1,0,0])
    val cameraDirectionUp = Vector3f() // = cameraRotation.transform([0,1,0])
    val cameraDirection = Vector3f() // = cameraRotation.transform([0,0,-1])

    fun calculateDirections(isPerspective: Boolean) {
        cameraDirection
            .set(0.0, 0.0, -1.0)
            .rotate(cameraRotation)
        cameraDirectionRight
            .set(1.0, 0.0, 0.0)
            .rotate(cameraRotation)
        cameraDirectionUp
            .set(0.0, 1.0, 0.0)
            .rotate(cameraRotation)
        cameraMatrix.invert(cameraMatrixInv)
        this.isPerspective = isPerspective
    }

    val prevCameraMatrix = Matrix4f()
    val prevCameraPosition = Vector3d()
    val prevCameraRotation = Quaterniond()

    var isPerspective = true
        private set

    var fovXRadians = 1.57f
    var fovYRadians = 1.57f
    var fovXCenter = 0.5f
    var fovYCenter = 0.5f

    var near = 0f
    var far = 0f

    // 0 or 1; for effects, which need to store data between frames
    var viewIndex = 0

}