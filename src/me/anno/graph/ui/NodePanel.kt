package me.anno.graph.ui

import me.anno.config.DefaultStyle.black
import me.anno.fonts.FontManager.getBaselineY
import me.anno.gpu.drawing.DrawTexts
import me.anno.gpu.drawing.DrawTexts.drawText
import me.anno.gpu.drawing.GFXx2D.drawCircle
import me.anno.gpu.drawing.GFXx2D.drawHalfArrow
import me.anno.graph.Node
import me.anno.graph.NodeConnector
import me.anno.graph.NodeInput
import me.anno.input.MouseButton
import me.anno.io.serialization.NotSerializedProperty
import me.anno.maths.Maths.distance
import me.anno.maths.Maths.length
import me.anno.maths.Maths.mapClamped
import me.anno.maths.Maths.mixARGB
import me.anno.ui.Panel
import me.anno.ui.base.Font
import me.anno.ui.base.constraints.AxisAlignment
import me.anno.ui.base.groups.PanelList
import me.anno.ui.style.Style
import kotlin.math.max
import kotlin.math.roundToInt

class NodePanel(
    val node: Node,
    val gp: GraphPanel,
    style: Style
) : PanelList(style) {

    var lineCount = 0
    val baseTextSize get() = gp.baseTextSize

    var lineSpacing = 0.5

    init {
        backgroundColor = mixARGB(backgroundColor, black, 0.5f)
    }

    var isDragged = false

    val inputFields = HashMap<NodeConnector, Panel>()

    override fun calculateSize(w: Int, h: Int) {

        val expectedChars = 20

        lineCount = 1 + // title
                // 1 + // desc
                max(node.inputs?.size ?: 0, node.outputs?.size ?: 0)

        minW = (expectedChars * baseTextSize).toInt()
        minH = ((lineCount * (1.0 + lineSpacing) + lineSpacing) * baseTextSize).toInt()

        // todo calculate how many lines, and space we need
        // todo base that calculation on w maybe

        val radius = w / 10
        backgroundRadiusX = radius
        backgroundRadiusY = radius

        val inputs = node.inputs
        if (inputs != null) for (con in inputs) {
            // add all needed new input fields
            val oldField = inputFields[con]
            val newField = getInputField(con, oldField)
            if (newField !== oldField) {
                if (oldField != null) remove(oldField)
                if (newField != null) {
                    add(newField)
                    inputFields[con] = newField
                } else inputFields.remove(con)
            }
            newField?.calculateSize(minW, minH)
        } else if (inputFields.isNotEmpty()) {
            // remove all input fields that are no longer needed
            for ((_, panel) in inputFields) {
                remove(panel)
            }
            inputFields.clear()
        }

        this.w = minW
        this.h = minH

    }

    override fun setPosition(x: Int, y: Int) {
        super.setPosition(x, y)
        // calculate positions for connectors
        placeConnectors(node.inputs, gp.windowToCoordsX(this.x + baseTextSize))
        placeConnectors(node.outputs, gp.windowToCoordsX(this.x + this.w - baseTextSize))
        // place all input fields to the correct position
        val baseTextSize = baseTextSize.toInt()
        if (inputFields.isNotEmpty()) for ((key, panel) in inputFields) {
            val pos = key.position
            val cx = gp.coordsToWindowX(pos.x).toInt()
            val cy = gp.coordsToWindowY(pos.y).toInt()
            // place to the right by radius
            panel.setPosSize(cx + baseTextSize, cy - panel.minW / 2, panel.minW, panel.minH)
        }
    }

    fun getInputField(con: NodeConnector, old: Panel?): Panel? {
        if (!con.isEmpty()) return null
        // todo give int,long,float,double,bool,string input fields
        // dx += inputWidth
        // todo enum input for enums...
        return null
    }

    @NotSerializedProperty
    private var lmx = 0

    @NotSerializedProperty
    private var lmy = 0

    override fun tickUpdate() {
        super.tickUpdate()
        if (canBeSeen) {
            val window = window
            if (window != null) {
                if (lmx != window.mouseXi || lmy != window.mouseYi) {
                    lmx = window.mouseXi
                    lmy = window.mouseYi
                    invalidateDrawing()
                }
            }
        }
    }

    private fun <V : NodeConnector> placeConnectors(connectors: Array<V>?, x: Double) {
        connectors ?: return
        for ((index, con) in connectors.withIndex()) {
            con.position.set(x, gp.windowToCoordsY(this.y + (index + 1.5) * baseTextSize * (1.0 + lineSpacing)), 0.0)
        }
    }

    var focusOutlineColor = -1
    var focusOutlineThickness = 1

    override fun onDraw(x0: Int, y0: Int, x1: Int, y1: Int) {

        // draw whether the node is in focus, maybe will small outline
        if (isInFocus) {

            val bc = backgroundColor
            backgroundColor = focusOutlineColor
            drawBackground(x0, y0, x1, y1)
            backgroundColor = bc
            drawBackground(x0, y0, x1, y1, focusOutlineThickness)

        } else {

            drawBackground(x0, y0, x1, y1)

        }

        val font = gp.font
        val textSize = DrawTexts.getTextSizeY(font, "w", -1, -1)

        val backgroundColor = backgroundColor
        val textColor = -1

        // node title
        drawText(
            x + w / 2, y + textSize / 2, font, node.name, textColor,
            backgroundColor, w * 3 / 4, -1, AxisAlignment.CENTER
        )

        val window = window
        val mouseX = window?.mouseX ?: 0f
        val mouseY = window?.mouseY ?: 0f

        // draw sockets, and their names
        val dxTxt = (baseTextSize * 0.7).toInt()
        val dyTxt = getBaselineY(font).toInt()
        val baseTextSize = baseTextSize.toFloat()

        // to do generally, weights could be useful on either end (maybe?)

        val inputs = node.inputs
        if (inputs != null) for (con in inputs) {
            var dx = dxTxt
            val panel = inputFields[con]
            if (panel != null) {
                // todo plus padding
                dx += panel.w
            }
            drawConnector(con, baseTextSize, mouseX, mouseY, dx, dyTxt, font, textColor)
        }

        val outputs = node.outputs
        if (outputs != null) for (con in outputs) {
            drawConnector(con, baseTextSize, mouseX, mouseY, -dxTxt, dyTxt, font, textColor)
        }

        drawChildren(x0, y0, x1, y1)

    }

    fun drawConnector(
        con: NodeConnector,
        baseTextSize: Float,
        mouseX: Float,
        mouseY: Float,
        dx: Int,
        dy: Int,
        font: Font,
        textColor: Int
    ) {
        val pos = con.position
        val px = gp.coordsToWindowX(pos.x).toFloat()
        val py = gp.coordsToWindowY(pos.y).toFloat()
        val pxi = px.toInt()
        val pyi = py.toInt()
        val radius = baseTextSize * 0.4f
        val radius2 = mapClamped(
            length(px - mouseX, py - mouseY),
            0.9f * radius, 1.3f * radius,
            radius * 1.2f, radius
        )
        val innerRadius = if (con.others.isEmpty()) 0.8f else 0f
        if (con.type == "Flow") {
            // if the type is flow, draw an arrow instead of circle
            val ry = radius2.toInt()
            val rx = ry * 3 / 4
            // apply inner radius
            drawHalfArrow(
                pxi - rx, pyi - ry, 2 * rx, 2 * ry,
                gp.getTypeColor(con), backgroundColor
            )
            if (innerRadius > 0f) {
                val dx2 = ((1f - innerRadius) * ry).roundToInt()
                val rx2 = rx - dx2
                val ry2 = ry - dx2 + 1
                drawHalfArrow(
                    pxi - rx2 - 1, pyi - ry2, 2 * rx2, 2 * ry2,
                    backgroundColor, gp.getTypeColor(con)
                )
            }
        } else {
            drawCircle(
                pxi, pyi, radius2, radius2, innerRadius,
                backgroundColor, gp.getTypeColor(con), backgroundColor
            )
        }
        drawText(
            pxi + dx, pyi + dy, font, con.name, textColor,
            backgroundColor, -1, -1,
            if (dx < 0) AxisAlignment.MAX else AxisAlignment.MIN
        )
    }

    fun getConnectorAt(x: Float, y: Float): NodeConnector? {
        val radius = baseTextSize * 0.5 + 5 // 5 for padding
        val radiusSq = radius * radius
        val cx = gp.windowToCoordsX(x.toDouble())
        val cy = gp.windowToCoordsY(y.toDouble())
        var bestDistance =radiusSq
        var bestCon: NodeConnector? = null
        val inputs = node.inputs
        if(inputs != null) for (con in inputs) {
            val distance = con.position.distanceSquared(cx, cy, 0.0)
            if (distance < bestDistance) {
                bestDistance = distance
                bestCon = con
            }
        }
        val outputs = node.outputs
        if(outputs != null) for (con in outputs) {
            val distance = con.position.distanceSquared(cx, cy, 0.0)
            if (distance < bestDistance) {
                bestDistance = distance
                bestCon = con
            }
        }
        return bestCon
    }

    /*override fun getCursor(): Long? {
        val window = window ?: return null
        return when {
            getConnectorAt(window.mouseX, window.mouseY) != null -> Cursor.hand
            // todo move-cursor
            isOpaqueAt(window.mouseXi, window.mouseYi) -> null // if (isInFocus) Cursor.vResize else null
            else -> null
        }
    }*/

    override fun onMouseDown(x: Float, y: Float, button: MouseButton) {
        val con = getConnectorAt(x, y)
        isDragged = false
        when {
            button.isLeft && con != null -> {
                gp.dragged = con
                gp.invalidateDrawing()
            }
            button.isLeft -> {
                if (isOpaqueAt(x.toInt(), y.toInt())) {
                    isDragged = true
                }
            }
            else -> super.onMouseDown(x, y, button)
        }
    }

    override fun onMouseMoved(x: Float, y: Float, dx: Float, dy: Float) {
        if (isDragged) {
            val wx = gp.coordsToWindowX(node.position.x) + dx
            val wy = gp.coordsToWindowY(node.position.y) + dy
            gp.moveIfOnEdge(x, y)
            node.position.set(
                gp.windowToCoordsX(wx),
                gp.windowToCoordsY(wy),
                node.position.z
            )
            gp.invalidateLayout()
        } else super.onMouseMoved(x, y, dx, dy)
    }

    override fun onMouseUp(x: Float, y: Float, button: MouseButton) {
        val con0 = gp.dragged
        val con1 = (gp.getPanelAt(x.toInt(), y.toInt()) as? NodePanel)?.getConnectorAt(x, y)
        val window = window
        when {
            con0 != null && con1 != null && con0 !== con1 &&
                    con0::class != con1::class -> {
                // also only connect, if not already connected
                if (con1 in con0) {
                    con0.disconnect(con1)
                } else {
                    // todo only connect, if types are compatible (flow only to flow)
                    val input = if (con0 is NodeInput) con0 else con1
                    val output = if (con0 === input) con1 else con0
                    // only connect, if connection is supported, otherwise replace
                    if (!input.isEmpty() && !input.node!!.supportsMultipleInputs(input)) {
                        input.disconnectAll()
                    }
                    if (!output.isEnabled && !output.node!!.supportsMultipleOutputs(output)) {
                        output.disconnectAll()
                    }
                    con0.connect(con1)
                }
            }
            con0 != null && (window == null ||
                    distance(window.mouseDownX, window.mouseDownY, window.mouseX, window.mouseY) < 50f) -> {
                // loosen this connection
                con0.disconnectAll()
            }
            else -> super.onMouseUp(x, y, button)
        }
        isDragged = false
        gp.dragged = null
        gp.invalidateDrawing()
    }

    override fun onDoubleClick(x: Float, y: Float, button: MouseButton) {
        gp.target.set(node.position)
        invalidateLayout()
    }

    override fun getTooltipText(x: Float, y: Float): String? {
        val con = getConnectorAt(x, y)
        return con?.type
    }

    override fun getMultiSelectablePanel() = this

}