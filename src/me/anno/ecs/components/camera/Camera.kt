package me.anno.ecs.components.camera

import me.anno.ecs.Component
import me.anno.ecs.annotations.DebugAction
import me.anno.ecs.annotations.Type
import me.anno.ecs.components.camera.effects.CameraEffect
import me.anno.ecs.components.player.LocalPlayer.Companion.currentLocalPlayer
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.engine.ui.LineShapes
import me.anno.maths.Maths.clamp
import org.joml.Vector2f
import org.joml.Vector4f

// like the studio camera,
// a custom state, which stores all related rendering information
class Camera : Component() {

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

    val effects = ArrayList<CameraEffect>()

    override fun listChildTypes(): String = "e"
    override fun getChildListByType(type: Char) = effects
    override fun getChildListNiceName(type: Char) = "Effects"
    override fun getOptionsByType(type: Char) = getOptionsByClass(this, CameraEffect::class)
    override fun addChildByType(index: Int, type: Char, child: PrefabSaveable) {
        if (child is CameraEffect) {
            effects.add(clamp(index, 0, effects.size), child)
        } else super.addChildByType(index, type, child)
    }

    override fun removeChild(child: PrefabSaveable) {
        effects.remove(child)
    }

    @Type("Color4")
    var clearColor = Vector4f(0.1f, 0.2f, 0.3f, 1f)

    val bloomStrength = 0.5f
    val bloomOffset = 10f

    /**
     * offset of the center relative to the screen center; in OpenGL coordinates [-1, +1]²
     * */
    var center = Vector2f()

    @DebugAction
    fun use() {
        use(1f)
    }

    // function to blend to the next one
    fun use(blendingTime: Float) {
        val player = currentLocalPlayer!!
        val state = player.cameraState
        // only if not already set as target
        if (state.currentCamera != this) {
            state.cameraBlendingTime = blendingTime
            state.cameraBlendingProgress = 0f
            state.previousCamera = state.currentCamera
            state.currentCamera = this
        }
    }

    override fun clone(): Camera {
        val clone = Camera()
        copy(clone)
        return clone
    }

    override fun onDrawGUI(all: Boolean) {
        // todo draw camera symbol with all the properties
        LineShapes.drawArrowZ(entity, 0.0, 1.0) // not showing up?
    }

    override fun copy(clone: PrefabSaveable) {
        super.copy(clone)
        clone as Camera
        clone.isPerspective = isPerspective
        clone.near = near
        clone.far = far
        clone.fovY = fovY
        clone.clearColor.set(clearColor)
    }

    override val className get() = "Camera"

}