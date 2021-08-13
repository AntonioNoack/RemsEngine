package me.anno.engine.ui.control

import me.anno.engine.ui.render.RenderView
import me.anno.input.Input
import me.anno.input.MouseButton
import me.anno.utils.Maths.length
import org.joml.Math
import org.joml.Vector3d
import kotlin.math.max

// todo mode to place it on top of things using mesh bounds

// todo draw the gizmos

// todo shift to activate g/s/r-number control modes for exact scaling? mmh..

class DraggingControls(view: RenderView) : ControlScheme(view) {

    override fun onMouseDown(x: Float, y: Float, button: MouseButton) {
        super.onMouseDown(x, y, button)
    }

    override fun onMouseMoved(x: Float, y: Float, dx: Float, dy: Float) {
        super.onMouseMoved(x, y, dx, dy)
        if (Input.isLeftDown && isSelected) {
            val targets = selectedEntities
            if (targets.isNotEmpty()) {
                // drag the selected object
                // for that transform dx,dy into global space,
                // and then update the local space
                val fovYRadians = view.editorCamera.fov
                val speed = Math.tan(fovYRadians * 0.5) / h // todo include fov in this calculation
                val camTransform = camera.transform!!.globalTransform
                val offset = camTransform.transformDirection(Vector3d(dx * speed, -dy * speed, 0.0))
                val sorted = targets.sortedBy { it.depthInHierarchy }
                for (target in sorted) {// for correct transformation when parent and child are selected together
                    val transform = target.transform
                    val global = transform.globalTransform
                    val distance = max(
                        1e-300, length(
                            camTransform.m30() - global.m30(),
                            camTransform.m31() - global.m31(),
                            camTransform.m32() - global.m32()
                        )
                    )
                    // translate would be in local space,
                    // translateLocal is in global space
                    global.translateLocal(Vector3d(offset).mul(distance))
                }
                for (target in sorted) {
                    val transform = target.transform
                    transform.calculateLocalTransform(target.parentEntity?.transform)
                    transform.teleportUpdate()
                }
            }
        }
    }

}