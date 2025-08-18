package me.anno.graph.octtree

import me.anno.utils.algorithms.Recursion
import me.anno.utils.types.Booleans.hasFlag

object KdTreePairs {

    /**
     * Pairs where both elements are identical (reflexive/diagonal)
     * */
    const val FLAG_SELF_PAIRS = 1

    /**
     * Return not only (a,b), but also (b,a).
     * */
    const val FLAG_SWAPPED_PAIRS = 2

    /**
     * Calls callback on all overlapping pairs, either (a,b) or (b,a), until true is returned by it.
     * Returns whether true was ever returned;
     * */
    fun <Point, Value> KdTree<Point, Value>.queryPairs(
        flags: Int, hasFound: (Value, Value) -> Boolean
    ): Boolean = queryPairs(flags, this, hasFound)

    /**
     * Calls callback on all overlapping pairs, either (a,b) or (b,a), until true is returned by it.
     * Returns whether true was ever returned;
     *
     * Extra parameter, so you could overlap two trees, too.
     * */
    fun <Point, OwnValue, OtherValue> KdTree<Point, OwnValue>.queryPairs(
        flags: Int, other: KdTree<Point, OtherValue>, hasFound: (OwnValue, OtherValue) -> Boolean
    ): Boolean {
        return Recursion.anyRecursivePairs(this, other) { self, other, remaining ->
            self.queryPairsStep(flags, other, hasFound, remaining)
        }
    }

    private fun <Point, OwnValue, OtherValue> KdTree<Point, OwnValue>.queryPairsStep(
        flags: Int, other: KdTree<Point, OtherValue>, hasFound: (OwnValue, OtherValue) -> Boolean,
        remaining: ArrayList<Any?>
    ): Boolean {

        if (!overlapsOtherTree(other)) return false
        val ownChildren = values
        val otherChildren = other.values
        val sameNode = other === this
        when {
            ownChildren != null && otherChildren != null -> {
                val returnSelfPairs = flags.hasFlag(FLAG_SELF_PAIRS)
                val returnSwappedPairs = flags.hasFlag(FLAG_SWAPPED_PAIRS)
                for (i in ownChildren.indices) {
                    val a = ownChildren[i]
                    val aMin = getMin(a)
                    val aMax = getMax(a)

                    // i+1 would prevent (a,a) from being returned
                    val jStart = if (sameNode) (if (returnSelfPairs) i else i + 1) else 0
                    for (j in jStart until otherChildren.size) {

                        val b = otherChildren[j]
                        val bMin = other.getMin(b)
                        val bMax = other.getMax(b)

                        // preventing returning (a,a)
                        if (!returnSelfPairs && a === b) continue

                        if (overlapsOtherTree(aMin, aMax, bMin, bMax)) {
                            if (hasFound(a, b)) {
                                // returning (a,b)
                                return true
                            }
                            @Suppress("UNCHECKED_CAST")
                            if (returnSwappedPairs && a !== b &&
                                hasFound(b as OwnValue, a as OtherValue)
                            ) {
                                // returning (b,a)
                                return true
                            }
                        }
                    }
                }
            }
            ownChildren != null -> {
                remaining.addPair(this, other.left)
                remaining.addPair(this, other.right)
            }
            otherChildren != null -> {
                remaining.addPair(left, other)
                remaining.addPair(right, other)
            }
            else -> {

                val left = left ?: return false
                val right = right ?: return false

                remaining.addPair(left, other.left)
                remaining.addPair(left, other.right)
                remaining.addPair(right, other.right)

                if (!sameNode) {
                    // we also need to handle this fourth case:
                    remaining.addPair(right, other.right)
                }
            }
        }
        return false
    }

    private fun <V> ArrayList<V>.addPair(first: V, second: V) {
        add(first)
        add(second)
    }
}