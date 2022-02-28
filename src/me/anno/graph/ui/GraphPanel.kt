package me.anno.graph.ui

import me.anno.Engine
import me.anno.config.DefaultConfig
import me.anno.config.DefaultStyle.black
import me.anno.gpu.GFX
import me.anno.gpu.drawing.DrawGradients.drawRectGradient
import me.anno.gpu.drawing.DrawRectangles.drawRect
import me.anno.gpu.drawing.DrawTexts.monospaceFont
import me.anno.graph.Graph
import me.anno.graph.Node
import me.anno.graph.NodeConnector
import me.anno.graph.NodeInput
import me.anno.graph.types.FlowGraph
import me.anno.graph.types.NodeLibrary
import me.anno.graph.ui.NodePositionOptimization.calculateNodePositions
import me.anno.input.Input
import me.anno.input.MouseButton
import me.anno.language.translation.NameDesc
import me.anno.maths.Maths.clamp
import me.anno.maths.Maths.distance
import me.anno.maths.Maths.length
import me.anno.maths.Maths.max
import me.anno.maths.Maths.mixARGB
import me.anno.maths.Maths.pow
import me.anno.ui.base.groups.PanelList
import me.anno.ui.base.menu.Menu.openMenu
import me.anno.ui.base.menu.MenuOption
import me.anno.ui.debug.TestStudio
import me.anno.ui.editor.sceneView.Grid.drawSmoothLine
import me.anno.ui.style.Style
import me.anno.utils.Color.a
import org.joml.Vector3d
import kotlin.math.*

open class GraphPanel(var graph: Graph? = null, style: Style) :
    PanelList(null, style) {

    // todo graphics: billboards: light only / override color (decals)
    // todo rendered when point is visible, or always (for nice light camera-bug effects, e.g. stars with many blades)

    // todo reset transform to get back in case you are lost

    // todo add scroll bars, when the content goes over the borders

    var center = Vector3d()
    var target = Vector3d()

    var dragged: NodeConnector? = null

    // large scale = fast movement
    var scale = 1.0
        set(value) {
            if (field != value) {
                field = value
                font = font.withSize(baseTextSize.toFloat())
            }
        }

    // what should the default size of a node be?
    var defaultNodeSize = 250.0
    val baseTextSize get() = 20 * scale

    var font = monospaceFont

    private val nodeToPanel = HashMap<Node, NodePanel>()

    val centerX get() = x + w / 2
    val centerY get() = y + h / 2

    var minScale = 0.001
    var maxScale = 2.0

    var gridColor = 0x10ffffff

    var lineThickness = max(1, sqrt(GFX.height / 120f).roundToInt())
    var lineThicknessBold = max(1, sqrt(GFX.height / 50f).roundToInt())

    var library = NodeLibrary.flowNodes

    private fun ensureChildren() {
        val graph = graph ?: return
        for (node in graph.nodes) {
            nodeToPanel.getOrPut(node) {
                val panel = NodePanel(node, this, style)
                children.add(panel)
                panel
            }
        }
    }

    init {
        addRightClickListener {
            // todo grid snapping
            // todo select multiple nodes using shift
            // todo groups for them
            // todo reset graph? idk...
            // todo button to save graph (?)
            // todo button to create new sub function (?)
            val window = window!!
            val mouseX = window.mouseX
            val mouseY = window.mouseY
            openMenu(windowStack,
                library.nodes.map {
                    MenuOption(NameDesc(it.name)) {
                        // place node at mouse position
                        val node = it.clone()
                        // todo if placed on line, connect left & right sides where types match from top to bottom
                        node.position.set(windowToCoordsX(mouseX.toDouble()), windowToCoordsY(mouseY.toDouble()), 0.0)
                        graph!!.nodes.add(node)
                        invalidateLayout()
                    }
                }
            )
        }
    }

    override fun tickUpdate() {
        super.tickUpdate()
        val dtx = min(Engine.deltaTime * 10f, 1f)
        if (target.distanceSquared(center) > 1e-5) {
            invalidateLayout()
        }
        center.lerp(target, dtx.toDouble())
    }

    override fun calculateSize(w: Int, h: Int) {
        super.calculateSize(w, h)
        ensureChildren()
        for (child in children) {
            val childSize = (scale * defaultNodeSize).toInt()
            child.calculateSize(childSize, childSize)
        }
        minW = w
        minH = h
    }

    override fun setPosition(x: Int, y: Int) {
        super.setPosition(x, y)
        ensureChildren()
        // place all children
        val graph = graph ?: return
        for (node in graph.nodes) {
            val panel = nodeToPanel[node] ?: continue
            val xi = coordsToWindowX(node.position.x).toInt() - panel.w / 2
            val yi = coordsToWindowY(node.position.y).toInt()// - panel.h / 2
            panel.setPosition(xi, yi)
        }
    }

    fun windowToCoordsDirX(wx: Double) = wx / scale
    fun windowToCoordsDirY(wy: Double) = wy / scale

    fun coordsToWindowDirX(cx: Double) = cx * scale
    fun coordsToWindowDirY(cy: Double) = cy * scale

    fun windowToCoordsX(wx: Double) = (wx - centerX) / scale + center.x
    fun windowToCoordsY(wy: Double) = (wy - centerY) / scale + center.y

    fun coordsToWindowX(cx: Double) = (cx - center.x) * scale + centerX
    fun coordsToWindowY(cy: Double) = (cy - center.y) * scale + centerY

    override fun onMouseDown(x: Float, y: Float, button: MouseButton) {
        // if we start dragging from a node, and it isn't yet in focus,
        // quickly solve that by making bringing it into focus
        if (children.none { it.isInFocus && it.contains(x, y) }) {
            val match = children.firstOrNull { it is NodePanel && it.getConnectorAt(x, y) != null }
            if (match != null) {
                match.requestFocus(true)
                match.isInFocus = true
                match.onMouseDown(x, y, button)
            } else super.onMouseDown(x, y, button)
        } else super.onMouseDown(x, y, button)
    }

    override fun onMouseMoved(x: Float, y: Float, dx: Float, dy: Float) {
        if (Input.isLeftDown) {
            if (dragged == null) {
                // moving around
                center.sub(dx / scale, dy / scale, 0.0)
                target.set(center)
                invalidateLayout()
            } else {
                // if on side, move towards there
                moveIfOnEdge(x, y)
            }
        }
    }

    open fun moveIfOnEdge(x: Float, y: Float) {
        val maxSpeed = (w + h) / 6f // ~ 500px / s on FHD
        var dx2 = x - centerX
        var dy2 = y - centerY
        val border = max(w / 10f, 4f)
        val speed = maxSpeed * min(
            max(
                max((this.x + border) - x, x - (this.x + this.w - border)),
                max((this.y + border) - y, y - (this.y + this.h - border))
            ) / border, 1f
        )
        val multiplier = speed * Engine.deltaTime / length(dx2, dy2)
        if (multiplier > 0f) {
            dx2 *= multiplier
            dy2 *= multiplier
            center.add(dx2 / scale, dy2 / scale, 0.0)
            target.set(center)
            invalidateLayout()
        } else {
            invalidateDrawing()
        }
    }

    override fun onMouseUp(x: Float, y: Float, button: MouseButton) {
        if (dragged != null) {
            dragged = null
            invalidateDrawing()
        }
    }

    override fun onMouseWheel(x: Float, y: Float, dx: Float, dy: Float, byMouse: Boolean) {
        val oldX = windowToCoordsX(x.toDouble())
        val oldY = windowToCoordsY(y.toDouble())
        val multiplier = pow(1.05, dy.toDouble())
        scale = clamp(scale * multiplier, minScale, maxScale)
        val newX = windowToCoordsX(x.toDouble())
        val newY = windowToCoordsY(y.toDouble())
        // zoom in on the mouse pointer
        center.add(oldX - newX, oldY - newY, 0.0)
        target.add(oldX - newX, oldY - newY, 0.0)
        invalidateLayout()
    }

    override fun onDraw(x0: Int, y0: Int, x1: Int, y1: Int) {
        drawBackground(x0, y0, x1, y1)
        drawGrid(x0, y0, x1, y1)
        drawNodeConnections(x0, y0, x1, y1)
        drawChildren(x0, y0, x1, y1)
    }

    open fun drawGrid(x0: Int, y0: Int, x1: Int, y1: Int) {
        val gridColor = mixARGB(backgroundColor, gridColor, gridColor.a() / 255f) or black
        // what grid makes sense? power of 2
        // what is a good grid? one stripe every 10-20 px maybe
        val targetStripeDistancePx = 30.0
        val gridSize = toPowerOf2(targetStripeDistancePx / scale)
        val gridX0 = windowToCoordsX(x0.toDouble())
        val gridX1 = windowToCoordsX(x1.toDouble())
        val gridY0 = windowToCoordsY(y0.toDouble())
        val gridY1 = windowToCoordsY(y1.toDouble())
        val i0 = floor(gridX0 / gridSize).toLong()
        val i1 = ceil(gridX1 / gridSize).toLong()
        val j0 = floor(gridY0 / gridSize).toLong()
        val j1 = ceil(gridY1 / gridSize).toLong()
        for (i in i0 until i1) {
            val gridX = i * gridSize
            val windowX = coordsToWindowX(gridX).toInt()
            if (windowX in x0 until x1) drawRect(windowX, y0, 1, y1 - y0, gridColor)
        }
        for (j in j0 until j1) {
            val gridY = j * gridSize
            val windowY = coordsToWindowY(gridY).toInt()
            if (windowY in y0 until y1) drawRect(x0, windowY, x1 - x0, 1, gridColor)
        }
    }

    private fun toPowerOf2(x: Double): Double {
        return pow(2.0, round(log2(x)))
    }

    open fun drawNodeConnections(x0: Int, y0: Int, x1: Int, y1: Int) {
        // todo we could use different styles..
        // todo it would make sense to implement multiple styles, so this could be used in a game in the future as well
        val graph = graph ?: return
        for (srcNode in graph.nodes) {
            for ((outIndex, nodeOutput) in srcNode.outputs?.withIndex() ?: continue) {
                val outPosition = nodeOutput.position
                val outColor = getTypeColor(nodeOutput)
                val px0 = coordsToWindowX(outPosition.x).toFloat()
                val py0 = coordsToWindowY(outPosition.y).toFloat()
                for (nodeInput in nodeOutput.others) {
                    if (nodeInput is NodeInput) {
                        val pos = nodeInput.position
                        val inNode = nodeInput.node
                        val inIndex = max(inNode?.inputs?.indexOf(nodeInput) ?: 0, 0)
                        val inColor = getTypeColor(nodeInput)
                        val px1 = coordsToWindowX(pos.x).toFloat()
                        val py1 = coordsToWindowY(pos.y).toFloat()
                        if (distance(px0, py0, px1, py1) > 1f) {
                            drawNodeConnection(px0, py0, px1, py1, inIndex, outIndex, outColor, inColor, nodeInput.type)
                        }
                    }
                }
            }
        }
        val dragged = dragged
        if (dragged != null) {
            // done if hovers over a socket, bake it bigger
            // todo if hovers over a socket, use its color as end color
            // draw dragged on mouse position
            val outPosition = dragged.position
            val px0 = coordsToWindowX(outPosition.x).toFloat()
            val py0 = coordsToWindowY(outPosition.y).toFloat()
            val ws = windowStack
            val color = getTypeColor(dragged)
            val node = dragged.node
            val type = dragged.type
            if (dragged is NodeInput) {
                val inIndex = max(0, node?.inputs?.indexOf(dragged) ?: 0)
                val outIndex = 0
                drawNodeConnection(ws.mouseX, ws.mouseY, px0, py0, inIndex, outIndex, color, color, type)
            } else {
                val inIndex = 0
                val outIndex = max(0, node?.outputs?.indexOf(dragged) ?: 0)
                drawNodeConnection(px0, py0, ws.mouseX, ws.mouseY, inIndex, outIndex, color, color, type)
            }
        }
    }

    // todo function to draw splines, so we can draw smoother curves easily
    open fun drawNodeConnection(
        x0: Float, y0: Float, x1: Float, y1: Float,
        inIndex: Int, outIndex: Int, c0: Int, c1: Int,
        type: String
    ) {
        val yc = (y0 + y1) * 0.5f
        val d0 = (30f + outIndex * 10f) * scale.toFloat()
        val d1 = (30f + inIndex * 10f) * scale.toFloat()
        // line thickness depending on flow/non-flow
        val lt = if (type == "Flow") lineThicknessBold else lineThickness
        // go back distance x, and draw around
        if (x0 + d0 > x1 - d1) {
            val s0 = abs(d0)
            val s1 = abs(yc - y0)
            val s2 = abs((x0 + d0) - (x1 - d1))
            val s3 = abs(yc - y1)
            val s4 = abs(d1)
            val ss = s0 + s1 + s2 + s3 + s4
            if (ss > 0f) {
                val ci0 = mixARGB(c0, c1, s0 / ss)
                val ci1 = mixARGB(c0, c1, (s0 + s1) / ss)
                val ci2 = mixARGB(c0, c1, (s0 + s1 + s2) / ss)
                val ci3 = mixARGB(c0, c1, (s0 + s1 + s2 + s3) / ss)
                // right
                drawLine(x0, y0, x0 + d0, y0, c0, ci0, lt)
                // up
                drawLine(x0 + d0, y0, x0 + d0, yc, ci0, ci1, lt)
                // sideways
                drawLine(x0 + d0, yc, x1 - d1, yc, ci1, ci2, lt)
                // down
                drawLine(x1 - d1, yc, x1 - d1, y1, ci2, ci3, lt)
                // left
                drawLine(x1, y1, x1 - d1, y1, ci3, c1, lt)
            }
        } else {
            val xc = (x0 + x1) * 0.5f
            // right, down, right
            val s0 = abs(xc - x0)
            val s1 = abs(y0 - y1)
            val s2 = abs(xc - x1)
            val ss = s0 + s1 + s2
            if (ss > 0f) {
                val ci1 = mixARGB(c0, c1, s0 / ss)
                val ci2 = mixARGB(c0, c1, (s0 + s1) / ss)
                drawLine(x0, y0, xc, y0, c0, ci1, lt)
                drawLine(xc, y0, xc, y1, ci1, ci2, lt)
                drawLine(xc, y1, x1, y1, ci2, c1, lt)
            }
        }
    }

    fun drawLine(x0: Float, y0: Float, x1: Float, y1: Float, c0: Int, c1: Int, lt: Int) {
        if ((x0 > x1 && y0 == y1) || (y0 > y1 && x0 == x1)) {// flip
            drawLine(x1, y1, x0, y0, c1, c0, lt)
        } else {
            val lt2 = lt / 2
            when {
                x0 == x1 -> drawRectGradient(
                    x0.toInt() - lt2, y0.toInt() - lt2, lt, (y1 - y0).toInt() + lt,
                    c0, c1, false
                )
                y0 == y1 -> drawRectGradient(
                    x0.toInt() - lt2, y0.toInt() - lt2, (x1 - x0).toInt() + lt, lt,
                    c0, c1, true
                )
                else -> drawSmoothLine(x0, y0, x1, y1, c0, c0.a() / 255f)
            }
        }
    }

    open fun getTypeColor(con: NodeConnector): Int {
        return when (con.type) {
            "Int", "Long" -> 0x1cdeaa
            "Float", "Double" -> 0x9cf841
            "Flow" -> 0xffffff
            "Bool", "Boolean" -> 0xa90505
            "Vector2f", "Vector3f", "Vector4f",
            "Vector2d", "Vector3d", "Vector4d" -> 0xf8c522 // like in UE4
            "Quaternion4f", "Quaternion4d" -> 0x707fb0 // like in UE4
            "Transform", "Matrix4x3f", "Matrix4x3d" -> 0xfc7100
            "String" -> 0xdf199a
            "?" -> 0x819ef3
            else -> 0x00a7f2 // object
        } or black
    }

    override val canDrawOverBorders: Boolean = true

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            TestStudio.testUI {
                val g = FlowGraph.testLocalVariables()
                calculateNodePositions(g.nodes)
                GraphPanel(g, DefaultConfig.style)
            }
        }
    }

}