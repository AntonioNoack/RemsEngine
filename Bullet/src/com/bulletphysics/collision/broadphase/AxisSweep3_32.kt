/*
 * Java port of Bullet (c) 2008 Martin Dvorak <jezek2@advel.cz>
 *
 * AxisSweep3
 * Copyright (c) 2006 Simon Hobbs
 *
 * Bullet Continuous Collision Detection and Physics Library
 * Copyright (c) 2003-2008 Erwin Coumans  http://www.bulletphysics.com/
 *
 * This software is provided 'as-is', without any express or implied warranty.
 * In no event will the authors be held liable for any damages arising from
 * the use of this software.
 *
 * Permission is granted to anyone to use this software for any purpose,
 * including commercial applications, and to alter it and redistribute it
 * freely, subject to the following restrictions:
 *
 * 1. The origin of this software must not be misrepresented; you must not
 *    claim that you wrote the original software. If you use this software
 *    in a product, an acknowledgment in the product documentation would be
 *    appreciated but is not required.
 * 2. Altered source versions must be plainly marked as such, and must not be
 *    misrepresented as being the original software.
 * 3. This notice may not be removed or altered from any source distribution.
 */
package com.bulletphysics.collision.broadphase

import org.joml.Vector3d

/**
 * AxisSweep3_32 allows higher precision quantization and more objects compared
 * to the [AxisSweep3] sweep and prune. This comes at the cost of more memory
 * per handle, and a bit slower performance.
 *
 * @author jezek2
 */
class AxisSweep3_32(
    worldAabbMin: Vector3d, worldAabbMax: Vector3d, maxHandles: Int,  /* = 1500000*/
    pairCache: OverlappingPairCache? /* = 0*/
) : AxisSweep3Internal(worldAabbMin, worldAabbMax, -0x2, 0x7fffffff, maxHandles, pairCache) {

    constructor(worldAabbMin: Vector3d, worldAabbMax: Vector3d) :
            this(worldAabbMin, worldAabbMax, 1500000, null)

    constructor(worldAabbMin: Vector3d, worldAabbMax: Vector3d, maxHandles: Int) :
            this(worldAabbMin, worldAabbMax, maxHandles, null)

    init {
        // 1 handle is reserved as sentinel
        assert(maxHandles > 1 && maxHandles < 2147483647)
    }

    override fun createEdgeArray(size: Int): EdgeArray {
        return EdgeArrayImpl(size)
    }

    override fun createHandle(): Handle {
        return HandleImpl()
    }

    override fun getMaskI(): Int {
        return -0x1
    }

    class EdgeArrayImpl(size: Int) : EdgeArray {
        private val pos: IntArray = IntArray(size)
        private val handle: IntArray = IntArray(size)

        override fun swap(idx1: Int, idx2: Int) {
            val tmpPos = pos[idx1]
            val tmpHandle = handle[idx1]

            pos[idx1] = pos[idx2]
            handle[idx1] = handle[idx2]

            pos[idx2] = tmpPos
            handle[idx2] = tmpHandle
        }

        override fun set(dst: Int, src: Int) {
            pos[dst] = pos[src]
            handle[dst] = handle[src]
        }

        override fun getPos(index: Int): Int {
            return pos[index]
        }

        override fun setPos(index: Int, value: Int) {
            pos[index] = value
        }

        override fun getHandle(index: Int): Int {
            return handle[index]
        }

        override fun setHandle(index: Int, value: Int) {
            handle[index] = value
        }
    }

    private class HandleImpl : Handle() {
        private var minEdges0 = 0
        private var minEdges1 = 0
        private var minEdges2 = 0

        private var maxEdges0 = 0
        private var maxEdges1 = 0
        private var maxEdges2 = 0

        override fun getMinEdges(edgeIndex: Int): Int {
            return when (edgeIndex) {
                1 -> minEdges1
                2 -> minEdges2
                else -> minEdges0
            }
        }

        override fun setMinEdges(edgeIndex: Int, value: Int) {
            when (edgeIndex) {
                0 -> minEdges0 = value
                1 -> minEdges1 = value
                2 -> minEdges2 = value
            }
        }

        override fun getMaxEdges(edgeIndex: Int): Int {
            return when (edgeIndex) {
                1 -> maxEdges1
                2 -> maxEdges2
                else -> maxEdges0
            }
        }

        override fun setMaxEdges(edgeIndex: Int, value: Int) {
            when (edgeIndex) {
                0 -> maxEdges0 = value
                1 -> maxEdges1 = value
                2 -> maxEdges2 = value
            }
        }
    }
}
