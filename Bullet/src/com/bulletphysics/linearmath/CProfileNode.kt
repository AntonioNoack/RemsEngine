package com.bulletphysics.linearmath

import com.bulletphysics.BulletStats
import com.bulletphysics.BulletStats.profileGetTickRate
import com.bulletphysics.BulletStats.profileGetTicks

/***************************************************************************************************
 *
 *
 * Real-Time Hierarchical Profiling for Game Programming Gems 3
 *
 *
 * by Greg Hjelstrom & Byon Garrabrant
 *
 *
 *
 *
 * A node in the Profile Hierarchy Tree.
 *
 * @author jezek2
 */
class CProfileNode(var name: String?, val parent: CProfileNode?) {
    var totalCalls: Int = 0
    var totalTime: Double = 0.0

    private var startTime: Long = 0
    private var recursionCounter = 0

    var child: CProfileNode? = null
        private set
    var sibling: CProfileNode? = null
        private set

    init {
        reset()
    }

    fun getSubNode(name: String?): CProfileNode {
        // Try to find this sub node
        var child = this.child
        while (child != null) {
            if (child.name == name) {
                return child
            }
            child = child.sibling
        }

        // We didn't find it, so add it
        val node = CProfileNode(name, this)
        node.sibling = this.child
        this.child = node
        return node
    }

    fun cleanupMemory() {
        child = null
        sibling = null
    }

    fun reset() {
        totalCalls = 0
        totalTime = 0.0
        BulletStats.profileClock.reset()

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
            startTime = profileGetTicks()
        }
    }

    fun Return(): Boolean {
        if (--recursionCounter == 0 && totalCalls != 0) {
            val time = profileGetTicks() - startTime
            totalTime += time.toDouble() / profileGetTickRate()
        }
        return (recursionCounter == 0)
    }
}
