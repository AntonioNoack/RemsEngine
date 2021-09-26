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
import me.anno.studio.rems.ui.StudioTreeView
import me.anno.ui.base.Panel
import me.anno.ui.editor.PropertyInspector
import me.anno.ui.editor.TimelinePanel
import me.anno.ui.editor.sceneView.Gizmos
import me.anno.ui.editor.sceneView.ISceneView
import me.anno.utils.maths.Maths
import me.anno.utils.pooling.JomlPools
import me.anno.utils.types.Quaternions.toQuaternionDegrees
import me.anno.utils.types.Vectors.safeNormalize
import org.apache.logging.log4j.LogManager
import org.joml.AABBd
import org.joml.Quaterniond
import org.joml.Vector3d

open class ControlScheme(val camera: CameraComponent, val library: ECSTypeLibrary, val view: RenderView) :
    Panel(style) {

    constructor(view: RenderView) : this(view.editorCamera, view.library, view)

    val cameraNode = camera.entity!!

    val selectedEntities get() = library.selection.filterIsInstance<Entity>()
    val selectedTransforms get() = selectedEntities.map { it.transform }

    val isSelected get() = parent!!.children.any { it.isInFocus }

    private val tmpAABB = AABBd()
    private val hit = RayHit()

    private val velX = Vector3d()
    private val velY = Vector3d()
    private val velZ = Vector3d()
    private val rotQuad = Quaterniond()
    private val velocity = Vector3d()

    override fun onKeyDown(x: Float, y: Float, key: Int) {
        super.onKeyDown(x, y, key)
        invalidateDrawing()
    }

    override fun onKeyUp(x: Float, y: Float, key: Int) {
        super.onKeyUp(x, y, key)
        invalidateDrawing()
    }

    open fun drawGizmos(){

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

    override fun onMouseWheel(x: Float, y: Float, dx: Float, dy: Float, byMouse: Boolean) {
        if (isSelected) {
            val factor = Maths.pow(2f, (dx - dy) / 16f)
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

    fun invalidateInspector(){
        for (window in GFX.windowStack) {
            for (panel in window.panel.listOfVisible) {
                when (panel) {
                    is PropertyInspector -> {
                        panel.invalidate()
                    }
                }
            }
        }
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

    open fun checkMovement() {
        val view = view
        val dt = GFX.deltaTime
        val factor = Maths.clamp(20.0 * dt, 0.0, 1.0)
        val velocity = velocity.mul(1.0 - factor)
        val radius = view.radius
        val s = factor * radius * 1.2
        if (isSelected) {
            if (Input.isKeyDown('a')) velocity.x -= s
            if (Input.isKeyDown('d')) velocity.x += s
            if (Input.isKeyDown('w')) velocity.z -= s
            if (Input.isKeyDown('s')) velocity.z += s
            if (Input.isKeyDown('q')) velocity.y -= s
            if (Input.isKeyDown('e')) velocity.y += s
        }
        val normXZ = !Input.isShiftDown // todo use UI toggle instead
        val rotQuad = view.rotation.toQuaternionDegrees(rotQuad).invert()
        val velX = velX.set(1.0, 0.0, 0.0)
        val velY = velY.set(0.0, 1.0, 0.0)
        val velZ = velZ.set(0.0, 0.0, 1.0)
        rotQuad.transform(velX)
        rotQuad.transform(velZ)
        if (normXZ) {
            velX.y = 0.0
            velZ.y = 0.0
            velX.safeNormalize()
            velZ.safeNormalize()
        } else {
            rotQuad.transform(velY)
        }
        val position = view.position
        /*position.x += velocity.dot(velX.x, velY.x, velZ.x) * dt
        position.y += velocity.dot(velX.y, velY.y, velZ.y) * dt
        position.z += velocity.dot(velX.z, velY.z, velZ.z) * dt*/
        position.x += velocity.dot(velX) * dt
        position.y += velocity.dot(velY) * dt
        position.z += velocity.dot(velZ) * dt
        if (!position.isFinite) {
            LOGGER.warn("Invalid position $position from $velocity * mat($velX, $velY, $velZ)")
            position.set(0.0)
            Thread.sleep(100)
        }
    }

    override fun onDraw(x0: Int, y0: Int, x1: Int, y1: Int) {
        // no background
        // testHits()
        checkMovement()
    }

    companion object {
        private val LOGGER = LogManager.getLogger(ControlScheme::class)
    }

}