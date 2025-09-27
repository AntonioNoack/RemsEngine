package com.bulletphysics.extras.gimpact

import com.bulletphysics.linearmath.Transform
import me.anno.utils.structures.arrays.IntArrayList
import org.joml.Vector3d

/**
 * @author jezek2
 */
class GImpactBvh {
    private val bvhTree = BvhTree()
    var primitiveManager: PrimitiveManagerBase?

    /**
     * This constructor doesn't build the tree. you must call buildSet.
     */
    constructor() {
        primitiveManager = null
    }

    /**
     * This constructor doesn't build the tree. you must call buildSet.
     */
    constructor(primitiveManager: PrimitiveManagerBase?) {
        this.primitiveManager = primitiveManager
    }

    fun getGlobalBox(out: AABB): AABB {
        getNodeBound(0, out)
        return out
    }

    // stackless refit
    fun refit() {
        val leafBox = AABB()
        val bound = AABB()
        val tmpBox = AABB()

        var i = this.nodeCount
        while ((i--) != 0) {
            if (isLeafNode(i)) {
                primitiveManager!!.getPrimitiveBox(getNodeData(i), leafBox)
                setNodeBound(i, leafBox)
            } else {
                //const BT_BVH_TREE_NODE * nodepointer = get_node_pointer(nodecount);
                //get left bound
                bound.invalidate()

                var childNode = getLeftNode(i)
                if (childNode != 0) {
                    getNodeBound(childNode, tmpBox)
                    bound.merge(tmpBox)
                }

                childNode = getRightNode(i)
                if (childNode != 0) {
                    getNodeBound(childNode, tmpBox)
                    bound.merge(tmpBox)
                }

                setNodeBound(i, bound)
            }
        }
    }

    /**
     * This attemps to refit the box set.
     */
    fun update() {
        refit()
    }

    /**
     * This rebuild the entire set.
     */
    fun buildSet() {
        // obtain primitive boxes
        val primitiveBoxes = BvhDataArray()
        val primitiveManager = primitiveManager!!
        primitiveBoxes.resize(primitiveManager.primitiveCount)

        val tmpAABB = AABB()
        for (i in 0 until primitiveBoxes.size()) {
            //primitive_manager.get_primitive_box(i,primitive_boxes[i].bound);
            primitiveManager.getPrimitiveBox(i, tmpAABB)
            primitiveBoxes.setBounds(i, tmpAABB)
            primitiveBoxes.setData(i, i)
        }
        bvhTree.buildTree(primitiveBoxes)
    }

    /**
     * Returns the indices of the primitives in the primitive_manager field.
     */
    fun boxQuery(box: AABB, collidedResults: IntArrayList): Boolean {
        var curIndex = 0
        val numNodes = this.nodeCount

        val bound = AABB()

        while (curIndex < numNodes) {
            getNodeBound(curIndex, bound)

            // catch bugs in tree data
            val aabbOverlap = bound.hasCollision(box)
            val isLeafNode = isLeafNode(curIndex)

            if (isLeafNode && aabbOverlap) {
                collidedResults.add(getNodeData(curIndex))
            }

            if (aabbOverlap || isLeafNode) {
                // next subnode
                curIndex++
            } else {
                // skip node
                curIndex += getEscapeNodeIndex(curIndex)
            }
        }
        return collidedResults.size > 0
    }

    /**
     * Returns the indices of the primitives in the primitive_manager field.
     */
    fun boxQueryTrans(box: AABB, transform: Transform, collidedResults: IntArrayList): Boolean {
        val bounds = AABB(box)
        bounds.applyTransform(transform)
        return boxQuery(bounds, collidedResults)
    }

    /**
     * Returns the indices of the primitives in the primitive_manager field.
     */
    fun rayQuery(rayDir: Vector3d, rayOrigin: Vector3d, collidedResults: IntArrayList): Boolean {
        var curIndex = 0
        val numNodes = this.nodeCount

        val bound = AABB()

        while (curIndex < numNodes) {
            getNodeBound(curIndex, bound)

            // catch bugs in tree data
            val aabbOverlap = bound.collideRay(rayOrigin, rayDir)
            val isleafnode = isLeafNode(curIndex)

            if (isleafnode && aabbOverlap) {
                collidedResults.add(getNodeData(curIndex))
            }

            if (aabbOverlap || isleafnode) {
                // next subnode
                curIndex++
            } else {
                // skip node
                curIndex += getEscapeNodeIndex(curIndex)
            }
        }
        return collidedResults.isNotEmpty()
    }

    /**
     * Tells if this set has hierarchy.
     */
    fun hasHierarchy(): Boolean {
        return true
    }

    val isTrimesh: Boolean
        /**
         * Tells if this set is a trimesh.
         */
        get() = primitiveManager!!.isTrimesh

    val nodeCount: Int
        get() = bvhTree.nodeCount

    /**
     * Tells if the node is a leaf.
     */
    fun isLeafNode(nodeIndex: Int): Boolean {
        return bvhTree.isLeafNode(nodeIndex)
    }

    fun getNodeData(nodeIndex: Int): Int {
        return bvhTree.getNodeData(nodeIndex)
    }

    fun getNodeBound(nodeIndex: Int, bound: AABB) {
        bvhTree.getNodeBound(nodeIndex, bound)
    }

    fun setNodeBound(nodeIndex: Int, bound: AABB) {
        bvhTree.setNodeBound(nodeIndex, bound)
    }

    fun getLeftNode(nodeIndex: Int): Int {
        return bvhTree.getLeftNode(nodeIndex)
    }

    fun getRightNode(nodeIndex: Int): Int {
        return bvhTree.getRightNode(nodeIndex)
    }

    fun getEscapeNodeIndex(nodeIndex: Int): Int {
        return bvhTree.getEscapeNodeIndex(nodeIndex)
    }

    fun getNodeTriangle(nodeIndex: Int, triangle: PrimitiveTriangle) {
        primitiveManager!!.getPrimitiveTriangle(getNodeData(nodeIndex), triangle)
    }

    val nodePointer: BvhTreeNodeArray
        get() = bvhTree.nodePointer

    companion object {

        private fun nodeCollision(
            boxset0: GImpactBvh, boxset1: GImpactBvh,
            transCache1to0: BoxBoxTransformCache,
            node0: Int, node1: Int,
            completePrimitiveTests: Boolean
        ): Boolean {
            val box0 = AABB()
            boxset0.getNodeBound(node0, box0)
            val box1 = AABB()
            boxset1.getNodeBound(node1, box1)
            return box0.overlappingTransCache(box1, transCache1to0, completePrimitiveTests)
        }

        /**
         * Stackless recursive collision routine.
         */
        private fun findCollisionPairsRecursive(
            boxset0: GImpactBvh, boxset1: GImpactBvh,
            collisionPairs: IntPairList,
            transCache1to0: BoxBoxTransformCache,
            node0: Int, node1: Int,
            completePrimitiveTests: Boolean
        ) {
            if (!nodeCollision(
                    boxset0, boxset1, transCache1to0,
                    node0, node1, completePrimitiveTests
                )
            ) {
                return  //avoid colliding internal nodes
            }
            if (boxset0.isLeafNode(node0)) {
                if (boxset1.isLeafNode(node1)) {
                    // collision result
                    collisionPairs.pushPair(boxset0.getNodeData(node0), boxset1.getNodeData(node1))
                    return
                } else {
                    // collide left recursive
                    findCollisionPairsRecursive(
                        boxset0, boxset1,
                        collisionPairs, transCache1to0,
                        node0, boxset1.getLeftNode(node1), false
                    )

                    // collide right recursive
                    findCollisionPairsRecursive(
                        boxset0, boxset1,
                        collisionPairs, transCache1to0,
                        node0, boxset1.getRightNode(node1), false
                    )
                }
            } else {
                if (boxset1.isLeafNode(node1)) {
                    // collide left recursive
                    findCollisionPairsRecursive(
                        boxset0, boxset1,
                        collisionPairs, transCache1to0,
                        boxset0.getLeftNode(node0), node1, false
                    )


                    // collide right recursive
                    findCollisionPairsRecursive(
                        boxset0, boxset1,
                        collisionPairs, transCache1to0,
                        boxset0.getRightNode(node0), node1, false
                    )
                } else {
                    // collide left0 left1
                    findCollisionPairsRecursive(
                        boxset0, boxset1,
                        collisionPairs, transCache1to0,
                        boxset0.getLeftNode(node0), boxset1.getLeftNode(node1), false
                    )

                    // collide left0 right1
                    findCollisionPairsRecursive(
                        boxset0, boxset1,
                        collisionPairs, transCache1to0,
                        boxset0.getLeftNode(node0), boxset1.getRightNode(node1), false
                    )

                    // collide right0 left1
                    findCollisionPairsRecursive(
                        boxset0, boxset1,
                        collisionPairs, transCache1to0,
                        boxset0.getRightNode(node0), boxset1.getLeftNode(node1), false
                    )

                    // collide right0 right1
                    findCollisionPairsRecursive(
                        boxset0, boxset1,
                        collisionPairs, transCache1to0,
                        boxset0.getRightNode(node0), boxset1.getRightNode(node1), false
                    )
                } // else if node1 is not a leaf
            } // else if node0 is not a leaf
        }

        fun findCollision(
            box0: GImpactBvh,
            trans0: Transform,
            box1: GImpactBvh,
            trans1: Transform,
            collisionPairs: IntPairList
        ) {
            if (box0.nodeCount == 0 || box1.nodeCount == 0) return

            val transCache1To0 = BoxBoxTransformCache()
            transCache1To0.calcFromHomogenic(trans0, trans1)

            findCollisionPairsRecursive(
                box0, box1,
                collisionPairs, transCache1To0, 0, 0, true
            )
        }
    }
}
