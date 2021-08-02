package me.anno.engine.ui.control

import me.anno.config.DefaultConfig.style
import me.anno.ecs.Entity
import me.anno.ecs.components.camera.CameraComponent
import me.anno.engine.ui.ECSTypeLibrary
import me.anno.engine.ui.render.RenderView
import me.anno.gpu.GFX
import me.anno.input.Input
import me.anno.input.MouseButton
import me.anno.ui.base.Panel
import me.anno.utils.Maths
import me.anno.utils.types.Quaternions.toQuaternionDegrees

open class ControlScheme(val camera: CameraComponent, val library: ECSTypeLibrary, val view: RenderView): Panel(style) {

    constructor(view: RenderView): this(view.editorCamera, view.library, view)

    val cameraNode = camera.entity!!

    val selectedEntities get() = library.selection.filterIsInstance<Entity>()
    val selectedTransforms get() = selectedEntities.map { it.transform }

    override fun onKeyDown(x: Float, y: Float, key: Int) {
        super.onKeyDown(x, y, key)
        invalidateDrawing()
    }

    override fun onKeyUp(x: Float, y: Float, key: Int) {
        super.onKeyUp(x, y, key)
        invalidateDrawing()
    }

    override fun onMouseMoved(x: Float, y: Float, dx: Float, dy: Float) {
        if (1 in Input.mouseKeysDown) {
            // right mouse key down -> move the camera
            val speed = -500f / Maths.max(GFX.height, h)
            val rotation = view.rotation
            rotation.x = Maths.clamp(rotation.x + dy * speed, -90.0, 90.0)
            rotation.y += dx * speed
            cameraNode.transform.localRotation = rotation.toQuaternionDegrees()
            invalidateDrawing()
        }
    }

    override fun onMouseWheel(x: Float, y: Float, dx: Float, dy: Float) {
        val factor = Maths.pow(0.5f, (dx + dy) / 16f)
        view.radius *= factor
        val radius = view.radius
        camera.far = radius * 1e100
        camera.near = if (Input.isKeyDown('r')) radius * 1e-2 else radius * 1e-10
        invalidateDrawing()
    }

    override fun onKeyTyped(x: Float, y: Float, key: Int) {
        super.onKeyTyped(x, y, key)
        if (key in '1'.code..'9'.code) {
            view.selectedAttribute = key - '1'.code
        }
    }

    override fun onMouseClicked(x: Float, y: Float, button: MouseButton, long: Boolean) {
        view.resolveClick(x, y) { e, c ->
            // show the entity in the property editor
            // but highlight the specific mesh
            library.select(e ?: c?.entity, e ?: c)
        }
    }

}