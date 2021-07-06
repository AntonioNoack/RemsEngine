package me.anno.ecs.components.camera

import me.anno.ecs.Component
import me.anno.ecs.annotations.Range
import me.anno.ecs.components.player.LocalPlayer.Companion.currentLocalPlayer
import org.joml.Vector4f

// todo like the studio camera,
// todo function to blend to the next one
class Camera : Component() {

    // todo different settings for orthographic and non-orthographic
    // todo different classes? maybe...

    var isPerspective = false

    @Range(1e-308, 1e308)
    var near = 0.01

    @Range(1e-308, 1e308)
    var far = 5000.0

    var postProcessingEffects = ArrayList<PPE>()

    val clearColor = Vector4f(0f, 0f, 0f, 1f)

    class PPE

    fun use(blendingTime: Double) {
        val player = currentLocalPlayer!!
        val state = player.camera
        // only if not already set as target
        if (state.currentCamera != this) {
            state.cameraBlendingTime = blendingTime
            state.cameraBlendingProgress = 0.0
            state.previousCamera = state.currentCamera
            state.currentCamera = this
        }
    }

    override val className get() = "CameraComponent"

    companion object {

        // todo these are instance depending values
        var blendingTime = 0.0
        var blendingProgress = 0.0

    }

}