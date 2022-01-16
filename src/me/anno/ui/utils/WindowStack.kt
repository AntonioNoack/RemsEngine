package me.anno.ui.utils

import me.anno.config.DefaultConfig
import me.anno.gpu.Window
import me.anno.gpu.framebuffer.Framebuffer
import me.anno.input.Input
import me.anno.ui.base.Panel
import me.anno.utils.pooling.JomlPools
import org.joml.Matrix4f
import java.util.*
import kotlin.math.max

class WindowStack : Stack<Window>() {

    /**
     * transforms the on-OS-window cursor position to local coordinates
     * */
    val viewTransform = Matrix4f()

    var mouseX = 0f
    var mouseY = 0f

    var mouseDownX = 0f
    var mouseDownY = 0f

    fun push(panel: Panel): Window {
        val window = Window(panel, this)
        push(window)
        return window
    }

    fun push(panel: Panel, fullscreen: Boolean, x: Int, y: Int) {
        push(Window(panel, fullscreen, this, x, y))
    }

    fun getPanelAndWindowAt(x: Float, y: Float) =
        getPanelAndWindowAt(x.toInt(), y.toInt())

    fun getPanelAndWindowAt(x: Int, y: Int): Pair<Panel, Window>? {
        for (index in size - 1 downTo 0) {
            val root = this[index]
            val panel = root.panel.getPanelAt(x, y)
            if (panel != null) return panel to root
        }
        return null
    }

    fun getPanelAt(x: Float, y: Float) = getPanelAt(x.toInt(), y.toInt())
    fun getPanelAt(x: Int, y: Int): Panel? {
        for (i in size - 1 downTo 0) {
            val root = this[i]
            val panel = root.panel.getPanelAt(x, y)
            if (panel != null) return panel
        }
        return null
    }

    private var x0 = 0
    private var y0 = 0
    private var w0 = 0
    private var h0 = 0
    private var x1 = 0
    private var y1 = 0
    private var w1 = 0
    private var h1 = 0

    fun updateTransform(w: Int, h: Int) {

        viewTransform.identity()

        mouseX = Input.mouseX
        mouseY = Input.mouseY
        mouseDownX = Input.mouseDownX
        mouseDownY = Input.mouseDownY

        this.x0 = 0
        this.y0 = 0
        this.w0 = w
        this.h0 = h
        this.x1 = 0
        this.y1 = 0
        this.w1 = w
        this.h1 = h

    }


    fun updateTransform(transform: Matrix4f, x0: Int, y0: Int, w0: Int, h0: Int, x1: Int, y1: Int, w1: Int, h1: Int) {

        viewTransform.set(transform)

        this.x0 = x0
        this.y0 = y0
        this.w0 = w0
        this.h0 = h0
        this.x1 = x1
        this.y1 = y1
        this.w1 = w1
        this.h1 = h1

        updateMousePosition()

    }


    fun updateMousePosition() {

        val tmp = JomlPools.vec3f.create()

        viewTransform.transformProject(
            (Input.mouseX - x0) / w0 * 2f - 1f,
            (Input.mouseY - y0) / h0 * 2f - 1f,
            0f, tmp
        )
        mouseX = x1 + (tmp.x * .5f + .5f) * w1
        mouseY = y1 + (tmp.y * .5f + .5f) * h1

        viewTransform.transformProject(
            (Input.mouseDownX - x0) / w0 * 2f - 1f,
            (Input.mouseDownY - y0) / h0 * 2f - 1f,
            0f, tmp
        )
        mouseDownX = x1 + (tmp.x * .5f + .5f) * w1
        mouseDownY = y1 + (tmp.y * .5f + .5f) * h1

        JomlPools.vec3f.sub(1)

    }

    fun draw(w: Int, h: Int, didSomething0: Boolean, forceRedraw: Boolean, dstBuffer: Framebuffer?): Boolean {
        val sparseRedraw = DefaultConfig["ui.sparseRedraw", true]
        var didSomething = didSomething0
        val windowStack = this
        val lastFullscreenIndex = max(windowStack.indexOfLast { it.isFullscreen }, 0)
        for (index in lastFullscreenIndex until windowStack.size) {
            didSomething = windowStack[index].draw(w, h, sparseRedraw, didSomething, forceRedraw, dstBuffer)
        }
        return didSomething
    }

    fun destroy() {
        forEach { it.destroy() }
        clear()
    }


}