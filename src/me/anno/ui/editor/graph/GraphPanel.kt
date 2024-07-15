package me.anno.ui.editor.graph

import me.anno.Time
import me.anno.gpu.drawing.DrawCurves
import me.anno.gpu.drawing.DrawGradients
import me.anno.gpu.drawing.DrawRectangles
import me.anno.gpu.drawing.DrawTexts
import me.anno.graph.visual.Graph
import me.anno.graph.visual.node.Node
import me.anno.graph.visual.node.NodeConnector
import me.anno.graph.visual.node.NodeInput
import me.anno.graph.visual.render.NodeGroup
import me.anno.input.Key
import me.anno.io.saveable.Saveable
import me.anno.io.saveable.SaveableArray
import me.anno.io.files.FileReference
import me.anno.io.files.InvalidRef
import me.anno.language.translation.NameDesc
import me.anno.maths.Maths
import me.anno.ui.Panel
import me.anno.ui.Style
import me.anno.ui.UIColors
import me.anno.ui.base.groups.MapPanel
import me.anno.ui.editor.sceneView.Grid
import me.anno.ui.input.EnumInput
import me.anno.ui.input.FileInput
import me.anno.ui.input.FloatInput
import me.anno.ui.input.IntInput
import me.anno.ui.input.TextInput
import me.anno.ui.input.components.Checkbox
import me.anno.utils.Color
import me.anno.utils.Color.a
import me.anno.utils.Color.withAlpha
import me.anno.utils.Warning
import me.anno.utils.structures.maps.Maps.removeIf
import org.apache.logging.log4j.LogManager
import org.joml.Vector3d
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.log2
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt

open class GraphPanel(graph: Graph? = null, style: Style) : MapPanel(style) {

    private val graphs = ArrayList<Graph>()
    private val nodeToPanel = HashMap<Node, NodePanel>()
    private val nodeToPanel2 = HashMap<NodeGroup, NodeGroupPanel>()

    var graph: Graph? = graph
        set(value) {
            if (field !== value) {
                field = value
                invalidateLayout()
                children.removeAll { it is NodePanel || it is NodeGroupPanel }
                nodeToPanel.clear()
                nodeToPanel2.clear()
            }
        }

    init {
        if (graph != null) {
            graphs.add(graph)
        }
        minScale.set(0.001)
        maxScale.set(10.0)
    }

    fun requestFocus(node: Node) {
        nodeToPanel[node]?.requestFocus()
    }

    fun requestFocus(node: NodeGroup) {
        nodeToPanel2[node]?.requestFocus()
    }

    // large scale = fast movement
    override fun onChangeSize() {
        font = font.withSize(baseTextSize.toFloat())
    }

    var cornerRadius = 24f
    val baseTextSize: Double get() = 20 * scale.y

    var scrollLeft = 0
    var scrollTop = 0
    var scrollRight = 0
    var scrollBottom = 0

    // todo: how/where are they used??
    override var scrollPositionX: Double
        get() = scrollLeft.toDouble()
        set(value) {
            Warning.unused(value)
        }

    override var scrollPositionY: Double
        get() = scrollTop.toDouble()
        set(value) {
            Warning.unused(value)
        }

    override val maxScrollPositionX: Long get() = (scrollLeft + scrollRight).toLong()
    override val maxScrollPositionY: Long get() = (scrollTop + scrollBottom).toLong()
    override val childSizeX: Long get() = width + maxScrollPositionX
    override val childSizeY: Long get() = height + maxScrollPositionY

    var font = DrawTexts.monospaceFont

    var gridColor = 0x10ffffff

    var lineThickness = -1
    var lineThicknessBold = -1

    fun getNodePanel(node: Node): NodePanel {
        return nodeToPanel.getOrPut(node) {
            val panel = NodePanel(node, this, style)
            addChild(panel)
            invalidateLayout()
            panel
        }
    }

    fun getNodeGroupPanel(group: NodeGroup): NodeGroupPanel {
        return nodeToPanel2.getOrPut(group) {
            val panel = NodeGroupPanel(group, this, style)
            addChild(0, panel)
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
                val size = max(window.height, 0)
                if (lineThickness < 0) lineThickness = Maths.max(1, sqrt(size / 120f).roundToInt())
                if (lineThicknessBold < 0) lineThicknessBold = Maths.max(1, sqrt(size / 50f).roundToInt())
            }
        }
    }

    override fun calculateSize(w: Int, h: Int) {
        super.calculateSize(w, h)
        ensureChildren()
        val cornerRadius = (scale.y * cornerRadius).toFloat()
        for (i in children.indices) {
            val panel = children[i]
            panel.backgroundRadius = cornerRadius
            panel.calculateSize(w, h)
        }
    }

    override fun setPosition(x: Int, y: Int) {
        super.setPosition(x, y)
        ensureChildren()
        // place all children
        val graph = graph ?: return
        scrollLeft = 0
        scrollRight = 0
        scrollTop = 0
        scrollBottom = 0
        val xe = x + width
        val ye = y + height
        val nodes = graph.nodes
        for (i in nodes.indices) {
            val node = nodes[i]
            val panel = nodeToPanel[node] ?: continue
            place(node.position, panel, xe, ye)
        }
        val groups = graph.groups
        for (i in groups.indices) {
            val group = groups[i]
            val panel = nodeToPanel2[group] ?: continue
            place(group.position, panel, xe, ye)
        }
    }

    private fun place(position: Vector3d, panel: Panel, xe: Int, ye: Int) {
        val xi = coordsToWindowX(position.x).toInt() - panel.width / 2
        val yi = coordsToWindowY(position.y).toInt()
        panel.setPosSize(xi, yi, panel.minW, panel.minH)
        scrollLeft = Maths.max(scrollLeft, x - xi)
        scrollTop = Maths.max(scrollTop, y - yi)
        scrollRight = Maths.max(scrollRight, (xi + panel.width) - xe)
        scrollBottom = Maths.max(scrollBottom, (yi + panel.height) - ye)
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
        val border = Maths.max(width / 10f, 4f)
        val speed = maxSpeed * min(
            Maths.max(
                Maths.max((this.x + border) - x, x - (this.x + this.width - border)),
                Maths.max((this.y + border) - y, y - (this.y + this.height - border))
            ) / border, 1f
        )
        val multiplier = speed * Time.uiDeltaTime.toFloat() / Maths.length(dx2, dy2)
        if (multiplier > 0f) {
            dx2 *= multiplier
            dy2 *= multiplier
            center.add(dx2 / scale.x, dy2 / scale.y)
            target.set(center)
            invalidateLayout()
        } else {
            invalidateDrawing()
        }
    }

    override fun onDraw(x0: Int, y0: Int, x1: Int, y1: Int) {
        drawBackground(x0, y0, x1, y1)
        drawGrid(x0, y0, x1, y1)
        drawNodeGroups(x0, y0, x1, y1)
        drawNodeConnections(x0, y0, x1, y1)
        drawNodePanels(x0, y0, x1, y1)
        drawScrollbars(x0, y0, x1, y1)
    }

    open fun drawNodeGroups(x0: Int, y0: Int, x1: Int, y1: Int) {
        val children = children
        for (index in children.indices) {
            val child = children[index]
            if (child.isVisible && child is NodeGroupPanel) {
                drawChild(child, x0, y0, x1, y1)
            }
        }
    }

    open fun drawNodePanels(x0: Int, y0: Int, x1: Int, y1: Int) {
        val children = children
        for (index in children.indices) {
            val child = children[index]
            if (child.isVisible && child is NodePanel) {
                drawChild(child, x0, y0, x1, y1)
            }
        }
    }

    open fun drawGrid(x0: Int, y0: Int, x1: Int, y1: Int) {
        val gridColor = Color.mixARGB(backgroundColor, gridColor, gridColor.a() / 255f) or Color.black
        // what grid makes sense? power of 2
        // what is a good grid? one stripe every 10-20 px maybe
        val targetStripeDistancePx = 30.0
        val log = log2(targetStripeDistancePx / scale.y)
        val fract = Maths.fract(log.toFloat())
        val size = Maths.pow(2.0, floor(log))
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
            for (i1 in outputs.indices) {
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
                        val inIndex = Maths.max(inNode?.inputs?.indexOf(nodeInput) ?: 0, 0)
                        val inColor = getTypeColor(nodeInput)
                        val px1 = coordsToWindowX(pos.x).toFloat()
                        val py1 = coordsToWindowY(pos.y).toFloat()
                        if (Maths.distance(px0, py0, px1, py1) > 1f) {
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
        val d0 = 20f * scale.y.toFloat() + (abs(y1 - y0) + abs(x1 - x0)) / 8f
        // line thickness depending on flow/non-flow
        val lt = if (type == "Flow") lineThicknessBold else lineThickness
        val xc = (x0 + x1) * 0.5f
        DrawCurves.drawQuartBezier(
            x0, y0, c0,
            x0 + d0, y0, Color.mixARGB(c0, c1, 0.333f),
            xc, yc, Color.mixARGB(c0, c1, 0.5f),
            x1 - d0, y1, Color.mixARGB(c0, c1, 0.667f),
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
        val d0 = (30f + outIndex * 10f) * scale.y.toFloat()
        val d1 = (30f + inIndex * 10f) * scale.y.toFloat()
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
                val ci0 = Color.mixARGB(c0, c1, s0 / ss)
                val ci1 = Color.mixARGB(c0, c1, (s0 + s1) / ss)
                val ci2 = Color.mixARGB(c0, c1, (s0 + s1 + s2) / ss)
                val ci3 = Color.mixARGB(c0, c1, (s0 + s1 + s2 + s3) / ss)
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
                val ci1 = Color.mixARGB(c0, c1, s0 / ss)
                val ci2 = Color.mixARGB(c0, c1, (s0 + s1) / ss)
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
                x0 == x1 -> DrawGradients.drawRectGradient(
                    x0.toInt() - lt2, y0.toInt() - lt2, lt, (y1 - y0).toInt() + lt,
                    c0, c1, false
                )
                y0 == y1 -> DrawGradients.drawRectGradient(
                    x0.toInt() - lt2, y0.toInt() - lt2, (x1 - x0).toInt() + lt, lt,
                    c0, c1, true
                )
                else -> Grid.drawSmoothLine(x0, y0, x1, y1, c0, c0.a() / 255f)
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
                    .addChangeListener {
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
                return EnumInput(
                    NameDesc(con.name), NameDesc(con.currValue.toString()),
                    values.map { NameDesc(it.toString()) }, style
                )
                    .apply { titleView?.hide() }
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
                for (input in inputs) input.others = emptyList()
                val outputs = clone.outputs
                for (output in outputs) output.others = emptyList()
                clone.position.sub(center)
                clone
            }
            else -> {
                // clone, but remove all external connections
                val cloned = SaveableArray(focussedNodes).clone()
                val containedNodes = HashSet<Saveable?>(cloned)
                for (node in cloned) {
                    node as Node
                    val inputs = node.inputs
                    for (input in inputs)
                        input.others = input.others.filter { it.node in containedNodes }
                    val outputs = node.outputs
                    for (output in outputs)
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
        return typeColors[con.type] ?: UIColors.dodgerBlue
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

    companion object {

        private val LOGGER = LogManager.getLogger(GraphPanel::class)

        val typeColors = hashMapOf(
            "Int" to UIColors.mediumAquamarine,
            "Long" to UIColors.mediumAquamarine,
            "Float" to UIColors.greenYellow,
            "Double" to UIColors.greenYellow,
            "Flow" to Color.white,
            "Bool" to UIColors.fireBrick,
            "Boolean" to UIColors.fireBrick,
            "Vector2f" to UIColors.gold,
            "Vector3f" to UIColors.gold,
            "Vector4f" to UIColors.gold,
            "Vector2d" to UIColors.gold,
            "Vector3d" to UIColors.gold,
            "Vector4d" to UIColors.gold,
            "Quaternion4f" to UIColors.blueishGray,
            "Quaternion4d" to UIColors.blueishGray,
            "Transform" to UIColors.darkOrange,
            "Matrix4x3f" to UIColors.darkOrange,
            "Matrix4x3d" to UIColors.darkOrange,
            "String" to UIColors.deepPink,
            "Texture" to UIColors.paleGoldenRod,
            "?" to UIColors.cornFlowerBlue,
        )
    }
}