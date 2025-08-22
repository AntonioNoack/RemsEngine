package me.anno.ui.base.groups

import me.anno.maths.Maths
import me.anno.ui.Panel
import me.anno.ui.Style
import me.anno.ui.base.components.Padding
import me.anno.utils.structures.arrays.IntArrayList

/**
 * Use one of the following implementations: MinSizeTablePanel, WeightBasedTablePanel, or implement your own.
 * */
abstract class TablePanel(sizeX: Int, sizeY: Int, style: Style) : PanelGroup(style) {

    val placeholder = Panel(style)
    final override val children = ArrayList<Panel>(Maths.multiplyExact(sizeX, sizeY))

    val xs = IntArrayList(sizeX + 1)
    val ys = IntArrayList(sizeY + 1)
    val minWs = IntArrayList(sizeX + 1)
    val minHs = IntArrayList(sizeY + 1)

    var spacing = style.getSize("customList.spacing", 4)
    val padding = Padding(0)

    val totalSpacingX get() = (sizeX - 1) * spacing + padding.width
    val totalSpacingY get() = (sizeY - 1) * spacing + padding.height

    init {
        for (i in 0 until sizeX * sizeY) {
            children.add(placeholder)
        }
        placeholder.parent = this
        placeholder.isVisible = false
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
        }
    }

    fun getIndex(x: Int, y: Int): Int {
        return x + y * sizeX
    }

    override fun placeChildren(x: Int, y: Int, width: Int, height: Int) {
        placeholder.setPosSize(x, y, 0, 0)
        for (yi in 0 until sizeY) {
            val y0 = ys[yi]
            val y1 = ys[yi + 1] - spacing
            for (xi in 0 until sizeX) {
                val child = children[xi + yi * sizeX]
                if (!child.isVisible) continue
                val x0 = xs[xi]
                val x1 = xs[xi + 1] - spacing
                child.setPosSizeAligned(
                    x + x0, y + y0,
                    x1 - x0, y1 - y0
                )
            }
        }
    }

    override fun remove(child: Panel) {
        if (child === placeholder) return
        val idx = children.indexOf(child)
        if (idx >= 0) children[idx] = placeholder
    }
}