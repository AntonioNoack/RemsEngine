package me.anno.ecs.components.ui

import me.anno.ecs.interfaces.InputListener
import me.anno.extensions.events.Event
import me.anno.input.Key
import me.anno.ui.Window

// make our UI library use these? -> no, hierarchical handling is best for now
class UIEvent(
    var window: Window?,
    var x: Float,
    var y: Float,
    var dx: Float,
    var dy: Float,
    var key: Key,
    var codepoint: Int,
    var byMouse: Boolean,
    var isLong: Boolean,
    var type: UIEventType,
    var action: String = ""
) : Event() {

    override fun toString(): String {
        return when (type) {
            UIEventType.MOUSE_WHEEL -> "mouse wheel $x $y += $dx $dy ($byMouse)"
            UIEventType.MOUSE_MOVE -> "mouse move $x $y += $dx $dy"
            UIEventType.MOUSE_CLICK ->"mouse click $key, $isLong"
            UIEventType.KEY_DOWN -> "key down $key"
            UIEventType.KEY_UP ->  "key up $key"
            UIEventType.KEY_TYPED ->"key typed $key"
            UIEventType.CHAR_TYPED -> "char typed $key"
            UIEventType.ACTION -> "action \"$action\", $x $y += $dx $dy"
        }
    }

    constructor(window: Window?, x: Float, y: Float, key: Key, type: UIEventType) :
            this(window, x, y, 0f, 0f, key, -1, false, false, type)

    // empty constructor for serialisation
    @Suppress("unused")
    constructor() : this(
        null,
        0f, 0f, 0f, 0f, Key.KEY_UNKNOWN, -1, false,
        false, UIEventType.MOUSE_WHEEL
    )

    constructor(window: Window?, x: Float, y: Float, byMouse: Boolean, button: Key, type: UIEventType) :
            this(window, x, y, 0f, 0f, button, -1, byMouse, false, type)

    fun call(r: InputListener): Boolean {
        return when (type) {
            UIEventType.MOUSE_WHEEL -> r.onMouseWheel(x, y, dx, dy, byMouse)
            UIEventType.MOUSE_MOVE -> r.onMouseMoved(x, y, dx, dy)
            UIEventType.MOUSE_CLICK -> r.onMouseClicked(key, isLong)
            UIEventType.KEY_DOWN -> r.onKeyDown(key)
            UIEventType.KEY_UP -> r.onKeyUp(key)
            UIEventType.KEY_TYPED -> r.onKeyTyped(key)
            UIEventType.CHAR_TYPED -> r.onCharTyped(codepoint)
            UIEventType.ACTION -> r.onGotAction(x, y, dx, dy, action)
        }
    }
}