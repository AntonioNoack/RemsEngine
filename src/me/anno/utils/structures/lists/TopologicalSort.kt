package me.anno.utils.structures.lists

import me.anno.maths.Maths.clamp
import me.anno.utils.assertions.assertTrue

/**
 * sorts a list of items topologically
 * */
abstract class TopologicalSort<NodeType : Any, ListType : MutableCollection<NodeType>>(val list: ListType) {

    private val remaining = list.toHashSet()
    private val currentlyChecked = HashSet<NodeType>(clamp(list.size, 16, 64))

    private var isFindingCycle = false
    private var nextCycleNode: NodeType? = null

    /**
     * in this method, call visit() for all dependencies of <node>;
     * if you encounter any 'true' return 'true';
     *
     * will only be called once per instance in finish();
     * might be called multiple times in findCycle();
     * */
    abstract fun visitDependencies(node: NodeType): Boolean

    /**
     * returns true if cycle was found
     * */
    fun visit(node: NodeType): Boolean {
        if (isFindingCycle) {
            if (node in currentlyChecked) nextCycleNode = node
            return nextCycleNode != null
        }

        if (node !in remaining) return false // already done
        if (node in currentlyChecked) return true // cycle found :/
        currentlyChecked.add(node)
        if (visitDependencies(node)) {
            return true // cycle found
        }
        currentlyChecked.remove(node)
        remaining.remove(node)
        list.add(node)
        return false // we may continue
    }

    /**
     * returns null if cycle was found;
     * contents if original list may be anything - or sorted on success
     * */
    fun finish(restoreOriginal: Boolean): ListType? {
        list.clear()
        while (remaining.isNotEmpty()) {
            if (visit(remaining.first())) {
                if (restoreOriginal) {
                    // add back all elements that have been removed from that list; will be unordered
                    list.addAll(remaining)
                    remaining.clear() // not used elsewhere
                }
                return null
            }
        }
        return list
    }

    /**
     * finds the dependency cycle;
     * must be executed after finish();
     * returns empty list, if there isn't any cycle
     * */
    fun findCycle(): List<NodeType> {

        if (currentlyChecked.isEmpty()) {
            return emptyList()
        }

        // we have a guaranteed cycle, so let's find it
        val cycle = ArrayList<NodeType>()
        val cycleSet = HashSet<NodeType>()

        isFindingCycle = true

        var node = currentlyChecked.first()
        while (true) {
            if (!cycleSet.add(node)) {
                // we found a cycle, let's complete it by removing unnecessary stuff at the start
                val index = cycle.indexOf(node)
                if (index > 0) cycle.subList(0, index).clear()
                isFindingCycle = false // just in case...
                return cycle
            }
            cycle.add(node)

            // given node, find the next dependency, which is part of currentlyChecked
            nextCycleNode = null
            assertTrue(visitDependencies(node))
            node = nextCycleNode!!
        }
    }
}