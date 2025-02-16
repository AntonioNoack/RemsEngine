package me.anno.ecs.components.camera

import me.anno.ecs.Component
import me.anno.ecs.annotations.DebugAction
import me.anno.ecs.annotations.Docs
import me.anno.ecs.annotations.Range
import me.anno.ecs.components.player.LocalPlayer.Companion.currentLocalPlayer
import me.anno.ecs.components.player.Player
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.ecs.systems.OnDrawGUI
import me.anno.engine.ui.LineShapes
import me.anno.engine.ui.LineShapes.drawLine
import me.anno.engine.ui.LineShapes.drawRect
import me.anno.engine.ui.render.RenderView
import me.anno.gpu.pipeline.Pipeline
import me.anno.utils.Color.black
import me.anno.utils.pooling.JomlPools
import me.anno.utils.types.Floats.toRadians
import org.joml.AABBd
import org.joml.Matrix4x3m
import org.joml.Vector2f
import org.joml.Vector3d

// like the studio camera,
// a custom state, which stores all related rendering information
class Camera : Component(), OnDrawGUI {

    // todo this is missing from EditorUI... why???
    var isPerspective = true

    @Range(1e-38, 1e35)
    var near = 0.01f

    @Range(1e-35, 1e38)
    var far = 5000.0f

    @Range(0.0, 180.0)
    @Docs("the fov when perspective, in degrees")
    var fovY = 90f

    @Range(0.0, 1e308)
    @Docs("the fov when orthographic, in base units")
    var fovOrthographic = 5f

    @Docs("offset of the center relative to the screen center; in OpenGL coordinates [-1, +1]²")
    var center = Vector2f()

    @DebugAction
    fun use() {
        use(1f)
    }

    // function to blend to the next one
    fun use(blendingTime: Float) {
        val player = currentLocalPlayer!!
        use(player, blendingTime)
    }

    fun use(player: Player, blendingTime: Float = 1f) {
        val state = player.cameraState
        // only if not already set as target
        if (state.currentCamera != this) {
            state.cameraBlendingTime = blendingTime
            state.cameraBlendingProgress = 0f
            state.previousCamera = state.currentCamera
            state.currentCamera = this
        }
    }

    override fun fillSpace(globalTransform: Matrix4x3m, dstUnion: AABBd): Boolean {
        dstUnion.union(globalTransform.getTranslation(Vector3d()))
        return true
    }

    private fun defineRect(
        aspect: Float, size: Double, z: Double,
        n00: Vector3d, n01: Vector3d, n10: Vector3d, n11: Vector3d
    ) {
        val sx = aspect * size
        n00.set(-sx, -size, z)
        n01.set(-sx, +size, z)
        n10.set(+sx, -size, z)
        n11.set(+sx, +size, z)
    }

    override fun onDrawGUI(pipeline: Pipeline, all: Boolean) {
        drawForwardArrows()
        drawCameraLines()
    }

    fun drawForwardArrows() {
        LineShapes.drawArrowZ(entity, 0.0, -near.toDouble())
        LineShapes.drawArrowZ(entity, -near.toDouble(), -far.toDouble())
    }

    fun drawCameraLines() {
        val rv = RenderView.currentInstance
        val aspectRatio = if (rv != null) rv.width.toFloat() / rv.height.toFloat() else 1f

        val color = black or (if (far > near && near > 0.0) 0x77ff77 else 0xff7777)

        val n00 = JomlPools.vec3d.create()
        val n01 = JomlPools.vec3d.create()
        val n10 = JomlPools.vec3d.create()
        val n11 = JomlPools.vec3d.create()
        val f00 = JomlPools.vec3d.create()
        val f01 = JomlPools.vec3d.create()
        val f10 = JomlPools.vec3d.create()
        val f11 = JomlPools.vec3d.create()

        if (isPerspective) {
            val fovYRadians = fovY.toDouble().toRadians()
            defineRect(aspectRatio, near * fovYRadians, -near.toDouble(), n00, n01, n10, n11)
            defineRect(aspectRatio, far * fovYRadians, -far.toDouble(), f00, f01, f10, f11)
        } else {
            val size = fovOrthographic
            defineRect(aspectRatio, size.toDouble(), -near.toDouble(), n00, n01, n10, n11)
            defineRect(aspectRatio, size.toDouble(), -far.toDouble(), f00, f01, f10, f11)
        }

        val entity = entity
        drawRect(entity, n00, n01, n11, n10, color)
        drawRect(entity, f00, f01, f11, f10, color)
        drawLine(entity, n00, f00, color)
        drawLine(entity, n01, f01, color)
        drawLine(entity, n10, f10, color)
        drawLine(entity, n11, f11, color)

        JomlPools.vec3d.sub(8)
    }

    override fun copyInto(dst: PrefabSaveable) {
        super.copyInto(dst)
        if (dst !is Camera) return
        dst.isPerspective = isPerspective
        dst.near = near
        dst.far = far
        dst.fovY = fovY
    }
}