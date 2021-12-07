package me.anno.ecs.components.camera

import me.anno.ecs.Component
import me.anno.ecs.components.player.LocalPlayer.Companion.currentLocalPlayer
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.engine.gui.LineShapes
import org.joml.Vector2f
import org.joml.Vector4f

// like the studio camera,
// a custom state, which stores all related rendering information
class CameraComponent : Component() {

    var isPerspective = true

    var near = 0.01

    var far = 5000.0

    /**
     * the fov when perspective, in degrees
     * */
    var fovY = 90f

    /**
     * the fov when orthographic, in base units
     * */
    var fovOrthographic = 500f

    // val pipeline = lazy { Pipeline() }

    val postProcessingEffects
        get() = components
            .filterIsInstance<PostProcessingEffectComponent>()

    var clearColor = Vector4f(0.1f, 0.2f, 0.3f, 1f)

    val bloomStrength = 0.5f
    val bloomOffset = 10f

    /**
     * offset of the center relative to the screen center; in OpenGL coordinates [-1, +1]²
     * */
    var center = Vector2f()

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

    override fun clone(): CameraComponent {
        val clone = CameraComponent()
        copy(clone)
        return clone
    }

    override fun onDrawGUI() {
        // todo draw camera symbol with all the properties
        LineShapes.drawArrowZ(entity, 0.0, 1.0) // not showing up?
    }

    override fun copy(clone: PrefabSaveable) {
        super.copy(clone)
        clone as CameraComponent
        clone.isPerspective = isPerspective
        clone.near = near
        clone.far = far
        clone.fovY = fovY
        clone.clearColor = clearColor
    }

    override val className get() = "CameraComponent"

}