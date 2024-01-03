package me.anno.ui.base.groups

import me.anno.ui.Panel
import me.anno.ui.Style
import me.anno.ui.base.components.AxisAlignment
import me.anno.utils.structures.arrays.ExpandingFloatArray
import me.anno.utils.structures.arrays.ExpandingIntArray
import kotlin.math.max
import kotlin.math.min

open class TablePanel(sizeX: Int, sizeY: Int, style: Style) : PanelGroup(style) {

    val nothing = Panel(style)

    final override val children = ArrayList<Panel>(Math.multiplyExact(sizeX, sizeY))

    init {
        for (i in 0 until sizeX * sizeY) {
            children.add(nothing)
        }
        nothing.parent = this
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
                        if (child !== nothing) {
                            child.destroy()
                            children[oi] = nothing
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
                val size = Math.multiplyExact(value, sizeY)
                children.ensureCapacity(size)
                for (i in children.size until size) children.add(nothing)
                // move children from old positions into new ones
                for (y in sizeY - 1 downTo 1) { // last row is already perfect
                    for (x in field - 1 downTo 0) {
                        val oi = x + y * field
                        val ni = x + y * value
                        children[ni] = children[oi]
                        children[oi] = nothing
                    }
                }
                field = value
            }
        }

    var sizeY: Int = sizeY
        set(value) {
            field = value
            val size = Math.multiplyExact(sizeX, sizeY)
            children.ensureCapacity(size)
            for (i in children.size until size) {
                children.add(nothing)
            }
            if (children.size > size) {
                // this works apparently
                children.subList(size, children.size).clear()
            }
        }

    operator fun set(x: Int, y: Int, child: Panel) {
        if (x !in 0 until sizeX || y !in 0 until sizeY) throw IndexOutOfBoundsException()
        children[x + y * sizeX] = child
        invalidateLayout()
    }

    open fun getAvailableSizeX(xi: Int, yi: Int, w: Int, h: Int, child: Panel): Int {
        return w
    }

    open fun getAvailableSizeY(xi: Int, yi: Int, w: Int, h: Int, child: Panel): Int {
        return h
    }

    val xs = ExpandingIntArray(sizeX)
    val ys = ExpandingIntArray(sizeY)

    // weights along x/y-axis
    val wxs = ExpandingFloatArray(sizeX)
    val wys = ExpandingFloatArray(sizeY)

    override fun calculateSize(w: Int, h: Int) {
        super.calculateSize(w, h)
        val sizeX = sizeX
        val sizeY = sizeY
        for (y in 0 until sizeY) {
            for (x in 0 until sizeX) {
                val child = children[x + y * sizeX]
                if (child === nothing) continue
                val w2 = getAvailableSizeX(x, y, w, h, child)
                val h2 = getAvailableSizeY(x, y, w, h, child)
                child.calculateSize(w2, h2)
                child.width = min(w2, child.minW)
                child.height = min(h2, child.minH)
            }
        }
        var sumW = 0
        var sumH = 0
        xs.ensureCapacity(sizeX)
        ys.ensureCapacity(sizeY)
        for (x in 0 until sizeX) {
            var maxW = 0
            for (y in 0 until sizeY) {
                val child = children[x + y * sizeX]
                if (child === nothing) continue
                maxW = max(maxW, child.width)
            }
            sumW += maxW
            xs[x] = sumW
        }
        for (y in 0 until sizeY) {
            var maxH = 0
            for (x in 0 until sizeX) {
                val child = children[x + y * sizeX]
                if (child === nothing) continue
                maxH = max(maxH, child.height)
            }
            sumH += maxH
            ys[y] = sumH
        }

        if (alignmentX == AxisAlignment.FILL && sumW < w) {
            wxs.ensureCapacity(sizeX)
            wxs.size = sizeX

            val s = wxs.sum()
            if (s <= 0f) wxs.fill(1f / sizeX)
            else wxs.scale(1f / s)

            val total = w - sumW + 0.5f
            var sum = 0f
            for (x in 0 until sizeX) {
                sum += wxs[x]
                xs[x] += (total * sum).toInt()
            }
            minW = w
        }

        if (alignmentY == AxisAlignment.FILL && sumH < h) {
            wys.ensureCapacity(sizeY)
            wys.size = sizeY

            val s = wys.sum()
            if (s <= 0f) wys.fill(1f / sizeY)
            else wys.scale(s)

            val total = h - sumH + 0.5f
            var sum = 0f
            for (y in 0 until sizeY) {
                sum += wys[y]
                ys[y] += (total * sum).toInt()
            }
            minH = h
        }

        minW = sumW
        minH = sumH
    }

    override fun setPosition(x: Int, y: Int) {
        super.setPosition(x, y)
        nothing.setPosSize(x, y, 0, 0)
        for (yi in 0 until sizeY) {
            val y0 = ys.getOrNull(yi - 1) ?: 0
            val y1 = ys.getOrNull(yi) ?: height
            for (xi in 0 until sizeX) {
                val child = children[xi + yi * sizeX]
                if (child === nothing) continue
                val x0 = xs.getOrNull(xi - 1) ?: 0
                val x1 = xs.getOrNull(xi) ?: width
                val w = x1 - x0
                val h = y1 - y0
                val cw = child.alignmentX.getSize(w, child.minW)
                val ch = child.alignmentY.getSize(h, child.minH)
                val cx = child.alignmentX.getOffset(w, child.minW)
                val cy = child.alignmentY.getOffset(h, child.minH)
                child.setPosSize(cx, cy, cw, ch)
            }
        }
    }

    override fun remove(child: Panel) {
        if (child === nothing) return
        val ix = children.indexOf(child)
        if (ix >= 0) {
            children[ix] = nothing
            invalidateLayout()
        }
    }

    override fun invalidateLayout() {
        window?.addNeedsLayout(this)
    }
}