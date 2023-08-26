package me.anno.graph.octtree

/**
 * a generic node for quad trees and oct trees, or more/fewer dimensions;
 * just specify a different type of point
 *
 * the Any can be a single element (not splittable/joinable),
 * or a list, which is split at a certain size
 *
 * it also could be a dual list, e.g., list of positions, and triangle indices
 *
 * modifications must be in order, queries can be multi-threaded
 * */
abstract class KdTree<Point, Data>(
    val maxNumChildren: Int,
    var min: Point, var max: Point
) : Iterable<Data> {

    var axis = 0
    val parent: KdTree<Point, Data>? = null

    override fun toString(): String {
        val bld = StringBuilder()
        append(0, bld)
        return bld.toString()
    }

    open fun append(depth: Int, dst: StringBuilder) {
        for (i in 0 until depth) dst.append(' ')
        if (left != null) {
            dst.append("[$min, $max] | $axis:\n")
            left?.append(depth + 1, dst)
            right?.append(depth + 1, dst)
        } else dst.append("[$min, $max]: $children\n")
    }

    abstract fun chooseSplitDimension(min: Point, max: Point): Int
    abstract fun overlaps(min0: Point, max0: Point, min1: Point, max1: Point): Boolean

    open fun getPoint(data: Data): Point = getMin(data)
    open fun getMin(data: Data): Point = getPoint(data)
    open fun getMax(data: Data): Point = getPoint(data)

    abstract fun get(p: Point, axis: Int): Double
    abstract fun min(a: Point, b: Point): Point
    abstract fun max(a: Point, b: Point): Point
    open fun createChild(
        children: ArrayList<Data>,
        min: Point,
        max: Point
    ): KdTree<Point, Data> {
        val child = createChild()
        child.children = children
        child.min = min
        child.max = max
        return child
    }

    abstract fun createChild(): KdTree<Point, Data>

    var left: KdTree<Point, Data>? = null
    var right: KdTree<Point, Data>? = null
    var size = 0

    var children: ArrayList<Data>? = null

    fun add(d: Data) {

        val minI = getMin(d)
        val maxI = getMax(d)
        val px = get(minI, axis)

        min = min(min, minI)
        max = max(max, maxI)

        // if already has children, add it to them...
        if (left != null) {
            val left = left!!
            val right = right!!
            if (px <= get(left.max, axis)) {
                // add to left
                left.add(d)
            } else if (px >= get(right.min, axis)) {
                // add to right
                right.add(d)
            } else {
                // add to which has fewer children
                if (left.size < right.size) {
                    left.add(d)
                } else {
                    right.add(d)
                }
            }
        } else {

            if (children == null) children = ArrayList(maxNumChildren)
            val children = children!!

            // split: evenly partition children into left/self/right
            if (children.size >= maxNumChildren) {

                this.children = null
                val axis = chooseSplitDimension(min, max)
                children.sortBy { get(getPoint(it), axis) }
                this.axis = axis

                val median = (children.size - 1) / 2
                val leftMax = children[median]
                val rightMin = children[median + 1]

                var lMin = getMin(leftMax)
                var lMax = getMax(leftMax)

                var rMin = getMin(rightMin)
                var rMax = getMax(rightMin)

                // calculate bounds and split up children
                val rightChildren = ArrayList<Data>(maxNumChildren)
                for (i in children.size - 1 downTo median + 1) {
                    val child = children.removeAt(i)
                    rightChildren.add(child)
                    rMin = min(rMin, getMin(child))
                    rMax = max(rMax, getMax(child))
                }

                for (i in children.indices) {
                    val child = children[i]
                    lMin = min(lMin, getMin(child))
                    lMax = max(lMax, getMax(child))
                }

                left = createChild(children, lMin, lMax)
                right = createChild(rightChildren, rMin, rMax)

                return add(d)
            } else children.add(d)
        }

        size++
    }

    fun query(min: Point, max: Point, hasFound: (Data) -> Boolean): Boolean {
        val left = left
        val minV = get(min, axis)
        val maxV = get(max, axis)
        if (left != null && get(left.max, axis) >= minV) {
            if (left.query(min, max, hasFound)) return true
        }
        val right = right
        if (right != null && get(right.min, axis) <= maxV) {
            if (right.query(min, max, hasFound)) return true
        }
        val children = children
        if (children != null) {
            for (i in children.indices) {
                val d = children.getOrNull(i) ?: break
                val minI = getMin(d)
                val maxI = getMax(d)
                if (overlaps(min, max, minI, maxI)) {
                    if (hasFound(d)) return true
                }
            }
        }
        return false
    }

    fun find(hasFound: (Data) -> Boolean): Boolean {
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

    fun sum(partialSum: (Data) -> Long): Long {
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

    override fun iterator(): Iterator<Data> =
        sequence {
            val left = left
            val right = right
            if (left != null && right != null) {
                yieldAll(left.iterator())
                yieldAll(right.iterator())
            }
            val children = children
            if (children != null) {
                yieldAll(children.iterator())
            }
        }.iterator()

    fun remove(d: Data, minI: Point = getMin(d), maxI: Point = getMax(d)): Boolean {
        val left = left
        if (left != null) {
            val right = right!!
            if (left.overlaps(left.min, left.max, minI, maxI)) {
                if (left.remove(d)) {
                    size--
                    recalculateBounds()
                    return true
                }
            }
            if (right.overlaps(right.min, right.max, minI, maxI)) {
                if (right.remove(d)) {
                    size--
                    recalculateBounds()
                    return true
                }
            }
        } else {
            val children = children
            if (children != null && children.remove(d)) {
                // todo if children is empty, or |left|+|right| <= maxNumChildren/2
                val parent = parent
                if (parent != null) {
                    if (children.isEmpty()) {
                        // join with partner
                        parent.join()
                    }
                }
                // update bounds
                size--
                recalculateBounds()
                return false
            }
        }
        return false
    }

    private fun recalculateBounds() {
        val children = children ?: return
        val d0 = children.firstOrNull() ?: return
        var min = getMin(d0)
        var max = getMax(d0)
        for (i in 1 until children.size) {
            val child = children[i]
            min = min(min, getMin(child))
            max = max(max, getMax(child))
        }
        this.min = min
        this.max = max
    }

    private fun join() {
        val left = left!!
        val right = right!!
        if (left.size == 0) {
            // copy states from right
            copyFrom(left)
        } else if (right.size == 0) {
            // copy states from left
            copyFrom(right)
        } else throw IllegalStateException()
    }

    private fun copyFrom(src: KdTree<Point, Data>) {
        left = src.left
        right = src.right
        children = src.children
        size = src.size
        min = src.min
        max = src.max
        axis = src.axis
    }

    fun clear() {
        left = null
        right = null
        children?.clear()
    }
}