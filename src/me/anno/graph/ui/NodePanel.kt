package me.anno.graph.ui

import me.anno.config.DefaultStyle.black
import me.anno.gpu.drawing.DrawTexts.drawText
import me.anno.gpu.drawing.GFXx2D.drawCircle
import me.anno.graph.Node
import me.anno.graph.NodeConnector
import me.anno.input.MouseButton
import me.anno.ui.base.constraints.AxisAlignment
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.style.Style
import org.joml.Vector4f
import kotlin.math.max

class NodePanel(
    val node: Node,
    val gp: GraphPanel,
    style: Style
) : PanelListY(style) {

    var lineCount = 0
    val baseTextSize get() = gp.baseTextSize

    val lineSpacing = 0.5

    override fun calculateSize(w: Int, h: Int) {

        val expectedChars = 20

        lineCount = 1 + // title
                1 + // desc
                max(node.inputs?.size ?: 0, node.outputs?.size ?: 0)

        minW = (expectedChars * baseTextSize).toInt()
        minH = ((lineCount * (1.0 + lineSpacing) + lineSpacing) * baseTextSize).toInt()

        // todo calculate how many lines, and space we need
        // todo base that calculation on w maybe

        val radius = w / 10
        backgroundRadiusX = radius
        backgroundRadiusY = radius

        this.w = minW
        this.h = minH


    }

    override fun placeInParent(x: Int, y: Int) {
        super.placeInParent(x, y)
        // calculate positions for connectors
        placeConnectors(node.inputs, gp.windowToCoordsX(this.x + baseTextSize))
        placeConnectors(node.outputs, gp.windowToCoordsX(this.x + this.w - baseTextSize))
        // todo calculate connector positions: calculate left & right average, and then average that
    }

    private fun <V : NodeConnector> placeConnectors(connectors: Array<V>?, x: Double) {
        connectors ?: return
        for ((index, con) in connectors.withIndex()) {
            con.position.set(x, gp.windowToCoordsY(this.y + (index + 1.5) * baseTextSize * (1.0 + lineSpacing)), 0.0)
        }
    }

    init {
        backgroundColor = (Math.random() * 1e9).toInt().and(0x777777) or black
    }

    override fun onDraw(x0: Int, y0: Int, x1: Int, y1: Int) {

        drawBackground()

        val font = gp.font

        val textSize = font.sizeInt * 3 / 4
        drawText(x + w / 2, y + textSize, font, node.name, -1, backgroundColor, -1, -1, AxisAlignment.CENTER)

        // draw sockets, and their names
        val radius = baseTextSize.toFloat() * 0.7f
        val innerRadius = 0.8f
        val dxTxt = (radius * 1.3f).toInt()
        for (con in node.inputs ?: emptyArray()) {
            val pos = con.position
            val px = gp.coordsToWindowX(pos.x).toInt()
            val py = gp.coordsToWindowY(pos.y).toInt()
            drawCircle(px, py, radius, radius, innerRadius, 0f, 360f, Vector4f(1f))
            drawText(px + dxTxt, py - textSize, font, con.name, -1, backgroundColor, -1, -1, AxisAlignment.MIN)
        }

        for (con in node.outputs ?: emptyArray()) {
            val pos = con.position
            val px = gp.coordsToWindowX(pos.x).toInt()
            val py = gp.coordsToWindowY(pos.y).toInt()
            drawCircle(px, py, radius, radius, innerRadius, 0f, 360f, Vector4f(1f))
            drawText(px - dxTxt, py - textSize, font, con.name, -1, backgroundColor, -1, -1, AxisAlignment.MAX)
        }

    }

    fun getConnectorAt(x: Float, y: Float): NodeConnector? {
        val radius = baseTextSize
        val radiusSq = radius * radius
        val cx = gp.windowToCoordsX(x.toDouble())
        val cy = gp.windowToCoordsY(y.toDouble())
        for (con in node.inputs ?: emptyArray()) {
            if (con.position.distanceSquared(cx, cy, 0.0) < radiusSq) {
                return con
            }
        }
        for (con in node.outputs ?: emptyArray()) {
            if (con.position.distanceSquared(cx, cy, 0.0) < radiusSq) {
                return con
            }
        }
        return null
    }

    var isDragged = false

    override fun onMouseDown(x: Float, y: Float, button: MouseButton) {
        val con = getConnectorAt(x, y)
        isDragged = false
        when {
            button.isLeft && con != null -> {
                gp.dragged = con
                gp.invalidateDrawing()
            }
            button.isLeft -> {
                // todo check if mouse is within rounded corners
                isDragged = true
            }
            else -> super.onMouseDown(x, y, button)
        }
    }

    override fun onMouseMoved(x: Float, y: Float, dx: Float, dy: Float) {
        if (isDragged) {
            node.position.add(
                gp.windowToCoordsDirX(dx.toDouble()),
                gp.windowToCoordsDirY(dy.toDouble()),
                0.0
            )
            gp.invalidateLayout()
        } else super.onMouseMoved(x, y, dx, dy)
    }

    override fun onMouseUp(x: Float, y: Float, button: MouseButton) {
        val con0 = gp.dragged
        val con1 = getConnectorAt(x, y)
        isDragged = false
        if (con0 != null && con1 != null && con0 !== con1 &&
            con0::class != con1::class
        ) {
            // todo if touches connector, and had connector, connect them
            gp.invalidateDrawing()
        } else super.onMouseUp(x, y, button)
    }

}