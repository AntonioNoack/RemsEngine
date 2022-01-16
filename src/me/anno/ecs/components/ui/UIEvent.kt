package me.anno.ecs.components.ui

import me.anno.ecs.interfaces.ControlReceiver
import me.anno.input.MouseButton

class UIEvent(
    var x: Float,
    var y: Float,
    var dx: Float,
    var dy: Float,
    var key: Int,
    var byMouse: Boolean,
    var button: MouseButton,
    var isLong: Boolean,
    var type: UIEventType,
    var action: String = ""
) {

    constructor(x: Float, y: Float, key: Int, type: UIEventType) :
            this(x, y, 0f, 0f, key, false, MouseButton.UNKNOWN, false, type)

    constructor() : this(
        0f, 0f, 0f, 0f, 0, false,
        MouseButton.UNKNOWN, false, UIEventType.MOUSE_WHEEL
    )

    constructor(x: Float, y: Float, byMouse: Boolean, button: MouseButton, type: UIEventType) :
            this(x, y, 0f, 0f, 0, byMouse, button, false, type)

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
            UIEventType.ACTION -> r.onGotAction(x, y, dx, dy, action)
        }
    }


}