package me.anno.ui.base.groups

import me.anno.gpu.Cursor
import me.anno.input.Key
import me.anno.maths.Maths
import me.anno.maths.Maths.clamp
import me.anno.ui.Panel
import me.anno.ui.Style
import me.anno.ui.Window
import me.anno.ui.base.scrolling.Scrollbar
import me.anno.utils.structures.arrays.FloatArrayList
import me.anno.utils.structures.arrays.IntArrayList
import kotlin.math.max
import kotlin.math.min

open class TablePanel(sizeX: Int, sizeY: Int, style: Style) : PanelGroup(style) {

    private val placeholder = Panel(style)

    final override val children = ArrayList<Panel>(Maths.multiplyExact(sizeX, sizeY))
    private val scrollbarsX = ArrayList<Scrollbar>()
    private val scrollbarsY = ArrayList<Scrollbar>()

    val xs = IntArrayList(sizeX + 1)
    val ys = IntArrayList(sizeY + 1)

    // weights along x/y-axis
    val weightsX = FloatArrayList(sizeX)
    val weightsY = FloatArrayList(sizeY)

    var spacing = style.getSize("customList.spacing", 4)

    init {
        for (i in 0 until sizeX * sizeY) {
            children.add(placeholder)
        }
        for (i in 1 until sizeX) {
            scrollbarsX.add(Scrollbar(this, style))
        }
        for (i in 1 until sizeY) {
            scrollbarsY.add(Scrollbar(this, style))
        }
        placeholder.parent = this
        weightsX.size = sizeX
        weightsY.size = sizeY
        weightsX.fill(1f / sizeX)
        weightsY.fill(1f / sizeY)
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

    private fun getIndex(x: Int, y: Int): Int {
        return x + y * sizeX
    }

    // todo bring back size calculation for when space is unlimited
    //  -> calculate actual min size
    override fun calculateSize(w: Int, h: Int) {
        super.calculateSize(w, h)

        val sizeX = sizeX
        val sizeY = sizeY

        val cw = w - (sizeX - 1) * spacing
        val ch = h - (sizeY - 1) * spacing
        for (y in 0 until sizeY) {
            val h2 = (weightsY[y] * ch).toInt()
            for (x in 0 until sizeX) {
                val child = children[getIndex(x, y)]
                val w2 = (weightsX[x] * cw).toInt()
                child.calculateSize(w2, h2) // for its children
                child.width = w2
                child.height = h2
            }
        }

        var sumW = 0
        var sumH = 0
        xs.size = sizeX + 1
        ys.size = sizeY + 1
        weightsX.size = sizeX
        weightsY.size = sizeY

        placeholder.width = 0
        placeholder.height = 0

        xs[0] = 0
        for (x in 0 until sizeX) {
            val wi = (weightsX[x] * cw).toInt() + spacing
            sumW += wi
            xs[x + 1] = sumW
        }

        ys[0] = 0
        for (y in 0 until sizeY) {
            val hi = (weightsY[y] * ch).toInt() + spacing
            sumH += hi
            ys[y + 1] = sumH
        }

        minW = sumW - spacing
        minH = sumH - spacing
    }

    override fun placeChildren(x: Int, y: Int, width: Int, height: Int) {
        placeholder.setPosSize(x, y, 0, 0)
        for (yi in 0 until sizeY) {
            val y0 = ys[yi]
            val y1 = ys[yi + 1] - spacing
            val h = y1 - y0
            for (xi in 0 until sizeX) {
                val child = children[xi + yi * sizeX]
                if (child === placeholder) continue
                val x0 = xs[xi]
                val x1 = xs[xi + 1] - spacing
                val w = x1 - x0
                child.setPosSizeAligned(x + x0, y + y0, w, h)
            }
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

    override fun onDraw(x0: Int, y0: Int, x1: Int, y1: Int) {
        super.onDraw(x0, y0, x1, y1)
        val window = window!!
        val padding = interactionPadding
        for (xi in scrollbarsX.indices) {
            val xc = x + xs[xi + 1]
            val x0i = max(x0, xc - spacing)
            val x1i = min(x1, xc)
            val sb = scrollbarsX[xi]
            sb.isHovered = containsMouse(window, x0i - padding, y0, x1i + padding, y1)
            sb.updateAlpha(this)
            sb.draw(x0i, y0, x1i, y1)
        }
        for (yi in scrollbarsY.indices) {
            val yc = y + ys[yi + 1]
            val y0i = max(y0, yc - spacing)
            val y1i = min(y1, yc)
            val sb = scrollbarsY[yi]
            sb.isHovered = containsMouse(window, x0 - padding, y0i, x1 + padding, y1i)
            sb.updateAlpha(this)
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
        invalidateLayout()
        val newDx = dx - dxi
        return if (newDx != 0f) move(idx + (if (dx > 0f) 1 else -1), weights, dx) else 0f
    }

    override fun onMouseMoved(x: Float, y: Float, dx: Float, dy: Float) {
        val w = max(width - (sizeX - 1) * spacing, 1)
        val h = max(height - (sizeY - 1) * spacing, 1)
        val rx = move(isDownIndexX, weightsX, dx / w) * w
        val ry = move(isDownIndexY, weightsY, dy / h) * h
        super.onMouseMoved(x, y, rx, ry)
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