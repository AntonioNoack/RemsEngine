package com.bulletphysics.extras.gimpact

import com.bulletphysics.linearmath.VectorUtil.getCoord
import com.bulletphysics.linearmath.VectorUtil.maxAxis
import com.bulletphysics.linearmath.VectorUtil.mul
import cz.advel.stack.Stack
import com.bulletphysics.util.setAdd
import com.bulletphysics.util.setSub

/**
 * @author jezek2
 */
internal class BvhTree {
    var nodeCount: Int = 0
    var nodePointer: BvhTreeNodeArray = BvhTreeNodeArray()

    fun calcSplittingAxis(primitiveBoxes: BvhDataArray, startIndex: Int, endIndex: Int): Int {
        val means = Stack.newVec(0.0)
        val variance = Stack.newVec(0.0)

        val numIndices = endIndex - startIndex

        val center = Stack.newVec()
        val diff2 = Stack.newVec()

        val tmp1 = Stack.newVec()
        val tmp2 = Stack.newVec()

        for (i in startIndex until endIndex) {
            primitiveBoxes.getBoundsMax(i, tmp1)
            primitiveBoxes.getBoundsMin(i, tmp2)
            center.setAdd(tmp1, tmp2)
            center.mul(0.5)
            means.add(center)
        }
        means.mul(1.0 / numIndices.toDouble())

        for (i in startIndex until endIndex) {
            primitiveBoxes.getBoundsMax(i, tmp1)
            primitiveBoxes.getBoundsMin(i, tmp2)
            center.setAdd(tmp1, tmp2)
            center.mul(0.5)
            diff2.setSub(center, means)
            mul(diff2, diff2, diff2)
            variance.add(diff2)
        }
        variance.mul(1.0 / (numIndices - 1).toDouble())

        return maxAxis(variance)
    }

    fun sortAndCalcSplittingIndex(
        primitiveBoxes: BvhDataArray,
        startIndex: Int,
        endIndex: Int,
        splitAxis: Int
    ): Int {
        var splitIndex = startIndex
        val numIndices = endIndex - startIndex

        val means = Stack.newVec(0.0)

        val center = Stack.newVec()

        val tmp1 = Stack.newVec()
        val tmp2 = Stack.newVec()

        for (i in startIndex until endIndex) {
            primitiveBoxes.getBoundsMax(i, tmp1)
            primitiveBoxes.getBoundsMin(i, tmp2)
            center.setAdd(tmp1, tmp2)
            center.mul(0.5)
            means.add(center)
        }
        means.mul(1.0 / numIndices)

        // average of centers
        val splitValue = getCoord(means, splitAxis)

        // sort leafNodes so all values larger than splitValue comes first, and smaller values start from 'splitIndex'.
        for (i in startIndex until endIndex) {
            primitiveBoxes.getBoundsMax(i, tmp1)
            primitiveBoxes.getBoundsMin(i, tmp2)
            center.setAdd(tmp1, tmp2)
            center.mul(0.5)

            if (getCoord(center, splitAxis) > splitValue) {
                // swap
                primitiveBoxes.swap(i, splitIndex)
                //swapLeafNodes(i,splitIndex);
                splitIndex++
            }
        }

        // if the splitIndex causes unbalanced trees, fix this by using the center in between startIndex and endIndex
        // otherwise the tree-building might fail due to stack-overflows in certain cases.
        // unbalanced1 is unsafe: it can cause stack overflows
        //bool unbalanced1 = ((splitIndex==startIndex) || (splitIndex == (endIndex-1)));

        // unbalanced2 should work too: always use center (perfect balanced trees)
        //bool unbalanced2 = true;

        // this should be safe too:
        val rangeBalancedIndices = numIndices / 3
        val unbalanced =
            ((splitIndex <= (startIndex + rangeBalancedIndices)) || (splitIndex >= (endIndex - 1 - rangeBalancedIndices)))

        if (unbalanced) {
            splitIndex = startIndex + (numIndices shr 1)
        }

        val unbal = (splitIndex == startIndex) || (splitIndex == (endIndex))
        assert(!unbal)

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

        nodePointer.setEscapeIndex(curIndex, this.nodeCount - curIndex)
    }

    fun buildTree(primitiveBoxes: BvhDataArray) {
        // initialize node count to 0
        this.nodeCount = 0
        // allocate nodes
        nodePointer.resize(primitiveBoxes.size() * 2)

        buildSubTree(primitiveBoxes, 0, primitiveBoxes.size())
    }

    fun clearNodes() {
        nodePointer.clear()
        this.nodeCount = 0
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
