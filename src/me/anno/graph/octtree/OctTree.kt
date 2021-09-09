package me.anno.graph.octtree

/**
 * a generic node for quad trees and oct trees, or more/less dimensions
 * just specify a different type of point
 *
 * the Any can be a single element (not splittable/joinable),
 * or a list, which is split at a certain size
 *
 * it also could be a dual list, e.g. list of positions, and triangle indices
 * */
abstract class OctTree<Point>(
    val parent: OctTree<Point>? = null,
    val min: Point,
    val max: Point
) {

    abstract fun getIndex(point: Point): Int
    abstract fun trySplit(): SplitResult<Point>?
    abstract fun tryJoin(node: Any): Boolean
    abstract fun split(other: Any): SplitResult<Point>

    var splitPoint: Point? = null
    var hasAny: Boolean = false
    var children: Array<OctTree<Point>?>? = null

    fun iterate(callback: (Any) -> Boolean): Boolean {
        val children = children
        return if (children != null) {
            for (child in children) {
                child ?: continue
                if (child.iterate(callback)) return true
            }
            false
        } else {
            if (hasAny) {
                callback(this as Any)
            } else false
        }
    }

    fun iterate(min: Point, max: Point, callback: (Any) -> Boolean): Boolean {
        val children = children
        return if (children != null) {
            val minIndex = getIndex(min)
            val maxIndex = getIndex(max)
            val or = minIndex or maxIndex
            val and = minIndex and maxIndex
            for (index in children.indices) {
                if (bitsAreInBetween(or, and, index)) {
                    val child = children[index]
                    child ?: continue
                    if (child.iterate(callback)) return true
                }
            }
            false
        } else {
            if (hasAny) {
                callback(this as Any)
            } else false
        }
    }

    abstract fun setContent(newContent: Any)

    fun add(newAny: Any) {
        val children = children
        if (children == null) {
            if (!hasAny) {
                // this is the first Any
                setContent(newAny)
                hasAny = true
            } else {
                if (!tryJoin(newAny)) {
                    // we cannot join them -> we need to split them
                    val result = split(newAny)
                    hasAny = false
                    this.splitPoint = result.splitPoint
                    this.children = result.children
                }
            }
        } else {
            newAny as OctTree<Point>
            val index = newAny.getIndex(splitPoint!!)
            val oldNode = children[index]
            if (oldNode == null) {
                // easy: just write it here
                children[index] = newAny
            } else {
                // merge it with the existing node
                oldNode.add(newAny)
            }
        }
    }

    fun bitsAreInBetween(or: Int, and: Int, bits: Int): Boolean {

        // this does a test, whether bits.and(idx) is in and.and(idx) .. or.and(idx)
        // it does this test for every power of two

        val isBetween = or and and.inv() // from 0 to 1
        val isZero = or.inv() and bits.inv()
        val isOne = and and bits

        val answer = isBetween or isZero or isOne
        return answer == -1
    }

    fun remove(v: Any) {
        // todo find its coordinates somehow...
        // todo find where it is included
        // todo then remove it
        TODO()
    }

}