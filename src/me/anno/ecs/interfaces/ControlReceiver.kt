package me.anno.ecs.interfaces

import javafx.scene.input.MouseButton

/**
 * the class that you can use to control your components by mouse/keyboard
 * if you want control in edit mode, use @CustomEditMode
 * */
interface ControlReceiver {

    fun onKeyDown(key: Int)
    fun onKeyUp(key: Int)

    fun onMouseDown(button: MouseButton)
    fun onMouseUp(button: MouseButton)

    fun onMouseMoved(x: Float, y: Float, dx: Float, dy: Float)

    fun onCharTyped(char: Char)

    // todo ???, do we do that? we would need to track all control receivers constantly, and raycast a ray once per frame
    // todo this is cool, and really would be nice to have
    // todo we need a 2D mode
    // todo maybe the UI is just 3D meshes (with a different renderer)? :)
    fun onMouseEnter()
    fun onMouseExit()

}