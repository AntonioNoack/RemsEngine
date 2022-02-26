package me.anno.graph.ui

import me.anno.config.DefaultConfig
import me.anno.config.DefaultStyle.black
import me.anno.gpu.GFX
import me.anno.gpu.drawing.DrawRectangles.drawRect
import me.anno.gpu.drawing.DrawTexts.monospaceFont
import me.anno.graph.Graph
import me.anno.graph.Node
import me.anno.graph.NodeConnector
import me.anno.graph.NodeInput
import me.anno.graph.types.FlowGraph
import me.anno.input.Input
import me.anno.input.MouseButton
import me.anno.language.translation.NameDesc
import me.anno.maths.Maths.clamp
import me.anno.maths.Maths.distance
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
            // todo list of all node options
            // todo groups for them
            // todo reset graph? idk...
            // todo button to save graph (?)
            // todo button to create new sub function (?)
            openMenu(windowStack, listOf(
                MenuOption(NameDesc("New Node")) {

                }
            ))
        }
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

    val centerX get() = x + w / 2
    val centerY get() = y + h / 2

    fun windowToCoordsDirX(wx: Double) = wx / scale
    fun windowToCoordsDirY(wy: Double) = wy / scale

    fun coordsToWindowDirX(cx: Double) = cx * scale
    fun coordsToWindowDirY(cy: Double) = cy * scale

    fun windowToCoordsX(wx: Double) = (wx - centerX) / scale + center.x
    fun windowToCoordsY(wy: Double) = (wy - centerY) / scale + center.y

    fun coordsToWindowX(cx: Double) = (cx - center.x) * scale + centerX
    fun coordsToWindowY(cy: Double) = (cy - center.y) * scale + centerY

    override fun onMouseMoved(x: Float, y: Float, dx: Float, dy: Float) {
        if (Input.isLeftDown) {
            if (dragged == null) {
                // moving around
                center.x -= dx / scale
                center.y -= dy / scale
                invalidateLayout()
            } else {
                // todo if on side, move towards there
                invalidateDrawing()
            }
        }
    }

    override fun onMouseUp(x: Float, y: Float, button: MouseButton) {
        if (dragged != null) {
            dragged = null
            invalidateDrawing()
        }
    }

    var minScale = 0.001
    var maxScale = 2.0

    override fun onMouseWheel(x: Float, y: Float, dx: Float, dy: Float, byMouse: Boolean) {
        val oldX = windowToCoordsX(x.toDouble())
        val oldY = windowToCoordsY(y.toDouble())
        val multiplier = pow(1.05, dy.toDouble())
        scale = clamp(scale * multiplier, minScale, maxScale)
        val newX = windowToCoordsX(x.toDouble())
        val newY = windowToCoordsY(y.toDouble())
        // zoom in on the mouse pointer
        center.x += (oldX - newX)
        center.y += (oldY - newY)
        invalidateLayout()
    }

    override fun onDraw(x0: Int, y0: Int, x1: Int, y1: Int) {
        drawBackground(x0, y0, x1, y1)
        drawGrid(x0, y0, x1, y1)
        drawNodeConnections(x0, y0, x1, y1)
        drawChildren(x0, y0, x1, y1)
    }

    var gridColor = 0x10ffffff

    fun drawGrid(x0: Int, y0: Int, x1: Int, y1: Int) {
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
                val outColor = nodeOutput.color or black
                val px0 = coordsToWindowX(outPosition.x).toFloat()
                val py0 = coordsToWindowY(outPosition.y).toFloat()
                for (nodeInput in nodeOutput.others) {
                    if (nodeInput is NodeInput) {
                        val pos = nodeInput.position
                        val inNode = nodeInput.node
                        val inIndex = max(inNode?.inputs?.indexOf(nodeInput) ?: 0, 0)
                        // val inColor = nodeInput.color or black
                        val px1 = coordsToWindowX(pos.x).toFloat()
                        val py1 = coordsToWindowY(pos.y).toFloat()
                        if (distance(px0, py0, px1, py1) > 1f) {
                            connect(px0, py0, px1, py1, inIndex, outIndex, outColor)
                        }
                    }
                }
            }
        }
        val dragged = dragged
        if (dragged != null) {
            // todo if hovers over a socket, bake it bigger
            // draw dragged on mouse position
            val outPosition = dragged.position
            val px0 = coordsToWindowX(outPosition.x).toFloat()
            val py0 = coordsToWindowY(outPosition.y).toFloat()
            val ws = windowStack
            if (dragged is NodeInput) {
                val inIndex = max(0, dragged.node?.inputs?.indexOf(dragged) ?: 0)
                val outIndex = 0
                connect(ws.mouseX, ws.mouseY, px0, py0, inIndex, outIndex)
            } else {
                val inIndex = 0
                val outIndex = max(0, dragged.node?.outputs?.indexOf(dragged) ?: 0)
                connect(px0, py0, ws.mouseX, ws.mouseY, inIndex, outIndex)
            }
        }
    }

    var lineThickness = max(1, sqrt(GFX.height / 120f).roundToInt())

    open fun connect(x0: Float, y0: Float, x1: Float, y1: Float, inIndex: Int, outIndex: Int, color: Int = -1) {
        val yc = (y0 + y1) * 0.5f
        val d0 = (30f + outIndex * 10f) * scale.toFloat()
        val d1 = (30f + inIndex * 10f) * scale.toFloat()
        // go back distance x, and draw around
        if (x1 - x0 < d0 + d1) {
            // right/left
            drawLine(x0, y0, x0 + d0, y0, color)
            drawLine(x1, y1, x1 - d1, y1, color)
            // up/down
            drawLine(x0 + d0, y0, x0 + d0, yc, color)
            drawLine(x1 - d1, y1, x1 - d1, yc, color)
            // sideways
            drawLine(x0 + d0, yc, x1 - d1, yc, color)
        } else {
            val xc = (x0 + x1) * 0.5f
            // right, down, right
            drawLine(x0, y0, xc, y0, color)
            drawLine(xc, y0, xc, y1, color)
            drawLine(xc, y1, x1, y1, color)
        }
    }

    fun drawLine(x0: Float, y0: Float, x1: Float, y1: Float, color: Int = -1) {
        if (x0 > x1 || y0 > y1) {
            drawLine(min(x0, x1), min(y0, y1), max(x0, x1), max(y0, y1), color)
        } else {
            val lt = lineThickness
            val lt2 = lt / 2
            when {
                x0 == x1 -> drawRect(x0.toInt() - lt2, y0.toInt() - lt2, lt, (y1 - y0).toInt() + lt, color)
                y0 == y1 -> drawRect(x0.toInt() - lt2, y0.toInt() - lt2, (x1 - x0).toInt() + lt, lt, color)
                else -> drawSmoothLine(x0, y0, x1, y1, color, color.a() / 255f)
            }
        }
    }

    override val canDrawOverBorders: Boolean = true

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            TestStudio.testUI {
                val g = FlowGraph.testLocalVariables()
                g.calculateNodePositions()
                GraphPanel(g, DefaultConfig.style)
            }
        }
    }

}