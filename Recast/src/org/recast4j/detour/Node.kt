/*
Copyright (c) 2009-2010 Mikko Mononen memon@inside.org
recast4j copyright (c) 2015-2019 Piotr Piastucki piotr@jtilia.org

This software is provided 'as-is', without any express or implied
warranty.  In no event will the authors be held liable for any damages
arising from the use of this software.
Permission is granted to anyone to use this software for any purpose,
including commercial applications, and to alter it and redistribute it
freely, subject to the following restrictions:
1. The origin of this software must not be misrepresented; you must not
 claim that you wrote the original software. If you use this software
 in a product, an acknowledgment in the product documentation would be
 appreciated but is not required.
2. Altered source versions must be plainly marked as such, and must not be
 misrepresented as being the original software.
3. This notice may not be removed or altered from any source distribution.
*/
package org.recast4j.detour

import org.joml.Vector3f
import org.recast4j.LongArrayList

class Node() {

    var index = 0

    /**
     * Position of the node.
     */
    val pos = Vector3f()

    /**
     * Cost of reaching the given node.
     */
    var cost = 0f

    /**
     * Total cost of reaching the goal via the given node including heuristics.
     */
    var totalCost = 0f

    /**
     * Index to parent node.
     */
    var parentIndex = 0

    /**
     * extra state information. A polyRef can have multiple nodes with different extra info. see DT_MAX_STATES_PER_NODE
     */
    var state = 0

    /**
     * Node flags. A combination of dtNodeFlags.
     */
    var flags = 0

    /**
     * Polygon ref the node corresponds to.
     */
    var polygonRef = 0L

    /**
     * Shortcut found by raycast.
     */
    var shortcut: LongArrayList? = null

    override fun toString(): String {
        return "Node [id=$polygonRef]"
    }

    companion object {
        const val OPEN = 0x01
        const val CLOSED = 0x02

        /**
         * parent of the node is not adjacent. Found using raycast.
         */
        const val PARENT_DETACHED = 0x04
    }
}