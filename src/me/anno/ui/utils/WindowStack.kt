package me.anno.ui.utils

import me.anno.config.DefaultConfig
import me.anno.gpu.GFX
import me.anno.gpu.Window
import me.anno.input.Input
import me.anno.ui.base.Panel
import org.joml.Matrix4f
import org.joml.Vector3f
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

    fun push(panel: Panel) {
        push(Window(panel, this))
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

    fun update(){
        viewTransform.identity()
        mouseX = Input.mouseX
        mouseY = Input.mouseY
        mouseDownX = Input.mouseDownX
        mouseDownY = Input.mouseDownY
    }

    fun update(transform: Matrix4f, x0: Int, y0: Int, w0: Int, h0: Int, x1: Int, y1: Int, w1: Int, h1: Int) {

        viewTransform.set(transform)

        // update mouse position
        val tmp = Vector3f()
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

        // todo where are our touch positions stored? transform them as well

    }

    fun draw(w: Int, h: Int, didSomething0: Boolean, forceRedraw: Boolean): Boolean {
        val sparseRedraw = DefaultConfig["ui.sparseRedraw", true]
        var didSomething = didSomething0
        val windowStack = this
        val lastFullscreenIndex = max(windowStack.indexOfLast { it.isFullscreen }, 0)
        for (index in lastFullscreenIndex until windowStack.size) {
            didSomething = windowStack[index].draw(w, h, sparseRedraw, didSomething, forceRedraw)
        }
        return didSomething
    }

    fun destroy() {
        forEach { it.destroy() }
        clear()
    }


}