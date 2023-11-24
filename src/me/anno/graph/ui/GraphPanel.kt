package me.anno.graph.ui

import me.anno.Time
import me.anno.gpu.drawing.DrawCurves.drawQuartBezier
import me.anno.gpu.drawing.DrawGradients.drawRectGradient
import me.anno.gpu.drawing.DrawRectangles
import me.anno.gpu.drawing.DrawTexts.monospaceFont
import me.anno.graph.Graph
import me.anno.graph.Node
import me.anno.graph.NodeConnector
import me.anno.graph.NodeInput
import me.anno.graph.render.NodeGroup
import me.anno.input.Key
import me.anno.io.SaveableArray
import me.anno.io.files.FileReference
import me.anno.io.files.InvalidRef
import me.anno.language.translation.NameDesc
import me.anno.maths.Maths.distance
import me.anno.maths.Maths.fract
import me.anno.maths.Maths.length
import me.anno.maths.Maths.max
import me.anno.utils.Color.mixARGB
import me.anno.maths.Maths.pow
import me.anno.ui.Panel
import me.anno.ui.Style
import me.anno.ui.base.groups.MapPanel
import me.anno.ui.editor.sceneView.Grid.drawSmoothLine
import me.anno.ui.input.*
import me.anno.ui.input.components.Checkbox
import me.anno.utils.Color.a
import me.anno.utils.Color.black
import me.anno.utils.Color.white
import me.anno.utils.Color.withAlpha
import me.anno.utils.Warning.unused
import me.anno.utils.pooling.JomlPools
import me.anno.utils.structures.maps.Maps.removeIf
import org.apache.logging.log4j.LogManager
import kotlin.math.*

open class GraphPanel(graph: Graph? = null, style: Style) : MapPanel(style) {

    private val graphs = ArrayList<Graph>()
    private val nodeToPanel = HashMap<Node, NodePanel>()
    private val nodeToPanel2 = HashMap<NodeGroup, NodeGroupPanel>()

    var graph: Graph? = graph
        set(value) {
            if (field !== value) {
                field = value
                invalidateLayout()
                children.removeAll { it is NodePanel }
                nodeToPanel.clear()
            }
        }

    init {
        if (graph != null) {
            graphs.add(graph)
        }
        minScale = 0.001
        maxScale = 10.0
    }

    fun requestFocus(node: Node) {
        nodeToPanel[node]?.requestFocus()
    }

    fun requestFocus(node: NodeGroup) {
        nodeToPanel2[node]?.requestFocus()
    }

    // large scale = fast movement
    override var scale = 1.0
        set(value) {
            if (field != value) {
                field = value
                font = font.withSize(baseTextSize.toFloat())
            }
        }

    var cornerRadius = 24f
    val baseTextSize: Double get() = 20 * scale

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
    override val childSizeX: Long get() = width + maxScrollPositionX
    override val childSizeY: Long get() = height + maxScrollPositionY

    fun centerOnNodes(scaleFactor: Float = 0.9f) {
        // todo not correct yet :/
        val graph = graph ?: return
        val bounds = JomlPools.aabbd.borrow()
        for (node in graph.nodes) {
            bounds.union(node.position)
            val inputs = node.inputs
            if (inputs != null) for (con in inputs) {
                bounds.union(con.position)
            }
            val outputs = node.outputs
            if (outputs != null) for (con in outputs) {
                bounds.union(con.position)
            }
        }
        if (!bounds.isEmpty()) {
            center.set(bounds.centerX, bounds.centerY)
            val factor = scaleFactor / max(bounds.deltaX / width, bounds.deltaY / height)
            if (factor.isFinite() && factor > 0.0) scale *= factor
        }
    }

    var font = monospaceFont

    var gridColor = 0x10ffffff

    var lineThickness = -1
    var lineThicknessBold = -1

    fun getNodePanel(node: Node): NodePanel {
        return nodeToPanel.getOrPut(node) {
            val panel = NodePanel(node, this, style)
            children.add(panel)
            panel.window = window
            invalidateLayout()
            panel
        }
    }

    fun getNodeGroupPanel(group: NodeGroup): NodeGroupPanel {
        return nodeToPanel2.getOrPut(group) {
            val panel = NodeGroupPanel(group, this, style)
            children.add(0, panel)
            panel.window = window
            invalidateLayout()
            panel
        }
    }

    private fun ensureChildren() {
        val graph = graph ?: return
        val nodes = graph.nodes
        for (i in nodes.indices) {
            getNodePanel(nodes[i])
        }
        if (nodeToPanel.size > nodes.size) {
            val set = nodes.toSet()
            nodeToPanel.removeIf {
                if (it.key !in set) {
                    children.remove(it.value)
                    true
                } else false
            }
        }
        val groups = graph.groups
        for (i in groups.indices) {
            getNodeGroupPanel(groups[i])
        }
        if (nodeToPanel2.size > groups.size) {
            val set = groups.toSet()
            nodeToPanel2.removeIf {
                if (it.key !in set) {
                    children.remove(it.value)
                    true
                } else false
            }
        }
    }

    // todo if node is dragged on a line, connect left & right sides where types match from top to bottom
    // todo -> detect, if a line is being hovered

    override fun onUpdate() {
        super.onUpdate()
        if (lineThickness < 0 || lineThicknessBold < 0) {
            val window = window
            if (window != null) {
                if (lineThickness < 0) lineThickness = max(1, sqrt(window.height / 120f).roundToInt())
                if (lineThicknessBold < 0) lineThicknessBold = max(1, sqrt(window.height / 50f).roundToInt())
            }
        }
    }

    override fun calculateSize(w: Int, h: Int) {
        super.calculateSize(w, h)
        ensureChildren()
        val cornerRadius = (scale * cornerRadius).toFloat()
        for (i in children.indices) {
            val panel = children[i]
            panel.backgroundRadius = cornerRadius
            panel.calculateSize(w, h)
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
        val xe = x + width
        val ye = y + height
        val nodes = graph.nodes
        for (i in nodes.indices) {
            val node = nodes[i]
            val panel = nodeToPanel[node] ?: continue
            val xi = coordsToWindowX(node.position.x).toInt() - panel.width / 2
            val yi = coordsToWindowY(node.position.y).toInt()// - panel.h / 2
            panel.setPosition(xi, yi)
            left = max(left, x - xi)
            top = max(top, y - yi)
            right = max(right, (xi + panel.width) - xe)
            bottom = max(bottom, (yi + panel.height) - ye)
        }
        scrollLeft = left
        scrollRight = right
        scrollTop = top
        scrollBottom = bottom
    }

    override fun onKeyDown(x: Float, y: Float, key: Key) {
        // if we start dragging from a node, and it isn't yet in focus,
        // quickly solve that by making bringing it into focus
        super.onKeyDown(x, y, key)
        if (key == Key.BUTTON_LEFT || key == Key.BUTTON_MIDDLE || key == Key.BUTTON_RIGHT) mapMouseDown(x, y)
    }

    override fun onKeyUp(x: Float, y: Float, key: Key) {
        super.onKeyUp(x, y, key)
        if (key == Key.BUTTON_LEFT || key == Key.BUTTON_MIDDLE || key == Key.BUTTON_RIGHT) mapMouseUp()
    }

    open fun moveIfOnEdge(x: Float, y: Float) {
        val maxSpeed = (width + height) / 6f // ~ 500px / s on FHD
        var dx2 = x - centerX
        var dy2 = y - centerY
        val border = max(width / 10f, 4f)
        val speed = maxSpeed * min(
            max(
                max((this.x + border) - x, x - (this.x + this.width - border)),
                max((this.y + border) - y, y - (this.y + this.height - border))
            ) / border, 1f
        )
        val multiplier = speed * Time.deltaTime.toFloat() / length(dx2, dy2)
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
        drawScrollbars(x0, y0, x1, y1)
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
        val batch = DrawRectangles.startBatch()
        draw2DLineGrid(x0, y0, x1, y1, gridColor.withAlpha(2f * (1f - fract)), size)
        draw2DLineGrid(x0, y0, x1, y1, gridColor.withAlpha(2f * (1f + fract)), size * 2.0)
        DrawRectangles.finishBatch(batch)
    }

    open fun drawNodeConnections(x0: Int, y0: Int, x1: Int, y1: Int) {
        // it would make sense to implement multiple styles, so this could be used in a game in the future as well
        // -> split into multiple subroutines, so you can implement your own style :)
        val graph = graph ?: return
        val nodes = graph.nodes
        for (i0 in nodes.indices) {
            val srcNode = nodes[i0]
            val outputs = srcNode.outputs
            if (outputs != null) for (i1 in outputs.indices) {
                val nodeOutput = outputs[i1]
                val outPosition = nodeOutput.position
                val outColor = getTypeColor(nodeOutput)
                val px0 = coordsToWindowX(outPosition.x).toFloat()
                val py0 = coordsToWindowY(outPosition.y).toFloat()
                val others = nodeOutput.others
                for (i2 in others.indices) {
                    val nodeInput = others[i2]
                    if (nodeInput is NodeInput) {
                        val pos = nodeInput.position
                        val inNode = nodeInput.node
                        val inIndex = max(inNode?.inputs?.indexOf(nodeInput) ?: 0, 0)
                        val inColor = getTypeColor(nodeInput)
                        val px1 = coordsToWindowX(pos.x).toFloat()
                        val py1 = coordsToWindowY(pos.y).toFloat()
                        if (distance(px0, py0, px1, py1) > 1f) {
                            drawNodeConnection(px0, py0, px1, py1, inIndex, i1, outColor, inColor, nodeInput.type)
                        }
                    }
                }
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
                    .setValue(con.currValue as? Float ?: 0f, false)
                    .setChangeListener {
                        con.currValue = it.toFloat()
                        onChange(false)
                    }
                    .setResetListener { con.currValue = con.defaultValue; con.defaultValue.toString() }
                    .apply { textSize = font.size }
            }
            "Double" -> {
                if (old is FloatInput) return old.apply { textSize = font.size }
                return FloatInput(style)
                    .setValue(con.currValue as? Double ?: 0.0, -1, false)
                    .setChangeListener {
                        con.currValue = it
                        onChange(false)
                    }
                    .setResetListener { con.currValue = con.defaultValue; con.defaultValue.toString() }
                    .apply { textSize = font.size }
            }
            "Int" -> {
                if (old is IntInput) return old.apply { textSize = font.size }
                return IntInput(style)
                    .setValue(con.currValue as? Int ?: 0, false)
                    .setChangeListener {
                        con.currValue = it.toInt()
                        onChange(false)
                    }
                    .setResetListener { con.currValue = con.defaultValue; con.defaultValue.toString() }
                    .apply { textSize = font.size }
            }
            "Long" -> {
                if (old is IntInput) return old.apply { textSize = font.size }
                return IntInput(style)
                    .setValue(con.currValue as? Long ?: 0L, -1, false)
                    .setChangeListener {
                        con.currValue = it
                        onChange(false)
                    }
                    .setResetListener { con.currValue = con.defaultValue; con.defaultValue.toString() }
                    .apply { textSize = font.size }
            }
            "String" -> {
                if (old is TextInput) return old.apply { textSize = font.size }
                return TextInput("", "", (con.currValue ?: "").toString(), style)
                    .addChangeListener {
                        con.currValue = it
                        onChange(false)
                    }
                    .setResetListener { con.currValue = con.defaultValue; (con.defaultValue ?: "").toString() }
                    .apply { textSize = font.size }
            }
            "File", "FileReference" -> {
                if (old is FileInput) return old.apply { old.textSize = font.size }
                return FileInput("", style, con.currValue as? FileReference ?: InvalidRef, emptyList(), false)
                    .setChangeListener {
                        con.currValue = it
                        onChange(false)
                    }
                    .setResetListener {
                        con.currValue = con.defaultValue; con.defaultValue as? FileReference ?: InvalidRef
                    }
                    .apply { textSize = font.size }
            }
            "Boolean", "Bool" -> {
                if (old is Checkbox) return old
                    .apply { size = font.sizeInt }
                return Checkbox(con.currValue == true, false, font.sizeInt, style)
                    .setChangeListener {
                        con.currValue = it
                        onChange(false)
                    }
                    .setResetListener { con.currValue = con.defaultValue; con.defaultValue == true }
                    .apply { makeBackgroundTransparent() }
            }
            "Vector2f", "Vector3f", "Vector4f",
            "Vector2d", "Vector3d", "Vector4d" -> {
                return null // would use too much space
                /*return ComponentUI.createUIByTypeName(null, "", object : IProperty<Any?> {
                    override val annotations: List<Annotation> get() = emptyList()
                    override fun get() = con.currValue
                    override fun getDefault() = con.defaultValue
                    override fun init(panel: Panel?) {}
                    override fun reset(panel: Panel?): Any? {
                        val value = getDefault()
                        con.currValue = value
                        return value
                    }

                    override fun set(panel: Panel?, value: Any?) {
                        con.currValue = value
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
                return EnumInput(NameDesc(con.currValue.toString()), values.map { NameDesc(it.toString()) }, style)
                    .setChangeListener { _, index, _ ->
                        con.currValue = values[index]
                        onChange(false)
                    }
            } catch (e: ClassNotFoundException) {
                e.printStackTrace()
            }
        }
        LOGGER.warn("Type $type is missing input UI definition")
        return null
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

    override fun drawsOverlayOverChildren(lx0: Int, ly0: Int, lx1: Int, ly1: Int): Boolean {
        return true
    }

    override fun capturesChildEvents(lx0: Int, ly0: Int, lx1: Int, ly1: Int): Boolean {
        return false
    }

    open fun getTypeColor(con: NodeConnector): Int {
        return typeColors.getOrDefault(con.type, blue)
    }

    fun onChange(isNodePositionChange: Boolean) {
        val graph = graph ?: return
        val listeners = changeListeners
        for (i in listeners.indices) {
            listeners[i](graph, isNodePositionChange)
        }
    }

    val changeListeners = ArrayList<(Graph, Boolean) -> Unit>()
    fun addChangeListener(listener: (graph: Graph, isNodePositionChange: Boolean) -> Unit): GraphPanel {
        changeListeners += listener
        return this
    }

    open fun canDeleteNode(node: Node): Boolean = false

    override val className: String get() = "GraphEditor"

    @Suppress("MayBeConstant")
    companion object {

        private val LOGGER = LogManager.getLogger(GraphPanel::class.java)

        val greenish = 0x1cdeaa or black
        val yellowGreenish = 0x9cf841 or black
        val red = 0xa90505 or black
        val yellow = 0xf8c522 or black
        val blueish = 0x707fb0 or black
        val orange = 0xfc7100 or black
        val pink = 0xdf199a or black
        val softYellow = 0xebe496 or black
        val lightBlueish = 0x819ef3 or black
        val blue = 0x00a7f2 or black

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
    }
}