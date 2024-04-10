package me.anno.gpu

import me.anno.cache.ICacheData
import me.anno.gpu.GFXBase.imageToGLFW
import me.anno.image.Image
import me.anno.utils.pooling.ByteBufferPool
import org.lwjgl.glfw.GLFW.GLFW_ARROW_CURSOR
import org.lwjgl.glfw.GLFW.GLFW_CROSSHAIR_CURSOR
import org.lwjgl.glfw.GLFW.GLFW_HAND_CURSOR
import org.lwjgl.glfw.GLFW.GLFW_HRESIZE_CURSOR
import org.lwjgl.glfw.GLFW.GLFW_IBEAM_CURSOR
import org.lwjgl.glfw.GLFW.GLFW_RESIZE_ALL_CURSOR
import org.lwjgl.glfw.GLFW.GLFW_VRESIZE_CURSOR
import org.lwjgl.glfw.GLFW.glfwCreateCursor
import org.lwjgl.glfw.GLFW.glfwCreateStandardCursor
import org.lwjgl.glfw.GLFW.glfwSetCursor

/**
 * What the cursor looks like, and a few default Cursors
 * */
class Cursor : ICacheData {

    private var image: Image? = null
    private var centerX = 0
    private var centerY = 0
    private var glfwType = -1
    private var pointer = 0L

    constructor(image: Image) : this(image, image.width / 2, image.height / 2)
    constructor(image: Image, centerX: Int, centerY: Int) {
        this.image = image
        this.centerX = centerX
        this.centerY = centerY
    }

    private constructor(glfwType: Int) {
        this.glfwType = glfwType
    }

    private fun create() {
        val image = image
        pointer = if (image != null) {
            val (image1, pixels) = imageToGLFW(image)
            val cursor = glfwCreateCursor(image1, centerX, centerY)
            ByteBufferPool.free(pixels)
            cursor
        } else if (glfwType != 0) {
            glfwCreateStandardCursor(glfwType)
        } else 0L
    }

    fun useCursor(window: OSWindow) {
        if (this.pointer == 0L) create()
        // the cursor is only updating when moving the mouse???
        // bug in the api maybe, how to fix that? -> we can't really fix that
        // -> don't use it as an important feature
        if (this == window.lastCursor) return
        glfwSetCursor(window.pointer, this.pointer)
        window.lastCursor = this
    }

    override fun destroy() {
        // crashes
        // glfwDestroyCursor(pointer)
    }

    @Suppress("unused")
    companion object {
        val default = Cursor(0)
        val resize = Cursor(GLFW_RESIZE_ALL_CURSOR)
        val hResize = Cursor(GLFW_HRESIZE_CURSOR)
        val vResize = Cursor(GLFW_VRESIZE_CURSOR)
        val editText = Cursor(GLFW_IBEAM_CURSOR)
        val hand = Cursor(GLFW_HAND_CURSOR)
        val drag = hand
        val crossHair = Cursor(GLFW_CROSSHAIR_CURSOR)
        val arrow = Cursor(GLFW_ARROW_CURSOR)
    }
}