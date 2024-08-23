package me.anno.utils.structures.lists

import me.anno.maths.Maths.min

/**
 * sorts a list of items topologically
 * */
abstract class TopologicalSort<NodeType, ListType : MutableCollection<NodeType>>(val list: ListType) {

    private val remaining = list.toHashSet()
    private val seen = HashSet<NodeType>(min(64, list.size))

    /**
     * in this method, call visit() for all dependencies of <node>;
     * if you encounter any 'true' return 'true'
     * */
    abstract fun visitDependencies(node: NodeType): Boolean

    /**
     * returns true if cycle was found
     * */
    fun visit(node: NodeType): Boolean {
        if (node !in remaining) return false // already done
        if (node in seen) return true // cycle found :/
        seen.add(node)
        if (visitDependencies(node)) {
            return true // cycle found
        }
        seen.remove(node)
        remaining.remove(node)
        list.add(node)
        return false // we may continue
    }

    /**
     * returns null if cycle was found;
     * contents if original list may be anything - or sorted on success
     * */
    fun finish(): ListType? {
        list.clear()
        while (remaining.isNotEmpty()) {
            if (visit(remaining.first())) {
                return null
            }
        }
        return list
    }
}