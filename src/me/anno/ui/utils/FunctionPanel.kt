package me.anno.ui.utils

import me.anno.gpu.drawing.DrawCurves.drawLine
import me.anno.gpu.drawing.DrawCurves.lineBatch
import me.anno.gpu.drawing.DrawTexts
import me.anno.gpu.drawing.DrawTexts.drawSimpleTextCharByChar
import me.anno.maths.Maths.clamp
import me.anno.ui.Style
import me.anno.ui.base.components.AxisAlignment
import me.anno.ui.base.groups.MapPanel
import me.anno.utils.Color.a
import me.anno.utils.Color.withAlpha
import me.anno.utils.types.Floats.roundToIntOr
import java.math.BigDecimal
import java.math.BigInteger
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt

// where should we move this class?
/**
 * Panel, which draws a numeric function as a graph, and lets the user navigate like on a map.
 * */
abstract class FunctionPanel(style: Style) : MapPanel(style) {

    abstract fun getNumFunctions(): Int
    abstract fun getValue(index: Int, x: Double): Double
    abstract fun getColor(index: Int): Int

    var lineThickness = 1f
    var functionName: String? = "f"

    init {
        minScale.set(1e-16)
        maxScale.set(1e16)
        targetScale.set(100.0)
        scale.set(targetScale)
    }

    private fun formatNumber(i: Long, pow: Int, pow10: Double): String {
        if (pow10 in 1.0..1e5) return (i * pow10).toLong().toString()
        return BigDecimal(BigInteger.valueOf(i), pow).toString()
    }

    private fun maxStringLength(i: Long, pow: Int): Int {
        var v = log10(max(i, 2).toDouble()).toInt()
        if (pow < 0) v++
        return v
    }

    private fun draw2DGridNumbers(
        x0: Int, y0: Int, x1: Int, y1: Int,
        color: Int, gridSize: Double, all: Boolean,
    ) {
        if (color.a() == 0) return
        val gridX0 = windowToCoordsX(x0.toDouble())
        val gridX1 = windowToCoordsX(x1.toDouble())
        val gridY0 = windowToCoordsY(y0.toDouble())
        val gridY1 = windowToCoordsY(y1.toDouble())
        val i0 = floor(gridX0 / gridSize).toLong()
        val i1 = ceil(gridX1 / gridSize).toLong()
        val j0 = floor(gridY0 / gridSize).toLong()
        val j1 = ceil(gridY1 / gridSize).toLong()
        // good positioning for numbers
        val pow = -log10(gridSize).roundToIntOr()
        val msl = maxStringLength(max(abs(i0), abs(i1)), pow)
        val dx = (DrawTexts.monospaceFont.sampleWidth * (2f + 0.5f * msl)).toInt()
        val dy = DrawTexts.monospaceFont.sampleHeight shr 1
        val textX = clamp(coordsToWindowX(0.0).toInt(), x0 + dx, x1 - dx)
        val textY = clamp(coordsToWindowY(0.0).toInt(), y0 + dy, y1 - dy)
        val bg = backgroundColor and 0xffffff
        var mod10i = i0 % 10
        if (mod10i < 0) mod10i += 10
        for (i in i0 until i1) {
            val gridX = i * gridSize
            val windowX = coordsToWindowX(gridX).toInt()
            if (mod10i != 0L || all) drawSimpleTextCharByChar(
                windowX, textY, 1, formatNumber(i, pow, gridSize), color, bg,
                AxisAlignment.CENTER, AxisAlignment.CENTER,
            )
            if (mod10i == 9L) mod10i = 0
            else mod10i++
        }
        var mod10j = j0 % 10
        if (mod10j < 0) mod10i += 10
        for (j in j0 until j1) {
            val gridY = j * gridSize
            val windowY = coordsToWindowY(gridY).toInt()
            if (mod10j != 0L || all) drawSimpleTextCharByChar(
                textX, windowY, 1, formatNumber(-j, pow, gridSize), color, bg,
                AxisAlignment.CENTER, AxisAlignment.CENTER
            )
            if (mod10j == 9L) mod10j = 0
            else mod10j++
        }
    }

    override fun onMouseMoved(x: Float, y: Float, dx: Float, dy: Float) {
        if (dx != 0f) invalidateDrawing()
        super.onMouseMoved(x, y, dx, dy)
    }

    override fun onDraw(x0: Int, y0: Int, x1: Int, y1: Int) {
        super.onDraw(x0, y0, x1, y1)
        drawNumbered2DLineGrid(x0, y0, x1, y1)
        for (funcIndex in 0 until getNumFunctions()) {
            val lineColor = getColor(funcIndex)
            drawCurve(x0, y0, x1, y1, funcIndex, lineColor, lineThickness)
        }
        if (isHovered) {
            showValueAtCursor()
        }
    }

    fun drawNumbered2DLineGrid(x0: Int, y0: Int, x1: Int, y1: Int) {
        val s0 = log10(max(windowToCoordsDirY(height.toDouble()), 1e-308))
        if (!s0.isFinite()) return
        val s1 = floor(s0)
        val s2 = 10.0.pow(s1)
        val sf = 1f - (s0 - s1).toFloat()
        val color = -1
        draw2DLineGrid(x0, y0, x1, y1, color.withAlpha(0.3f * sf), s2 * 0.1)
        draw2DLineGrid(x0, y0, x1, y1, color.withAlpha(0.3f), s2)
        draw2DGridNumbers(x0, y0, x1, y1, color.withAlpha(0.7f * max(sf * 2f - 1f, 0f)), s2 * 0.1, false)
        draw2DGridNumbers(x0, y0, x1, y1, color.withAlpha(0.7f), s2, true)
    }

    fun showValueAtCursor() {
        val window = window
        val funcName = functionName
        if (window != null && funcName != null) {
            val mx = window.mouseX.toDouble()
            val vx = windowToCoordsX(mx)
            val vy = (0 until getNumFunctions()).map { getValue(it, vx) }
            drawSimpleTextCharByChar(
                x, y, 2,
                "$funcName($vx): $vy"
            )
        }
    }

    fun drawCurve(x0: Int, y0: Int, x1: Int, y1: Int, funcIndex: Int, lineColor: Int, lineThickness: Float) {
        val bgColor = backgroundColor
        var lx = 0f
        var ly = 0f
        val batch = lineBatch.start()
        val thHalf = lineThickness * 0.5f
        val minY = y0 - thHalf
        val maxY = y1 + thHalf
        for (x in x0 until x1) {
            val xd = x.toDouble()
            val xv = windowToCoordsX(xd)
            val yv = -getValue(funcIndex, xv)
            val yf = coordsToWindowY(yv).toFloat()
            val xf = xd.toFloat()
            if (x > x0 && min(ly, yf) > minY && max(ly, yf) < maxY) {
                drawLine(lx, ly, xf, yf, lineThickness, lineColor, bgColor, true)
            }
            lx = xf
            ly = yf
        }
        lineBatch.finish(batch)
    }
}