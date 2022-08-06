package me.anno.ecs.components.ui

import me.anno.ecs.interfaces.ControlReceiver
import me.anno.extensions.events.Event
import me.anno.input.MouseButton
import me.anno.ui.Window

class UIEvent(
    var window: Window?,
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
) : Event() {

    override fun toString(): String {
        return when (type) {
            UIEventType.MOUSE_WHEEL -> "mouse wheel $x $y += $dx $dy ($byMouse)"
            UIEventType.MOUSE_MOVE -> "mouse move $x $y += $dx $dy"
            UIEventType.MOUSE_DOWN -> "mouse down $button"
            UIEventType.MOUSE_UP -> "mouse up $button"
            UIEventType.MOUSE_CLICK ->"mouse click $button, $isLong"
            UIEventType.KEY_DOWN -> "key down $key"
            UIEventType.KEY_UP ->  "key up $key"
            UIEventType.KEY_TYPED ->"key typed $key"
            UIEventType.CHAR_TYPED -> "char typed $key"
            UIEventType.ACTION -> "action \"$action\", $x $y += $dx $dy"
        }
    }

    constructor(window: Window?, x: Float, y: Float, key: Int, type: UIEventType) :
            this(window, x, y, 0f, 0f, key, false, MouseButton.UNKNOWN, false, type)

    // empty constructor for serialisation
    @Suppress("unused")
    constructor() : this(
        null,
        0f, 0f, 0f, 0f, 0, false,
        MouseButton.UNKNOWN, false, UIEventType.MOUSE_WHEEL
    )

    constructor(window: Window?, x: Float, y: Float, byMouse: Boolean, button: MouseButton, type: UIEventType) :
            this(window, x, y, 0f, 0f, 0, byMouse, button, false, type)

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