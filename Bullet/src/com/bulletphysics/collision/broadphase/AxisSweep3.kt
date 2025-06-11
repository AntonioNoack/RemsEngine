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
 * AxisSweep3 is an efficient implementation of the 3D axis sweep and prune broadphase.
 *
 * It uses arrays rather than lists for storage of the 3 axis. Also it operates using 16 bit
 * integer coordinates instead of doubles. For large worlds and many objects, use [AxisSweep3_32]
 * instead. AxisSweep3_32 has higher precision and allows more than 16384 objects at the cost
 * of more memory and a bit of performance.
 *
 * @author jezek2
 */
class AxisSweep3 @JvmOverloads constructor(
    worldAabbMin: Vector3d, worldAabbMax: Vector3d, maxHandles: Int = 16384,  /* = 16384*/
    pairCache: OverlappingPairCache? = null /* = 0*/
) : AxisSweep3Internal(worldAabbMin, worldAabbMax, 0xfffe, 0xffff, maxHandles, pairCache) {
    init {
        // 1 handle is reserved as sentinel
        assert(maxHandles > 1 && maxHandles < 32767)
    }

    override fun createEdgeArray(size: Int): EdgeArray {
        return EdgeArrayImpl(size)
    }

    override fun createHandle(): Handle {
        return HandleImpl()
    }

    override fun getMaskI(): Int {
        return 0xFFFF
    }

    class EdgeArrayImpl(size: Int) : EdgeArray {
        private val pos: ShortArray = ShortArray(size)
        private val handle: ShortArray = ShortArray(size)

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
            return pos[index].toInt() and 0xFFFF
        }

        override fun setPos(index: Int, value: Int) {
            pos[index] = value.toShort()
        }

        override fun getHandle(index: Int): Int {
            return handle[index].toInt() and 0xFFFF
        }

        override fun setHandle(index: Int, value: Int) {
            handle[index] = value.toShort()
        }
    }

    private class HandleImpl : Handle() {
        private var minEdges0: Short = 0
        private var minEdges1: Short = 0
        private var minEdges2: Short = 0

        private var maxEdges0: Short = 0
        private var maxEdges1: Short = 0
        private var maxEdges2: Short = 0

        override fun getMinEdges(edgeIndex: Int): Int {
            return when (edgeIndex) {
                1 -> minEdges1.toInt() and 0xFFFF
                2 -> minEdges2.toInt() and 0xFFFF
                else -> minEdges0.toInt() and 0xFFFF
            }
        }

        override fun setMinEdges(edgeIndex: Int, value: Int) {
            val sValue = value.toShort()
            when (edgeIndex) {
                0 -> minEdges0 = sValue
                1 -> minEdges1 = sValue
                2 -> minEdges2 = sValue
            }
        }

        override fun getMaxEdges(edgeIndex: Int): Int {
            return when (edgeIndex) {
                1 -> maxEdges1.toInt() and 0xFFFF
                2 -> maxEdges2.toInt() and 0xFFFF
                else -> maxEdges0.toInt() and 0xFFFF
            }
        }

        override fun setMaxEdges(edgeIndex: Int, value: Int) {
            val sValue = value.toShort()
            when (edgeIndex) {
                0 -> maxEdges0 = sValue
                1 -> maxEdges1 = sValue
                2 -> maxEdges2 = sValue
            }
        }
    }
}
