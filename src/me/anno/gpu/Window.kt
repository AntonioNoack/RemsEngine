package me.anno.gpu

import me.anno.gpu.framebuffer.DepthBufferType
import me.anno.gpu.framebuffer.Framebuffer
import me.anno.input.MouseButton
import me.anno.ui.base.Panel
import me.anno.ui.utils.WindowStack
import me.anno.utils.types.Floats.f3
import org.apache.logging.log4j.LogManager
import kotlin.math.min

open class Window(
    val panel: Panel,
    val isFullscreen: Boolean,
    val windowStack: WindowStack,
    var x: Int, var y: Int
) {

    constructor(panel: Panel, windowStack: WindowStack) : this(panel, true, windowStack, 0, 0)
    constructor(panel: Panel, windowStack: WindowStack, x: Int, y: Int) : this(panel, false, windowStack, x, y)

    var canBeClosedByUser = true

    fun cannotClose(): Window {
        canBeClosedByUser = false
        return this
    }

    val needsRedraw = HashSet<Panel>()
    val needsLayout = HashSet<Panel>()

    fun addNeedsRedraw(panel: Panel) {
        if (panel.canBeSeen) {
            needsRedraw.add(panel.getOverlayParent() ?: panel)
        }
    }

    var lastW = -1
    var lastH = -1

    // the graphics may want to draw directly on the panel in 3D, so we need a depth texture
    // we could use multiple samples, but for performance reasons, let's not do that, when it's not explicitly requested
    val buffer = Framebuffer("window-${panel.className}", 1, 1, 1, 1, false, DepthBufferType.TEXTURE)

    init {
        panel.window = this
    }

    fun calculateFullLayout(w: Int, h: Int, isFirstFrame: Boolean) {
        val window = this
        val t0 = System.nanoTime()
        panel.calculateSize(min(w - window.x, w), min(h - window.y, h))
        // panel.applyPlacement(min(w - window.x, w), min(h - window.y, h))
        // if(panel.w > w || panel.h > h) throw RuntimeException("Panel is too large...")
        // panel.applyConstraints()
        val t1 = System.nanoTime()
        panel.place(window.x, window.y, w, h)
        val t2 = System.nanoTime()
        val dt1 = (t1 - t0) * 1e-9f
        val dt2 = (t2 - t1) * 1e-9f
        if (dt1 > 0.01f && !isFirstFrame) LOGGER.warn("Used ${dt1.f3()}s + ${dt2.f3()}s for layout")
    }

    fun setAcceptsClickAway(boolean: Boolean) {
        acceptsClickAway = { boolean }
    }

    var acceptsClickAway = { _: MouseButton -> canBeClosedByUser }

    open fun destroy() {
        buffer.destroy()
    }

    companion object {
        private val LOGGER = LogManager.getLogger(Window::class.java)
    }
}