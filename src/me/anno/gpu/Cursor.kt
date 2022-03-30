package me.anno.gpu

import org.lwjgl.glfw.GLFW.*

object Cursor {

    // const val default = 0L
    var hResize = 0L
    var vResize = 0L
    var editText = 0L
    var drag = 0L
    val hand get() = drag
    var crossHair = 0L
    var arrow = 0L

    fun init() {
        hResize = glfwCreateStandardCursor(GLFW_HRESIZE_CURSOR)
        vResize = glfwCreateStandardCursor(GLFW_VRESIZE_CURSOR)
        editText = glfwCreateStandardCursor(GLFW_IBEAM_CURSOR)
        drag = glfwCreateStandardCursor(GLFW_HAND_CURSOR)
        crossHair = glfwCreateStandardCursor(GLFW_CROSSHAIR_CURSOR)
        arrow = glfwCreateStandardCursor(GLFW_ARROW_CURSOR)
    }

    fun Long.useCursor(window: WindowX) {
        // the cursor is only updating when moving the mouse???
        // bug in the api maybe, how to fix that? -> we can't really fix that
        // -> don't use it as an important feature
        if (this == window.lastCursor) return
        glfwSetCursor(window.pointer, this)
        window.lastCursor = this
    }

    fun destroy() {
        // crashes
        /*0L.useCursor()
        for(cursor in listOf(
            hResize, vResize, editText, drag, crossHair
        )){
            glfwDestroyCursor(cursor)
        }*/
    }

}