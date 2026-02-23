package me.anno.ui.base.groups

import me.anno.gpu.Cursor
import me.anno.input.Key
import me.anno.maths.Maths.clamp
import me.anno.ui.Style
import me.anno.ui.Window
import me.anno.ui.base.scrolling.Scrollbar
import me.anno.utils.structures.arrays.FloatArrayList
import me.anno.utils.structures.arrays.IntArrayList
import kotlin.math.max
import kotlin.math.min

/**
 * Table layout where children are sized by weights.
 * */
open class WeightBasedTablePanel(sizeX: Int, sizeY: Int, style: Style) :
    TablePanel(sizeX, sizeY, style) {

    private val scrollbarsX = ArrayList<Scrollbar>()
    private val scrollbarsY = ArrayList<Scrollbar>()

    // weights along x/y-axis
    val weightsX = FloatArrayList(sizeX)
    val weightsY = FloatArrayList(sizeY)

    init {
        for (i in 1 until sizeX) {
            scrollbarsX.add(Scrollbar(this, style))
        }
        for (i in 1 until sizeY) {
            scrollbarsY.add(Scrollbar(this, style))
        }
        weightsX.size = sizeX
        weightsY.size = sizeY
        weightsX.fill(1f / sizeX)
        weightsY.fill(1f / sizeY)
    }

    override fun calculateSize(w: Int, h: Int) {
        super.calculateSize(w, h)

        val sizeX = sizeX
        val sizeY = sizeY

        val minWs = minWs
        val minHs = minHs
        minWs.size = sizeX + 1
        minHs.size = sizeY + 1
        minWs.fill(0)
        minHs.fill(0)

        val perWeightX = (w - totalSpacingX) / weightsX.sum()
        val perWeightY = (h - totalSpacingY) / weightsY.sum()
        repeat(sizeY) { y ->
            val sizeYGuess = (weightsY[y] * perWeightY).toInt()
            repeat(sizeX) { x ->
                val child = children[getIndex(x, y)]
                if (child != placeholder) {
                    val sizeXGuess = (weightsX[x] * perWeightX).toInt()
                    child.calculateSize(sizeXGuess, sizeYGuess) // for its children
                    minWs[x] = max(minWs[x], child.minW)
                    minHs[y] = max(minHs[y], child.minH)
                }
            }
        }

        minW = minWs.sum().toInt() + totalSpacingX
        minH = minHs.sum().toInt() + totalSpacingY
    }

    override fun placeChildren(x: Int, y: Int, width: Int, height: Int) {
        placeChildrenAxis(width, sizeX, xs, weightsX, padding.left, padding.right)
        placeChildrenAxis(height, sizeY, ys, weightsY, padding.top, padding.bottom)
        super.placeChildren(x, y, width, height)
    }

    private fun placeChildrenAxis(
        totalSize: Int, sizeI: Int,
        dst: IntArrayList, weights: FloatArrayList,
        paddingLeft: Int, paddingRight: Int
    ) {

        weights.size = sizeI

        val spacing = spacing
        val availableW = totalSize - (sizeI - 1) * spacing - (paddingLeft + paddingRight)
        val perWeightX = availableW / weights.sum()

        var sumSize = 0f
        for (i in 0..sizeI) {
            dst[i] = paddingLeft + i * spacing + sumSize.toInt()
            sumSize += weights[i] * perWeightX
        }
    }

    override fun updateChildrenVisibility(mx: Int, my: Int, canBeHovered: Boolean, x0: Int, y0: Int, x1: Int, y1: Int) {
        super.updateChildrenVisibility(mx, my, canBeHovered, x0, y0, x1, y1)
        for (i in scrollbarsX.indices) {
            scrollbarsX[i].updateVisibility(mx, my, canBeHovered, x0, y0, x1, y1)
        }
        for (i in scrollbarsY.indices) {
            scrollbarsY[i].updateVisibility(mx, my, canBeHovered, x0, y0, x1, y1)
        }
    }

    override fun draw(x0: Int, y0: Int, x1: Int, y1: Int) {
        super.draw(x0, y0, x1, y1)
        val window = window!!
        val padding = interactionPadding
        for (xi in scrollbarsX.indices) {
            val xc = x + xs[xi + 1]
            val x0i = max(x0, xc - spacing)
            val x1i = min(x1, xc)
            val sb = scrollbarsX[xi]
            sb.isHovered = containsMouse(window, x0i - padding, y0, x1i + padding, y1)
            sb.updateAlpha()
            sb.draw(x0i, y0, x1i, y1)
        }
        for (yi in scrollbarsY.indices) {
            val yc = y + ys[yi + 1]
            val y0i = max(y0, yc - spacing)
            val y1i = min(y1, yc)
            val sb = scrollbarsY[yi]
            sb.isHovered = containsMouse(window, x0 - padding, y0i, x1 + padding, y1i)
            sb.updateAlpha()
            sb.draw(x0, y0i, x1, y1i)
        }
    }

    fun containsMouse(window: Window, x0: Int, y0: Int, x1: Int, y1: Int): Boolean {
        return window.mouseXi in x0 until x1 && window.mouseYi in y0 until y1
    }

    fun sx() = scrollbarsX.indexOfFirst { it.isHovered }
    fun sy() = scrollbarsY.indexOfFirst { it.isHovered }

    override fun getCursor(): Cursor? {
        val sx = sx() >= 0
        val sy = sy() >= 0
        return when {
            sx && sy -> Cursor.resize
            sx -> Cursor.hResize
            sy -> Cursor.vResize
            else -> super.getCursor()
        }
    }

    var isDownIndexX = -1
    var isDownIndexY = -1

    override fun onKeyDown(x: Float, y: Float, key: Key) {
        if (key == Key.BUTTON_LEFT) {
            isDownIndexX = sx()
            isDownIndexY = sy()
        } else super.onKeyDown(x, y, key)
    }

    override fun onKeyUp(x: Float, y: Float, key: Key) {
        isDownIndexX = -1
        isDownIndexY = -1
        super.onKeyUp(x, y, key)
    }

    private fun move(idx: Int, weights: FloatArrayList, dx: Float): Float {
        if (idx !in 0 until weights.size) return dx
        // clamp, and roll-over-to-next
        val dxi = clamp(dx, -weights[idx], weights[idx + 1])
        weights[idx] += dxi
        weights[idx + 1] -= dxi
        val newDx = dx - dxi
        return if (newDx != 0f) move(idx + (if (dx > 0f) 1 else -1), weights, dx) else 0f
    }

    override fun onMouseMoved(x: Float, y: Float, dx: Float, dy: Float) {
        val w = max(width - totalSpacingX, 1)
        val h = max(height - totalSpacingY, 1)
        val rx = move(isDownIndexX, weightsX, dx / w) * w
        val ry = move(isDownIndexY, weightsY, dy / h) * h
        super.onMouseMoved(x, y, rx, ry)
    }
}