package me.anno.utils.structures

import me.anno.utils.algorithms.Recursion
import me.anno.utils.assertions.assertFalse
import kotlin.reflect.KClass

interface Hierarchical<V : Hierarchical<V>> {

    var isCollapsed: Boolean
    var isEnabled: Boolean

    val symbol: String

    var name: String
    val description: String

    val defaultDisplayName: String

    var parent: V?

    val children: List<V>

    fun addChild(index: Int, child: V)

    fun deleteChild(child: V)

    fun addChild(child: V) {
        @Suppress("unchecked_cast")
        assertFalse(child.contains(this as V), "this cannot contain its parent!")
        assertFalse(children.contains(child), "Cannot add child twice")
        child.parent = this
        (children as MutableList<V>).add(child)
    }

    fun removeChild(child: V) {
        if (child.parent === this) {
            // called by .parent = ...
            // child.parent = null
            deleteChild(child)
        }
    }

    fun contains(t: V): Boolean {
        if (t === this) return true
        @Suppress("SENSELESS_COMPARISON")
        if (children != null) {// can be null on init
            for (child in children) {
                if (child === t || child.contains(t)) return true
            }
        }
        return false
    }

    fun removeFromParent() {
        @Suppress("unchecked_cast")
        parent?.removeChild(this as V)
        parent = null
    }

    @Suppress("unchecked_cast")
    fun <V : Any> getRoot(type: KClass<V>): V {
        val parent = parent ?: return this as V
        return if (type.isInstance(parent)) parent.getRoot(type)
        else this as V
    }

    @Suppress("unchecked_cast")
    val root: V
        get() = parent?.root ?: this as V

    @Suppress("unchecked_cast", "unused")
    fun listOfHierarchy(callback: (V) -> Unit) {
        parent?.listOfHierarchy(callback)
        callback(this as V)
    }

    val depthInHierarchy
        get(): Int {
            val parent = parent ?: return 0
            return parent.depthInHierarchy + 1
        }

    @Suppress("unchecked_cast", "unused")
    fun forAllInHierarchy(lambda: (V) -> Unit) {
        var v = this as V
        while (true) {
            lambda(v)
            v = v.parent ?: return
        }
    }

    @Suppress("unchecked_cast")
    fun allInHierarchy(lambda: (V) -> Boolean): Boolean {
        var v = this as V
        while (true) {
            if (!lambda(v)) return false
            v = v.parent ?: return true
        }
    }

    fun anyInHierarchy(lambda: (V) -> Boolean): Boolean {
        return firstInHierarchy(lambda) != null
    }

    @Suppress("unchecked_cast")
    fun firstInHierarchy(lambda: (V) -> Boolean): V? {
        var v = this as V
        while (true) {
            if (lambda(v)) return v
            v = v.parent ?: return null
        }
    }

    @Suppress("unchecked_cast", "unused")
    fun lastInHierarchy(lambda: (V) -> Boolean): V? {
        val v = this as V
        val p = v.parent
        if (p != null) {
            val r = p.lastInHierarchy(lambda)
            if (r != null) return r
        }
        return if (lambda(v)) v else null
    }

    val listOfHierarchy: List<V>
        get() = listOfHierarchyReversed.asReversed()

    val listOfHierarchyReversed: List<V>
        get() {
            val result = ArrayList<V>()
            @Suppress("unchecked_cast")
            result.add(this as V)
            var workerIndex = 0
            while (workerIndex < result.size) {
                val item = result[workerIndex++]
                result.add(item.parent ?: continue)
            }
            return result
        }

    val listOfAll: List<V>
        get() {
            val result = ArrayList<V>()
            @Suppress("unchecked_cast")
            result.add(this as V)
            var workerIndex = 0
            while (workerIndex < result.size) {
                val item = result[workerIndex++]
                result.addAll(item.children)
            }
            return result
        }

    val indexInParent: Int
        get() {
            val parent = parent ?: return -1
            return parent.children.indexOf(this)
        }

    fun simpleTraversal(processDisabled: Boolean = false, func: (V) -> Boolean): V? {
        return depthFirstTraversal(processDisabled, func)
    }

    fun depthFirstTraversal(processDisabled: Boolean, func: (V) -> Boolean): V? {
        @Suppress("UNCHECKED_CAST")
        return Recursion.findRecursive(this as V) { item, remaining ->
            if (processDisabled || item.isEnabled) {
                if (func(item)) item
                else {
                    remaining.addAll(item.children)
                    null
                }
            } else null
        }
    }

    @Suppress("unused")
    fun breadthFirstTraversal(processDisabled: Boolean, func: (V) -> Boolean): V? {
        if (processDisabled || isEnabled) {
            val remaining = ArrayList<V>()
            val wasExplored = HashSet<V>()
            @Suppress("unchecked_cast")
            remaining.add(this as V)
            wasExplored.add(this)
            var readIndex = 0
            while (readIndex < remaining.size) {
                val v = remaining[readIndex++]
                if (func(v)) return v
                val children = children
                for (i in children.indices) {
                    val child = children[i]
                    if (processDisabled || child.isEnabled) {
                        if (child !in wasExplored) {
                            wasExplored.add(child)
                            remaining.add(child)
                        }
                    }
                }
            }
        }
        return null
    }
}
