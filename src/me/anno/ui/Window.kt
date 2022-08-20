package me.anno.ui

import me.anno.config.DefaultConfig
import me.anno.gpu.GFX
import me.anno.gpu.GFXState.renderDefault
import me.anno.gpu.GFXState.renderPurely
import me.anno.gpu.GFXState.useFrame
import me.anno.gpu.drawing.DrawRectangles
import me.anno.gpu.drawing.DrawTextures.drawTexture
import me.anno.gpu.framebuffer.DepthBufferType
import me.anno.gpu.framebuffer.Frame
import me.anno.gpu.framebuffer.Framebuffer
import me.anno.gpu.shader.Renderer
import me.anno.input.Input
import me.anno.input.MouseButton
import me.anno.ui.utils.WindowStack
import me.anno.utils.structures.lists.LimitedList
import me.anno.utils.types.Floats.f3
import org.apache.logging.log4j.LogManager
import kotlin.math.max
import kotlin.math.min

/**
 * a virtual window within one GLFW window
 * */
open class Window(
    panel: Panel,
    var isTransparent: Boolean,
    val isFullscreen: Boolean,
    val windowStack: WindowStack,
    var x: Int, var y: Int
) {

    constructor(panel: Panel, isTransparent: Boolean, windowStack: WindowStack) :
            this(panel, isTransparent, true, windowStack, 0, 0)

    constructor(panel: Panel, isTransparent: Boolean, windowStack: WindowStack, x: Int, y: Int) :
            this(panel, isTransparent, false, windowStack, x, y)

    var backgroundColor = 0

    val mouseX get() = windowStack.mouseX
    val mouseY get() = windowStack.mouseY
    val mouseXi get() = windowStack.mouseXi
    val mouseYi get() = windowStack.mouseYi
    val mouseDownX get() = windowStack.mouseDownX
    val mouseDownY get() = windowStack.mouseDownY

    var isClosingQuestion = false

    var panel: Panel = panel
        set(value) {
            if (field !== value) {
                field = value
                isFirstFrame = true
            }
        }

    var canBeClosedByUser = true

    var isFirstFrame = true

    fun cannotClose(): Window {
        canBeClosedByUser = false
        return this
    }

    // todo optimized way to request redraw-updates: e.g., for blinking cursors only a very small section actually changes
    val needsRedraw = LimitedList<Panel>(16)
    val needsLayout = LimitedList<Panel>(16)

    fun addNeedsRedraw(panel: Panel) {
        if (panel.canBeSeen) {
            needsRedraw.add(panel.getOverlayParent() ?: panel)
        }
    }

    var lastW = -1
    var lastH = -1

    // the graphics may want to draw directly on the panel in 3D, so we need a depth texture
    // we could use multiple samples, but for performance reasons, let's not do that, when it's not explicitly requested
    // to do use buffer without depth, if no component uses it
    val buffer = Framebuffer(
        "window-${panel.className}",
        1, 1, 1, 1,
        false, DepthBufferType.TEXTURE_16
    )

    init {
        panel.window = this
    }

    private fun calculateFullLayout(w: Int, h: Int) {
        val window = this
        val t0 = System.nanoTime()
        panel.calculateSize(min(w - window.x, w), min(h - window.y, h))
        val t1 = System.nanoTime()
        panel.setPosSize(window.x, window.y, w, h)
        val t2 = System.nanoTime()
        val dt1 = (t1 - t0) * 1e-9f
        val dt2 = (t2 - t1) * 1e-9f
        if (dt1 > 0.01f && !isFirstFrame) {
            LOGGER.warn("Used ${dt1.f3()}s + ${dt2.f3()}s for layout")
            isFirstFrame = false
        }
    }

    fun setAcceptsClickAway(boolean: Boolean) {
        acceptsClickAway = { boolean }
    }

    var acceptsClickAway = { _: MouseButton -> canBeClosedByUser }

    open fun destroy() {
        buffer.destroy()
        panel.destroy()
    }

    fun draw(
        w: Int, h: Int,
        sparseRedraw: Boolean,
        didSomething0: Boolean,
        forceRedraw: Boolean
    ): Boolean {

        var didSomething = didSomething0
        val panel = panel

        panel.updateVisibility(mouseX.toInt(), mouseY.toInt())

        if (this == windowStack.peek()) {
            val inFocus = windowStack.inFocus
            for (index in inFocus.indices) {
                val p = inFocus[index]
                if (p.window == this@Window) {
                    p.isInFocus = true
                    var pi: Panel? = p
                    while (pi != null) {
                        pi.isAnyChildInFocus = true
                        pi = pi.parent as? Panel
                    }
                }
            }
        }

        // resolve missing parents...
        // which still happens...
        // panel.findMissingParents()

        panel.forAllVisiblePanels { p -> p.onUpdate() }
        panel.forAllVisiblePanels { p -> p.tick() }

        validateLayouts(w, h, panel)

        if (panel.w > 0 && panel.h > 0) {

            // overlays get missing...
            // this somehow needs to be circumvented...
            when {
                sparseRedraw -> {
                    didSomething = sparseRedraw(panel, didSomething, forceRedraw)
                }
                didSomething || forceRedraw -> {
                    needsRedraw.clear()
                    fullRedraw(w, h, panel)
                    didSomething = true
                }
                // else no buffer needs to be updated
            }
        }
        return didSomething
    }

    fun validateLayouts(w: Int, h: Int, panel: Panel) {
        val needsLayout = needsLayout
        if (lastW != w || lastH != h || panel in needsLayout || needsLayout.isFull()) {
            lastW = w
            lastH = h
            calculateFullLayout(w, h)
            needsRedraw.add(panel)
            needsLayout.clear()
        } else {
            var i = 0
            while (needsLayout.isNotEmpty() && i++ < needsLayout.size) {
                val p = needsLayout.minByOrNull { it.depth }!!
                // recalculate layout
                p.calculateSize(p.lx1 - p.lx0, p.ly1 - p.ly0)
                p.setPosSize(p.lx0, p.ly0, p.lx1 - p.lx0, p.ly1 - p.ly0)
                needsLayout.removeIf { entry ->
                    entry.anyInHierarchy { it == p }
                }
                // needsLayout.removeAll(p.listOfAll.toSet())
                addNeedsRedraw(p)
            }
        }
    }

    private fun fullRedraw(
        w: Int, h: Int,
        panel0: Panel
    ) {

        GFX.loadTexturesSync.clear()
        GFX.loadTexturesSync.push(false)

        if (Input.needsLayoutUpdate(GFX.activeWindow!!)) {
            calculateFullLayout(w, h)
        }

        val w2 = min(panel0.w, w)
        val h2 = min(panel0.h, h)
        useFrame(panel0.x, panel0.y, w2, h2, Renderer.colorRenderer) {
            panel0.canBeSeen = true
            panel0.draw(panel0.x, panel0.y, panel0.x + w2, panel0.y + h2)
        }

    }

    private val wasRedrawn = ArrayList<Panel>()
    private fun sparseRedraw(
        panel0: Panel, didSomething0: Boolean,
        forceRedraw: Boolean
    ): Boolean {

        var didSomething = didSomething0

        val needsRedraw = needsRedraw
        val wasRedrawn = wasRedrawn
        wasRedrawn.clear()

        if (needsRedraw.isNotEmpty()) {

            didSomething = true

            GFX.resetFBStack()
            Frame.reset()

            val buffer = buffer
            GFX.useWindowXY(max(panel0.x, 0), max(panel0.y, 0), buffer) {
                renderDefault {
                    sparseRedraw2(panel0, wasRedrawn)
                }
            }

        }

        if (didSomething || forceRedraw) {
            drawCachedImage(panel0, wasRedrawn)
        }// else no buffer needs to be updated

        return didSomething

    }

    private fun sparseRedraw2(panel0: Panel, wasRedrawn: MutableCollection<Panel>) {

        val x0 = max(panel0.x, 0)
        val y0 = max(panel0.y, 0)
        // we don't need to draw more than is visible
        val x1 = min(panel0.x + panel0.w, windowStack.width)
        val y1 = min(panel0.y + panel0.h, windowStack.height)

        if (x1 > x0 && y1 > y0) {

            if (panel0 in needsRedraw || // if the main panel is here, all needs to be redrawn anyway
                needsRedraw.isFull() || // needs redraw is full = we didn't save everything that needs redrawing
                // if we would need to redraw more pixels than the whole screen, just redraw it, doesn't matter
                needsRedraw.sumOf {
                    if (it != null) max((it.lx1 - it.lx0) * (it.ly1 - it.ly0), 0) else 0
                } >= panel0.w * panel0.h
            ) {

                wasRedrawn += panel0

                GFX.loadTexturesSync.clear()
                GFX.loadTexturesSync.push(false)

                if (buffer.w != x1 - x0 || buffer.h != y1 - y0) {
                    buffer.w = x1 - x0
                    buffer.h = y1 - y0
                    buffer.destroy()
                }

                // todo while the window is being rescaled, reuse the old fb
                useFrame(
                    x0, y0, x1 - x0, y1 - y0,
                    buffer, Renderer.colorRenderer
                ) {
                    buffer.clearColor(backgroundColor)
                    panel0.canBeSeen = true
                    panel0.draw(x0, y0, x1, y1)
                }

            } else {

                while (needsRedraw.isNotEmpty()) {
                    val panel1 = needsRedraw.minByOrNull { it.depth } ?: break
                    val panel = panel1.getOverlayParent() ?: panel1
                    GFX.loadTexturesSync.clear()
                    GFX.loadTexturesSync.push(false)
                    if (panel.canBeSeen) {
                        val x2 = panel.lx0
                        val y2 = panel.ly0
                        val x3 = panel.lx1 - panel.lx0
                        val y3 = panel.ly1 - panel.ly0
                        useFrame(
                            x2, y2, x3, y3, buffer,
                            Renderer.colorRenderer,
                            panel::redraw
                        )
                    }
                    wasRedrawn += panel
                    panel.forAll { child ->
                        if (child is Panel)
                            needsRedraw.remove(child)
                    }
                    // doesn't work somehow...
                    /*needsRedraw.removeIf { pi ->
                        pi === panel || pi.anyInHierarchy { it === panel }
                    }*/
                }

            }

            needsRedraw.clear()

        }
    }

    private fun drawCachedImage(panel: Panel, wasRedrawn: Collection<Panel>) {
        val x0 = max(panel.x, 0)
        val y0 = max(panel.y, 0)
        // we don't need to draw more than is visible
        val x1 = min(panel.x + panel.w, windowStack.width)
        val y1 = min(panel.y + panel.h, windowStack.height)
        val tex = buffer.getTexture0()
        if (isTransparent) {
            renderDefault {
                drawTexture(x0, y1, x1 - x0, y0 - y1, tex, -1, null)
                if (showRedraws) {
                    showRedraws(wasRedrawn)
                }
            }
        } else {
            renderPurely {
                drawTexture(x0, y1, x1 - x0, y0 - y1, tex, -1, null)
            }
            if (showRedraws) {
                renderDefault {
                    showRedraws(wasRedrawn)
                }
            }
        }
    }

    private fun showRedraws(wasRedrawn: Collection<Panel>) {
        for (panel in wasRedrawn) {
            DrawRectangles.drawRect(
                panel.lx0,
                panel.ly0,
                panel.lx1 - panel.lx0,
                panel.ly1 - panel.ly0,
                redrawColor
            )
        }
    }

    companion object {
        private const val redrawColor = 0x33ff0000
        private val LOGGER = LogManager.getLogger(Window::class.java)
        private val showRedraws get() = DefaultConfig["debug.ui.showRedraws", false]
    }
}