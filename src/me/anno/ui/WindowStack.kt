package me.anno.ui

import me.anno.config.DefaultConfig
import me.anno.gpu.GFX
import me.anno.gpu.OSWindow
import me.anno.input.Input
import me.anno.studio.StudioBase
import me.anno.utils.pooling.JomlPools
import me.anno.utils.structures.lists.Lists.firstOrNull2
import org.apache.logging.log4j.LogManager
import org.joml.Matrix4f
import java.util.*
import kotlin.math.max

/**
 * Stack of windows within one OS-level window.
 * Could be within a virtual window, too (CanvasComponent)
 * */
@Suppress("MemberVisibilityCanBePrivate")
class WindowStack : Stack<Window>() {

    // typically few elements, so a list
    val inFocus = ArrayList<Panel>()
    val inFocus0 get() = inFocus.firstOrNull2()

    /**
     * transforms the on-OS-window cursor position to local coordinates
     * */
    val viewTransform = Matrix4f()

    var mouseX = 0f
        set(value) {
            field = value
            mouseXi = value.toInt()
        }
    var mouseY = 0f
        set(value) {
            field = value
            mouseYi = value.toInt()
        }

    var mouseXi = 0
        private set
    var mouseYi = 0
        private set

    var mouseDownX = 0f
    var mouseDownY = 0f

    val width get() = w1
    val height get() = h1

    fun requestFocus(panel: Panel?, exclusive: Boolean) {
        if (StudioBase.dragged != null) return
        if (panel != null && panel.windowStack.peek() != panel.window) {
            LOGGER.warn("illegal focus request")
        }
        val inFocus = inFocus
        if (exclusive) {
            for (index in inFocus.indices) {
                inFocus[index].invalidateDrawing()
            }
            inFocus.clear()
        }
        if (panel != null && panel !in inFocus) {
            inFocus.add(panel)
            panel.invalidateDrawing()
        }
    }

    fun push(panel: Panel, isTransparent: Boolean = false): Window {
        val window = Window(panel, isTransparent, this)
        push(window)
        return window
    }

    fun push(panel: Panel, isTransparent: Boolean, isFullscreen: Boolean, x: Int, y: Int) {
        push(Window(panel, isTransparent, isFullscreen, this, x, y))
    }

    fun getPanelAndWindowAt(x: Float, y: Float) =
        getPanelAndWindowAt(x.toInt(), y.toInt())

    fun getPanelAndWindowAt(x: Int, y: Int): Pair<Panel, Window>? {
        for (windowIndex in size - 1 downTo 0) {
            val window = this[windowIndex]
            val panel = window.panel.getPanelAt(x, y)
            if (panel != null) return Pair(panel, window)
            else if (!window.isTransparent && window.panel.contains(x, y))
                return Pair(window.panel, window)
        }
        return null
    }

    fun getPanelAt(x: Float, y: Float) = getPanelAt(x.toInt(), y.toInt())
    fun getPanelAt(x: Int, y: Int): Panel? {
        for (i in size - 1 downTo 0) {
            val window = this[i]
            val panel = window.panel.getPanelAt(x, y)
            if (panel != null) return panel
            else if (!window.isTransparent && window.panel.contains(x, y))
                return window.panel
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

    fun updateTransform(window: OSWindow, x: Int, y: Int, w: Int, h: Int) {

        viewTransform.identity()

        mouseX = window.mouseX
        mouseY = window.mouseY
        mouseDownX = Input.mouseDownX
        mouseDownY = Input.mouseDownY

        this.x0 = x
        this.y0 = y
        this.w0 = w
        this.h0 = h
        this.x1 = x
        this.y1 = y
        this.w1 = w
        this.h1 = h
    }

    fun updateTransform(
        window: OSWindow,
        transform: Matrix4f,
        x0: Int, y0: Int, w0: Int, h0: Int,
        x1: Int, y1: Int, w1: Int, h1: Int
    ) {

        viewTransform.set(transform)

        this.x0 = x0
        this.y0 = y0
        this.w0 = w0
        this.h0 = h0
        this.x1 = x1
        this.y1 = y1
        this.w1 = w1
        this.h1 = h1

        updateMousePosition(window)
    }

    fun updateMousePosition(window: OSWindow) {

        val tmp = JomlPools.vec3f.create()

        viewTransform.transformProject(
            (window.mouseX - x0) / w0 * 2f - 1f,
            (window.mouseY - y0) / h0 * 2f - 1f,
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

    fun draw(dx: Int, dy: Int, windowW: Int, windowH: Int, didSomething0: Boolean, forceRedraw: Boolean): Boolean {
        val sparseRedraw = DefaultConfig["ui.sparseRedraw", true]
        var didSomething = didSomething0
        val windowStack = this
        val lastFullscreenIndex = max(windowStack.indexOfLast { it.isFullscreen }, 0)
        for (index in lastFullscreenIndex until windowStack.size) {
            val window = windowStack.getOrNull(index) ?: break
            didSomething = window.draw(dx, dy, windowW, windowH, sparseRedraw, didSomething, forceRedraw)
        }
        return didSomething
    }

    fun destroy() {
        forEach { it.destroy() }
        clear()
    }

    companion object {

        @JvmStatic
        private val LOGGER = LogManager.getLogger(WindowStack::class)

        /**
         * prints the layout for UI debugging
         * */
        @JvmStatic
        fun printLayout() {
            LOGGER.info("Layout:")
            for (window1 in GFX.focusedWindow?.windowStack ?: return) {
                window1.panel.printLayout(1)
            }
        }

        @JvmStatic
        fun createReloadWindow(panel: Panel, transparent: Boolean, fullscreen: Boolean, reload: () -> Unit): Window {
            val window = GFX.someWindow!!
            return object : Window(
                panel, transparent, fullscreen, window.windowStack,
                if (fullscreen) 0 else window.mouseX.toInt(),
                if (fullscreen) 0 else window.mouseY.toInt()
            ) {
                override fun destroy() {
                    reload()
                }
            }
        }
    }
}