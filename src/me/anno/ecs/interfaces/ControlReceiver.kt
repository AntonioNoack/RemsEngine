package me.anno.ecs.interfaces

import me.anno.input.MouseButton

/**
 * the class, that you can use to control your components by mouse/keyboard
 * if you want control in edit mode, use @CustomEditMode
 *
 * mouseEnter and mouseExit are not yet supported
 * */
interface ControlReceiver {

    fun onGotAction(x: Float, y: Float, dx: Float, dy: Float, action: String) = false

    fun onKeyDown(key: Int) = false
    fun onKeyUp(key: Int) = false
    fun onKeyTyped(key: Int) = false

    fun onMouseDown(button: MouseButton) = false
    fun onMouseUp(button: MouseButton) = false
    fun onMouseClicked(button: MouseButton, long: Boolean) = false

    fun onMouseMoved(x: Float, y: Float, dx: Float, dy: Float) = false
    fun onMouseWheel(x: Float, y: Float, dx: Float, dy: Float, byMouse: Boolean) = false

    fun onCharTyped(char: Int) = false

    // todo ???, do we do that? we would need to track all control receivers constantly, and raycast a ray once per frame
    // todo this is cool, and really would be nice to have
    // todo we need a 2D mode
    // todo maybe the UI is just 3D meshes (with a different renderer)? :)
    fun onMouseEnter() = false
    fun onMouseExit() = false

}