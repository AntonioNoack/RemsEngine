package me.anno.graph.octtree

import me.anno.utils.algorithms.Recursion
import me.anno.utils.assertions.assertNull

/**
 * a generic node for quad trees and oct trees, or more/fewer dimensions;
 * just specify a different type of point
 *
 * the Any can be a single element (not splittable/joinable),
 * or a list, which is split at a certain size
 *
 * it also could be a dual list, e.g., list of positions, and triangle indices
 *
 * modifications must be in order, queries can be multithreaded
 * */
abstract class KdTree<Point, Value>(
    val maxNumChildren: Int,
    var min: Point, var max: Point
) {

    var axis = 0
    val parent: KdTree<Point, Value>? = null

    override fun toString(): String {
        val bld = StringBuilder()
        append(0, bld)
        return bld.toString()
    }

    open fun append(depth: Int, dst: StringBuilder) {
        repeat(depth) { dst.append(' ') }
        if (left != null) {
            dst.append("[$min, $max] | $axis:\n")
            left?.append(depth + 1, dst)
            right?.append(depth + 1, dst)
        } else dst.append("[$min, $max]: $children\n")
    }

    abstract fun chooseSplitDimension(min: Point, max: Point): Int
    abstract fun overlaps(min0: Point, max0: Point, min1: Point, max1: Point): Boolean

    open fun getPoint(data: Value): Point = getMin(data)
    open fun getMin(data: Value): Point = getPoint(data)
    open fun getMax(data: Value): Point = getPoint(data)

    abstract fun get(p: Point, axis: Int): Double
    abstract fun min(a: Point, b: Point, dst: Point): Point
    abstract fun max(a: Point, b: Point, dst: Point): Point
    abstract fun copy(a: Point): Point

    open fun createChild(
        children: ArrayList<Value>,
        min: Point, max: Point
    ): KdTree<Point, Value> {
        val child = createChild()
        child.children = children
        child.size = children.size
        child.min = min
        child.max = max
        return child
    }

    abstract fun createChild(): KdTree<Point, Value>
    open fun destroyChild(child: KdTree<Point, Value>) {}

    open fun createList(): ArrayList<Value> = ArrayList(maxNumChildren)
    open fun destroyList(list: ArrayList<Value>) {}

    var left: KdTree<Point, Value>? = null
    var right: KdTree<Point, Value>? = null
    var size = 0

    var children: ArrayList<Value>? = null

    fun add(value: Value) {
        var node = this
        val minI = node.getMin(value)
        val maxI = node.getMax(value)
        while (true) {

            node.min = min(node.min, minI, node.min)
            node.max = max(node.max, maxI, node.max)
            node.size++

            // if already has children, add it to them...
            if (node.left != null) {
                val px = node.get(minI, node.axis)
                node = node.findBestSideForPoint(px)
            } else {
                val children = node.ensureChildren()
                if (children.size >= node.maxNumChildren) {
                    node.split(children)
                    assertNull(node.children)
                    val px = node.get(minI, node.axis)
                    node = node.findBestSideForPoint(px)
                } else {
                    // finally done :)
                    children.add(value)
                    return
                }
            }
        }
    }

    fun addAll(values: List<Value>) {
        if (size > 0) {
            val children = children
            if (children != null) {
                children.addAll(values)
                size = children.size
                recalculateBoundsChildren()
                splitIfTooBig()
            } else {
                // cannot optimize that easily -> use the trivial algorithm
                for (i in values.indices) {
                    add(values[i])
                }
            }
        } else {
            // optimized variant :)
            val tmp = createList()
            tmp.addAll(values)
            children = tmp
            size = tmp.size
            recalculateBoundsChildren()
            splitIfTooBig()
        }
    }

    private fun splitIfTooBig() {
        if (size > maxNumChildren) {
            split(children!!)
            left!!.splitIfTooBig()
            right!!.splitIfTooBig()
        }
    }

    private fun ensureChildren(): ArrayList<Value> {
        var children = children
        if (children == null) {
            children = createList()
            this.children = children
        }
        return children
    }

    private fun findBestSideForPoint(px: Double): KdTree<Point, Value> {
        val left = left!!
        val right = right!!
        return when {
            px <= get(left.max, axis) -> left
            px >= get(right.min, axis) -> right
            // add to which has fewer children
            left.size < right.size -> left
            else -> right
        }
    }

    private fun getValue(value: Value, axis: Int): Double {
        return get(getPoint(value), axis)
    }

    /**
     * evenly partition children into left/self/right
     * */
    private fun split(children: ArrayList<Value>) {

        this.children = null // passed to the left child -> recycling not necessary
        val axis = chooseSplitDimension(min, max)
        children.sortWith { a, b ->
            getValue(a, axis)
                .compareTo(getValue(b, axis))
        }
        this.axis = axis

        val median = (children.size - 1).shr(1)
        val leftMax = children[median]
        val rightMin = children[median + 1]

        // todo this copying of vectors could be prevented...
        var lMin = copy(getMin(leftMax))
        var lMax = copy(getMax(leftMax))

        var rMin = copy(getMin(rightMin))
        var rMax = copy(getMax(rightMin))

        // calculate bounds and split up children
        val rightChildren = createList()
        repeat(children.size - (median + 1)) {
            val child = children.removeLast()
            rightChildren.add(child)
            rMin = min(rMin, getMin(child), rMin)
            rMax = max(rMax, getMax(child), rMax)
        }

        for (i in children.indices) {
            val child = children[i]
            lMin = min(lMin, getMin(child), lMin)
            lMax = max(lMax, getMax(child), lMax)
        }

        left = createChild(children, lMin, lMax)
        right = createChild(rightChildren, rMin, rMax)
    }

    /**
     * Calls callback on all overlapping children until true is returned by it.
     * Returns the first (any) child to return true.
     * */
    fun query(min: Point, max: Point, hasFound: (Value) -> Boolean): Value? {
        return Recursion.findRecursive(this) { node, remaining ->
            val left = node.left
            val axis = node.axis
            if (left != null && node.get(left.max, axis) >= node.get(min, axis)) {
                remaining.add(left)
            }
            val right = node.right
            if (right != null && get(right.min, axis) <= node.get(max, axis)) {
                remaining.add(right)
            }
            var found: Value? = null
            val values = node.children
            if (values != null) {
                for (i in values.indices) {
                    val value = values.getOrNull(i) ?: break
                    val minI = node.getMin(value)
                    val maxI = node.getMax(value)
                    if (overlaps(min, max, minI, maxI)) {
                        if (hasFound(value)) {
                            found = value
                            break
                        }
                    }
                }
            }
            found
        }
    }

    /**
     * Calls callback on all overlapping children-lists (and root) until true is returned by it.
     * Returns the first (any) child to return true.
     * */
    @Suppress("unused")
    fun queryLists(min: Point, max: Point, hasFound: (List<Value>) -> Boolean): List<Value>? {
        return Recursion.findRecursive(this) { node, remaining ->
            val left = node.left
            if (left != null && node.get(left.max, axis) >= node.get(min, node.axis)) {
                remaining.add(left)
            }
            val right = node.right
            if (right != null && get(right.min, axis) <= node.get(max, node.axis)) {
                remaining.add(right)
            }
            val values = node.children
            if (values != null && hasFound(values)) values else null
        }
    }

    @Suppress("unused")
    fun containsValue(searched: Value): Boolean {
        return query(getMin(searched), getMax(searched)) { value ->
            value == searched
        } != null
    }

    @Suppress("unused")
    fun containsPoint(point: Point): Boolean {
        return query(point, point) { value ->
            getPoint(value) == point
        } != null
    }

    fun overlaps(other: KdTree<Point, *>): Boolean {
        return overlaps(min, max, other.min, other.max)
    }

    fun overlaps(other: Value): Boolean {
        return overlaps(min, max, getMin(other), getMax(other))
    }

    fun overlaps(valueA: Value, valueB: Value): Boolean {
        return overlaps(
            getMin(valueA), getMax(valueA),
            getMin(valueB), getMax(valueB)
        )
    }

    fun find(hasFound: (Value) -> Boolean): Boolean {
        val left = left
        if (left != null && left.find(hasFound)) return true
        val right = right
        if (right != null && right.find(hasFound)) return true
        val children = children
        if (children != null) {
            for (i in children.indices) {
                if (hasFound(children.getOrNull(i) ?: break)) {
                    return true
                }
            }
        }
        return false
    }

    fun sum(partialSum: (Value) -> Long): Long {
        val left = left
        val right = right
        var sum = if (left != null && right != null) left.sum(partialSum) + right.sum(partialSum) else 0
        val children = children
        if (children != null) {
            for (i in children.indices) {
                sum += partialSum(children.getOrNull(i) ?: break)
            }
        }
        return sum
    }

    fun forEach(action: (Value) -> Unit) {
        Recursion.processRecursive(this) { node, remaining ->
            val children = node.children
            if (children != null) {
                for (i in children.indices) {
                    action(children[i])
                }
            } else {
                val left = node.left
                val right = node.right
                if (left != null && right != null) {
                    remaining.add(left)
                    remaining.add(right)
                }
            }
        }
    }

    /**
     * removes the element, and returns true on success
     * */
    fun remove(d: Value): Boolean {
        return remove(d, getMin(d), getMax(d))
    }

    /**
     * removes the element, and returns true on success
     * */
    fun remove(d: Value, minI: Point, maxI: Point): Boolean {
        val left = left
        val right = right
        if (left != null && right != null) {
            if (overlaps(left.min, left.max, minI, maxI)) {
                if (left.remove(d)) {
                    size--
                    recalculateBoundsLeftRight(left, right)
                    return true
                }
            }
            if (overlaps(right.min, right.max, minI, maxI)) {
                if (right.remove(d)) {
                    size--
                    recalculateBoundsLeftRight(left, right)
                    return true
                }
            }
        } else {
            val children = children
            if (children != null && children.remove(d)) {
                // if |left|+|right| <= maxNumChildren/2, join siblings to reduce tree-height
                val parent = parent
                if (parent != null) {
                    var other = parent.left
                    if (other === this) other = parent.right
                    val otherChildren = other?.children
                    if (other == null || otherChildren == null ||
                        children.size + otherChildren.size <= maxNumChildren shr 1
                    ) {
                        // join with partner
                        parent.join()
                        return true
                    }
                }
                // update bounds
                size--
                recalculateBoundsChildren()
                return true
            }
        }
        return false
    }

    /**
     * returns whether it was found
     * */
    fun update(d: Value, oldMin: Point, oldMax: Point = oldMin): Boolean {
        val left = left
        val right = right
        if (left != null && right != null) {
            if (overlaps(left.min, left.max, oldMin, oldMax) && left.update(d, oldMin, oldMax)) {
                recalculateBoundsLeftRight(left, right)
                return true
            }
            if (overlaps(right.min, right.max, oldMin, oldMax) && right.update(d, oldMin, oldMax)) {
                recalculateBoundsLeftRight(left, right)
                return true
            }
        } else {
            val children = children
            if (children != null && d in children) {
                recalculateBoundsChildren()
                return true
            }
        }
        return false
    }

    /**
     * returns the node, where d is contained
     * */
    fun find(d: Value, oldMin: Point, oldMax: Point = oldMin): KdTree<Point, Value>? {
        val left = left
        val right = right
        if (left != null && right != null) {
            if (overlaps(left.min, left.max, oldMin, oldMax)) {
                val v0 = left.find(d, oldMin, oldMax)
                if (v0 != null) return v0
            }
            if (overlaps(right.min, right.max, oldMin, oldMax)) {
                val v0 = right.find(d, oldMin, oldMax)
                if (v0 != null) return v0
            }
        } else {
            val children = children
            if (children != null && d in children) {
                return this
            }
        }
        return null
    }

    private fun recalculateBoundsChildren() {
        val children = children ?: return
        val d0 = children.firstOrNull() ?: return
        val min0 = getMin(d0)
        val max0 = getMax(d0)
        var min = min(min0, min0, this.min)
        var max = max(max0, max0, this.max)
        for (i in 1 until children.size) {
            val child = children[i]
            min = min(min, getMin(child), min)
            max = max(max, getMax(child), max)
        }
        this.min = min
        this.max = max
    }

    private fun recalculateBoundsLeftRight(left: KdTree<Point, Value>, right: KdTree<Point, Value>) {
        min = min(left.min, right.min, this.min)
        max = max(left.max, right.max, this.max)
    }

    private fun join() {
        val left = left!!
        val right = right!!
        if (left.size == 0) {
            // copy states from right
            copyFrom(right)
            left.clear()
        } else if (right.size == 0) {
            // copy states from left
            copyFrom(left)
            right.clear()
        } else {

            val prevSize = size
            copyFrom(left)

            // why are these guaranteed?
            assertNull(right.left)
            assertNull(right.right)

            val rightChildren = right.children!!
            children!!.addAll(rightChildren)
            size = prevSize
            recalculateBoundsLeftRight(left, right)

            // clear right
            rightChildren.clear()
            destroyList(rightChildren)
            right.children = null
        }

        destroyChild(left)
        destroyChild(right)
    }

    private fun copyFrom(src: KdTree<Point, Value>) {
        left = src.left
        right = src.right
        children = src.children
        size = src.size
        min = src.min
        max = src.max
        axis = src.axis

        src.left = null
        src.right = null
        src.children = null
        src.size = 0
    }

    fun clear() {
        children?.clear()

        // recursive deletion of nodes and children lists
        val self = this
        Recursion.processRecursive(self) { node, remaining ->
            val left = node.left
            if (left != null) {
                remaining.add(left)
                node.left = null
            }
            val right = node.right
            if (right != null) {
                remaining.add(right)
                node.right = null
            }
            // finish up destruction by destroying itself
            if (node !== self) {
                val children = node.children
                if (children != null) {
                    children.clear()
                    destroyList(children)
                    node.children = null
                }
                destroyChild(node)
            }
        }
    }
}