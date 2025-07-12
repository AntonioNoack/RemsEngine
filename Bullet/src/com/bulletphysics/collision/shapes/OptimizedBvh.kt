package com.bulletphysics.collision.shapes

import com.bulletphysics.collision.shapes.QuantizedBvhNodes.Companion.getCoord
import com.bulletphysics.collision.shapes.QuantizedBvhNodes.Companion.nodeSize
import com.bulletphysics.linearmath.AabbUtil
import com.bulletphysics.linearmath.MiscUtil
import com.bulletphysics.linearmath.VectorUtil.div
import com.bulletphysics.linearmath.VectorUtil.getCoord
import com.bulletphysics.linearmath.VectorUtil.maxAxis
import com.bulletphysics.linearmath.VectorUtil.mul
import com.bulletphysics.linearmath.VectorUtil.setMax
import com.bulletphysics.linearmath.VectorUtil.setMin
import cz.advel.stack.Stack
import java.io.Serializable
import org.joml.Vector3d
import com.bulletphysics.util.setAdd
import com.bulletphysics.util.setSub
import me.anno.utils.structures.lists.Lists.swap
import kotlin.math.max
import kotlin.math.min

// JAVA NOTE: OptimizedBvh still from 2.66, update it for 2.70b1
/**
 * OptimizedBvh store an AABB tree that can be quickly traversed on CPU (and SPU, GPU in future).
 *
 * @author jezek2
 */
class OptimizedBvh : Serializable {

    private val leafNodes = ArrayList<OptimizedBvhNode>()
    private val contiguousNodes = ArrayList<OptimizedBvhNode>()

    private val quantizedLeafNodes = QuantizedBvhNodes()
    private val quantizedContiguousNodes = QuantizedBvhNodes()

    private var curNodeIndex = 0

    // quantization data
    private var useQuantization = false
    private val bvhAabbMin = Vector3d()
    private val bvhAabbMax = Vector3d()
    private val bvhQuantization = Vector3d()

    var traversalMode: TraversalMode = TraversalMode.STACKLESS
    val subtreeHeaders = ArrayList<BvhSubtreeInfo>()

    // This is only used for serialization so we don't have to add serialization directly to btAlignedObjectArray
    var subtreeHeaderCount: Int = 0

    // two versions, one for quantized and normal nodes. This allows code-reuse while maintaining readability (no template/macro!)
    // this might be refactored into a virtual, it is usually not calculated at run-time
    fun setInternalNodeAabbMin(nodeIndex: Int, aabbMin: Vector3d) {
        if (useQuantization) {
            quantizedContiguousNodes.setQuantizedAabbMin(nodeIndex, quantizeWithClamp(aabbMin))
        } else {
            contiguousNodes[nodeIndex].aabbMinOrg.set(aabbMin)
        }
    }

    fun setInternalNodeAabbMax(nodeIndex: Int, aabbMax: Vector3d) {
        if (useQuantization) {
            quantizedContiguousNodes.setQuantizedAabbMax(nodeIndex, quantizeWithClamp(aabbMax))
        } else {
            contiguousNodes[nodeIndex].aabbMaxOrg.set(aabbMax)
        }
    }

    fun getAabbMin(nodeIndex: Int): Vector3d {
        if (useQuantization) {
            val tmp = Vector3d()
            unQuantize(tmp, quantizedLeafNodes.getQuantizedAabbMin(nodeIndex))
            return tmp
        }

        // non-quantized
        return leafNodes[nodeIndex].aabbMinOrg
    }

    fun getAabbMax(nodeIndex: Int): Vector3d {
        if (useQuantization) {
            val tmp = Vector3d()
            unQuantize(tmp, quantizedLeafNodes.getQuantizedAabbMax(nodeIndex))
            return tmp
        }

        // non-quantized
        return leafNodes[nodeIndex].aabbMaxOrg
    }

    fun setQuantizationValues(aabbMin: Vector3d, aabbMax: Vector3d) {
        setQuantizationValues(aabbMin, aabbMax, 1.0)
    }

    fun setQuantizationValues(aabbMin: Vector3d, aabbMax: Vector3d, quantizationMargin: Double) {
        // enlarge the AABB to avoid division by zero when initializing the quantization values
        val clampValue = Stack.newVec()
        clampValue.set(quantizationMargin, quantizationMargin, quantizationMargin)
        bvhAabbMin.setSub(aabbMin, clampValue)
        bvhAabbMax.setAdd(aabbMax, clampValue)
        val aabbSize = Stack.newVec()
        aabbSize.setSub(bvhAabbMax, bvhAabbMin)
        bvhQuantization.set(65535.0, 65535.0, 65535.0)
        div(bvhQuantization, bvhQuantization, aabbSize)
        Stack.subVec(2)
    }

    fun setInternalNodeEscapeIndex(nodeIndex: Int, escapeIndex: Int) {
        if (useQuantization) {
            quantizedContiguousNodes.setEscapeIndexOrTriangleIndex(nodeIndex, -escapeIndex)
        } else {
            contiguousNodes[nodeIndex].escapeIndex = escapeIndex
        }
    }

    fun mergeInternalNodeAabb(nodeIndex: Int, newAabbMin: Vector3d, newAabbMax: Vector3d) {
        if (useQuantization) {
            val quantizedAabbMin: Long = quantizeWithClamp(newAabbMin)
            val quantizedAabbMax: Long = quantizeWithClamp(newAabbMax)
            for (i in 0..2) {
                if (quantizedContiguousNodes.getQuantizedAabbMin(nodeIndex, i) > getCoord(quantizedAabbMin, i)) {
                    quantizedContiguousNodes.setQuantizedAabbMin(nodeIndex, i, getCoord(quantizedAabbMin, i))
                }

                if (quantizedContiguousNodes.getQuantizedAabbMax(nodeIndex, i) < getCoord(quantizedAabbMax, i)) {
                    quantizedContiguousNodes.setQuantizedAabbMax(nodeIndex, i, getCoord(quantizedAabbMax, i))
                }
            }
        } else {
            // non-quantized
            setMin(contiguousNodes[nodeIndex].aabbMinOrg, newAabbMin)
            setMax(contiguousNodes[nodeIndex].aabbMaxOrg, newAabbMax)
        }
    }

    fun swapLeafNodes(i: Int, splitIndex: Int) {
        if (useQuantization) {
            quantizedLeafNodes.swap(i, splitIndex)
        } else {
            // JAVA NOTE: changing reference instead of copy
            leafNodes.swap(i, splitIndex)
        }
    }

    fun assignInternalNodeFromLeafNode(internalNode: Int, leafNodeIndex: Int) {
        if (useQuantization) {
            quantizedContiguousNodes.set(internalNode, quantizedLeafNodes, leafNodeIndex)
        } else {
            contiguousNodes[internalNode].set(leafNodes[leafNodeIndex])
        }
    }

    private class NodeTriangleCallback(val triangleNodes: ArrayList<OptimizedBvhNode>) :
        InternalTriangleIndexCallback {
        private val aabbMin = Vector3d()
        private val aabbMax = Vector3d()

        override fun internalProcessTriangleIndex(triangle: Array<Vector3d>, partId: Int, triangleIndex: Int) {
            val node = OptimizedBvhNode()
            aabbMin.set(1e308, 1e308, 1e308)
            aabbMax.set(-1e308, -1e308, -1e308)
            setMin(aabbMin, triangle[0])
            setMax(aabbMax, triangle[0])
            setMin(aabbMin, triangle[1])
            setMax(aabbMax, triangle[1])
            setMin(aabbMin, triangle[2])
            setMax(aabbMax, triangle[2])

            // with quantization?
            node.aabbMinOrg.set(aabbMin)
            node.aabbMaxOrg.set(aabbMax)

            node.escapeIndex = -1

            // for child nodes
            node.subPart = partId
            node.triangleIndex = triangleIndex
            triangleNodes.add(node)
        }
    }

    private class QuantizedNodeTriangleCallback(
        var triangleNodes: QuantizedBvhNodes, // for quantization
        var optimizedTree: OptimizedBvh
    ) : InternalTriangleIndexCallback {

        override fun internalProcessTriangleIndex(triangle: Array<Vector3d>, partId: Int, triangleIndex: Int) {
            // The partId and triangle index must fit in the same (positive) integer
            assert(partId < (1 shl MAX_NUM_PARTS_IN_BITS))
            assert(triangleIndex < (1 shl (31 - MAX_NUM_PARTS_IN_BITS)))
            // negative indices are reserved for escapeIndex
            assert(triangleIndex >= 0)

            val nodeId = triangleNodes.add()
            val aabbMin = Stack.newVec()
            val aabbMax = Stack.newVec()
            aabbMin.set(1e308, 1e308, 1e308)
            aabbMax.set(-1e308, -1e308, -1e308)
            setMin(aabbMin, triangle[0])
            setMax(aabbMax, triangle[0])
            setMin(aabbMin, triangle[1])
            setMax(aabbMax, triangle[1])
            setMin(aabbMin, triangle[2])
            setMax(aabbMax, triangle[2])

            // PCK: add these checks for zero dimensions of aabb
            val MIN_AABB_DIMENSION = 0.002
            val MIN_AABB_HALF_DIMENSION = 0.001
            if (aabbMax.x - aabbMin.x < MIN_AABB_DIMENSION) {
                aabbMax.x = (aabbMax.x + MIN_AABB_HALF_DIMENSION)
                aabbMin.x = (aabbMin.x - MIN_AABB_HALF_DIMENSION)
            }
            if (aabbMax.y - aabbMin.y < MIN_AABB_DIMENSION) {
                aabbMax.y = (aabbMax.y + MIN_AABB_HALF_DIMENSION)
                aabbMin.y = (aabbMin.y - MIN_AABB_HALF_DIMENSION)
            }
            if (aabbMax.z - aabbMin.z < MIN_AABB_DIMENSION) {
                aabbMax.z = (aabbMax.z + MIN_AABB_HALF_DIMENSION)
                aabbMin.z = (aabbMin.z - MIN_AABB_HALF_DIMENSION)
            }

            triangleNodes.setQuantizedAabbMin(nodeId, optimizedTree.quantizeWithClamp(aabbMin))
            triangleNodes.setQuantizedAabbMax(nodeId, optimizedTree.quantizeWithClamp(aabbMax))

            triangleNodes.setEscapeIndexOrTriangleIndex(
                nodeId,
                (partId shl (31 - MAX_NUM_PARTS_IN_BITS)) or triangleIndex
            )
            Stack.subVec(2)
        }
    }

    fun build(
        triangles: StridingMeshInterface,
        useQuantizedAabbCompression: Boolean,
        _aabbMin: Vector3d?, _aabbMax: Vector3d?
    ) {
        this.useQuantization = useQuantizedAabbCompression

        // NodeArray	triangleNodes;
        val numLeafNodes: Int

        if (useQuantization) {
            // initialize quantization values
            setQuantizationValues(_aabbMin!!, _aabbMax!!)

            val callback = QuantizedNodeTriangleCallback(quantizedLeafNodes, this)

            triangles.internalProcessAllTriangles(callback)

            // now we have an array of leafnodes in m_leafNodes
            numLeafNodes = quantizedLeafNodes.size()

            quantizedContiguousNodes.resize(2 * numLeafNodes)
        } else {
            val callback = NodeTriangleCallback(leafNodes)

            val aabbMin = Stack.newVec()
            val aabbMax = Stack.newVec()
            aabbMin.set(-1e308, -1e308, -1e308)
            aabbMax.set(1e308, 1e308, 1e308)

            triangles.internalProcessAllTriangles(callback)
            Stack.subVec(2)

            // now we have an array of leafnodes in m_leafNodes
            numLeafNodes = leafNodes.size

            // TODO: check
            //contiguousNodes.resize(2*numLeafNodes);
            MiscUtil.resize(contiguousNodes, 2 * numLeafNodes, OptimizedBvhNode::class.java)
        }

        curNodeIndex = 0

        buildTree(0, numLeafNodes)

        //  if the entire tree is small then subtree size, we need to create a header info for the tree
        if (useQuantization && subtreeHeaders.isEmpty()) {
            val subtree = BvhSubtreeInfo()
            subtreeHeaders.add(subtree)

            subtree.setAabbFromQuantizeNode(quantizedContiguousNodes, 0)
            subtree.rootNodeIndex = 0
            subtree.subtreeSize =
                if (quantizedContiguousNodes.isLeafNode(0)) 1 else quantizedContiguousNodes.getEscapeIndex(0)
        }

        // PCK: update the copy of the size
        subtreeHeaderCount = subtreeHeaders.size

        // PCK: clear m_quantizedLeafNodes and m_leafNodes, they are temporary
        quantizedLeafNodes.clear()
        leafNodes.clear()
    }

    fun refit(meshInterface: StridingMeshInterface) {
        if (useQuantization) {
            // calculate new aabb
            val aabbMin = Stack.newVec()
            val aabbMax = Stack.newVec()
            meshInterface.calculateAabbBruteForce(aabbMin, aabbMax)

            setQuantizationValues(aabbMin, aabbMax)

            updateBvhNodes(meshInterface, 0, curNodeIndex)

            // now update all subtree headers
            for (i in 0 until subtreeHeaders.size) {
                val subtree = subtreeHeaders[i]
                subtree.setAabbFromQuantizeNode(quantizedContiguousNodes, subtree.rootNodeIndex)
            }
        } else {
            // JAVA NOTE: added for testing, it's too slow for practical use
            build(meshInterface, false, null, null)
        }
    }

    fun refitPartial(meshInterface: StridingMeshInterface?, aabbMin: Vector3d, aabbMax: Vector3d) {
        throw UnsupportedOperationException()
        //		// incrementally initialize quantization values
//		assert (useQuantization);
//
//		btAssert(aabbMin.getX() > m_bvhAabbMin.getX());
//		btAssert(aabbMin.getY() > m_bvhAabbMin.getY());
//		btAssert(aabbMin.getZ() > m_bvhAabbMin.getZ());
//
//		btAssert(aabbMax.getX() < m_bvhAabbMax.getX());
//		btAssert(aabbMax.getY() < m_bvhAabbMax.getY());
//		btAssert(aabbMax.getZ() < m_bvhAabbMax.getZ());
//
//		///we should update all quantization values, using updateBvhNodes(meshInterface);
//		///but we only update chunks that overlap the given aabb
//
//		unsigned short	quantizedQueryAabbMin[3];
//		unsigned short	quantizedQueryAabbMax[3];
//
//		quantizeWithClamp(&quantizedQueryAabbMin[0],aabbMin);
//		quantizeWithClamp(&quantizedQueryAabbMax[0],aabbMax);
//
//		int i;
//		for (i=0;i<this->m_SubtreeHeaders.size();i++)
//		{
//			btBvhSubtreeInfo& subtree = m_SubtreeHeaders[i];
//
//			//PCK: unsigned instead of bool
//			unsigned overlap = testQuantizedAabbAgainstQuantizedAabb(quantizedQueryAabbMin,quantizedQueryAabbMax,subtree.m_quantizedAabbMin,subtree.m_quantizedAabbMax);
//			if (overlap != 0)
//			{
//				updateBvhNodes(meshInterface,subtree.m_rootNodeIndex,subtree.m_rootNodeIndex+subtree.m_subtreeSize,i);
//
//				subtree.setAabbFromQuantizeNode(m_quantizedContiguousNodes[subtree.m_rootNodeIndex]);
//			}
//		}
    }

    fun updateBvhNodes(meshInterface: StridingMeshInterface, firstNode: Int, endNode: Int) {
        assert(useQuantization)

        val curNodeSubPart = -1

        val triangleVerts = arrayOf<Vector3d>(Stack.newVec(), Stack.newVec(), Stack.newVec())
        val aabbMin = Stack.newVec()
        val aabbMax = Stack.newVec()
        val meshScaling = meshInterface.getScaling(Stack.newVec())

        var data: VertexData? = null

        for (i in endNode - 1 downTo firstNode) {
            val curNodes = quantizedContiguousNodes

            if (curNodes.isLeafNode(i)) {
                // recalc aabb from triangle data
                val nodeSubPart = curNodes.getPartId(i)
                val nodeTriangleIndex = curNodes.getTriangleIndex(i)
                if (nodeSubPart != curNodeSubPart) {
                    data = meshInterface.getLockedReadOnlyVertexIndexBase(nodeSubPart)
                }

                checkNotNull(data)
                data.getTriangle(nodeTriangleIndex * 3, meshScaling, triangleVerts)

                aabbMin.set(1e308, 1e308, 1e308)
                aabbMax.set(-1e308, -1e308, -1e308)
                setMin(aabbMin, triangleVerts[0])
                setMax(aabbMax, triangleVerts[0])
                setMin(aabbMin, triangleVerts[1])
                setMax(aabbMax, triangleVerts[1])
                setMin(aabbMin, triangleVerts[2])
                setMax(aabbMax, triangleVerts[2])

                curNodes.setQuantizedAabbMin(i, quantizeWithClamp(aabbMin))
                curNodes.setQuantizedAabbMax(i, quantizeWithClamp(aabbMax))
            } else {
                // combine aabb from both children

                //quantizedContiguousNodes

                val leftChildNodeId = i + 1

                val rightChildNodeId =
                    if (quantizedContiguousNodes.isLeafNode(leftChildNodeId)) i + 2 else i + 1 + quantizedContiguousNodes.getEscapeIndex(
                        leftChildNodeId
                    )

                for (i2 in 0..2) {
                    curNodes.setQuantizedAabbMin(
                        i,
                        i2,
                        quantizedContiguousNodes.getQuantizedAabbMin(leftChildNodeId, i2)
                    )
                    if (curNodes.getQuantizedAabbMin(i, i2) > quantizedContiguousNodes.getQuantizedAabbMin(
                            rightChildNodeId,
                            i2
                        )
                    ) {
                        curNodes.setQuantizedAabbMin(
                            i,
                            i2,
                            quantizedContiguousNodes.getQuantizedAabbMin(rightChildNodeId, i2)
                        )
                    }

                    curNodes.setQuantizedAabbMax(
                        i,
                        i2,
                        quantizedContiguousNodes.getQuantizedAabbMax(leftChildNodeId, i2)
                    )
                    if (curNodes.getQuantizedAabbMax(i, i2) < quantizedContiguousNodes.getQuantizedAabbMax(
                            rightChildNodeId,
                            i2
                        )
                    ) {
                        curNodes.setQuantizedAabbMax(
                            i,
                            i2,
                            quantizedContiguousNodes.getQuantizedAabbMax(rightChildNodeId, i2)
                        )
                    }
                }
            }
        }
        Stack.subVec(6)
    }

    fun buildTree(startIndex: Int, endIndex: Int) {
        //#ifdef DEBUG_TREE_BUILDING
        if (DEBUG_TREE_BUILDING) {
            gStackDepth++
            if (gStackDepth > gMaxStackDepth) {
                gMaxStackDepth = gStackDepth
            }
        }

        //#endif //DEBUG_TREE_BUILDING
        val splitAxis: Int
        val splitIndex: Int
        var i: Int
        val numIndices = endIndex - startIndex
        val curIndex = curNodeIndex

        assert(numIndices > 0)

        if (numIndices == 1) {
            //#ifdef DEBUG_TREE_BUILDING
            if (DEBUG_TREE_BUILDING) {
                gStackDepth--
            }

            //#endif //DEBUG_TREE_BUILDING
            assignInternalNodeFromLeafNode(curNodeIndex, startIndex)

            curNodeIndex++
            return
        }

        // calculate Best Splitting Axis and where to split it. Sort the incoming 'leafNodes' array within range 'startIndex/endIndex'.
        splitAxis = calcSplittingAxis(startIndex, endIndex)
        splitIndex = sortAndCalcSplittingIndex(startIndex, endIndex, splitAxis)

        val internalNodeIndex = curNodeIndex

        val tmp = Stack.newVec()
        tmp.set(-1e308, -1e308, -1e308)
        setInternalNodeAabbMax(curNodeIndex, tmp)
        tmp.set(1e308, 1e308, 1e308)
        setInternalNodeAabbMin(curNodeIndex, tmp)
        Stack.subVec(1)

        i = startIndex
        while (i < endIndex) {
            mergeInternalNodeAabb(curNodeIndex, getAabbMin(i), getAabbMax(i))
            i++
        }

        curNodeIndex++

        //internalNode->m_escapeIndex;
        val leftChildNodexIndex = curNodeIndex

        //build left child tree
        buildTree(startIndex, splitIndex)

        val rightChildNodexIndex = curNodeIndex
        // build right child tree
        buildTree(splitIndex, endIndex)

        //#ifdef DEBUG_TREE_BUILDING
        if (DEBUG_TREE_BUILDING) {
            gStackDepth--
        }

        //#endif //DEBUG_TREE_BUILDING
        val escapeIndex = curNodeIndex - curIndex

        if (useQuantization) {
            // escapeIndex is the number of nodes of this subtree
            val sizeQuantizedNode = nodeSize
            val treeSizeInBytes = escapeIndex * sizeQuantizedNode
            if (treeSizeInBytes > MAX_SUBTREE_SIZE_IN_BYTES) {
                updateSubtreeHeaders(leftChildNodexIndex, rightChildNodexIndex)
            }
        }

        setInternalNodeEscapeIndex(internalNodeIndex, escapeIndex)
    }

    fun testQuantizedAabbAgainstQuantizedAabb(
        aabbMin1: Long, aabbMax1: Long,
        aabbMin2: Long, aabbMax2: Long
    ): Boolean {
        val aabbMin1_0 = getCoord(aabbMin1, 0)
        val aabbMin1_1 = getCoord(aabbMin1, 1)
        val aabbMin1_2 = getCoord(aabbMin1, 2)

        val aabbMax1_0 = getCoord(aabbMax1, 0)
        val aabbMax1_1 = getCoord(aabbMax1, 1)
        val aabbMax1_2 = getCoord(aabbMax1, 2)

        val aabbMin2_0 = getCoord(aabbMin2, 0)
        val aabbMin2_1 = getCoord(aabbMin2, 1)
        val aabbMin2_2 = getCoord(aabbMin2, 2)

        val aabbMax2_0 = getCoord(aabbMax2, 0)
        val aabbMax2_1 = getCoord(aabbMax2, 1)
        val aabbMax2_2 = getCoord(aabbMax2, 2)

        var overlap: Boolean
        overlap = aabbMin1_0 <= aabbMax2_0 && aabbMax1_0 >= aabbMin2_0
        overlap = aabbMin1_2 <= aabbMax2_2 && aabbMax1_2 >= aabbMin2_2 && overlap
        overlap = aabbMin1_1 <= aabbMax2_1 && aabbMax1_1 >= aabbMin2_1 && overlap
        return overlap
    }

    fun updateSubtreeHeaders(leftChildNodexIndex: Int, rightChildNodexIndex: Int) {
        assert(useQuantization)

        //btQuantizedBvhNode& leftChildNode = m_quantizedContiguousNodes[leftChildNodexIndex];
        val leftSubTreeSize =
            if (quantizedContiguousNodes.isLeafNode(leftChildNodexIndex)) 1 else quantizedContiguousNodes.getEscapeIndex(
                leftChildNodexIndex
            )
        val leftSubTreeSizeInBytes = leftSubTreeSize * nodeSize

        //btQuantizedBvhNode& rightChildNode = m_quantizedContiguousNodes[rightChildNodexIndex];
        val rightSubTreeSize =
            if (quantizedContiguousNodes.isLeafNode(rightChildNodexIndex)) 1 else quantizedContiguousNodes.getEscapeIndex(
                rightChildNodexIndex
            )
        val rightSubTreeSizeInBytes = rightSubTreeSize * nodeSize

        if (leftSubTreeSizeInBytes <= MAX_SUBTREE_SIZE_IN_BYTES) {
            val subtree = BvhSubtreeInfo()
            subtreeHeaders.add(subtree)

            subtree.setAabbFromQuantizeNode(quantizedContiguousNodes, leftChildNodexIndex)
            subtree.rootNodeIndex = leftChildNodexIndex
            subtree.subtreeSize = leftSubTreeSize
        }

        if (rightSubTreeSizeInBytes <= MAX_SUBTREE_SIZE_IN_BYTES) {
            val subtree = BvhSubtreeInfo()
            subtreeHeaders.add(subtree)

            subtree.setAabbFromQuantizeNode(quantizedContiguousNodes, rightChildNodexIndex)
            subtree.rootNodeIndex = rightChildNodexIndex
            subtree.subtreeSize = rightSubTreeSize
        }

        // PCK: update the copy of the size
        subtreeHeaderCount = subtreeHeaders.size
    }

    fun sortAndCalcSplittingIndex(startIndex: Int, endIndex: Int, splitAxis: Int): Int {
        var splitIndex = startIndex
        val numIndices = endIndex - startIndex
        val splitValue: Double

        val means = Stack.newVec()
        means.set(0.0, 0.0, 0.0)
        val center = Stack.newVec()
        for (i in startIndex until endIndex) {
            center.setAdd(getAabbMax(i), getAabbMin(i))
            center.mul(0.5)
            means.add(center)
        }
        means.mul(1.0 / numIndices.toDouble())

        splitValue = getCoord(means, splitAxis)

        //sort leafNodes so all values larger than splitValue comes first, and smaller values start from 'splitIndex'.
        for (i in startIndex until endIndex) {
            center.setAdd(getAabbMax(i), getAabbMin(i))
            center.mul(0.5)

            if (getCoord(center, splitAxis) > splitValue) {
                // swap
                swapLeafNodes(i, splitIndex)
                splitIndex++
            }
        }
        Stack.subVec(2)

        // if the splitIndex causes unbalanced trees, fix this by using the center in between startIndex and endIndex
        // otherwise the tree-building might fail due to stack-overflows in certain cases.
        // unbalanced1 is unsafe: it can cause stack overflows
        // bool unbalanced1 = ((splitIndex==startIndex) || (splitIndex == (endIndex-1)));

        // unbalanced2 should work too: always use center (perfect balanced trees)
        // bool unbalanced2 = true;

        // this should be safe too:
        val rangeBalancedIndices = numIndices / 3
        val unbalanced =
            ((splitIndex <= (startIndex + rangeBalancedIndices)) || (splitIndex >= (endIndex - 1 - rangeBalancedIndices)))

        if (unbalanced) {
            splitIndex = startIndex + (numIndices shr 1)
        }

        val unbalanced1 = (splitIndex == startIndex) || (splitIndex == (endIndex))
        assert(!unbalanced1)

        return splitIndex
    }

    fun calcSplittingAxis(startIndex: Int, endIndex: Int): Int {
        var i: Int

        val means = Stack.newVec()
        means.set(0.0, 0.0, 0.0)
        val variance = Stack.newVec()
        variance.set(0.0, 0.0, 0.0)
        val numIndices = endIndex - startIndex

        val center = Stack.newVec()
        i = startIndex
        while (i < endIndex) {
            center.setAdd(getAabbMax(i), getAabbMin(i))
            center.mul(0.5)
            means.add(center)
            i++
        }
        means.mul(1.0 / numIndices.toDouble())

        val diff2 = Stack.newVec()
        i = startIndex
        while (i < endIndex) {
            center.setAdd(getAabbMax(i), getAabbMin(i))
            center.mul(0.5)
            diff2.setSub(center, means)
            //diff2 = diff2 * diff2;
            mul(diff2, diff2, diff2)
            variance.add(diff2)
            i++
        }
        variance.mul(1.0 / (numIndices.toDouble() - 1))
        Stack.subVec(4)

        return maxAxis(variance)
    }

    fun reportAabbOverlappingNodes(nodeCallback: NodeOverlapCallback, aabbMin: Vector3d, aabbMax: Vector3d) {
        // either choose recursive traversal (walkTree) or stackless (walkStacklessTree)

        if (useQuantization) {
            // quantize query AABB
            val quantizedQueryAabbMin: Long = quantizeWithClamp(aabbMin)
            val quantizedQueryAabbMax: Long = quantizeWithClamp(aabbMax)

            when (traversalMode) {
                TraversalMode.STACKLESS -> walkStacklessQuantizedTree(
                    nodeCallback,
                    quantizedQueryAabbMin,
                    quantizedQueryAabbMax,
                    0,
                    curNodeIndex
                )
                TraversalMode.RECURSIVE -> walkRecursiveQuantizedTreeAgainstQueryAabb(
                    quantizedContiguousNodes,
                    0,
                    nodeCallback,
                    quantizedQueryAabbMin,
                    quantizedQueryAabbMax
                )
                else -> assert(false) // unsupported
            }
        } else {
            walkStacklessTree(nodeCallback, aabbMin, aabbMax)
        }
    }

    fun walkStacklessTree(nodeCallback: NodeOverlapCallback, aabbMin: Vector3d, aabbMax: Vector3d) {
        assert(!useQuantization)

        // JAVA NOTE: rewritten
        var rootNode: OptimizedBvhNode //contiguousNodes.get(0);
        var rootNodeIndex = 0

        var escapeIndex: Int
        var curIndex = 0
        var walkIterations = 0
        var isLeafNode: Boolean
        //PCK: unsigned instead of bool
        //unsigned aabbOverlap;
        var aabbOverlap: Boolean

        while (curIndex < curNodeIndex) {
            // catch bugs in tree data
            assert(walkIterations < curNodeIndex)

            walkIterations++

            rootNode = contiguousNodes[rootNodeIndex]

            aabbOverlap = AabbUtil.testAabbAgainstAabb2(aabbMin, aabbMax, rootNode.aabbMinOrg, rootNode.aabbMaxOrg)
            isLeafNode = (rootNode.escapeIndex == -1)

            // PCK: unsigned instead of bool
            if (isLeafNode && (aabbOverlap /* != 0*/)) {
                nodeCallback.processNode(rootNode.subPart, rootNode.triangleIndex)
            }

            //PCK: unsigned instead of bool
            if ((aabbOverlap /* != 0*/) || isLeafNode) {
                rootNodeIndex++
                curIndex++
            } else {
                escapeIndex =  /*rootNode*/contiguousNodes[rootNodeIndex].escapeIndex
                rootNodeIndex += escapeIndex
                curIndex += escapeIndex
            }
        }
        if (maxIterations < walkIterations) {
            maxIterations = walkIterations
        }
    }

    fun walkRecursiveQuantizedTreeAgainstQueryAabb(
        currentNodes: QuantizedBvhNodes, currentNodeId: Int, nodeCallback: NodeOverlapCallback,
        quantizedQueryAabbMin: Long, quantizedQueryAabbMax: Long
    ) {
        assert(useQuantization)

        val aabbOverlap = testQuantizedAabbAgainstQuantizedAabb(
            quantizedQueryAabbMin, quantizedQueryAabbMax,
            currentNodes.getQuantizedAabbMin(currentNodeId), currentNodes.getQuantizedAabbMax(currentNodeId)
        )
        val isLeafNode = currentNodes.isLeafNode(currentNodeId)

        if (aabbOverlap) {
            if (isLeafNode) {
                nodeCallback.processNode(
                    currentNodes.getPartId(currentNodeId),
                    currentNodes.getTriangleIndex(currentNodeId)
                )
            } else {
                // process left and right children
                val leftChildNodeId = currentNodeId + 1
                walkRecursiveQuantizedTreeAgainstQueryAabb(
                    currentNodes,
                    leftChildNodeId,
                    nodeCallback,
                    quantizedQueryAabbMin,
                    quantizedQueryAabbMax
                )

                val rightChildNodeId =
                    if (currentNodes.isLeafNode(leftChildNodeId)) leftChildNodeId + 1 else leftChildNodeId + currentNodes.getEscapeIndex(
                        leftChildNodeId
                    )
                walkRecursiveQuantizedTreeAgainstQueryAabb(
                    currentNodes,
                    rightChildNodeId,
                    nodeCallback,
                    quantizedQueryAabbMin,
                    quantizedQueryAabbMax
                )
            }
        }
    }

    fun walkStacklessQuantizedTreeAgainstRay(
        nodeCallback: NodeOverlapCallback, raySource: Vector3d, rayTarget: Vector3d,
        aabbMin: Vector3d, aabbMax: Vector3d, startNodeIndex: Int, endNodeIndex: Int
    ) {
        assert(useQuantization)

        val tmp = Stack.newVec()

        var curIndex = startNodeIndex
        var walkIterations = 0
        val subTreeSize = endNodeIndex - startNodeIndex

        val rootNode = quantizedContiguousNodes
        var rootNode_idx = startNodeIndex
        var escapeIndex: Int

        var isLeafNode: Boolean
        var boxBoxOverlap: Boolean
        var rayBoxOverlap: Boolean

        val rayDirection = Stack.newVec()
        tmp.setSub(rayTarget, raySource)
        tmp.normalize(rayDirection)
        rayDirection.x = 1.0 / rayDirection.x
        rayDirection.y = 1.0 / rayDirection.y
        rayDirection.z = 1.0 / rayDirection.z

        /* Quick pruning by quantized box */
        val rayAabbMin = Stack.newVec(raySource)
        val rayAabbMax = Stack.newVec(raySource)
        setMin(rayAabbMin, rayTarget)
        setMax(rayAabbMax, rayTarget)

        /* Add box cast extents to bounding box */
        rayAabbMin.add(aabbMin)
        rayAabbMax.add(aabbMax)

        val quantizedQueryAabbMin: Long
        val quantizedQueryAabbMax: Long
        quantizedQueryAabbMin = quantizeWithClamp(rayAabbMin)
        quantizedQueryAabbMax = quantizeWithClamp(rayAabbMax)

        val bounds_0 = Stack.newVec()
        val bounds_1 = Stack.newVec()
        val normal = Stack.newVec()
        val param = Stack.newDoublePtr()

        while (curIndex < endNodeIndex) {
            // catch bugs in tree data

            assert(walkIterations < subTreeSize)

            walkIterations++
            // only interested if this is closer than any previous hit
            param[0] = 1.0
            rayBoxOverlap = false
            boxBoxOverlap = testQuantizedAabbAgainstQuantizedAabb(
                quantizedQueryAabbMin,
                quantizedQueryAabbMax,
                rootNode.getQuantizedAabbMin(rootNode_idx),
                rootNode.getQuantizedAabbMax(rootNode_idx)
            )
            isLeafNode = rootNode.isLeafNode(rootNode_idx)
            if (boxBoxOverlap) {
                unQuantize(bounds_0, rootNode.getQuantizedAabbMin(rootNode_idx))
                unQuantize(bounds_1, rootNode.getQuantizedAabbMax(rootNode_idx))
                /* Add box cast extents */
                bounds_0.add(aabbMin)
                bounds_1.add(aabbMax)
                rayBoxOverlap = AabbUtil.rayAabb(raySource, rayTarget, bounds_0, bounds_1, param, normal)
            }

            if (isLeafNode && rayBoxOverlap) {
                nodeCallback.processNode(rootNode.getPartId(rootNode_idx), rootNode.getTriangleIndex(rootNode_idx))
            }

            if (rayBoxOverlap || isLeafNode) {
                rootNode_idx++
                curIndex++
            } else {
                escapeIndex = rootNode.getEscapeIndex(rootNode_idx)
                rootNode_idx += escapeIndex
                curIndex += escapeIndex
            }
        }

        if (maxIterations < walkIterations) {
            maxIterations = walkIterations
        }

        Stack.subVec(7)
        Stack.subDoublePtr(1)
    }

    fun walkStacklessQuantizedTree(
        nodeCallback: NodeOverlapCallback, quantizedQueryAabbMin: Long, quantizedQueryAabbMax: Long,
        startNodeIndex: Int, endNodeIndex: Int
    ) {
        assert(useQuantization)

        var curIndex = startNodeIndex
        var walkIterations = 0
        val subTreeSize = endNodeIndex - startNodeIndex

        val rootNode = quantizedContiguousNodes
        var rootNodeIdx = startNodeIndex
        var escapeIndex: Int

        var isLeafNode: Boolean
        var aabbOverlap: Boolean

        while (curIndex < endNodeIndex) {
            // catch bugs in tree data

            assert(walkIterations < subTreeSize)

            walkIterations++
            aabbOverlap = testQuantizedAabbAgainstQuantizedAabb(
                quantizedQueryAabbMin,
                quantizedQueryAabbMax,
                rootNode.getQuantizedAabbMin(rootNodeIdx),
                rootNode.getQuantizedAabbMax(rootNodeIdx)
            )
            isLeafNode = rootNode.isLeafNode(rootNodeIdx)

            if (isLeafNode && aabbOverlap) {
                nodeCallback.processNode(rootNode.getPartId(rootNodeIdx), rootNode.getTriangleIndex(rootNodeIdx))
            }

            if (aabbOverlap || isLeafNode) {
                rootNodeIdx++
                curIndex++
            } else {
                escapeIndex = rootNode.getEscapeIndex(rootNodeIdx)
                rootNodeIdx += escapeIndex
                curIndex += escapeIndex
            }
        }

        if (maxIterations < walkIterations) {
            maxIterations = walkIterations
        }
    }

    fun reportRayOverlappingNodex(nodeCallback: NodeOverlapCallback, raySource: Vector3d, rayTarget: Vector3d) {
        val fast_path = useQuantization && traversalMode == TraversalMode.STACKLESS
        if (fast_path) {
            val tmp = Stack.newVec()
            tmp.set(0.0, 0.0, 0.0)
            walkStacklessQuantizedTreeAgainstRay(nodeCallback, raySource, rayTarget, tmp, tmp, 0, curNodeIndex)
        } else {
            /* Otherwise fallback to AABB overlap test */
            val aabbMin = Stack.newVec(raySource)
            val aabbMax = Stack.newVec(raySource)
            setMin(aabbMin, rayTarget)
            setMax(aabbMax, rayTarget)
            reportAabbOverlappingNodes(nodeCallback, aabbMin, aabbMax)
        }
    }

    fun reportBoxCastOverlappingNodex(
        nodeCallback: NodeOverlapCallback,
        raySource: Vector3d, rayTarget: Vector3d,
        aabbMin: Vector3d, aabbMax: Vector3d
    ) {
        val fastPath = useQuantization && traversalMode == TraversalMode.STACKLESS
        if (fastPath) {
            walkStacklessQuantizedTreeAgainstRay(nodeCallback, raySource, rayTarget, aabbMin, aabbMax, 0, curNodeIndex)
        } else {
            /* Slow path:
			Construct the bounding box for the entire box cast and send that down the tree */
            val qaabbMin = Stack.newVec(raySource)
            val qaabbMax = Stack.newVec(raySource)
            setMin(qaabbMin, rayTarget)
            setMax(qaabbMax, rayTarget)
            qaabbMin.add(aabbMin)
            qaabbMax.add(aabbMax)
            reportAabbOverlappingNodes(nodeCallback, qaabbMin, qaabbMax)
            Stack.subVec(2)
        }
    }

    fun quantizeWithClamp(point: Vector3d): Long {
        assert(useQuantization)

        val vx = (min(max(point.x, bvhAabbMin.x), bvhAabbMax.x) - bvhAabbMin.x) * bvhQuantization.x
        val vy = (min(max(point.y, bvhAabbMin.y), bvhAabbMax.y) - bvhAabbMin.y) * bvhQuantization.y
        val vz = (min(max(point.z, bvhAabbMin.z), bvhAabbMax.z) - bvhAabbMin.z) * bvhQuantization.z

        val out0 = (vx + 0.5).toInt() and 0xFFFF
        val out1 = (vy + 0.5).toInt() and 0xFFFF
        val out2 = (vz + 0.5).toInt() and 0xFFFF

        return (out0.toLong()) or ((out1.toLong()) shl 16) or ((out2.toLong()) shl 32)
    }

    fun unQuantize(vecOut: Vector3d, vecIn: Long) {
        val vecIn0 = vecIn.toInt() and 0xFFFF
        val vecIn1 = (vecIn ushr 16).toInt() and 0xFFFF
        val vecIn2 = (vecIn ushr 32).toInt() and 0xFFFF

        vecOut.x = vecIn0.toDouble() / bvhQuantization.x
        vecOut.y = vecIn1.toDouble() / bvhQuantization.y
        vecOut.z = vecIn2.toDouble() / bvhQuantization.z

        vecOut.add(bvhAabbMin)
    }

    companion object {
        private const val DEBUG_TREE_BUILDING = false
        private var gStackDepth = 0
        private var gMaxStackDepth = 0

        private var maxIterations = 0

        // Note: currently we have 16 bytes per quantized node
        const val MAX_SUBTREE_SIZE_IN_BYTES: Int = 2048

        // 10 gives the potential for 1024 parts, with at most 2^21 (2097152) (minus one
        // actually) triangles each (since the sign bit is reserved
        const val MAX_NUM_PARTS_IN_BITS: Int = 10
    }
}
