package me.anno.graph.ui

import me.anno.gpu.drawing.DrawRectangles.drawBorder
import me.anno.graph.Graph
import me.anno.graph.Node
import me.anno.graph.NodeConnector
import me.anno.graph.NodeInput
import me.anno.graph.types.NodeLibrary
import me.anno.graph.ui.NodePositionOptimization.calculateNodePositions
import me.anno.input.Input
import me.anno.input.Key
import me.anno.io.SaveableArray
import me.anno.io.json.saveable.JsonStringReader
import me.anno.language.translation.NameDesc
import me.anno.maths.Maths.max
import me.anno.engine.EngineBase.Companion.workspace
import me.anno.ui.Panel
import me.anno.ui.Style
import me.anno.ui.base.menu.Menu.openMenu
import me.anno.ui.base.menu.MenuOption
import me.anno.utils.Color.withAlpha
import me.anno.utils.structures.lists.Lists.none2
import org.joml.Vector2f
import kotlin.math.abs
import kotlin.math.min

open class GraphEditor(graph: Graph? = null, style: Style) : GraphPanel(graph, style) {

    var dragged: NodeConnector? = null

    private val graphs = ArrayList<Graph>()
    private val libraries = ArrayList<NodeLibrary>()

    var library = NodeLibrary.flowNodes

    init {
        if (graph != null) libraries.add(library)
    }

    fun push(graph: Graph, library: NodeLibrary) {
        synchronized(this) {
            graphs.add(graph)
            libraries.add(library)
            this.graph = graph
            this.library = library
        }
        invalidateLayout()
    }

    fun pop(): Boolean {
        val ret = synchronized(this) {
            if (graphs.size > 1) {
                graphs.removeAt(graphs.lastIndex)
                libraries.removeAt(libraries.lastIndex)
                this.graph = graphs.last()
                this.library = libraries.last()
                true
            } else false
        }
        if (ret) invalidateLayout()
        return ret
    }

    override fun onEscapeKey(x: Float, y: Float) {
        if (!pop()) super.onEscapeKey(x, y) // else consume event
    }

    // large scale = fast movement
    override var scale = 1.0
        set(value) {
            if (field != value) {
                field = value
                font = font.withSize(baseTextSize.toFloat())
            }
        }

    override fun onCharTyped(x: Float, y: Float, codepoint: Int) {
        val graph = graph
        if (graph != null && codepoint == 'r'.code) {
            calculateNodePositions(graph.nodes)
        } else super.onCharTyped(x, y, codepoint)
    }

    init {
        addRightClickListener {
            // todo snap all selected nodes to the grid
            // todo groups for them
            // todo reset graph? idk...
            // todo button to save graph (?)
            // todo button to create new sub function (?)
            openNewNodeMenu(null, false)
        }
    }

    // todo if node is dragged on a line, connect left & right sides where types match from top to bottom
    // todo -> detect, if a line is being hovered

    fun openNewNodeMenu(type: String?, typeIsInput: Boolean, callback: ((Node) -> Unit)? = null) {
        val window = window ?: return
        val graph = graph ?: return
        val mouseX = window.mouseX
        val mouseY = window.mouseY
        var candidates = library.allNodes
        if (type != null) {
            candidates = if (typeIsInput) {
                candidates.filter { (sample, _) ->
                    sample.outputs.any { graph.canConnectTypeToOtherType(type, it.type) }
                }.sortedBy { (sample, _) ->
                    !sample.outputs.any { type == it.type }
                }
            } else {
                candidates.filter { (sample, _) ->
                    sample.inputs.any { graph.canConnectTypeToOtherType(it.type, type) }
                }.sortedBy { (sample, _) ->
                    !sample.inputs.any { type == it.type }
                }
            }
        }
        openMenu(windowStack,
            candidates.map { (sample, newNodeGenerator) ->
                MenuOption(NameDesc(sample.name, sample.description, "")) {
                    // place node at mouse position
                    val node = newNodeGenerator()
                    node.position.set(windowToCoordsX(mouseX.toDouble()), windowToCoordsY(mouseY.toDouble()), 0.0)
                    graph.add(node)
                    if (callback != null) callback(node)
                    onChange(false)
                    invalidateLayout()
                }
            }
        )
    }

    var selectingStart: Vector2f? = null

    fun overlapsSelection(child: Panel): Boolean {
        val start = selectingStart ?: return false
        val window = window ?: return false
        val x0 = start.x.toInt()
        val y0 = start.y.toInt()
        val x1 = window.mouseXi
        val y1 = window.mouseYi
        return max(x0, x1) >= child.x &&
                max(y0, y1) >= child.y &&
                min(x0, x1) <= child.x + child.width &&
                min(y0, y1) <= child.y + child.height
    }

    override fun onKeyDown(x: Float, y: Float, key: Key) {
        // if we start dragging from a node, and it isn't yet in focus,
        // quickly solve that by making bringing it into focus
        if ((key == Key.BUTTON_LEFT || key == Key.BUTTON_RIGHT) &&
            !isDownOnScrollbarX && !isDownOnScrollbarY &&
            children.none2 { it.isInFocus && it.contains(x, y) }
        ) {
            val match = children.firstOrNull { it is NodePanel && it.getConnectorAt(x, y) != null }
            if (match != null) {
                mapMouseDown(x, y)
                match.requestFocus(true)
                match.isInFocus = true
                match.onKeyDown(x, y, key)
            } else if (key == Key.BUTTON_LEFT && Input.isShiftDown) {
                mapMouseDown(x, y)
                selectingStart = Vector2f(x, y)
            } else super.onKeyDown(x, y, key)
        } else super.onKeyDown(x, y, key)
    }

    override fun onKeyUp(x: Float, y: Float, key: Key) {
        if (key == Key.BUTTON_LEFT || key == Key.BUTTON_RIGHT) {
            val dragged = dragged
            if (dragged != null) {
                val child = getPanelAt(x.toInt(), y.toInt())
                if (child is NodePanel) child.onKeyUp(x, y, key)
                else getNodePanel(dragged.node!!).onKeyUp(x, y, key)
                this.dragged = null
                invalidateDrawing()
            } else if (selectingStart != null && key == Key.BUTTON_LEFT) {
                // select all panels within the border :)
                var first = true
                for (i in children.indices) {
                    val child = children[i]
                    if (child is NodePanel && overlapsSelection(child)) {
                        child.requestFocus(first)
                        first = false
                    }
                }
                invalidateDrawing()
                this.selectingStart = null
            } else super.onKeyUp(x, y, key)
            mapMouseUp()
        } else super.onKeyUp(x, y, key)
    }

    override fun shallMoveMap(): Boolean = Input.isLeftDown && dragged == null && selectingStart == null

    override fun onMouseMoved(x: Float, y: Float, dx: Float, dy: Float) {
        if (selectingStart != null) invalidateDrawing()
        super.onMouseMoved(x, y, dx, dy)
    }

    override fun onDraw(x0: Int, y0: Int, x1: Int, y1: Int) {
        drawBackground(x0, y0, x1, y1)
        drawGrid(x0, y0, x1, y1)
        drawNodeGroups(x0, y0, x1, y1)
        drawNodeConnections(x0, y0, x1, y1)
        drawNodePanels(x0, y0, x1, y1)
        drawSelection(x0, y0, x1, y1)
        drawScrollbars(x0, y0, x1, y1)
    }

    open fun drawSelection(x0: Int, y0: Int, x1: Int, y1: Int) {
        val start = selectingStart
        val window = window
        if (start != null && window != null) {
            val staX = start.x.toInt()
            val staY = start.y.toInt()
            val endX = window.mouseXi
            val endY = window.mouseYi
            drawBorder(
                min(staX, endX), min(staY, endY),
                abs(endX - staX) + 1, abs(endY - staY) + 1,
                gridColor.withAlpha(1f), lineThickness
            )
        }
    }

    override fun drawNodeConnections(x0: Int, y0: Int, x1: Int, y1: Int) {
        // it would make sense to implement multiple styles, so this could be used in a game in the future as well
        // -> split into multiple subroutines, so you can implement your own style :)
        super.drawNodeConnections(x0, y0, x1, y1)
        val graph = graph
        val dragged = dragged
        if (dragged != null) {
            // done if hovers over a socket, bake it bigger
            // draw dragged on mouse position
            val outPosition = dragged.position
            val px0 = coordsToWindowX(outPosition.x).toFloat()
            val py0 = coordsToWindowY(outPosition.y).toFloat()
            val ws = windowStack
            val startColor = getTypeColor(dragged)
            val mx = ws.mouseX
            val my = ws.mouseY
            // if hovers over a socket, use its color as end color
            val endSocket = children.firstNotNullOfOrNull {
                if (it is NodePanel) {
                    val con = it.getConnectorAt(mx, my)
                    if (con != null && graph != null && graph.canConnectTo(dragged, con)) con
                    else null
                } else null
            }
            val endColor = if (endSocket == null) startColor else getTypeColor(endSocket)
            val node = dragged.node
            val type = dragged.type
            if (dragged is NodeInput) {
                val inIndex = max(0, node?.inputs?.indexOf(dragged) ?: 0)
                val outIndex = 0
                drawNodeConnection(mx, my, px0, py0, inIndex, outIndex, endColor, startColor, type)
            } else {
                val inIndex = 0
                val outIndex = max(0, node?.outputs?.indexOf(dragged) ?: 0)
                drawNodeConnection(px0, py0, mx, my, inIndex, outIndex, startColor, endColor, type)
            }
        }
    }

    override fun onPaste(x: Float, y: Float, data: String, type: String) {
        val graph = graph ?: return super.onPaste(x, y, data, type)
        var done = false
        try {
            val data2 = JsonStringReader.read(data, workspace, true).first()
            // add centered at mouse cursor :3
            val center = getCursorPosition(x, y)
            when (data2) {
                is Node -> {
                    // add at mouse cursor
                    data2.position.add(center)
                    graph.add(data2)
                    getNodePanel(data2).requestFocus()
                    onChange(false)
                    done = true
                }
                is SaveableArray -> {
                    val nodes = data2.filterIsInstance<Node>()
                    if (nodes.isNotEmpty()) {
                        for (node in nodes) graph.add(node)
                        for (index in nodes.indices) {
                            val node = nodes[index]
                            node.position.add(center)
                            getNodePanel(node).requestFocus(index == 0)
                        }
                        onChange(false)
                        done = true
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        if (!done) super.onPaste(x, y, data, type)
    }

    override fun canDeleteNode(node: Node) = true

    override val className: String get() = "GraphEditor"
}