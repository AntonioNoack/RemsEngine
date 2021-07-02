package me.anno.ecs.components.camera

import me.anno.ecs.Component
import me.anno.ecs.annotations.Range

// todo like the studio camera,
// todo function to blend to the next one
class Camera : Component() {

    // todo different settings for orthographic and non-orthographic
    // todo different classes? maybe...

    var isPerspective = false

    @Range(1e-308, 1e308)
    var near = 0.01

    @Range(1e-308, 1e308)
    var far = 5000

    var postProcessingEffects = ArrayList<PPE>()

    class PPE


    fun use(blendingTime: Double) {
        // todo blend between the cameras...
    }

    override fun getClassName(): String = "CameraComponent"

    companion object {

        // todo these are instance depending values
        var blendingTime = 0.0
        var blendingProgress = 0.0

    }

}