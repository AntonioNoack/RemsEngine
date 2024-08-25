package me.anno.ui

import me.anno.Time
import me.anno.config.DefaultConfig
import me.anno.engine.EngineBase
import me.anno.gpu.GFX
import me.anno.gpu.GFXState
import me.anno.gpu.GFXState.renderDefault
import me.anno.gpu.GFXState.useFrame
import me.anno.gpu.blending.BlendMode
import me.anno.gpu.buffer.SimpleBuffer.Companion.flat01
import me.anno.gpu.drawing.DrawRectangles
import me.anno.gpu.drawing.DrawTextures.drawTexture
import me.anno.gpu.drawing.GFXx2D.noTiling
import me.anno.gpu.drawing.GFXx2D.posSize
import me.anno.gpu.framebuffer.DepthBufferType
import me.anno.gpu.framebuffer.Frame
import me.anno.gpu.framebuffer.Framebuffer
import me.anno.gpu.framebuffer.TargetType
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.Shader
import me.anno.gpu.shader.ShaderLib.uiVertexShader
import me.anno.gpu.shader.ShaderLib.uiVertexShaderList
import me.anno.gpu.shader.ShaderLib.uvList
import me.anno.gpu.shader.builder.Variable
import me.anno.gpu.shader.builder.VariableMode
import me.anno.gpu.shader.renderer.Renderer
import me.anno.graph.visual.render.effects.FrameGenInitNode
import me.anno.input.Input
import me.anno.input.Key
import me.anno.ui.base.components.AxisAlignment
import me.anno.utils.Color.a
import me.anno.utils.Color.black
import me.anno.utils.Color.withAlpha
import me.anno.utils.structures.lists.LimitedList
import me.anno.utils.structures.lists.RedrawRequest
import me.anno.utils.types.Floats.f3
import org.apache.logging.log4j.LogManager
import kotlin.math.max
import kotlin.math.min

/**
 * a virtual window within one OS-level window
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

    var isAskingUserAboutClosing = false

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

    private val granularity = 16
    private val needsRedraw = LimitedList<RedrawRequest>(granularity)
    private val needsLayout = LimitedList<Panel>(granularity)
    private val tmpNeeds = LimitedList<Panel>(granularity)
    private val tmpNeeds2 = LimitedList<RedrawRequest>(granularity)

    fun addNeedsRedraw(panel: Panel) {
        addNeedsRedraw(panel, panel.lx0, panel.ly0, panel.lx1, panel.ly1)
    }

    fun addNeedsRedraw(panel: Panel, x0: Int, y0: Int, x1: Int, y1: Int) {
        if (!needsRedraw.isFull) {
            val x2 = max(x0, 0)
            val y2 = max(y0, 0)
            val x3 = min(x1, windowStack.width)
            val y3 = min(y1, windowStack.height)
            if (x3 > x2 && y3 > y2) {
                val drawnPanel = panel.getOverlayParent(x2, y2, x3, y3) ?: panel
                needsRedraw.add(RedrawRequest(drawnPanel, x2, y2, x3, y3))
            }
        }
    }

    fun addNeedsLayout(panel: Panel) {
        needsLayout.add(panel)
    }

    var width = -1
    var height = -1

    // the graphics may want to draw directly on the panel in 3D, so we need a depth texture
    // we could use multiple samples, but for performance reasons, let's not do that, when it's not explicitly requested
    // to do use buffer without depth, if no component uses it
    val buffer = Framebuffer(
        "window-${panel.className}",
        1, 1, 1, TargetType.UInt8x4,
        DepthBufferType.INTERNAL
    )

    /**
     * Option to skip the background buffer, and directly draw;
     * probably slightly more efficient, and usable for most UI solutions;
     *
     * disables caching of drawn components, and always redraws everything
     * */
    var drawDirectly = false

    init {
        panel.window = this
    }

    private fun calculateFullLayout(dx: Int, dy: Int, windowW: Int, windowH: Int) {
        val t0 = Time.nanoTime
        val width = windowW - max(x + dx, dx)
        val height = windowH - max(y + dy, dy)
        panel.calculateSize(width, height)
        val width1 = if (isFullscreen || panel.alignmentX == AxisAlignment.FILL) width else min(width, panel.minW)
        val height1 = if (isFullscreen || panel.alignmentY == AxisAlignment.FILL) height else min(height, panel.minH)
        val px = x + dx + panel.alignmentX.getOffset(width, width1)
        val py = y + dy + panel.alignmentY.getOffset(height, height1)
        val t1 = Time.nanoTime
        panel.setPosSize(px, py, width1, height1)
        val t2 = Time.nanoTime
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

    val closingListeners = ArrayList<() -> Unit>()

    fun addClosingListener(listener: () -> Unit): Window {
        closingListeners.add(listener)
        return this
    }

    /**
     * returns whether the window can be closed, when that button was clicked outside this window's domain;
     * can be used as a listener for this event
     * */
    @Suppress("UNUSED_ANONYMOUS_PARAMETER")
    var acceptsClickAway = { mouseButton: Key -> canBeClosedByUser }

    open fun destroy() {
        buffer.destroy()
        panel.destroy()
        for (listener in closingListeners) {
            listener()
        }
    }

    fun draw(
        dx: Int, dy: Int, windowW: Int, windowH: Int,
        sparseRedraw: Boolean, didSomething0: Boolean, forceRedraw: Boolean
    ): Boolean {

        var didSomething = didSomething0
        val panel = panel

        update(dx, dy, windowW, windowH)

        if (panel.width > 0 && panel.height > 0) {
            when {
                drawDirectly -> {
                    fullRedraw(dx, dy, windowW, windowH, panel)
                    didSomething = true
                }
                sparseRedraw -> {
                    didSomething = sparseRedraw(panel, didSomething, forceRedraw)
                }
                didSomething || forceRedraw -> {
                    fullRedraw(dx, dy, windowW, windowH, panel)
                    didSomething = true
                }
                // else no buffer needs to be updated
            }
            if (didSomething && !isFullscreen && !isTransparent) {
                drawWindowShadow()
                didSomething = true
            }
        }

        if (FrameGenInitNode.isLastFrame()) {
            GFXState.timeRecords.clear()
        }

        return didSomething
    }

    fun otherWindowIsOverUs(): Boolean {
        val idx = max(0, windowStack.indexOf(this))
        return (idx + 1 until windowStack.size).any {
            val otherWindow = windowStack[it]
            mouseXi - otherWindow.x in 0 until otherWindow.width &&
                    mouseYi - otherWindow.y in 0 until otherWindow.height &&
                    otherWindow.panel.contains(mouseXi, mouseYi)
        }
    }

    fun otherWindowIsFocused(): Boolean {
        val focusedWindow = GFX.focusedWindow
        val ownWindow = windowStack.osWindow ?: GFX.activeWindow
        return focusedWindow != null && focusedWindow != ownWindow
    }

    fun update(dx: Int, dy: Int, windowW: Int, windowH: Int) {

        val panel = panel

        val canBeHovered = !otherWindowIsFocused() && !otherWindowIsOverUs()
        panel.updateVisibility(mouseXi, mouseYi, canBeHovered)

        fun markInFocus(p: Panel) {
            p.isInFocus = true
            var pi: Panel? = p
            while (pi != null) {
                pi.isAnyChildInFocus = true
                pi = pi.parent as? Panel
            }
        }

        val mlp = Input.mouseLockPanel
        if (mlp != null && mlp.window == this) {
            markInFocus(mlp)
        }

        if (this == windowStack.peek()) {
            val inFocus = windowStack.inFocus
            for (index in inFocus.indices) {
                val p = inFocus[index]
                if (p.window == this@Window) {
                    markInFocus(p)
                }
            }
        }

        // resolve missing parents...
        // which still happens...
        // panel.findMissingParents()

        panel.forAllVisiblePanels(Panel::onUpdate)
        panel.forAllVisiblePanels(Panel::tick)

        validateLayouts(dx, dy, windowW, windowH, panel)
    }

    fun drawWindowShadow() {

        val panel = panel
        val radius = DefaultConfig["ui.window.shadowRadius", 12]
        val color = DefaultConfig["ui.window.shadowColor", black.withAlpha(30)]
        val w0 = panel.lx1 - panel.lx0
        val h0 = panel.ly1 - panel.ly0
        val x1 = panel.lx0 - radius
        val y1 = panel.ly0 - radius
        val w1 = w0 + 2 * radius
        val h1 = h0 + 2 * radius

        if (radius <= 0 || color.a() == 0)
            return

        val fb = GFXState.currentBuffer
        GFX.clip2(
            max(x1, 0), max(y1, 0),
            min(x1 + w1, fb.width), min(y1 + h1, fb.height)
        ) {
            renderDefault {
                val shader = shadowShader
                shader.use()
                posSize(shader, x1, y1, w1, h1)
                val scale = 0.5f / radius
                shader.v2f("inner", w0 * scale, h0 * scale)
                shader.v2f("outer", w1 * scale, h1 * scale)
                noTiling(shader)
                shader.v4f("color", color)
                flat01.draw(shader)
            }
        }
    }

    fun processNeeds(
        toBeProcessed: LimitedList<Panel>, rootPanel: Panel,
        processAll: () -> Unit, processOne: (Panel) -> Unit
    ) {
        if (rootPanel in toBeProcessed) {
            toBeProcessed.clear()
            processAll()
        } else {
            val needs = tmpNeeds
            needs.clear()
            needs.addAll(toBeProcessed)
            toBeProcessed.clear()
            while (needs.isNotEmpty()) {
                val p = needs.minByOrNull { it.depth }!!
                processOne(p)
                needs.removeIf { entry ->
                    entry === p || entry.anyInHierarchy { it == p }
                }
            }
        }
    }

    fun processNeeds(
        toBeProcessed: LimitedList<RedrawRequest>, rootPanel: RedrawRequest,
        processAll: () -> Unit, processOne: (RedrawRequest) -> Unit
    ) {
        if (rootPanel in toBeProcessed) {
            toBeProcessed.clear()
            processAll()
        } else {
            val needs = tmpNeeds2
            needs.clear()
            needs.addAll(toBeProcessed)
            toBeProcessed.clear()
            while (needs.isNotEmpty()) {
                val p = needs.minByOrNull { it.panel.depth }!!
                processOne(p)
                needs.removeIf { entry -> // remove covered children
                    (entry === p || entry.panel.anyInHierarchy { it == p.panel }) &&
                            p.contains(entry)
                }
            }
        }
    }

    fun validateLayouts(dx: Int, dy: Int, windowW: Int, windowH: Int, panel: Panel) {
        if (this.width != windowW || this.height != windowH) {
            this.width = windowW
            this.height = windowH
            needsLayout.add(panel)
        }
        processNeeds(needsLayout, panel, {
            calculateFullLayout(dx, dy, windowW, windowH)
            needsRedraw.add(fullRR(panel))
        }, { p ->
            p.calculateSize(p.lx1 - p.lx0, p.ly1 - p.ly0)
            p.setPosSize(p.lx0, p.ly0, p.lx1 - p.lx0, p.ly1 - p.ly0)
            addNeedsRedraw(p)
        })
    }

    private fun fullRR(panel: Panel): RedrawRequest {
        val x0 = max(panel.x, 0)
        val y0 = max(panel.y, 0)
        // we don't need to draw more than is visible
        val x1 = min(panel.x + panel.width, windowStack.width)
        val y1 = min(panel.y + panel.height, windowStack.height)
        return RedrawRequest(panel, x0, y0, x1, y1)
    }

    private fun fullRedraw(x: Int, y: Int, w: Int, h: Int, panel0: Panel) {

        needsRedraw.clear()

        GFX.loadTexturesSync.clear()
        GFX.loadTexturesSync.push(false)

        if (Input.needsLayoutUpdate(GFX.activeWindow!!)) {
            calculateFullLayout(x, y, w, h)
        }

        val w2 = min(panel0.width, w - x)
        val h2 = min(panel0.height, h - y)
        panel0.canBeSeen = true
        if (drawDirectly) {
            useFrame(panel0.x, panel0.y, w2, h2, Renderer.colorRenderer) {
                panel0.draw(panel0.x, panel0.y, panel0.x + w2, panel0.y + h2)
                if (EngineBase.showRedraws) {
                    renderDefault {
                        showRedraws(listOf(fullRR(panel0)))
                    }
                }
            }
        } else {
            useFrame(panel0.x, panel0.y, w2, h2, buffer, Renderer.colorRenderer) {
                panel0.draw(panel0.x, panel0.y, panel0.x + w2, panel0.y + h2)
            }
            drawCachedImage(panel0, wasRedrawn)
        }
    }

    private val wasRedrawn = ArrayList<RedrawRequest>()
    private fun sparseRedraw(panel0: Panel, didSomething0: Boolean, forceRedraw: Boolean): Boolean {

        var didSomething = didSomething0

        val needsRedraw = needsRedraw
        val wasRedrawn = wasRedrawn
        wasRedrawn.clear()

        if (needsRedraw.isNotEmpty()) {

            didSomething = true

            GFX.resetFBStack()
            Frame.reset()

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

    private fun sparseRedraw2(panel0: Panel, wasRedrawn: MutableCollection<RedrawRequest>) {

        val x0 = max(panel0.x, 0)
        val y0 = max(panel0.y, 0)
        // we don't need to draw more than is visible
        val x1 = min(panel0.x + panel0.width, windowStack.width)
        val y1 = min(panel0.y + panel0.height, windowStack.height)

        if (x1 > x0 && y1 > y0) {

            val request0 = fullRR(panel0)
            val needsRedraw = needsRedraw
            if (needsRedraw.isFull || needsRedraw.sumOf {
                    if (it != null) max((it.x1 - it.x0) * (it.y1 - it.y0), 0) else 0
                } >= panel0.width * panel0.height) {
                needsRedraw.clear()
                needsRedraw.add(request0)
            }

            processNeeds(needsRedraw, request0, {
                wasRedrawn += request0
                clearLoadTexturesSync()
                if (buffer.width != x1 - x0 || buffer.height != y1 - y0) {
                    buffer.width = x1 - x0
                    buffer.height = y1 - y0
                    buffer.destroy()
                }
                useFrame(
                    x0, y0, x1 - x0, y1 - y0,
                    buffer, Renderer.colorRenderer
                ) {
                    buffer.clearColor(backgroundColor)
                    panel0.canBeSeen = true
                    panel0.draw(x0, y0, x1, y1)
                }
            }, { region ->
                clearLoadTexturesSync()
                calculateXY01(region.panel, x0, y0, x1, y1)
                useFrame(
                    region.x0, region.y0,
                    region.x1 - region.x0,
                    region.y1 - region.y0, buffer,
                    Renderer.colorRenderer
                ) { region.panel.redraw() }
                wasRedrawn += region
            })
        }
    }

    private fun clearLoadTexturesSync() {
        GFX.loadTexturesSync.clear()
        GFX.loadTexturesSync.push(false)
    }

    fun calculateXY01(panel: Panel, x0: Int, y0: Int, x1: Int, y1: Int) {
        val parent = panel.uiParent
        if (parent != null) {
            calculateXY01(parent, x0, y0, x1, y1)
            setXY01(panel, parent.lx0, parent.ly0, parent.lx1, parent.ly1)
        } else {
            setXY01(panel, x0, y0, x1, y1)
        }
    }

    private fun setXY01(panel: Panel, x0: Int, y0: Int, x1: Int, y1: Int) {
        panel.lx0 = max(panel.x, x0)
        panel.ly0 = max(panel.y, y0)
        panel.lx1 = min(panel.x + panel.width, x1)
        panel.ly1 = min(panel.y + panel.height, y1)
    }

    private fun drawCachedImage(panel: Panel, wasRedrawn: List<RedrawRequest>) {
        val x0 = max(panel.x, 0)
        val y0 = max(panel.y, 0)
        // we don't need to draw more than is visible
        val x1 = min(panel.x + panel.width, windowStack.width)
        val y1 = min(panel.y + panel.height, windowStack.height)
        val tex = buffer.getTexture0()
        if (!tex.isCreated()) return // shouldn't happen
        GFXState.depthMode.use(GFXState.alwaysDepthMode) {
            val blendMode = if (isTransparent) BlendMode.DEFAULT else null
            GFXState.blendMode.use(blendMode) {
                drawTexture(x0, y1, x1 - x0, y0 - y1, tex, -1, null)
            }
            if (EngineBase.showRedraws) {
                GFXState.blendMode.use(BlendMode.DEFAULT) {
                    showRedraws(wasRedrawn)
                }
            }
        }
    }

    private fun showRedraws(redrawnPanels: List<RedrawRequest>) {
        val batch = DrawRectangles.startBatch()
        for (i in redrawnPanels.indices) {
            val panel = redrawnPanels[i]
            DrawRectangles.drawRect(
                panel.x0, panel.y0,
                panel.x1 - panel.x0,
                panel.y1 - panel.y0,
                DEBUG_REDRAW_COLOR
            )
        }
        DrawRectangles.finishBatch(batch)
    }

    fun close(closeAllAbove: Boolean = true) {
        val windowStack = windowStack
        val idx = windowStack.indexOf(this)
        if (idx >= 0) {
            if (closeAllAbove) {
                for (i in idx until windowStack.size) {
                    windowStack.pop().destroy()
                }
            } else {
                windowStack.removeAt(idx).destroy()
            }
        }
    }

    companion object {
        private const val DEBUG_REDRAW_COLOR = 0x33ff0000
        private val LOGGER = LogManager.getLogger(Window::class)
        private val shadowShader = Shader(
            "shadow", uiVertexShaderList, uiVertexShader, uvList, listOf(
                Variable(GLSLType.V4F, "color"),
                Variable(GLSLType.V2F, "inner"),
                Variable(GLSLType.V2F, "outer"),
                Variable(GLSLType.V4F, "result", VariableMode.OUT)
            ), "" +
                    "void main(){\n" +
                    "   float dist = length(max(abs((uv*2.0-1.0)*outer)-inner, vec2(0.0)));\n" +
                    "   if(dist <= 0.0 || dist >= 1.0) discard;\n" +
                    "   result = color;\n" +
                    "   float alpha = smoothstep(0.0,1.0,1.0-dist);\n" +
                    "   result.a *= alpha * alpha;\n" +
                    "}\n"
        )
    }
}