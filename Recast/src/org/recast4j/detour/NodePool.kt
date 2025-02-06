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

import org.recast4j.LongHashMap

class NodePool {

    companion object {
        private val nodeCache = ArrayList<Node>()
        private val listCache = ArrayList<ArrayList<Node>>()
        private fun createList(): ArrayList<Node> {
            return synchronized(listCache) {
                listCache.removeLastOrNull() ?: ArrayList()
            }
        }

        private fun createNode(): Node {
            return synchronized(nodeCache) {
                nodeCache.removeLastOrNull() ?: Node()
            }
        }
    }

    private val nodeMap = LongHashMap<ArrayList<Node>>(64)
    private val nodeList = ArrayList<Node>()

    fun clear() {
        nodeList.clear()
        synchronized(listCache) {
            nodeMap.forEachValue { v ->
                if (nodeCache.size < 512) nodeCache.addAll(v)
                if (listCache.size < 512) listCache.add(v)
                v.clear()
            }
        }
        nodeMap.clear()
    }

    fun findNodes(id: Long): List<Node> {
        return nodeMap[id] ?: emptyList()
    }

    fun findNode(id: Long): Node? {
        return nodeMap[id]?.firstOrNull()
    }

    fun getOrCreateNode(id: Long, state: Int): Node {
        var nodes = nodeMap[id]
        if (nodes != null) {
            for (node in nodes) {
                if (node.state == state) {
                    return node
                }
            }
        }
        val node = createNode()
        node.index = nodeList.size + 1
        node.polygonRef = id
        node.state = state
        node.cost = 0f
        node.flags = 0
        node.shortcut = null
        node.parentIndex = 0
        node.pos.set(0f)
        nodeList.add(node)
        nodes = nodeMap.getOrPut(id) { createList() }
        nodes.add(node)
        return node
    }

    fun getNodeIdx(node: Node?): Int {
        return node?.index ?: 0
    }

    fun getNodeAtIdx(idx: Int): Node? {
        return if (idx != 0) nodeList[idx - 1] else null
    }

    fun getOrCreateNode(ref: Long): Node {
        return getOrCreateNode(ref, 0)
    }
}