package com.bulletphysics.linearmath

import me.anno.Time

/***************************************************************************************************
 * Real-Time Hierarchical Profiling for Game Programming Gems 3
 * by Greg Hjelstrom & Byon Garrabrant
 *
 * A node in the Profile Hierarchy Tree.
 *
 * @author jezek2
 */
internal class ProfilingNode(var name: String, val parent: ProfilingNode?) {

    var totalCalls: Int = 0
    var totalTimeNanos: Long = 0L
    val totalTimeSeconds: Double
        get() = totalTimeNanos * 1e-9

    private var startTime: Long = 0
    private var recursionCounter = 0

    var child: ProfilingNode? = null
        private set
    var sibling: ProfilingNode? = null
        private set

    init {
        reset()
    }

    fun getSubNode(name: String): ProfilingNode {
        // Try to find this sub node
        var child = this.child
        while (child != null) {
            if (child.name == name) {
                return child
            }
            child = child.sibling
        }

        // We didn't find it, so add it
        val node = ProfilingNode(name, this)
        node.sibling = this.child
        this.child = node
        return node
    }

    fun reset() {

        totalCalls = 0
        totalTimeNanos = 0L

        if (child != null) {
            child!!.reset()
        }
        if (sibling != null) {
            sibling!!.reset()
        }
    }

    fun call() {
        totalCalls++
        if (recursionCounter++ == 0) {
            startTime = Time.nanoTime
        }
    }

    fun end(): Boolean {
        if (--recursionCounter == 0 && totalCalls != 0) {
            totalTimeNanos += Time.nanoTime - startTime
        }
        return recursionCounter == 0
    }
}
