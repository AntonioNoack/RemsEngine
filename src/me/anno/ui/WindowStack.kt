package me.anno.ui

import me.anno.engine.EngineBase
import me.anno.gpu.GFX
import me.anno.gpu.OSWindow
import me.anno.input.Input
import me.anno.utils.pooling.JomlPools
import me.anno.utils.structures.lists.Lists.wrap
import me.anno.utils.structures.lists.SimpleList
import me.anno.utils.types.Floats.toIntOr
import org.apache.logging.log4j.LogManager
import org.joml.Matrix4f
import kotlin.math.max

/**
 * Stack of windows within one OS-level window.
 * Could be within a virtual window, too (CanvasComponent)
 *
 * todo when we're on the edge of a non-fullscreen window, allow resizing
 * done draw shadow on the edge of each non-fullscreen window like in Windows
 * */
@Suppress("MemberVisibilityCanBePrivate")
class WindowStack(val osWindow: OSWindow? = null) : SimpleList<Window>() {

    private val windows = ArrayList<Window>()

    override fun get(index: Int): Window = windows[index]
    override val size: Int get() = windows.size

    // typically few elements, so a list
    val inFocus = ArrayList<Panel>()
    val inFocus0 get() = inFocus.firstOrNull()

    /**
     * transforms the on-OS-window cursor position to local coordinates
     * */
    val viewTransform = Matrix4f()

    var mouseX = 0f
        set(value) {
            field = value
            mouseXi = value.toIntOr()
        }
    var mouseY = 0f
        set(value) {
            field = value
            mouseYi = value.toIntOr()
        }

    var mouseXi = 0
        private set
    var mouseYi = 0
        private set

    var mouseDownX = 0f
        private set
    var mouseDownY = 0f
        private set

    val width get() = w1
    val height get() = h1

    fun requestFocus(panel: Panel?, exclusive: Boolean) {
        if (EngineBase.dragged != null) {
            return
        }
        if (panel != null && panel.windowStack.peek() != panel.window) {
            LOGGER.warn("Only panels on the top window can request focus")
            return
        }
        requestFocus(panel.wrap(), exclusive)
    }

    fun requestFocus(panels: Collection<Panel>, exclusive: Boolean) {
        if (EngineBase.dragged != null) return
        val inFocus = inFocus
        if (exclusive) {
            inFocus.clear()
        } else {
            inFocus.removeAll(panels.toSet())
        }
        inFocus.addAll(panels)
    }

    fun push(window: Window): Window {
        windows.add(window)
        return window
    }

    fun push(panel: Panel, isTransparent: Boolean = false): Window {
        return push(Window(panel, isTransparent, this))
    }

    fun push(panel: Panel, isTransparent: Boolean, isFullscreen: Boolean, x: Int, y: Int): Window {
        return push(Window(panel, isTransparent, isFullscreen, this, x, y))
    }

    fun getPanelAt(x: Float, y: Float) = getPanelAt(x.toInt(), y.toInt())
    fun getPanelAt(x: Int, y: Int): Panel? {
        for (i in size - 1 downTo 0) {
            val window = this[i]
            val windowPanel = window.panel
            val panel = windowPanel.getPanelAt(x, y)
            if (panel != null) {
                panel.window = window
                return panel
            } else if (!window.isTransparent && windowPanel.contains(x, y)) {
                windowPanel.window = window
                return windowPanel
            }
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

    /**
     * x0,y0,w0,h0: where the window is rendered to
     * x1,y1,w1,h1: what the new coordinates are
     * */
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
        val rx = (window.mouseX - x0) / w0 * 2f - 1f
        val ry = (window.mouseY - y0) / h0 * 2f - 1f
        viewTransform.transformProject(rx, ry, 0f, tmp)
            .mul(0.5f).add(0.5f)

        mouseX = x1 + tmp.x * w1
        mouseY = y1 + tmp.y * h1

        val rx1 = (Input.mouseDownX - x0) / w0 * 2f - 1f
        val ry1 = (Input.mouseDownY - y0) / h0 * 2f - 1f
        viewTransform.transformProject(rx1, ry1, 0f, tmp)
            .mul(0.5f).add(0.5f)

        mouseDownX = x1 + tmp.x * w1
        mouseDownY = y1 + tmp.y * h1

        JomlPools.vec3f.sub(1)
    }

    fun draw(dx: Int, dy: Int, windowW: Int, windowH: Int) {
        val windowStack = this
        val lastFullscreenIndex = max(windowStack.indexOfLast { it.isFullscreen && !it.isTransparent }, 0)
        for (index in lastFullscreenIndex until windowStack.size) {
            val window = windowStack.getOrNull(index) ?: break
            window.draw(dx, dy, windowW, windowH)
        }
    }

    fun destroy() {
        for (i in indices) {
            windows[i].destroy()
        }
        windows.clear()
    }

    fun clear() = windows.clear()
    fun addAll(list: List<Window>) = windows.addAll(list)
    fun removeAll(condition: (Window) -> Boolean) = windows.removeAll(condition)
    fun removeAt(i: Int) = windows.removeAt(i)
    fun remove(window: Window) = windows.remove(window)

    fun peek() = windows.lastOrNull()
    fun pop() = windows.removeLast()

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
                println("Window:")
                window1.panel.printLayout(1)
            }
        }

        @JvmStatic
        fun createReloadWindow(panel: Panel, transparent: Boolean, fullscreen: Boolean, reload: () -> Unit): Window {
            val window = GFX.someWindow
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