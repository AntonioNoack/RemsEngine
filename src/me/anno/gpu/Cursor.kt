package me.anno.gpu

import me.anno.input.Input
import org.lwjgl.glfw.GLFW.*

object Cursor {

    var lastCursor = 0L
    // const val default = 0L
    var hResize = 0L
    var vResize = 0L
    var editText = 0L
    var drag = 0L
    var crossHair = 0L

    fun init(){
        hResize = glfwCreateStandardCursor(GLFW_HRESIZE_CURSOR)
        vResize = glfwCreateStandardCursor(GLFW_VRESIZE_CURSOR)
        editText = glfwCreateStandardCursor(GLFW_IBEAM_CURSOR)
        drag = glfwCreateStandardCursor(GLFW_HAND_CURSOR)
        crossHair = glfwCreateStandardCursor(GLFW_CROSSHAIR_CURSOR)
    }

    fun Long.useCursor(){
        if(this == lastCursor) return
        glfwSetCursor(GFX.window, this)
        lastCursor = this
    }

    fun destroy(){
        // crashes
        /*0L.useCursor()
        for(cursor in listOf(
            hResize, vResize, editText, drag, crossHair
        )){
            glfwDestroyCursor(cursor)
        }*/
    }

}