package me.anno.ui.base.groups

import me.anno.ui.Panel
import me.anno.ui.style.Style
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

    // todo alignment and weight options

    val xs = ExpandingIntArray(sizeX)
    val ys = ExpandingIntArray(sizeY)

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
                child.w = min(w2, child.minW)
                child.h = min(h2, child.minH)
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
                maxW = max(maxW, child.w)
            }
            sumW += maxW
            xs[x] = sumW
        }
        for (y in 0 until sizeY) {
            var maxH = 0
            for (x in 0 until sizeX) {
                val child = children[x + y * sizeX]
                if (child === nothing) continue
                maxH = max(maxH, child.h)
            }
            sumH += maxH
            ys[y] = sumH
        }
        minW = sumW
        minH = sumH
    }

    override fun setPosition(x: Int, y: Int) {
        super.setPosition(x, y)
        nothing.setPosSize(x, y, 0, 0)
        for (yi in 0 until sizeY) {
            val y0 = ys.getOrNull(yi - 1) ?: 0
            val y1 = ys.getOrNull(yi) ?: h
            for (xi in 0 until sizeX) {
                val child = children[xi + yi * sizeX]
                if (child === nothing) continue
                val x0 = xs.getOrNull(xi - 1) ?: 0
                val x1 = xs.getOrNull(xi) ?: w
                child.calculatePlacement(x + x0, y + y0, x1 - x0, y1 - y0)
                child.setPosSize(child.minX, child.minY, child.minW, child.minH)
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
        window?.needsLayout?.add(this)
    }

}