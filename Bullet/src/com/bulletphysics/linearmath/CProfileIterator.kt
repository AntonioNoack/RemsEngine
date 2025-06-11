package com.bulletphysics.linearmath

/**
 * ************************************************************************************************
 *
 *
 * Real-Time Hierarchical Profiling for Game Programming Gems 3
 *
 *
 * by Greg Hjelstrom & Byon Garrabrant
 * **************************************************************************************************
 *
 *
 * Iterator to navigate through profile tree.
 *
 * @author jezek2
 */
class CProfileIterator internal constructor(start: CProfileNode) {
    var currentParent: CProfileNode?
    var currentChild: CProfileNode?

    init {
        currentParent = start
        currentChild = currentParent!!.child
    }

    // Access all the children of the current parent
    fun first() {
        currentChild = currentParent!!.child
    }

    fun next() {
        currentChild = currentChild!!.sibling
    }

    val isDone: Boolean
        get() = currentChild == null

    val isRoot: Boolean
        get() = currentParent!!.parent == null

    /**
     * Make the given child the new parent.
     */
    fun enterChild(index: Int) {
        var index = index
        currentChild = currentParent!!.child
        while ((currentChild != null) && (index != 0)) {
            index--
            currentChild = currentChild!!.sibling
        }

        if (currentChild != null) {
            currentParent = currentChild
            currentChild = currentParent!!.child
        }
    }

    //public void enterLargestChild(); // Make the largest child the new parent
    /**
     * Make the current parent's parent the new parent.
     */
    fun enterParent() {
        if (currentParent!!.parent != null) {
            currentParent = currentParent!!.parent
        }
        currentChild = currentParent!!.child
    }
}
