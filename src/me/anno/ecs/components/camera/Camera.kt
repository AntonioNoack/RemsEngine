package me.anno.ecs.components.camera

import me.anno.ecs.Component
import me.anno.ecs.annotations.DebugAction
import me.anno.ecs.annotations.DebugProperty
import me.anno.ecs.annotations.Docs
import me.anno.ecs.annotations.Range
import me.anno.ecs.components.player.LocalPlayer.Companion.currentLocalPlayer
import me.anno.ecs.components.player.Player
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.ecs.components.FillSpace
import me.anno.ecs.systems.OnDrawGUI
import me.anno.engine.ui.LineShapes
import me.anno.engine.ui.LineShapes.drawLine
import me.anno.engine.ui.LineShapes.drawRect
import me.anno.engine.ui.LineShapes.getDrawMatrix
import me.anno.engine.ui.render.RenderView
import me.anno.gpu.pipeline.Pipeline
import me.anno.utils.Color.black
import me.anno.utils.pooling.JomlPools
import me.anno.utils.types.Floats.toRadians
import org.joml.AABBd
import org.joml.Matrix4x3
import org.joml.Vector2f
import org.joml.Vector3d
import kotlin.math.tan

// like the studio camera,
// a custom state, which stores all related rendering information
class Camera : Component(), OnDrawGUI, FillSpace {

    var isPerspective = true

    // todo support near = 0 or even negative for orthographic
    var near = 0.01f
    var far = 5000.0f

    @Range(0.0, 180.0)
    @Docs("the fov when perspective, in degrees")
    var fovYDegrees = 90f

    @Range(0.0, 1e308)
    @Docs("the fov when orthographic, in base units")
    var fovOrthographic = 5f

    @Docs("offset of the center relative to the screen center; in OpenGL coordinates [-1, +1]²")
    var center = Vector2f()

    @DebugProperty
    val previewAspectRatio: Float
        get() {
            val rv = RenderView.currentInstance
            return if (rv != null) rv.width.toFloat() / rv.height.toFloat() else 1f
        }

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

    override fun fillSpace(globalTransform: Matrix4x3, dstUnion: AABBd) {
        dstUnion.union(globalTransform.getTranslation(Vector3d()))
    }

    private fun defineRect(
        aspect: Float, sy: Double, z: Double,
        n00: Vector3d, n01: Vector3d, n10: Vector3d, n11: Vector3d
    ) {
        val sx = aspect * sy
        val x0 = center.x - sx
        val x1 = center.x + sx
        val y0 = center.y - sy
        val y1 = center.y + sy
        n00.set(x0, y0, z)
        n01.set(x0, y1, z)
        n10.set(x1, y0, z)
        n11.set(x1, y1, z)
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

        val aspectRatio = previewAspectRatio
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
            val tanFovY = tan(fovYDegrees.toDouble().toRadians() * 0.5)
            defineRect(aspectRatio, near * tanFovY, -near.toDouble(), n00, n01, n10, n11)
            defineRect(aspectRatio, far * tanFovY, -far.toDouble(), f00, f01, f10, f11)
        } else {
            val size = fovOrthographic * 0.5
            defineRect(aspectRatio, size, -near.toDouble(), n00, n01, n10, n11)
            defineRect(aspectRatio, size, -far.toDouble(), f00, f01, f10, f11)
        }

        val transform = getDrawMatrix(entity)
        drawRect(transform, n00, n01, n11, n10, color)
        drawRect(transform, f00, f01, f11, f10, color)
        drawLine(transform, n00, f00, color)
        drawLine(transform, n01, f01, color)
        drawLine(transform, n10, f10, color)
        drawLine(transform, n11, f11, color)

        JomlPools.vec3d.sub(8)
    }

    override fun copyInto(dst: PrefabSaveable) {
        super.copyInto(dst)
        if (dst !is Camera) return
        dst.isPerspective = isPerspective
        dst.near = near
        dst.far = far
        dst.fovYDegrees = fovYDegrees
    }
}