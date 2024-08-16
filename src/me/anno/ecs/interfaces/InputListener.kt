package me.anno.ecs.interfaces

import me.anno.input.Key

/**
 * the class, that you can use to control your components by mouse/keyboard
 * if you want control in edit mode, use @CustomEditMode
 *
 * mouseEnter and mouseExit are not yet supported
 * */
interface InputListener {

    fun onGotAction(x: Float, y: Float, dx: Float, dy: Float, action: String): Boolean = false

    fun onKeyDown(key: Key): Boolean = false
    fun onKeyUp(key: Key): Boolean = false
    fun onKeyTyped(key: Key): Boolean = false

    fun onMouseClicked(button: Key, long: Boolean): Boolean = false

    fun onMouseMoved(x: Float, y: Float, dx: Float, dy: Float): Boolean = false
    fun onMouseWheel(x: Float, y: Float, dx: Float, dy: Float, byMouse: Boolean): Boolean = false

    fun onCharTyped(codepoint: Int): Boolean = false

    // todo ???, do we do that? we would need to track all input listeners constantly, and raycast a ray once per frame
    // todo this is cool, and really would be nice to have
    // done we need a 2D mode: CanvasComponent.space = Space.CAMERA_SPACE
    // maybe the UI is just 3D meshes (with a different renderer)? :)
    fun onMouseEnter(): Boolean = false
    fun onMouseExit(): Boolean = false

}