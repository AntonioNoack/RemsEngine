package me.anno.tests.game.simslike

import me.anno.engine.ui.control.DraggingControls
import me.anno.engine.ui.render.RenderView
import me.anno.engine.ui.render.SceneView
import me.anno.input.Input
import me.anno.maths.Maths.mix
import me.anno.utils.types.Floats.toRadians
import kotlin.math.abs
import kotlin.math.sin
import kotlin.math.tan

abstract class SimsControlBase(
    val controls: SimsControls,
    rv: RenderView
) : DraggingControls(rv) {

    val scene get() = controls.scene
    val household get() = controls.household
    val sceneView get() = renderView.uiParent as SceneView

    override fun onMouseMoved(x: Float, y: Float, dx: Float, dy: Float) {
        if (Input.isLeftDown) {
            // move around by dragging
            val xSpeed = -pixelsToWorldFactor * renderView.radius / height
            val ry = rotationTarget.y.toRadians()
            val ySpeed = xSpeed / mix(1.0, abs(sin(ry)), 0.5)
            moveCamera(dx * xSpeed, 0.0, dy * ySpeed)
        } else super.onMouseMoved(x, y, dx, dy)
    }
}