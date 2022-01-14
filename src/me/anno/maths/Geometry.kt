package me.anno.maths

import me.anno.utils.Intersections
import me.anno.utils.LOGGER
import me.anno.utils.Tabs
import org.joml.Vector2d
import java.util.*
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/* tests for a university module of me; will not stay in Rem's Engine */
object Geometry {

    // balanced trees... is there existing ones in Java? yes, java.util.TreeSet

    class Node(var value: Any) {

        var parent: Node? = null
        var left: Node? = null
        var right: Node? = null

        val isLeft get() = parent?.left === this
        val isRight get() = parent?.right === this

        fun printStructure(depth: Int = 0) {
            left?.printStructure(depth + 1)
            println("${Tabs.spaces(depth * 2)}$value")
            right?.printStructure(depth + 1)
        }

        fun balance() {
            // todo balance somehow...
            // todo first check, if we need to be balanced
            if (left != null || right != null) {
                TODO()
            }
            val parent = parent ?: return
            val grand = parent.parent ?: return
            if (parent.hasOnlyOneChild() && grand.hasOnlyOneChild()) {
                when {
                    parent.left != null && grand.left != null -> {
                        this.parent = grand
                        grand.right = this
                        parent.left = null
                        parent.switchValue(grand)
                    }
                    parent.right != null && grand.right != null -> {
                        this.parent = grand
                        grand.left = this
                        parent.right = null
                        parent.switchValue(grand)
                    }
                    else -> {
                        LOGGER.warn("todo geo balance")
                    }
                }
            }
        }

        fun switchValue(other: Node) {
            val t = value
            value = other.value
            other.value = t
        }

        fun hasOnlyOneChild() = left == null || right == null

        fun addBalanced(v: Any): Node {
            val node = Node(v)
            add(v, node)
            node.balance()
            return node
        }

        fun add(v: Any, node: Node) {
            @Suppress("UNCHECKED_CAST")
            val compare = (v as Comparable<Any>).compareTo(value)
            return when {
                compare < 0 -> {
                    if (left == null) {
                        node.parent = this
                        left = node
                    } else left!!.add(v, node)
                    // left
                }
                compare > 0 -> {
                    // right
                    if (right == null) {
                        node.parent = this
                        right = node
                    } else right!!.add(v, node)
                }
                else -> throw IllegalArgumentException("Cannot add equal objects twice")
            }
        }

        fun removeBalanced(v: Any): Node? {
            val node = remove(v) ?: return null
            node.balance()
            // todo is this really correct? mmh... probably not
            return node
        }

        /**
         * returns the node of the previous member of that found node
         * */
        fun remove(v: Any): Node? {
            val node = find(v) ?: return null
            val previous = node.findPrevious()
            node.remove()
            return previous
        }

        /**
         * remove this single node from the tree
         * */
        fun remove() {
            val parent = parent!!
            if (parent.left === this) {
                parent.left = null
            } else {
                parent.right = null
            }
            val left = left
            if (left != null) parent.add(left.value, left)
            val right = right
            if (right != null) parent.add(right.value, right)
        }

        fun find(v: Any): Node? {
            @Suppress("UNCHECKED_CAST")
            val compare = (v as Comparable<Any>).compareTo(value)
            return when {
                compare < 0 -> left?.find(v)
                compare > 0 -> right?.find(v)
                else -> this
            }
        }

        fun findPrevious(): Node? {
            if (left != null) return left!!.findMaxDownwards()
            if (isLeft) return parent?.findPrevious()
            // else we are right -> our parent is the previous node
            return parent
        }

        fun findMaxDownwards(): Node {
            // left is not of interest
            return right?.findMaxDownwards() ?: this
        }

        fun findMinDownwards(): Node {
            return left?.findMinDownwards() ?: this
        }

        fun findNext(): Node? {
            if (right != null) return right!!.findMinDownwards()
            if (isRight) return parent?.findNext()
            return parent
        }

    }

    // todo rootement a balanced search tree...
    class SearchTree<V>(val comparator: Comparator<V>) {
        var root: Node? = null
        fun add(v: V): Node {
            v!!
            return if (root == null) {
                val node = Node(v)
                root = node
                node
            } else {
                root!!.addBalanced(v)
            }
        }

        fun remove(v: V): Node? {
            v!!
            return root?.removeBalanced(v)
        }

        fun find(v: V): Node? = root?.find(v!!)

    }

    class Interval(val min: Double, val max: Double) {
        val mid = (min + max) * 0.5
    }

    class IntervalTree(
        val n: Int,
        private val initialListSize: Int = 16,
        private val size: Interval
    ) {

        private val contents = arrayOfNulls<ArrayList<Interval>>(n * 2 - 1)
        private fun checkChildInterval(interval: Interval) {
            if (interval.min < size.min || interval.max > size.max) {
                throw IllegalArgumentException("Interval is out of bounds")
            }
        }

        fun add(interval: Interval) {
            checkChildInterval(interval)
            iterate(interval) { iv, it ->
                val oldList = contents[it]
                if (oldList == null) {
                    val newList = ArrayList<Interval>(initialListSize)
                    newList.add(iv)
                    contents[it] = newList
                } else oldList.add(iv)
            }
        }

        fun remove(interval: Interval) {
            checkChildInterval(interval)
            iterate(interval) { iv, it ->
                contents[it]?.remove(iv)
            }
        }

        fun iterate(interval: Interval, run: (iv: Interval, Int) -> Unit) {
            var index = 0
            var step = 1
            val center = size.mid
            when {
                interval.min >= center -> {
                    // right side only
                }
                interval.max <= center -> {
                    // left side only
                }
                else -> {
                    // both sides
                }
            }
        }

    }

    // todo all major algorithms from Algorithmic Geometry I (for practice and maybe we can use them :))

    /**
     * inputs: start end start end start end ...
     * */
    fun axisAlignedIntersections(horizontalSections: DoubleArray, verticalSections: DoubleArray): List<Vector2d> {
        TODO()
    }

    class Segment(val start: Vector2d, val end: Vector2d) {
        var currentY = 0.0
    }

    open class Task<V>(val x: Double)

    class Start(val data: Segment) : Task<Segment>(min(data.start.x, data.end.x))

    class End(val data: Segment) : Task<Segment>(max(data.start.x, data.end.x))

    class Intersection(val hit: Vector2d, val first: Segment, val second: Segment) : Task<Vector2d>(hit.x)

    fun intersections(lines: List<Segment>): List<Intersection> {

        val queue = PriorityQueue<Task<*>>()
        for (line in lines) {
            queue.add(Start(line))
            queue.add(End(line))
        }

        val currentSegments = SearchTree<Segment> { a, b -> a.currentY.compareTo(b.currentY) }
        fun check(first: Segment, second: Segment, x: Double) {
            // check if there is an intersection, and add only if it's after this x
            val intersection = Intersections.getStrictLineIntersection(first.start, first.end, second.start, second.end)
            if (intersection != null && intersection.x >= x) {
                queue.add(Intersection(intersection, first, second))
            }
        }

        fun check(firstNode: Node?, secondNode: Node, x: Double) {
            firstNode ?: return
            check(firstNode.value as Segment, secondNode.value as Segment, x)
        }

        fun findY(segment: Segment, x: Double): Double {
            val a = segment.start
            val b = segment.end
            val dx = b.x - a.x
            val dy = b.y - a.y
            // if this line is parallel to the y axis, we cannot compute x
            if (abs(dx) < 1e-15 * abs(dy)) return a.x
            return a.y + (x - a.x) * dy / dx
        }

        while (true) {
            val head = queue.remove()
            val x = head.x
            when (head) {
                is Start -> {
                    val segment = head.data
                    segment.currentY = findY(segment, x)
                    val center = currentSegments.add(segment)
                    check(center.findPrevious(), center, x)
                    check(center.findNext(), center, x)
                }
                is End -> {
                    val segment = head.data
                    val previous = currentSegments.remove(segment)
                    if (previous != null) check(previous.findNext(), previous, x)
                }
                is Intersection -> {
                    // switch both members in the list
                    val nodeA = currentSegments.find(head.first)!!
                    val nodeB = currentSegments.find(head.second)!!
                    val segmentA = nodeA.value as Segment
                    val segmentB = nodeB.value as Segment
                    nodeA.value = segmentB
                    nodeB.value = segmentA
                    val wasASmallerThanB = segmentA.currentY < segmentB.currentY
                    segmentA.currentY = head.hit.y
                    segmentB.currentY = head.hit.y * (1.0 + 1e-15 * (if (wasASmallerThanB) -1 else +1))
                }
            }
        }
    }

    private fun testTree() {
        val tree = Node(10)
        for (i in (0 until 20)) {
            if (i != tree.value) {
                tree.addBalanced(i)
            }
        }
        tree.printStructure()
        val minNode = tree.findMinDownwards()
        println("min: ${minNode.value}")
        minNode.remove()
        tree.printStructure()
        println("next min: ${tree.findMinDownwards().value}")
        println("max: ${tree.findMaxDownwards().value}")
    }

    @JvmStatic
    fun main(args: Array<String>) {
        testTree()
    }

}