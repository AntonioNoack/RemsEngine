package me.anno.ecs.components.camera

import me.anno.ecs.Component
import me.anno.ecs.annotations.DebugAction
import me.anno.ecs.annotations.Type
import me.anno.ecs.components.camera.effects.CameraEffect
import me.anno.ecs.components.collider.Collider.Companion.guiLineColor
import me.anno.ecs.components.player.LocalPlayer.Companion.currentLocalPlayer
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.engine.ui.LineShapes
import me.anno.engine.ui.LineShapes.drawLine
import me.anno.engine.ui.LineShapes.drawRect
import me.anno.engine.ui.render.DrawAABB
import me.anno.engine.ui.render.RenderState
import me.anno.maths.Maths.clamp
import me.anno.utils.pooling.JomlPools
import org.joml.Vector2f
import org.joml.Vector4f
import kotlin.math.tan

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
    var fovOrthographic = 5f

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
    val bloomOffset = 25f

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
        val entity = entity
        val aspect = 16f / 9f
        LineShapes.drawArrowZ(entity, 0.0, 1.0) // not showing up?
        if (isPerspective) {
            // draw camera symbol with all the properties
            val dy = tan(fovY / 2f)
            val dx = dy * aspect
            val n00 = JomlPools.vec3f.create()
            val n01 = JomlPools.vec3f.create()
            val n10 = JomlPools.vec3f.create()
            val n11 = JomlPools.vec3f.create()
            val f00 = JomlPools.vec3f.create()
            val f01 = JomlPools.vec3f.create()
            val f10 = JomlPools.vec3f.create()
            val f11 = JomlPools.vec3f.create()
            n00.set(+dx * near, +dy * near, near)
            n01.set(+dx * near, -dy * near, near)
            n10.set(-dx * near, +dy * near, near)
            n11.set(-dx * near, -dy * near, near)
            f00.set(+dx * far, +dy * far, far)
            f01.set(+dx * far, -dy * far, far)
            f10.set(-dx * far, +dy * far, far)
            f11.set(-dx * far, -dy * far, far)
            drawRect(entity, n00, n01, n11, n10, guiLineColor)
            drawRect(entity, f00, f01, f11, f10, guiLineColor)
            drawLine(entity, n00, f00, guiLineColor)
            drawLine(entity, n01, f01, guiLineColor)
            drawLine(entity, n10, f10, guiLineColor)
            drawLine(entity, n11, f11, guiLineColor)
            JomlPools.vec3f.sub(8)
        } else {
            val sy = fovOrthographic / 2.0
            val sx = sy * aspect
            DrawAABB.drawAABB(
                transform?.getDrawMatrix(),
                JomlPools.aabbd.create()
                    .setMin(-sx, -sy, -far)
                    .setMax(+sx, +sy, -near),
                RenderState.worldScale,
                guiLineColor
            )
            JomlPools.aabbd.sub(1)
        }
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