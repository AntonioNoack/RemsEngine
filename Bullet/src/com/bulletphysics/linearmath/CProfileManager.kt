package com.bulletphysics.linearmath

import com.bulletphysics.BulletStats.profileGetTicks

/***************************************************************************************************
 * Real-Time Hierarchical Profiling for Game Programming Gems 3
 *
 * by Greg Hjelstrom & Byon Garrabrant
 * @author jezek2
 */
object CProfileManager {

    val root = CProfileNode("Root", null)
    private var currentNode: CProfileNode = root
    var frameCountSinceReset: Int = 0
        private set

    private var resetTime: Long = 0

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
            currentNode = currentNode.parent!!
        }
    }

    fun reset() {
        root.reset()
        root.call()
        frameCountSinceReset = 0
        resetTime = profileGetTicks()
    }

    fun incrementFrameCounter() {
        frameCountSinceReset++
    }

}
