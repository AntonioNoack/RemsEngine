package me.anno.engine.ui.control

import me.anno.ecs.Entity
import me.anno.ecs.components.ui.UIEvent
import me.anno.ecs.components.ui.UIEventType
import me.anno.engine.ui.render.RenderView
import me.anno.input.Key

class PlayControls(renderer: RenderView) : ControlScheme(renderer) {

    fun callEvent(event: UIEvent) {
        val ecs = view.getWorld() as? Entity ?: return
        ecs.onUIEvent(event)
        invalidateDrawing()
    }

    override fun onMouseClicked(x: Float, y: Float, button: Key, long: Boolean) {
        callEvent(UIEvent(window, x, y, 0f, 0f, button, -1, true, long, UIEventType.MOUSE_CLICK))
    }

    override fun onMouseMoved(x: Float, y: Float, dx: Float, dy: Float) {
        callEvent(UIEvent(window, x, y, dx, dy, Key.KEY_UNKNOWN, -1, true, false, UIEventType.MOUSE_MOVE))
    }

    override fun onMouseWheel(x: Float, y: Float, dx: Float, dy: Float, byMouse: Boolean) {
        callEvent(UIEvent(window, x, y, dx, dy, Key.KEY_UNKNOWN, -1, byMouse, false, UIEventType.MOUSE_WHEEL))
    }

    override fun onKeyDown(x: Float, y: Float, key: Key) {
        callEvent(UIEvent(window, x, y, key, UIEventType.KEY_DOWN))
    }

    override fun onKeyUp(x: Float, y: Float, key: Key) {
        callEvent(UIEvent(window, x, y, key, UIEventType.KEY_UP))
    }

    override fun onKeyTyped(x: Float, y: Float, key: Key) {
        callEvent(UIEvent(window, x, y, key, UIEventType.KEY_TYPED))
    }

    override fun onCharTyped(x: Float, y: Float, codepoint: Int) {
        callEvent(UIEvent(window, x, y, 0f, 0f, Key.KEY_UNKNOWN, codepoint, false, false, UIEventType.CHAR_TYPED))
    }

    override fun onGotAction(x: Float, y: Float, dx: Float, dy: Float, action: String, isContinuous: Boolean): Boolean {
        callEvent(UIEvent(window, x, y, dx, dy, Key.KEY_UNKNOWN, -1, false, false, UIEventType.ACTION, action))
        return true
    }

    override fun checkMovement() {
        // no pre-defined movement
    }
}