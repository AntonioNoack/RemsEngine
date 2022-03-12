package me.anno.engine.ui.control

import me.anno.ecs.Entity
import me.anno.ecs.components.ui.UIEvent
import me.anno.ecs.components.ui.UIEventType
import me.anno.engine.ui.control.ControlScheme
import me.anno.engine.ui.render.RenderView
import me.anno.input.MouseButton

class PlayControls(renderer: RenderView) : ControlScheme(renderer) {

    fun callEvent(event: UIEvent) {
        val ecs = view.getWorld() as? Entity ?: return
        ecs.onUIEvent(event)
        invalidateDrawing()
    }

    override fun onMouseDown(x: Float, y: Float, button: MouseButton) {
        callEvent(UIEvent(x, y, true, button, UIEventType.MOUSE_DOWN))
    }

    override fun onMouseUp(x: Float, y: Float, button: MouseButton) {
        callEvent(UIEvent(x, y, true, button, UIEventType.MOUSE_UP))
    }

    override fun onMouseClicked(x: Float, y: Float, button: MouseButton, long: Boolean) {
        callEvent(UIEvent(x, y, 0f, 0f, 0, true, button, long, UIEventType.MOUSE_CLICK))
    }

    override fun onMouseMoved(x: Float, y: Float, dx: Float, dy: Float) {
        callEvent(UIEvent(x, y, dx, dy, 0, true, MouseButton.UNKNOWN, false, UIEventType.MOUSE_MOVE))
    }

    override fun onMouseWheel(x: Float, y: Float, dx: Float, dy: Float, byMouse: Boolean) {
        callEvent(UIEvent(x, y, dx, dy, 0, byMouse, MouseButton.UNKNOWN, false, UIEventType.MOUSE_WHEEL))
    }

    override fun onKeyDown(x: Float, y: Float, key: Int) {
        callEvent(UIEvent(x, y, key, UIEventType.KEY_DOWN))
    }

    override fun onKeyUp(x: Float, y: Float, key: Int) {
        callEvent(UIEvent(x, y, key, UIEventType.KEY_UP))
    }

    override fun onKeyTyped(x: Float, y: Float, key: Int) {
        callEvent(UIEvent(x, y, key, UIEventType.KEY_TYPED))
    }

    override fun onCharTyped(x: Float, y: Float, key: Int) {
        callEvent(UIEvent(x, y, key, UIEventType.CHAR_TYPED))
    }

    override fun onGotAction(x: Float, y: Float, dx: Float, dy: Float, action: String, isContinuous: Boolean): Boolean {
        callEvent(UIEvent(x, y, dx, dy, 0, false, MouseButton.UNKNOWN, false, UIEventType.ACTION, action))
        return true
    }

    override fun checkMovement() {
        // no pre-defined movement
    }

}