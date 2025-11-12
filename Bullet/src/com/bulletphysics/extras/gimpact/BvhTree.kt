package com.bulletphysics.extras.gimpact

import com.bulletphysics.linearmath.VectorUtil.maxAxis
import cz.advel.stack.Stack

/**
 * @author jezek2
 */
internal class BvhTree {
    var nodeCount: Int = 0
    var nodePointer: BvhTreeNodeArray = BvhTreeNodeArray()

    fun calcSplittingAxis(primitiveBoxes: BvhDataArray, startIndex: Int, endIndex: Int): Int {
        val means = Stack.newVec3d(0.0)
        val variance = Stack.newVec3d(0.0)

        val numIndices = endIndex - startIndex

        val center = Stack.newVec3d()
        val diff2 = Stack.newVec3d()

        val tmp1 = Stack.newVec3d()

        for (i in startIndex until endIndex) {
            primitiveBoxes.getBoundsMax(i, center)
            primitiveBoxes.getBoundsMin(i, tmp1)
            means.add(center).add(tmp1)
        }
        means.mul(1.0 / numIndices)

        for (i in startIndex until endIndex) {
            primitiveBoxes.getBoundsMax(i, center)
            primitiveBoxes.getBoundsMin(i, tmp1)
            center.add(tmp1)
            center.sub(means, diff2)
            diff2.mul(diff2)
            variance.add(diff2)
        }

        // only used for maxAxis -> scaling not necessary
        // variance.mul(1.0 / (numIndices - 1).toDouble())
        val result = maxAxis(variance)
        Stack.subVec3d(5)
        return result
    }

    fun sortAndCalcSplittingIndex(
        primitiveBoxes: BvhDataArray,
        startIndex: Int, endIndex: Int, splitAxis: Int
    ): Int {

        var splitIndex = startIndex
        val numIndices = endIndex - startIndex

        val tmp = Stack.newVec3d()
        val means = Stack.newVec3d(0.0)
        for (i in startIndex until endIndex) {
            means.add(primitiveBoxes.getBoundsMax(i, tmp))
            means.add(primitiveBoxes.getBoundsMin(i, tmp))
        }
        means.mul(1.0 / numIndices)

        // average of centers
        val splitValue = means[splitAxis]

        // sort leafNodes so all values larger than splitValue comes first, and smaller values start from 'splitIndex'.
        val center = Stack.newVec3d()
        for (i in startIndex until endIndex) {
            primitiveBoxes.getBoundsMax(i, center)
            primitiveBoxes.getBoundsMin(i, tmp)
            center.add(tmp)

            if (center[splitAxis] > splitValue) {
                // swap
                primitiveBoxes.swap(i, splitIndex)
                //swapLeafNodes(i,splitIndex);
                splitIndex++
            }
        }
        Stack.subVec3d(3)

        // if the splitIndex causes unbalanced trees, fix this by using the center in between startIndex and endIndex
        // otherwise the tree-building might fail due to stack-overflows in certain cases.
        // unbalanced1 is unsafe: it can cause stack overflows
        //bool unbalanced1 = ((splitIndex==startIndex) || (splitIndex == (endIndex-1)));

        // unbalanced2 should work too: always use center (perfect balanced trees)
        //bool unbalanced2 = true;

        // this should be safe too:
        val rangeBalancedIndices = numIndices / 3
        val isUnbalanced =
            ((splitIndex <= (startIndex + rangeBalancedIndices)) || (splitIndex >= (endIndex - 1 - rangeBalancedIndices)))

        if (isUnbalanced) {
            splitIndex = startIndex + (numIndices shr 1)
        }

        // val stillIsUnbalanced = (splitIndex == startIndex) || (splitIndex == (endIndex))
        // assert(!stillIsUnbalanced)

        return splitIndex
    }

    fun buildSubTree(primitiveBoxes: BvhDataArray, startIndex: Int, endIndex: Int) {
        val curIndex = this.nodeCount
        this.nodeCount++

        assert((endIndex - startIndex) > 0)

        if ((endIndex - startIndex) == 1) {
            // We have a leaf node
            //setNodeBound(curIndex,primitive_boxes[startIndex].m_bound);
            //m_node_array[curIndex].setDataIndex(primitive_boxes[startIndex].m_data);
            nodePointer.set(curIndex, primitiveBoxes, startIndex)

            return
        }

        // calculate Best Splitting Axis and where to split it. Sort the incoming 'leafNodes' array within range 'startIndex/endIndex'.

        // split axis
        var splitIndex = calcSplittingAxis(primitiveBoxes, startIndex, endIndex)

        splitIndex = sortAndCalcSplittingIndex(primitiveBoxes, startIndex, endIndex, splitIndex)

        //calc this node bounding box
        val nodeBound = AABB()
        val tmpAABB = AABB()

        nodeBound.invalidate()

        for (i in startIndex until endIndex) {
            primitiveBoxes.getBounds(i, tmpAABB)
            nodeBound.merge(tmpAABB)
        }

        setNodeBound(curIndex, nodeBound)

        // build left branch
        buildSubTree(primitiveBoxes, startIndex, splitIndex)

        // build right branch
        buildSubTree(primitiveBoxes, splitIndex, endIndex)

        nodePointer.setEscapeIndex(curIndex, nodeCount - curIndex)
    }

    fun buildTree(primitiveBoxes: BvhDataArray) {
        // initialize node count to 0
        nodeCount = 0
        // allocate nodes
        nodePointer.resize(primitiveBoxes.size() * 2)

        buildSubTree(primitiveBoxes, 0, primitiveBoxes.size())
    }

    fun clearNodes() {
        nodePointer.clear()
        nodeCount = 0
    }

    /**
     * Tells if the node is a leaf.
     */
    fun isLeafNode(nodeIndex: Int): Boolean {
        return nodePointer.isLeafNode(nodeIndex)
    }

    fun getNodeData(nodeIndex: Int): Int {
        return nodePointer.getDataIndex(nodeIndex)
    }

    fun getNodeBound(nodeIndex: Int, bound: AABB) {
        nodePointer.getBounds(nodeIndex, bound)
    }

    fun setNodeBound(nodeIndex: Int, bound: AABB) {
        nodePointer.setBounds(nodeIndex, bound)
    }

    fun getLeftNode(nodeIndex: Int): Int {
        return nodeIndex + 1
    }

    fun getRightNode(nodeIndex: Int): Int {
        if (nodePointer.isLeafNode(nodeIndex + 1)) {
            return nodeIndex + 2
        }
        return nodeIndex + 1 + nodePointer.getEscapeIndex(nodeIndex + 1)
    }

    fun getEscapeNodeIndex(nodeIndex: Int): Int {
        return nodePointer.getEscapeIndex(nodeIndex)
    }
}
