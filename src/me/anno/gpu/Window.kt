package me.anno.gpu

import me.anno.config.DefaultConfig
import me.anno.gpu.drawing.DrawRectangles
import me.anno.gpu.framebuffer.DepthBufferType
import me.anno.gpu.framebuffer.Frame
import me.anno.gpu.framebuffer.Framebuffer
import me.anno.gpu.shader.Renderer
import me.anno.input.Input
import me.anno.input.MouseButton
import me.anno.ui.base.Panel
import me.anno.ui.utils.WindowStack
import me.anno.utils.structures.lists.LimitedList
import me.anno.utils.types.Floats.f3
import org.apache.logging.log4j.LogManager
import org.lwjgl.opengl.GL11
import kotlin.math.min

open class Window(
    panel: Panel,
    val isFullscreen: Boolean,
    val windowStack: WindowStack,
    var x: Int, var y: Int
) {

    constructor(panel: Panel, windowStack: WindowStack) : this(panel, true, windowStack, 0, 0)
    constructor(panel: Panel, windowStack: WindowStack, x: Int, y: Int) : this(panel, false, windowStack, x, y)

    val mouseX get() = windowStack.mouseX
    val mouseY get() = windowStack.mouseY
    val mouseDownX get() = windowStack.mouseDownX
    val mouseDownY get() = windowStack.mouseDownY

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
    val buffer = Framebuffer("window-${panel.className}", 1, 1, 1, 1, false, DepthBufferType.TEXTURE)

    init {
        panel.window = this
    }

    fun calculateFullLayout(w: Int, h: Int) {
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
    }

    fun draw(
        w: Int, h: Int,
        sparseRedraw: Boolean,
        didSomething0: Boolean,
        forceRedraw: Boolean,
        dstBuffer: Framebuffer?
    ): Boolean {

        var didSomething = didSomething0
        val panel = panel

        // panel0.updateVisibility(lastMouseX.toInt(), lastMouseY.toInt())
        panel.updateVisibility(Input.mouseX.toInt(), Input.mouseY.toInt())

        for (p in GFX.inFocus) {
            if (p.window == this) {
                p.isInFocus = true
                var pi: Panel? = p
                while (pi != null) {
                    pi.isAnyChildInFocus = true
                    pi = pi.parent as? Panel
                }
            }
        }

        // resolve missing parents...
        // which still happens...
        panel.findMissingParents()

        panel.forAllVisiblePanels { p -> p.tickUpdate() }
        panel.forAllVisiblePanels { p -> p.tick() }

        validateLayouts(w, h, panel)

        if (panel.w > 0 && panel.h > 0) {

            // overlays get missing...
            // this somehow needs to be circumvented...
            if (sparseRedraw) {
                didSomething = sparseRedraw(panel, didSomething, forceRedraw, dstBuffer)
            } else {
                if (didSomething || forceRedraw) {
                    needsRedraw.clear()
                    fullRedraw(w, h, panel, dstBuffer)
                }// else no buffer needs to be updated
            }
        }
        return didSomething
    }

    private fun validateLayouts(w: Int, h: Int, panel: Panel) {
        val needsLayout = needsLayout
        if (lastW != w || lastH != h || panel in needsLayout || needsLayout.isFull()) {
            lastW = w
            lastH = h
            calculateFullLayout(w, h)
            needsRedraw.add(panel)
            needsLayout.clear()
        } else {
            while (needsLayout.isNotEmpty()) {
                val p = needsLayout.minByOrNull { it.depth }!!
                // recalculate layout
                p.calculateSize(p.lx1 - p.lx0, p.ly1 - p.ly0)
                p.place(p.lx0, p.ly0, p.lx1 - p.lx0, p.ly1 - p.ly0)
                needsLayout.removeAll(p.listOfAll)
                addNeedsRedraw(p)
            }
        }
    }

    private fun fullRedraw(
        w: Int, h: Int,
        panel0: Panel,
        dstBuffer: Framebuffer?
    ) {

        GFX.loadTexturesSync.clear()
        GFX.loadTexturesSync.push(false)
        if (Input.needsLayoutUpdate()) {
            calculateFullLayout(w, h)
        }

        OpenGL.useFrame(panel0.x, panel0.y, panel0.w, panel0.h, false, dstBuffer, Renderer.colorRenderer) {
            panel0.canBeSeen = true
            panel0.draw(panel0.x, panel0.y, panel0.x + panel0.w, panel0.y + panel0.h)
        }

    }

    fun sparseRedraw(
        panel0: Panel, didSomething0: Boolean,
        forceRedraw: Boolean,
        dstBuffer: Framebuffer?
    ): Boolean {

        var didSomething = didSomething0

        val wasRedrawn = ArrayList<Panel>()
        val needsRedraw = needsRedraw

        if (needsRedraw.isNotEmpty()) {

            didSomething = true

            GFX.ensureEmptyStack()
            Frame.reset()

            GFX.deltaX = panel0.x
            GFX.deltaY = panel0.y

            OpenGL.renderDefault {

                val buffer = buffer
                if (panel0 in needsRedraw || needsRedraw.isFull()) {

                    wasRedrawn += panel0

                    GFX.loadTexturesSync.clear()
                    GFX.loadTexturesSync.push(true)

                    OpenGL.useFrame(panel0.x, panel0.y, panel0.w, panel0.h, true, buffer, Renderer.colorRenderer) {
                        Frame.bind()
                        GL11.glClearColor(0f, 0f, 0f, 0f)
                        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT)
                        panel0.canBeSeen = true
                        panel0.draw(panel0.x, panel0.y, panel0.x + panel0.w, panel0.y + panel0.h)
                    }

                } else {

                    while (needsRedraw.isNotEmpty()) {
                        val panel = needsRedraw.minByOrNull { it.depth } ?: break
                        GFX.loadTexturesSync.clear()
                        GFX.loadTexturesSync.push(false)
                        if (panel.canBeSeen) {
                            val w = panel.lx1 - panel.lx0
                            val h = panel.ly1 - panel.ly0
                            OpenGL.useFrame(
                                panel.lx0, panel.ly0, w, h,
                                false, buffer,
                                Renderer.colorRenderer
                            ) { panel.redraw() }
                        }
                        wasRedrawn += panel
                        panel.forAll {
                            needsRedraw.remove(it)
                        }
                    }

                }

                needsRedraw.clear()

            }

            GFX.deltaX = 0
            GFX.deltaY = 0

        }

        if (didSomething || forceRedraw) {
            drawCachedImage(panel0, wasRedrawn, dstBuffer)
        }// else no buffer needs to be updated

        return didSomething

    }

    fun drawCachedImage(panel: Panel, wasRedrawn: Collection<Panel>, dstBuffer: Framebuffer?) {
        OpenGL.useFrame(panel.x, panel.y, panel.w, panel.h, false, dstBuffer) {
            OpenGL.renderDefault {
                GFX.copy(buffer)
                if (showRedraws) {
                    showRedraws(wasRedrawn)
                }
            }
        }
    }

    fun showRedraws(wasRedrawn: Collection<Panel>) {
        for (panel in wasRedrawn) {
            DrawRectangles.drawRect(
                panel.lx0,
                panel.ly0,
                panel.lx1 - panel.lx0,
                panel.ly1 - panel.ly0,
                0x33ff0000
            )
        }
    }

    companion object {
        private val LOGGER = LogManager.getLogger(Window::class.java)
        private val showRedraws get() = DefaultConfig["debug.ui.showRedraws", false]
    }
}