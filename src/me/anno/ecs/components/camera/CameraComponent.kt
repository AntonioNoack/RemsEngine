package me.anno.ecs.components.camera

import me.anno.ecs.Component
import me.anno.ecs.components.player.LocalPlayer.Companion.currentLocalPlayer
import me.anno.gpu.pipeline.Pipeline
import org.joml.Vector4f

// like the studio camera,
// a custom state, which stores all related rendering information
class CameraComponent : Component() {

    // todo different settings for orthographic and non-orthographic
    // todo different classes? maybe...

    var isPerspective = true

    var near = 0.01

    var far = 5000.0

    var fov = 90f

    val pipeline = Pipeline()

    val postProcessingEffects
        get() = components
            .filterIsInstance<PostProcessingEffectComponent>()

    val clearColor = Vector4f(0.1f, 0.2f, 0.3f, 1f)

    // function to blend to the next one
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

}