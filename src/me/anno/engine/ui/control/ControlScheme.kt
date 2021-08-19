package me.anno.engine.ui.control

import me.anno.config.DefaultConfig.style
import me.anno.ecs.Entity
import me.anno.ecs.components.camera.CameraComponent
import me.anno.engine.debug.DebugLine
import me.anno.engine.debug.DebugPoint
import me.anno.engine.debug.DebugShapes.debugLines
import me.anno.engine.debug.DebugShapes.debugPoints
import me.anno.engine.raycast.RayHit
import me.anno.engine.raycast.Raycast
import me.anno.engine.ui.ECSTypeLibrary
import me.anno.engine.ui.render.RenderView
import me.anno.engine.ui.render.RenderView.Companion.camDirection
import me.anno.engine.ui.render.RenderView.Companion.camPosition
import me.anno.gpu.GFX
import me.anno.input.Input
import me.anno.input.MouseButton
import me.anno.ui.base.Panel
import me.anno.utils.Maths
import org.joml.AABBd
import org.joml.Vector3d

open class ControlScheme(val camera: CameraComponent, val library: ECSTypeLibrary, val view: RenderView) :
    Panel(style) {

    constructor(view: RenderView) : this(view.editorCamera, view.library, view)

    val cameraNode = camera.entity!!

    val selectedEntities get() = library.selection.filterIsInstance<Entity>()
    val selectedTransforms get() = selectedEntities.map { it.transform }

    val isSelected get() = parent!!.children.any { it.isInFocus }

    override fun onKeyDown(x: Float, y: Float, key: Int) {
        super.onKeyDown(x, y, key)
        invalidateDrawing()
    }

    override fun onKeyUp(x: Float, y: Float, key: Int) {
        super.onKeyUp(x, y, key)
        invalidateDrawing()
    }

    override fun onMouseMoved(x: Float, y: Float, dx: Float, dy: Float) {
        if (isSelected && 1 in Input.mouseKeysDown) {
            // right mouse key down -> move the camera
            val speed = -500f / Maths.max(GFX.height, h)
            val rotation = view.rotation
            rotation.x = Maths.clamp(rotation.x + dy * speed, -90.0, 90.0)
            rotation.y += dx * speed
            view.updateTransform()
            invalidateDrawing()
        }
    }

    override fun onMouseWheel(x: Float, y: Float, dx: Float, dy: Float) {
        if (isSelected) {
            val factor = Maths.pow(0.5f, (dx + dy) / 16f)
            view.radius *= factor
            view.updateTransform()
            invalidateDrawing()
        }
    }

    override fun onKeyTyped(x: Float, y: Float, key: Int) {
        if (isSelected) {
            super.onKeyTyped(x, y, key)
            if (key in '1'.code..'9'.code) {
                view.selectedAttribute = key - '1'.code
            }
        }
    }

    override fun onMouseClicked(x: Float, y: Float, button: MouseButton, long: Boolean) {
        val (e, c) = view.resolveClick(x, y)
        // show the entity in the property editor
        // but highlight the specific mesh
        library.select(e ?: c?.entity, e ?: c)
        testHits()
    }

    fun testHits() {
        val world = library.world
        world.validateMasks()
        world.validateAABBs()
        val hit = Raycast.raycast(world, camPosition, camDirection, 1e6, Raycast.TypeMask.BOTH, -1, false, tmpAABB, hit)
        if (hit == null) {
            // draw red point in front of the camera
            debugPoints.add(DebugPoint(Vector3d(camDirection).mul(20.0).add(camPosition), 0xff0000))
        } else {
            // draw collision point
            debugPoints.add(DebugPoint(Vector3d(hit.positionWS), -1))
            // draw collision normal
            debugLines.add(
                DebugLine(
                    Vector3d(hit.positionWS),
                    Vector3d(hit.positionWS).add(hit.normalWS.normalize()),
                    0x00ff00
                )
            )
        }
    }

    private val tmpAABB = AABBd()
    private val hit = RayHit()

    override fun onDraw(x0: Int, y0: Int, x1: Int, y1: Int) {
        // no background
        // testHits()
    }

}