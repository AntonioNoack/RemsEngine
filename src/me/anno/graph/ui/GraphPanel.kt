package me.anno.graph.ui

import me.anno.config.DefaultConfig
import me.anno.config.DefaultStyle.black
import me.anno.gpu.drawing.DrawTexts.drawSimpleTextCharByChar
import me.anno.gpu.drawing.DrawTexts.monospaceFont
import me.anno.graph.Graph
import me.anno.graph.Node
import me.anno.graph.NodeConnector
import me.anno.graph.NodeInput
import me.anno.graph.types.FlowGraph
import me.anno.input.Input
import me.anno.input.MouseButton
import me.anno.language.translation.NameDesc
import me.anno.maths.Maths.distance
import me.anno.maths.Maths.max
import me.anno.maths.Maths.pow
import me.anno.ui.base.groups.PanelList
import me.anno.ui.base.menu.Menu.openMenu
import me.anno.ui.base.menu.MenuOption
import me.anno.ui.debug.TestStudio
import me.anno.ui.editor.sceneView.Grid.drawSmoothLine
import me.anno.ui.style.Style
import me.anno.utils.Color.a
import org.joml.Vector3d

open class GraphPanel(var graph: Graph? = null, style: Style) :
    PanelList(null, style) {

    // todo graphics: billboards: light only / override color
    // todo rendered when point is visible, or always (for nice light camera-bug effects, e.g. stars with many blades)

    // todo allow moving around
    // todo zooming in and out
    // todo show all nodes
    // todo reset transform to get back in case you are lost
    // todo zoom onto a node?

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

    fun windowToCoordsDirX(wx: Double) = wx / scale
    fun windowToCoordsDirY(wy: Double) = wy / scale

    fun windowToCoordsX(wx: Double) = (wx - x - w / 2) / scale + center.x
    fun windowToCoordsY(wy: Double) = (wy - y - h / 2) / scale + center.y

    fun coordsToWindowX(cx: Double) = x + ((cx - center.x) * scale) + w / 2
    fun coordsToWindowY(cy: Double) = y + ((cy - center.y) * scale) + h / 2

    fun coordsToWindowDirX(cx: Double) = cx * scale
    fun coordsToWindowDirY(cy: Double) = cy * scale

    // todo show soft coordinate lines
    // todo powers of two
    /*override fun onDraw(x0: Int, y0: Int, x1: Int, y1: Int) {
        // background
        super.onDraw(x0, y0, x1, y1)

    }*/

    override fun onMouseMoved(x: Float, y: Float, dx: Float, dy: Float) {
        if (Input.isLeftDown) {
            if (dragged == null) {
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
        dragged = null
    }

    override fun onMouseWheel(x: Float, y: Float, dx: Float, dy: Float, byMouse: Boolean) {
        val multiplier = pow(1.05, dy.toDouble())
        scale *= multiplier
        // zoom in on the mouse pointer
        val centerX = this.x + this.w / 2
        val centerY = this.y + this.h / 2
        center.x += (x - centerX) * (multiplier - 1.0)
        center.y += (y - centerY) * (multiplier - 1.0)
        invalidateLayout()
    }

    override fun onDraw(x0: Int, y0: Int, x1: Int, y1: Int) {
        drawBackground(x0, y0, x1, y1)
        drawNodeConnections(x0, y0, x1, y1)
        drawChildren(x0, y0, x1, y1)
        drawSimpleTextCharByChar(x, y, 2, "scale: $scale")
    }

    open fun drawNodeConnections(x0: Int, y0: Int, x1: Int, y1: Int) {
        // todo draw connection lines
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
                        val inPosition = nodeInput.position
                        val inNode = nodeInput.node
                        val inIndex = max(inNode?.inputs?.indexOf(nodeInput) ?: 0, 0)
                        // val inColor = nodeInput.color or black
                        val px1 = coordsToWindowX(inPosition.x).toFloat()
                        val py1 = coordsToWindowY(inPosition.y).toFloat()
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

    open fun connect(x0: Float, y0: Float, x1: Float, y1: Float, inIndex: Int, outIndex: Int, color: Int = -1) {
        val yc = (y0 + y1) * 0.5f
        val d0 = (30f + outIndex * 10f) * scale.toFloat()
        val d1 = (30f + inIndex * 10f) * scale.toFloat()
        // go back distance x, and draw around
        // todo thicker line, maybe 3px on 1080p
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
        // todo thicker line, maybe 3px on 1080p
        drawSmoothLine(x0, y0, x1, y1, x, y, w, h, color, color.a() / 255f)
    }

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