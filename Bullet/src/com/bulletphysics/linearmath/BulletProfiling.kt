package com.bulletphysics.linearmath

import me.anno.utils.types.Floats.f3
import me.anno.utils.types.Floats.formatPercent

/***************************************************************************************************
 * Real-Time Hierarchical Profiling for Game Programming Gems 3
 *
 * by Greg Hjelstrom & Byon Garrabrant
 * @author jezek2
 */
object BulletProfiling {

    private val root = ProfilingNode("", null)
    private var currentNode = root

    var frameCountSinceReset: Int = 0
        private set

    /**
     * @param name must be [interned][String.intern] String (not needed for String literals)
     */
    fun startProfile(name: String?) {
        if (name != currentNode.name) {
            currentNode = currentNode.getSubNode(name)
        }

        currentNode.call()
    }

    fun stopProfile() {
        // Return will indicate whether we should back up to our parent (we may
        // be profiling a recursive function)
        if (currentNode.end()) {
            currentNode = currentNode.parent ?: root
        }
    }

    @Suppress("unused")
    fun reset() {
        root.reset()
        root.call()
        frameCountSinceReset = 0
    }

    fun incrementFrameCounter() {
        frameCountSinceReset++
    }

    fun printProfiling() {
        var total = 0L
        var child = root.child
        while (child != null) {
            total += child.totalTimeNanos
            child = child.sibling
        }

        child = root.child
        while (child != null) {
            printProfiling(child, 0, total)
            child = child.sibling
        }
    }

    private fun printProfiling(node: ProfilingNode, depth: Int, total: Long) {
        println(
            "  ".repeat(depth) + "\"${node.name}\" ${node.totalCalls}x: " +
                    "${(node.totalTimeSeconds * 1e3).f3()} ms, (${(node.totalTimeNanos.toFloat() / total).formatPercent()} %)"
        )
        var child = node.child
        while (child != null) {
            printProfiling(child, depth + 1, total)
            child = child.sibling
        }
    }
}
