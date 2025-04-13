package me.anno.input.controller

import me.anno.Time
import me.anno.gpu.drawing.DrawCurves.drawQuadraticBezier
import me.anno.gpu.drawing.DrawRectangles.drawRect
import me.anno.maths.Maths.length
import me.anno.maths.Maths.min
import me.anno.ui.Panel
import me.anno.ui.Style
import org.joml.AABBf

abstract class CalibrationPanel(
    val controller: Controller,
    val axis0: Int,
    val axis1: Int,
    val axis2: Int,
    style: Style
) : Panel(style) {

    init {
        tooltip = "Click to reset"
        addLeftClickListener {
            reset()
        }
    }

    fun reset() {
        stillNanos = 0L
        bounds.clear()
        dead.clear()
    }

    override val canDrawOverBorders get() = true

    override fun calculateSize(w: Int, h: Int) {
        super.calculateSize(w, h)
        minH = 200
        minW = 200
    }

    val bounds = AABBf()
    val dead = AABBf()
    private var lastTime = 0L
    private var lastX = 0f
    private var lastY = 0f
    private var lastZ = 0f

    private var isStillSince = 0L
    var stillNanos = 0L

    private fun addValue(x: Float, y: Float, z: Float) {

        // update statistics

        bounds.union(x, y, 0f)
        val time = Time.nanoTime
        val deltaTime = time - lastTime
        if (lastTime != 0L && deltaTime != 0L) {

            val dx = kotlin.math.abs(x - lastX)
            val dy = kotlin.math.abs(y - lastY)
            val dz = kotlin.math.abs(z - lastZ)

            lastX = x
            lastY = y
            lastZ = z

            val velocity = length(dx, dy, dz) * 1e9f / deltaTime
            val isStill = velocity < 1e-3f
            if (isStill && isStillSince == 0L) {
                isStillSince = time
            } else if (!isStill) isStillSince = 0L

            // 3s still -> record the zero value
            if (isStillSince != 0L &&
                kotlin.math.abs(time - isStillSince) > 1e9 &&
                length(x, y, z) < 0.5
            ) {
                stillNanos += deltaTime
                dead.union(x, y, z)
            }
        }

        lastTime = time

    }

    override fun onUpdate() {
        super.onUpdate()
        addValue(
            if (axis0 < 0) 0f else controller.getRawAxis(axis0),
            if (axis1 < 0) 0f else controller.getRawAxis(axis1),
            if (axis2 < 0) 0f else controller.getRawAxis(axis2)
        )
    }

    private var size = min(width, height)
    private var cx = x + width / 2
    private var cy = y + height / 2

    private fun getX(x: Float) = (cx + x * size * 0.5f).toInt()
    private fun getY(y: Float) = (cy + y * size * 0.5f).toInt()
    private fun getD(x: Float) = (x * size * 0.5f).toInt()
    private fun getXf(x: Float) = (cx + x * size * 0.5f)
    private fun getYf(y: Float) = (cy + y * size * 0.5f)
    private fun getDf(x: Float) = (x * size * 0.5f)

    fun cross(x: Float, y: Float, r: Float, color: Int) {
        val x11 = getX(x)
        val y11 = getY(y)
        val dx = getD(r)
        val sz = dx * 2 - 1
        drawRect(x11 - dx, y11, sz, 1, color)
        drawRect(x11, y11 - dx, 1, sz, color)
    }

    fun circle(x: Float, y: Float, rx: Float, ry: Float, color: Int) {
        // true circle is broken :/
        val cx = getXf(x)
        val cy = getYf(y)
        val dx = getDf(rx)
        val dy = getDf(ry)
        val x0 = cx - dx
        val y0 = cy - dy
        val x1 = cx + dx
        val y1 = cy + dy
        val backgroundColor = background.color
        drawQuadraticBezier(cx, y0, x1, y0, x1, cy, 1f, color, backgroundColor, true)
        drawQuadraticBezier(x1, cy, x1, y1, cx, y1, 1f, color, backgroundColor, true)
        drawQuadraticBezier(cx, y1, x0, y1, x0, cy, 1f, color, backgroundColor, true)
        drawQuadraticBezier(x0, cy, x0, y0, cx, y0, 1f, color, backgroundColor, true)
    }

    fun showBounds(bounds: AABBf, color: Int) {
        if (!bounds.isEmpty()) {
            circle(
                bounds.centerX, bounds.centerY,
                kotlin.math.max(bounds.deltaX * 0.5f, 1f / size),
                kotlin.math.max(bounds.deltaY * 0.5f, 1f / size),
                color
            )
        }
    }

    abstract fun save(cali: ControllerCalibration)

    override fun draw(x0: Int, y0: Int, x1: Int, y1: Int) {
        super.draw(x0, y0, x1, y1)

        size = min(width, height)
        cx = x + width / 2
        cy = y + height / 2


    }

}
