package me.anno.graph.ui

import me.anno.Engine
import me.anno.config.DefaultConfig
import me.anno.engine.ECSRegistry
import me.anno.gpu.GFXBase
import me.anno.gpu.drawing.DrawCurves.drawQuartBezier
import me.anno.gpu.drawing.DrawGradients.drawRectGradient
import me.anno.gpu.drawing.DrawRectangles.drawBorder
import me.anno.gpu.drawing.DrawTexts.monospaceFont
import me.anno.graph.*
import me.anno.graph.types.FlowGraph
import me.anno.graph.types.NodeLibrary
import me.anno.graph.ui.NodePositionOptimization.calculateNodePositions
import me.anno.input.Input
import me.anno.input.MouseButton
import me.anno.io.SaveableArray
import me.anno.io.text.TextReader
import me.anno.language.translation.NameDesc
import me.anno.maths.Maths.distance
import me.anno.maths.Maths.fract
import me.anno.maths.Maths.length
import me.anno.maths.Maths.max
import me.anno.maths.Maths.mixARGB
import me.anno.maths.Maths.pow
import me.anno.studio.StudioBase.Companion.workspace
import me.anno.ui.Panel
import me.anno.ui.base.SpyPanel
import me.anno.ui.base.groups.MapPanel
import me.anno.ui.base.menu.Menu.openMenu
import me.anno.ui.base.menu.MenuOption
import me.anno.ui.debug.TestStudio.Companion.testUI
import me.anno.ui.editor.sceneView.Grid.drawSmoothLine
import me.anno.ui.input.EnumInput
import me.anno.ui.input.FloatInput
import me.anno.ui.input.IntInput
import me.anno.ui.input.TextInput
import me.anno.ui.input.components.Checkbox
import me.anno.ui.style.Style
import me.anno.utils.Color.a
import me.anno.utils.Color.black
import me.anno.utils.Color.white
import me.anno.utils.Color.withAlpha
import me.anno.utils.Warning.unused
import me.anno.utils.structures.maps.Maps.removeIf
import org.apache.logging.log4j.LogManager
import org.joml.Vector2f
import org.joml.Vector3d
import kotlin.math.*

open class GraphEditor(var graph: Graph? = null, style: Style) : MapPanel(style) {

    var dragged: NodeConnector? = null

    // large scale = fast movement
    override var scale = 1.0
        set(value) {
            if (field != value) {
                field = value
                font = font.withSize(baseTextSize.toFloat())
            }
        }

    var cornerRadius = 24f
    val baseTextSize get() = 20 * scale

    var scrollLeft = 0
    var scrollTop = 0
    var scrollRight = 0
    var scrollBottom = 0

    override var scrollPositionX: Double
        get() = scrollLeft.toDouble()
        set(value) {
            unused(value)
        }

    override var scrollPositionY: Double
        get() = scrollTop.toDouble()
        set(value) {
            unused(value)
        }

    override val maxScrollPositionX: Long get() = (scrollLeft + scrollRight).toLong()
    override val maxScrollPositionY: Long get() = (scrollTop + scrollBottom).toLong()
    override val childSizeX: Long get() = w + maxScrollPositionX
    override val childSizeY: Long get() = h + maxScrollPositionY

    var font = monospaceFont

    private val nodeToPanel = HashMap<Node, NodePanel>()

    var gridColor = 0x10ffffff

    var lineThickness = -1
    var lineThicknessBold = -1

    var library = NodeLibrary.flowNodes

    fun getNodePanel(node: Node): NodePanel {
        return nodeToPanel.getOrPut(node) {
            val panel = NodePanel(node, this, style)
            children.add(panel)
            panel.window = window
            invalidateLayout()
            panel
        }
    }

    private fun ensureChildren() {
        val graph = graph ?: return
        for (node in graph.nodes) {
            getNodePanel(node)
        }
        if (nodeToPanel.size > graph.nodes.size) {
            val set = graph.nodes.toSet()
            nodeToPanel.removeIf { it.key !in set }
        }
    }

    override fun onCharTyped(x: Float, y: Float, key: Int) {
        val graph = graph
        if (graph != null && key == 'r'.code) {
            calculateNodePositions(graph.nodes)
        } else super.onCharTyped(x, y, key)
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
                    sample.outputs?.any { graph.canConnectTypeToOtherType(type, it.type) } == true
                }.sortedBy { (sample, _) ->
                    sample.outputs?.any { type == it.type } != true
                }
            } else {
                candidates.filter { (sample, _) ->
                    sample.inputs?.any { graph.canConnectTypeToOtherType(it.type, type) } == true
                }.sortedBy { (sample, _) ->
                    sample.inputs?.any { type == it.type } != true
                }
            }
        }
        openMenu(windowStack,
            candidates.map { (sample, newNodeGenerator) ->
                MenuOption(NameDesc(sample.name)) {
                    // place node at mouse position
                    val node = newNodeGenerator()
                    node.position.set(windowToCoordsX(mouseX.toDouble()), windowToCoordsY(mouseY.toDouble()), 0.0)
                    graph.nodes.add(node)
                    if (callback != null) callback(node)
                    onChange(false)
                    invalidateLayout()
                }
            }
        )
    }

    override fun onUpdate() {
        super.onUpdate()
        if (lineThickness < 0 || lineThicknessBold < 0) {
            val window = window
            if (window != null) {
                if (lineThickness < 0) lineThickness = max(1, sqrt(window.h / 120f).roundToInt())
                if (lineThicknessBold < 0) lineThicknessBold = max(1, sqrt(window.h / 50f).roundToInt())
            }
        }
        val dtx = min(Engine.deltaTime * 10f, 1f)
        if (target.distanceSquared(center) > 1e-5) {
            invalidateLayout()
        }
        center.lerp(target, dtx.toDouble())
    }

    override fun calculateSize(w: Int, h: Int) {
        super.calculateSize(w, h)
        ensureChildren()
        val cornerRadius = (scale * cornerRadius).toFloat()
        for (nodePanel in children) {
            nodePanel.backgroundRadius = cornerRadius
            nodePanel.calculateSize(w, h)
        }
        minW = w
        minH = h
    }

    override fun setPosition(x: Int, y: Int) {
        super.setPosition(x, y)
        ensureChildren()
        // place all children
        val graph = graph ?: return
        var left = 0
        var right = 0
        var top = 0
        var bottom = 0
        val xe = x + w
        val ye = y + h
        for (node in graph.nodes) {
            val panel = nodeToPanel[node] ?: continue
            val xi = coordsToWindowX(node.position.x).toInt() - panel.w / 2
            val yi = coordsToWindowY(node.position.y).toInt()// - panel.h / 2
            panel.setPosition(xi, yi)
            left = max(left, x - xi)
            top = max(top, y - yi)
            right = max(right, (xi + panel.w) - xe)
            bottom = max(bottom, (yi + panel.h) - ye)
        }
        scrollLeft = left
        scrollRight = right
        scrollTop = top
        scrollBottom = bottom
    }

    var selectingStart: Vector2f? = null

    fun overlapsSelection(child: NodePanel): Boolean {
        val start = selectingStart ?: return false
        val window = window ?: return false
        val x0 = start.x.toInt()
        val y0 = start.y.toInt()
        val x1 = window.mouseXi
        val y1 = window.mouseYi
        return max(x0, x1) >= child.x &&
                max(y0, y1) >= child.y &&
                min(x0, x1) <= child.x + child.w &&
                min(y0, y1) <= child.y + child.h
    }

    override fun onMouseDown(x: Float, y: Float, button: MouseButton) {
        // if we start dragging from a node, and it isn't yet in focus,
        // quickly solve that by making bringing it into focus
        mapMouseDown(x, y)
        if (!isDownOnScrollbarX && !isDownOnScrollbarY &&
            children.none { it.isInFocus && it.contains(x, y) }
        ) {
            val match = children.firstOrNull { it is NodePanel && it.getConnectorAt(x, y) != null }
            if (match != null) {
                match.requestFocus(true)
                match.isInFocus = true
                match.onMouseDown(x, y, button)
            } else if (button.isLeft && Input.isShiftDown) {
                selectingStart = Vector2f(x, y)
            } else super.onMouseDown(x, y, button)
        } else super.onMouseDown(x, y, button)
    }

    override fun onMouseUp(x: Float, y: Float, button: MouseButton) {
        val dragged = dragged
        if (dragged != null) {
            val child = getPanelAt(x.toInt(), y.toInt())
            if (child is NodePanel) child.onMouseUp(x, y, button)
            else getNodePanel(dragged.node!!).onMouseUp(x, y, button)
            this.dragged = null
            invalidateDrawing()
        } else if (selectingStart != null && button.isLeft) {
            // select all panels within the border :)
            var first = true
            for (child in children) {
                if (child is NodePanel && overlapsSelection(child)) {
                    child.requestFocus(first)
                    first = false
                }
            }
            invalidateDrawing()
            this.selectingStart = null
        } else super.onMouseUp(x, y, button)
        mapMouseUp()
    }

    override fun shallMoveMap(): Boolean {
        return Input.isLeftDown && dragged == null
    }

    override fun onMouseMoved(x: Float, y: Float, dx: Float, dy: Float) {
        if (selectingStart != null) {
            invalidateDrawing()
        } else if (Input.isLeftDown) {
            if (dragged != null) {
                // if on side, move towards there
                moveIfOnEdge(x, y)
            } else super.onMouseMoved(x, y, dx, dy)
        } else super.onMouseMoved(x, y, dx, dy)
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
            center.add(dx2 / scale, dy2 / scale)
            target.set(center)
            invalidateLayout()
        } else {
            invalidateDrawing()
        }
    }

    override fun onDraw(x0: Int, y0: Int, x1: Int, y1: Int) {
        drawBackground(x0, y0, x1, y1)
        drawGrid(x0, y0, x1, y1)
        drawNodeConnections(x0, y0, x1, y1)
        drawChildren(x0, y0, x1, y1)
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

    open fun drawGrid(x0: Int, y0: Int, x1: Int, y1: Int) {
        val gridColor = mixARGB(backgroundColor, gridColor, gridColor.a() / 255f) or black
        // what grid makes sense? power of 2
        // what is a good grid? one stripe every 10-20 px maybe
        val targetStripeDistancePx = 30.0
        val log = log2(targetStripeDistancePx / scale)
        val fract = fract(log.toFloat())
        val size = pow(2.0, floor(log))
        // draw 2 grids, one fading, the other becoming more opaque
        draw2DLineGrid(x0, y0, x1, y1, gridColor.withAlpha(2f * (1f - fract)), size)
        draw2DLineGrid(x0, y0, x1, y1, gridColor.withAlpha(2f * (1f + fract)), size * 2.0)
    }

    open fun drawNodeConnections(x0: Int, y0: Int, x1: Int, y1: Int) {
        // it would make sense to implement multiple styles, so this could be used in a game in the future as well
        // -> split into multiple subroutines, so you can implement your own style :)
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
                    if (con != null && graph.canConnectTo(dragged, con)) con
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

    open fun drawNodeConnection(
        x0: Float, y0: Float, x1: Float, y1: Float,
        inIndex: Int, outIndex: Int, c0: Int, c1: Int,
        type: String
    ) {
        // if you want other connection styles, just implement them here :)
        drawCurvyNodeConnection(x0, y0, x1, y1, c0, c1, type)
        // e.g., straight connections
        // drawStraightNodeConnection(x0, y0, x1, y1, inIndex, outIndex, c0, c1, type)
    }

    fun drawCurvyNodeConnection(
        x0: Float, y0: Float, x1: Float, y1: Float,
        c0: Int, c1: Int, type: String
    ) {
        val yc = (y0 + y1) * 0.5f
        val d0 = 20f * scale.toFloat() + (abs(y1 - y0) + abs(x1 - x0)) / 8f
        // line thickness depending on flow/non-flow
        val lt = if (type == "Flow") lineThicknessBold else lineThickness
        val xc = (x0 + x1) * 0.5f
        drawQuartBezier(
            x0, y0, c0,
            x0 + d0, y0, mixARGB(c0, c1, 0.333f),
            xc, yc, mixARGB(c0, c1, 0.5f),
            x1 - d0, y1, mixARGB(c0, c1, 0.667f),
            x1, y1, c1,
            lt * 0.5f, 0,
            true, 1.5f
        )
    }

    @Suppress("unused")
    fun drawStraightNodeConnection(
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

    fun getInputField(con: NodeConnector, old: Panel?): Panel? {
        if (!con.isEmpty()) return null
        if (con !is NodeInput) return null
        val type = con.type
        when (type) {
            "Flow" -> return null
            "Float" -> {
                if (old is FloatInput) return old.apply { textSize = font.size }
                return FloatInput(style)
                    .setValue(con.value as? Float ?: 0f, false)
                    .setChangeListener {
                        con.value = it.toFloat()
                        onChange(false)
                    }
                    .setResetListener { con.value = con.defaultValue; con.defaultValue.toString() }
                    .apply { textSize = font.size }
            }
            "Double" -> {
                if (old is FloatInput) return old.apply { textSize = font.size }
                return FloatInput(style)
                    .setValue(con.value as? Double ?: 0.0, false)
                    .setChangeListener {
                        con.value = it
                        onChange(false)
                    }
                    .setResetListener { con.value = con.defaultValue; con.defaultValue.toString() }
                    .apply { textSize = font.size }
            }
            "Int" -> {
                if (old is IntInput) return old.apply { textSize = font.size }
                return IntInput(style)
                    .setValue(con.value as? Int ?: 0, false)
                    .setChangeListener {
                        con.value = it.toInt()
                        onChange(false)
                    }
                    .setResetListener { con.value = con.defaultValue; con.defaultValue.toString() }
                    .apply { textSize = font.size }
            }
            "Long" -> {
                if (old is IntInput) return old.apply { textSize = font.size }
                return IntInput(style)
                    .setValue(con.value as? Long ?: 0L, false)
                    .setChangeListener {
                        con.value = it
                        onChange(false)
                    }
                    .setResetListener { con.value = con.defaultValue; con.defaultValue.toString() }
                    .apply { textSize = font.size }
            }
            "String" -> {
                if (old is TextInput) return old.apply { textSize = font.size }
                return TextInput("", "", con.value.toString(), style)
                    .addChangeListener {
                        con.value = it
                        onChange(false)
                    }
                    .setResetListener { con.value = con.defaultValue; con.defaultValue.toString() }
                    .apply { textSize = font.size }
            }
            "Bool", "Boolean" -> {
                if (old is Checkbox) return old
                    .apply { size = font.sizeInt }
                return Checkbox(con.value == true, false, font.sizeInt, style)
                    .setChangeListener {
                        con.value = it
                        onChange(false)
                    }
                    .setResetListener { con.value = con.defaultValue; con.defaultValue == true }
                    .apply { makeBackgroundTransparent() }
            }
            "Vector2f", "Vector3f", "Vector4f",
            "Vector2d", "Vector3d", "Vector4d" -> {
                return null // would use too much space
                /*return ComponentUI.createUIByTypeName(null, "", object : IProperty<Any?> {
                    override val annotations: List<Annotation> get() = emptyList()
                    override fun get() = con.value
                    override fun getDefault() = con.defaultValue
                    override fun init(panel: Panel?) {}
                    override fun reset(panel: Panel?): Any? {
                        val value = getDefault()
                        con.value = value
                        return value
                    }

                    override fun set(panel: Panel?, value: Any?) {
                        con.value = value
                    }
                }, type, null, style)*/
            }
        }
        if (type.startsWith("Enum<") && type.endsWith(">")) {
            try {
                // enum input for enums
                // not tested -> please test 😄
                if (old is EnumInput) return old.apply { textSize = font.size }
                val clazz = javaClass.classLoader.loadClass(type.substring(5, type.length - 1))
                val values = EnumInput.getEnumConstants(clazz)
                return EnumInput(NameDesc(con.value.toString()), values.map { NameDesc(it.toString()) }, style)
                    .setChangeListener { _, index, _ ->
                        con.value = values[index]
                        onChange(false)
                    }
            } catch (e: ClassNotFoundException) {
                e.printStackTrace()
            }
        }
        LOGGER.warn("Type $type is missing input UI definition")
        return null
    }

    fun getCursorPosition(x: Float, y: Float, center: Vector3d = Vector3d()): Vector3d {
        center.set(0.0)
        center.x = windowToCoordsX(x.toDouble())
        center.y = windowToCoordsY(y.toDouble())
        return center
    }

    override fun onCopyRequested(x: Float, y: Float): Any? {
        val focussedNodes = children.mapNotNull { if (it is NodePanel && it.isAnyChildInFocus) it.node else null }
        if (focussedNodes.isEmpty()) return super.onCopyRequested(x, y)
        // center at mouse cursor
        val center = getCursorPosition(x, y)
        return when (focussedNodes.size) {
            1 -> {
                // clone, but remove all connections
                val original = focussedNodes.first()
                val clone = original.clone()
                val inputs = clone.inputs
                if (inputs != null) for (input in inputs) input.others = emptyList()
                val outputs = clone.outputs
                if (outputs != null) for (output in outputs) output.others = emptyList()
                clone.position.sub(center)
                clone
            }
            else -> {
                // clone, but remove all external connections
                val cloned = SaveableArray(focussedNodes).clone()
                val containedNodes = HashSet(cloned)
                for (node in cloned) {
                    node as Node
                    val inputs = node.inputs
                    if (inputs != null) for (input in inputs)
                        input.others = input.others.filter { it.node in containedNodes }
                    val outputs = node.outputs
                    if (outputs != null) for (output in outputs)
                        output.others = output.others.filter { it.node in containedNodes }
                    node.position.sub(center)
                }
                cloned
            }
        }
    }

    override fun onPaste(x: Float, y: Float, data: String, type: String) {
        val graph = graph ?: return super.onPaste(x, y, data, type)
        var done = false
        try {
            val data2 = TextReader.read(data, workspace, true).first()
            // add centered at mouse cursor :3
            val center = getCursorPosition(x, y)
            when (data2) {
                is Node -> {
                    // add at mouse cursor
                    data2.position.add(center)
                    graph.nodes.add(data2)
                    getNodePanel(data2).requestFocus()
                    onChange(false)
                    done = true
                }
                is SaveableArray -> {
                    val nodes = data2.filterIsInstance<Node>()
                    if (nodes.isNotEmpty()) {
                        graph.nodes.addAll(nodes)
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

    override fun drawsOverlayOverChildren(lx0: Int, ly0: Int, lx1: Int, ly1: Int): Boolean {
        return true
    }

    override fun capturesChildEvents(lx0: Int, ly0: Int, lx1: Int, ly1: Int): Boolean {
        return false
    }

    open fun getTypeColor(con: NodeConnector): Int {
        return typeColors.getOrDefault(con.type, blue) or black
    }

    fun onChange(isNodePositionChange: Boolean) {
        val graph = graph ?: return
        val listeners = changeListeners
        for (i in listeners.indices) {
            listeners[i](graph, isNodePositionChange)
        }
    }

    val changeListeners = ArrayList<(Graph, Boolean) -> Unit>()
    fun addChangeListener(listener: (graph: Graph, isNodePositionChange: Boolean) -> Unit): GraphEditor {
        changeListeners += listener
        return this
    }

    open fun canDeleteNode(node: Node): Boolean = true

    override val canDrawOverBorders get() = true
    override val className get() = "GraphEditor"

    @Suppress("MayBeConstant")
    companion object {

        private val LOGGER = LogManager.getLogger(GraphEditor::class.java)

        val greenish = 0x1cdeaa
        val yellowGreenish = 0x9cf841
        val red = 0xa90505
        val yellow = 0xf8c522
        val blueish = 0x707fb0
        val orange = 0xfc7100
        val pink = 0xdf199a
        val softYellow = 0xebe496
        val lightBlueish = 0x819ef3
        val blue = 0x00a7f2

        val typeColors = hashMapOf(
            "Int" to greenish,
            "Long" to greenish,
            "Float" to yellowGreenish,
            "Double" to yellowGreenish,
            "Flow" to white,
            "Bool" to red,
            "Boolean" to red,
            "Vector2f" to yellow,
            "Vector3f" to yellow,
            "Vector4f" to yellow,
            "Vector2d" to yellow,
            "Vector3d" to yellow,
            "Vector4d" to yellow,
            "Quaternion4f" to blueish,
            "Quaternion4d" to blueish,
            "Transform" to orange,
            "Matrix4x3f" to orange,
            "Matrix4x3d" to orange,
            "String" to pink,
            "Texture" to softYellow,
            "?" to lightBlueish,
        )

        @JvmStatic
        fun main(args: Array<String>) {
            ECSRegistry.init()
            GFXBase.forceLoadRenderDoc()
            val g = FlowGraph.testLocalVariables()
            calculateNodePositions(g.nodes)
            val ge = GraphEditor(g, DefaultConfig.style)
            testUI(
                listOf(
                    SpyPanel {
                        // performance test for generating lots of text
                        val testTextPerformance = false
                        if (testTextPerformance) {
                            ge.scale *= 1.02
                            if (ge.scale > 10.0) {
                                ge.scale = 1.0
                            }
                            ge.targetScale = ge.scale
                            ge.invalidateLayout()
                        } else ge.invalidateDrawing() // for testing normal performance
                    }, ge
                )
            )
        }
    }

}