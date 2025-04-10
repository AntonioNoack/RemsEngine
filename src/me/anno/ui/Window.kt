package me.anno.ui

import me.anno.config.DefaultConfig
import me.anno.gpu.Clipping
import me.anno.gpu.GFX
import me.anno.gpu.GFXState
import me.anno.gpu.GFXState.renderDefault
import me.anno.gpu.GFXState.useFrame
import me.anno.gpu.blending.BlendMode
import me.anno.gpu.buffer.SimpleBuffer.Companion.flat01
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
import me.anno.graph.visual.render.effects.framegen.FrameGenInitNode
import me.anno.input.Input
import me.anno.input.Key
import me.anno.ui.base.components.AxisAlignment
import me.anno.utils.Color.a
import me.anno.utils.Color.black
import me.anno.utils.Color.withAlpha
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
                value.window = this
            }
        }

    var canBeClosedByUser = true

    fun cannotClose(): Window {
        canBeClosedByUser = false
        return this
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
        val width = windowW - max(x + dx, dx)
        val height = windowH - max(y + dy, dy)
        panel.calculateSize(width, height)
        val width1 = if (isFullscreen || panel.alignmentX == AxisAlignment.FILL) width else min(width, panel.minW)
        val height1 = if (isFullscreen || panel.alignmentY == AxisAlignment.FILL) height else min(height, panel.minH)
        val px = x + dx + panel.alignmentX.getOffset(width, width1)
        val py = y + dy + panel.alignmentY.getOffset(height, height1)
        panel.setPosSize(px, py, width1, height1)
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

    fun draw(dx: Int, dy: Int, windowW: Int, windowH: Int) {

        update(dx, dy, windowW, windowH)

        val panel = panel
        val visible = panel.width > 0 && panel.height > 0
        if (visible) {

            GFX.resetFBStack()
            Frame.reset()

            useWindowXY(max(panel.x, 0), max(panel.y, 0), buffer) {
                renderDefault {
                    fullRedraw()
                }
            }

            drawCachedImage(panel)

            if (!isFullscreen && !isTransparent) {
                drawWindowShadow()
            }
        }

        if (FrameGenInitNode.isLastFrame()) {
            GFXState.timeRecords.clear()
        }
    }

    fun otherWindowIsOverUs(): Boolean {
        val selfI = max(0, windowStack.indexOf(this))
        for (otherI in selfI + 1 until windowStack.size) {
            val otherWindow = windowStack[otherI]
            if (mouseXi - otherWindow.x in 0 until otherWindow.width &&
                mouseYi - otherWindow.y in 0 until otherWindow.height &&
                otherWindow.panel.contains(mouseXi, mouseYi)
            ) return true
        }
        return false
    }

    fun otherWindowIsFocused(): Boolean {
        val focusedWindow = GFX.focusedWindow
        val ownWindow = windowStack.osWindow ?: GFX.activeWindow
        return focusedWindow != null && focusedWindow != ownWindow
    }

    fun update(dx: Int, dy: Int, windowW: Int, windowH: Int) {

        val panel = panel

        val canBeHovered = !otherWindowIsFocused() && !otherWindowIsOverUs()
        panel.updateVisibility(
            mouseXi, mouseYi, canBeHovered,
            dx, dy, dx + windowW, dy + windowH // correct?
        )

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

        this.width = windowW
        this.height = windowH
        calculateFullLayout(dx, dy, windowW, windowH)
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
        Clipping.clip2(
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

    private fun useWindowXY(x: Int, y: Int, buffer: Framebuffer, process: () -> Unit) {
        val ox = buffer.offsetX
        val oy = buffer.offsetY
        buffer.offsetX = x
        buffer.offsetY = y
        try {
            process()
        } finally {
            buffer.offsetX = ox
            buffer.offsetY = oy
        }
    }

    private fun fullRedraw() {

        val panel = panel
        val x0 = max(panel.x, 0)
        val y0 = max(panel.y, 0)
        // we don't need to draw more than is visible
        val x1 = min(panel.x + panel.width, windowStack.width)
        val y1 = min(panel.y + panel.height, windowStack.height)

        if (x1 <= x0 || y1 <= y0) return

        GFX.loadTexturesSync.clear()
        GFX.loadTexturesSync.push(false)

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
            panel.canBeSeen = true
            panel.draw(x0, y0, x1, y1)
        }
    }

    private fun drawCachedImage(panel: Panel) {
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
                drawTexture(x0, y0, x1 - x0, y1 - y0, tex, -1, null)
            }
        }
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
        if (windowStack.isEmpty()) {
            windowStack.osWindow?.requestClose()
        }
    }

    companion object {
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