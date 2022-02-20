package me.anno.utils.structures

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

    fun add(child: V)
    fun add(index: Int, child: V)

    fun deleteChild(child: V)

    fun addBefore(sibling: V) {
        val p = parent!!
        val index = p.children.indexOf(this)
        p.add(index, sibling)
        sibling.parent = p
    }

    fun addAfter(sibling: V) {
        val p = parent!!
        val index = p.children.indexOf(this)
        p.add(index + 1, sibling)
        sibling.parent = p
    }

    fun addChild(child: V) {
        /*if (
            GFX.glThread != null &&
            Thread.currentThread() != GFX.glThread &&
            this in RemsStudio.root.listOfAll
        ) throw RuntimeException("Called from wrong thread!")*/
        @Suppress("unchecked_cast")
        if (child.contains(this as V)) throw RuntimeException("this cannot contain its parent!")
        child.parent?.removeChild(child)
        child.parent = this
        add(child)
    }

    fun removeChild(child: V) {
        child.parent = null
        deleteChild(child)
    }

    fun contains(t: V): Boolean {
        if (t === this) return true
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

    fun onDestroy()

    fun destroy() {
        // removeFromParent()
        onDestroy()
    }

    @Suppress("unchecked_cast")
    fun listOfHierarchy(callback: (V) -> Unit) {
        parent?.listOfHierarchy(callback)
        callback(this as V)
    }

    val depthInHierarchy
        get(): Int {
            val parent = parent ?: return 0
            return parent.depthInHierarchy + 1
        }

    @Suppress("unchecked_cast")
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

    @Suppress("unchecked_cast")
    fun anyInHierarchy(lambda: (V) -> Boolean): Boolean {
        var v = this as V
        while (true) {
            if (lambda(v)) return true
            v = v.parent ?: return false
        }
    }

    @Suppress("unchecked_cast")
    fun firstInHierarchy(lambda: (V) -> Boolean): V? {
        var v = this as V
        while (true) {
            if (lambda(v)) return v
            v = v.parent ?: return null
        }
    }

    @Suppress("unchecked_cast")
    fun lastInHierarchy(lambda: (V) -> Boolean): V? {
        val v = this as V
        val p = v.parent
        if (p != null) {
            val r = p.lastInHierarchy(lambda)
            if (r != null) return r
        }
        return if (lambda(v)) v else null
    }

    val listOfHierarchy: Sequence<V>
        get() {
            val self = this
            return sequence {
                parent?.apply {
                    yieldAll(listOfHierarchy)
                }
                @Suppress("unchecked_cast")
                yield(self as V)
            }
        }

    val listOfHierarchyReversed: Sequence<V>
        get() {
            val self = this
            return sequence {
                @Suppress("unchecked_cast")
                yield(self as V)
                parent?.apply {
                    yieldAll(listOfHierarchyReversed)
                }
            }
        }

    val listOfAll: Sequence<V>
        get() = sequence {
            @Suppress("unchecked_cast")
            yield(this@Hierarchical as V)
            children.forEach { child ->
                yieldAll(child.listOfAll)
            }
        }

    fun findFirstInAll(callback: (element: V) -> Boolean): V? {
        @Suppress("unchecked_cast")
        if (callback(this as V)) return this
        val children = children
        for (index in children.indices) {
            val v = children[index].findFirstInAll(callback)
            if (v != null) return v
        }
        return null
    }

    val indexInParent: Int
        get() {
            val parent = parent ?: return -1
            return parent.children.indexOf(this)
        }

    fun simpleTraversal(processDisabled: Boolean, func: (V) -> Boolean): V? {
        return depthFirstTraversal(processDisabled, func)
    }

    /*fun depthFirstTraversal(processDisabled: Boolean, list: UnsafeArrayList<V>, func: (V) -> Boolean): V? {
        if (!processDisabled && !isEnabled) return null
        this as V
        list.quickClear()
        list.add(this)
        var index = 0
        while (index < list.size) {
            val element = list[index]
            if (func(element)) return element
            val children = element.children
            for (childIndex in children.indices) {
                val child = children[childIndex]
                if (processDisabled || child.isEnabled) {
                    list.add(child)
                }
            }
            index++
        }
        return null
    }*/

    fun depthFirstTraversal(processDisabled: Boolean, func: (V) -> Boolean): V? {
        @Suppress("unchecked_cast")
        this as V
        if (processDisabled || isEnabled) {
            if (func(this)) return this
            val children = children
            for (i in children.indices) {
                val child = children[i]
                if (processDisabled || child.isEnabled) {
                    val result = child.depthFirstTraversal(processDisabled, func)
                    if (result != null) return result
                }
            }
        }
        return null
    }

    fun breathFirstTraversal(processDisabled: Boolean, func: (V) -> Boolean): V? {
        if (processDisabled || isEnabled) {
            val queue = ArrayList<V>()
            val wasExplored = HashSet<V>()
            @Suppress("unchecked_cast")
            queue.add(this as V)
            wasExplored.add(this)
            var readIndex = 0
            while (readIndex < queue.size) {
                val v = queue[readIndex++]
                if (func(v)) return v
                val children = children
                for (i in children.indices) {
                    val child = children[i]
                    if (processDisabled || child.isEnabled) {
                        if (child !in wasExplored) {
                            wasExplored.add(child)
                            queue.add(child)
                        }
                    }
                }
            }
        }
        return null
    }

}
