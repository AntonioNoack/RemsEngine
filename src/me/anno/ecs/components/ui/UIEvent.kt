package me.anno.ecs.components.ui

import me.anno.ecs.interfaces.ControlReceiver
import me.anno.input.MouseButton

class UIEvent {

    var x = 0f
    var y = 0f
    var dx = 0f
    var dy = 0f

    var key = 0
    var byMouse = false

    var button = MouseButton.LEFT
    var isLong = false

    var type = UIEventType.MOUSE_WHEEL

    fun call(r: ControlReceiver): Boolean {
        return when (type) {
            UIEventType.MOUSE_WHEEL -> r.onMouseWheel(x, y, dx, dy, byMouse)
            UIEventType.MOUSE_MOVE -> r.onMouseMoved(x, y, dx, dy)
            UIEventType.MOUSE_DOWN -> r.onMouseDown(button)
            UIEventType.MOUSE_UP -> r.onMouseUp(button)
            UIEventType.MOUSE_CLICK -> r.onMouseClicked(button, isLong)
            UIEventType.KEY_DOWN -> r.onKeyUp(key)
            UIEventType.KEY_UP -> r.onKeyUp(key)
            UIEventType.KEY_TYPED -> r.onKeyTyped(key)
            UIEventType.CHAR_TYPED -> r.onCharTyped(key)
        }
    }

    enum class UIEventType {
        MOUSE_WHEEL,
        MOUSE_MOVE,
        MOUSE_DOWN,
        MOUSE_UP,
        MOUSE_CLICK,
        KEY_DOWN,
        KEY_UP,
        KEY_TYPED,
        CHAR_TYPED,
    }

}