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
 * modifications must be in order, queries can be multithreaded
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
    abstract fun contains(min: Point, max: Point, x: Point): Boolean
    abstract fun getPoint(data: Data): Point
    abstract fun get(p: Point, axis: Int): Double
    abstract fun min(a: Point, b: Point): Point
    abstract fun max(a: Point, b: Point): Point
    abstract fun createChild(
        children: ArrayList<Data>,
        min: Point,
        max: Point
    ): KdTree<Point, Data>

    var left: KdTree<Point, Data>? = null
    var right: KdTree<Point, Data>? = null
    var size = 0

    var children: ArrayList<Data>? = null

    fun add(d: Data) {

        val p = getPoint(d)
        val px = get(p, axis)

        min = min(min, p)
        max = max(max, p)

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

                var lMin = getPoint(leftMax)
                var lMax = lMin

                var rMin = getPoint(rightMin)
                var rMax = rMin

                // calculate bounds and split up children
                val rightChildren = ArrayList<Data>(maxNumChildren)
                for (i in children.size - 1 downTo median + 1) {
                    val rc = children.removeAt(i)
                    rightChildren.add(rc)
                    val rp = getPoint(rc)
                    rMin = min(rMin, rp)
                    rMax = max(rMax, rp)
                }

                for (i in children.indices) {
                    val lc = children[i]
                    val lp = getPoint(lc)
                    lMin = min(lMin, lp)
                    lMax = max(lMax, lp)
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
                val p = getPoint(d)
                val x = get(p, axis)
                if (x in minV..maxV && contains(min, max, p)) {
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

    // todo test this
    fun remove(d: Data): Boolean {
        val left = left
        if (left != null) {
            val right = right!!
            val p = getPoint(d)
            val px = get(p, axis)
            if (px <= get(left.max, axis)) {
                if (left.remove(d)) {
                    size--
                    // todo does this still work after joining?
                    min = min(left.min, right.min)
                    max = max(left.max, right.max)
                    return true
                }
            }
            if (px >= get(right.min, axis)) {
                if (right.remove(d)) {
                    // todo does this still work after joining?
                    size--
                    min = min(left.min, right.min)
                    max = max(left.max, right.max)
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
                val d0 = children.firstOrNull()
                if (d0 != null) {
                    val p0 = getPoint(d0)
                    min = p0
                    max = p0
                    for (i in 1 until children.size) {
                        val di = children[i]
                        val pi = getPoint(di)
                        min = min(min, pi)
                        max = max(max, pi)
                    }
                }
                size--
                return false
            }
        }
        return false
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

}