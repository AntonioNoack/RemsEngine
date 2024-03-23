package me.anno.ui.base.groups

import me.anno.maths.Maths
import me.anno.ui.Panel
import me.anno.ui.Style
import me.anno.ui.base.components.AxisAlignment
import me.anno.utils.structures.arrays.FloatArrayList
import me.anno.utils.structures.arrays.IntArrayList
import kotlin.math.max
import kotlin.math.min

open class TablePanel(sizeX: Int, sizeY: Int, style: Style) : PanelGroup(style) {

    private val placeholder = Panel(style)

    final override val children = ArrayList<Panel>(Maths.multiplyExact(sizeX, sizeY))

    init {
        for (i in 0 until sizeX * sizeY) {
            children.add(placeholder)
        }
        placeholder.parent = this
    }

    var sizeX: Int = sizeX
        set(value) {
            if (field > value) {
                // size down
                // destroy old children
                for (y in 0 until sizeY) {
                    for (x in value until field) {
                        val oi = x + y * field
                        val child = children[oi]
                        if (child !== placeholder) {
                            child.destroy()
                            children[oi] = placeholder
                        }
                    }
                }
                // move children from old positions into new ones
                for (y in 1 until sizeY) {
                    for (x in 0 until value) {
                        val oi = x + y * field
                        val ni = x + y * value
                        children[ni] = children[oi]
                    }
                }
                children.subList(value * sizeY, children.size).clear()
                field = value
            } else if (field < value) {
                // size up
                val size = Maths.multiplyExact(value, sizeY)
                children.ensureCapacity(size)
                for (i in children.size until size) children.add(placeholder)
                // move children from old positions into new ones
                for (y in sizeY - 1 downTo 1) { // last row is already perfect
                    for (x in field - 1 downTo 0) {
                        val oi = x + y * field
                        val ni = x + y * value
                        children[ni] = children[oi]
                        children[oi] = placeholder
                    }
                }
                field = value
            }
        }

    var sizeY: Int = sizeY
        set(value) {
            field = value
            val size = Maths.multiplyExact(sizeX, sizeY)
            children.ensureCapacity(size)
            for (i in children.size until size) {
                children.add(placeholder)
            }
            if (children.size > size) {
                // this works apparently
                children.subList(size, children.size).clear()
            }
        }

    operator fun get(x: Int, y: Int): Panel {
        return if (x in 0 until sizeX && y in 0 until sizeY) {
            children[getIndex(x, y)]
        } else placeholder
    }

    operator fun set(x: Int, y: Int, child: Panel) {
        if (x in 0 until sizeX && y in 0 until sizeY) {
            children[getIndex(x, y)] = child
            invalidateLayout()
        }
    }

    open fun getAvailableSizeX(xi: Int, yi: Int, w: Int, h: Int, child: Panel): Int {
        return w
    }

    open fun getAvailableSizeY(xi: Int, yi: Int, w: Int, h: Int, child: Panel): Int {
        return h
    }

    val xs = IntArrayList(sizeX + 1)
    val ys = IntArrayList(sizeY + 1)

    // weights along x/y-axis
    val wxs = FloatArrayList(sizeX)
    val wys = FloatArrayList(sizeY)

    private fun getIndex(x: Int, y: Int): Int {
        return x + y * sizeX
    }

    private fun calculateChildSizes(w: Int, h: Int) {
        for (y in 0 until sizeY) {
            for (x in 0 until sizeX) {
                val child = children[getIndex(x, y)]
                if (child === placeholder) continue
                val w2 = getAvailableSizeX(x, y, w, h, child)
                val h2 = getAvailableSizeY(x, y, w, h, child)
                child.calculateSize(w2, h2)
                child.width = min(w2, child.minW)
                child.height = min(h2, child.minH)
            }
        }
    }

    override fun calculateSize(w: Int, h: Int) {
        super.calculateSize(w, h)

        val sizeX = sizeX
        val sizeY = sizeY
        calculateChildSizes(w, h)

        var sumW = 0
        var sumH = 0
        xs.size = sizeX + 1
        ys.size = sizeY + 1
        wxs.size = sizeX
        wys.size = sizeY

        placeholder.width = 0
        placeholder.height = 0

        xs[0] = 0
        for (x in 0 until sizeX) {
            var maxW = 0
            for (y in 0 until sizeY) {
                maxW = max(maxW, this[x, y].width)
            }
            sumW += maxW
            xs[x + 1] = sumW
        }

        ys[0] = 0
        for (y in 0 until sizeY) {
            var maxH = 0
            for (x in 0 until sizeX) {
                maxH = max(maxH, this[x, y].height)
            }
            sumH += maxH
            ys[y + 1] = sumH
        }

        minW = sumW
        minH = sumH

        if (alignmentX == AxisAlignment.FILL && sumW < w) {
            distributeExtra(w - sumW, xs, wxs)
            minW = w
        }

        if (alignmentY == AxisAlignment.FILL && sumH < h) {
            distributeExtra(h - sumH, ys, wys)
            minH = h
        }
    }

    private fun distributeExtra(diff: Int, positions: IntArrayList, weights: FloatArrayList) {
        normalizeWeights(weights)
        val total = diff + 0.5f
        var sum = 0f
        for (i in 0 until weights.size) {
            sum += weights[i]
            positions[i + 1] += (total * sum).toInt()
        }
    }

    private fun normalizeWeights(weights: FloatArrayList) {
        val sum = weights.sum()
        if (sum <= 0f) weights.fill(1f / weights.size)
        else weights.scale(1f / sum)
    }

    override fun setPosition(x: Int, y: Int) {
        super.setPosition(x, y)
        placeholder.setPosSize(x, y, 0, 0)
        for (yi in 0 until sizeY) {
            val y0 = ys[yi]
            val y1 = ys[yi + 1]
            for (xi in 0 until sizeX) {
                val child = children[xi + yi * sizeX]
                if (child === placeholder) continue
                val x0 = xs[xi]
                val x1 = xs[xi + 1]
                val w = x1 - x0
                val h = y1 - y0
                val alignmentX = child.alignmentX
                val alignmentY = child.alignmentY
                val cw = alignmentX.getSize(w, child.minW)
                val ch = alignmentY.getSize(h, child.minH)
                val cx = x + x0 + alignmentX.getOffset(w, child.minW)
                val cy = y + y0 + alignmentY.getOffset(h, child.minH)
                child.setPosSize(cx, cy, cw, ch)
            }
        }
    }

    override fun remove(child: Panel) {
        if (child === placeholder) return
        val ix = children.indexOf(child)
        if (ix >= 0) {
            children[ix] = placeholder
            invalidateLayout()
        }
    }

    override fun invalidateLayout() {
        window?.addNeedsLayout(this)
    }
}