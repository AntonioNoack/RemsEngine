package me.anno.utils.structures

interface Hierarchical<V: Hierarchical<V>> {

    var isCollapsed: Boolean

    val symbol: String

    var name: String
    val description: String

    val defaultDisplayName: String

    var parent: V?

    val children: MutableList<V>

    fun addBefore(child: V) {
        val p = parent!!
        val index = p.children.indexOf(this)
        p.children.add(index, child)
        child.parent = p
    }

    fun addAfter(child: V) {
        val p = parent!!
        val index = p.children.indexOf(this)
        p.children.add(index + 1, child)
        child.parent = p
    }

    fun addChild(child: V) {
        /*if (
            GFX.glThread != null &&
            Thread.currentThread() != GFX.glThread &&
            this in RemsStudio.root.listOfAll
        ) throw RuntimeException("Called from wrong thread!")*/
        if (child.contains(this as V)) throw RuntimeException("this cannot contain its parent!")
        child.parent?.removeChild(child)
        child.parent = this
        children += child
    }

    fun removeChild(child: V) {
        child.parent = null
        children.remove(child)
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
        parent?.removeChild(this as V)
        parent = null
    }

    fun destroy()

    val listOfAll: Sequence<V>
        get() = sequence {
            yield(this@Hierarchical as V)
            children.forEach { child ->
                yieldAll(child.listOfAll)
            }
        }

}