// Dbvt implementation by Nathanael Presson
package com.bulletphysics.collision.broadphase

import cz.advel.stack.Stack
import org.joml.Vector3d

/**
 * @author jezek2
 */
class Dbvt {

    var root: DbvtNode? = null
    var free: DbvtNode? = null
    var lkhd: Int = -1
    var leaves: Int = 0
    /*unsigned*/var oPath: Int = 0

    fun clear() {
        if (root != null) {
            recurseDeleteNode(this, root!!)
        }
        //btAlignedFree(m_free);
        free = null
    }

    val isEmpty: Boolean
        get() = (root == null)

    fun optimizeIncremental(passes: Int) {
        var passes = passes
        if (passes < 0) {
            passes = leaves
        }

        if (root != null && (passes > 0)) {
            val rootRef = arrayOfNulls<DbvtNode>(1)
            do {
                var node = root
                var bit = 0
                while (node!!.isInternal) {
                    rootRef[0] = root
                    node = sort(node, rootRef)
                    node = if (((oPath ushr bit) and 1) == 0) node.child0 else node.child1
                    root = rootRef[0]

                    bit = (bit + 1) and ( /*sizeof(unsigned)*/4 * 8 - 1)
                }
                update(node)
                ++oPath
            } while ((--passes) != 0)
        }
    }

    fun insert(box: DbvtAabbMm, data: Any?): DbvtNode {
        val leaf: DbvtNode = createNode(this, null, box, data)
        insertLeaf(this, root, leaf)
        leaves++
        return leaf
    }

    @JvmOverloads
    fun update(leaf: DbvtNode, lookahead: Int = -1) {
        var root: DbvtNode? = removeLeaf(this, leaf)
        if (root != null) {
            if (lookahead >= 0) {
                var i = 0
                while ((i < lookahead) && root!!.parent != null) {
                    root = root.parent
                    i++
                }
            } else {
                root = this.root
            }
        }
        insertLeaf(this, root, leaf)
    }

    fun update(leaf: DbvtNode, volume: DbvtAabbMm) {
        var root: DbvtNode? = removeLeaf(this, leaf)
        if (root != null) {
            if (lkhd >= 0) {
                var i = 0
                while ((i < lkhd) && root!!.parent != null) {
                    root = root.parent
                    i++
                }
            } else {
                root = this.root
            }
        }
        leaf.volume.set(volume)
        insertLeaf(this, root, leaf)
    }

    fun update(leaf: DbvtNode, volume: DbvtAabbMm, velocity: Vector3d, margin: Double): Boolean {
        if (leaf.volume.contains(volume)) {
            return false
        }
        val tmp = Stack.newVec()
        tmp.set(margin, margin, margin)
        volume.addMargin(tmp)
        volume.addSignedMargin(velocity)
        update(leaf, volume)
        return true
    }

    fun update(leaf: DbvtNode, volume: DbvtAabbMm, velocity: Vector3d): Boolean {
        if (leaf.volume.contains(volume)) {
            return false
        }
        volume.addSignedMargin(velocity)
        update(leaf, volume)
        return true
    }

    fun update(leaf: DbvtNode, volume: DbvtAabbMm, margin: Double): Boolean {
        if (leaf.volume.contains(volume)) {
            return false
        }
        val tmp = Stack.newVec()
        tmp.set(margin, margin, margin)
        volume.addMargin(tmp)
        update(leaf, volume)
        return true
    }

    fun remove(leaf: DbvtNode) {
        removeLeaf(this, leaf)
        deleteNode(this, leaf)
        leaves--
    }

    /** ///////////////////////////////////////////////////////////////////////// */
    open class ICollide {
        open fun process(n1: DbvtNode, n2: DbvtNode) {
        }
    }

    companion object {
        private fun addBranch(remaining: ArrayList<DbvtNode>, na: DbvtNode?, nb: DbvtNode?) {
            // added in reverse, so they can be popped correctly
            remaining.add(nb!!)
            remaining.add(na!!)
        }

        fun collideTT(root0: DbvtNode?, root1: DbvtNode?, policy: ICollide) {
            //DBVT_CHECKTYPE
            if (root0 == null || root1 == null) return

            val remaining = Stack.newList<DbvtNode>()
            addBranch(remaining, root0, root1)
            while (!remaining.isEmpty()) {
                val pa = remaining.removeLast()
                val pb = remaining.removeLast()
                if (pa == pb) {
                    if (pa.isInternal) {
                        addBranch(remaining, pa.child0, pa.child0)
                        addBranch(remaining, pa.child1, pa.child1)
                        addBranch(remaining, pa.child0, pa.child1)
                    }
                } else if (DbvtAabbMm.Companion.intersect(pa.volume, pb.volume)) {
                    if (pa.isInternal) {
                        if (pb.isInternal) {
                            addBranch(remaining, pa.child0, pb.child0)
                            addBranch(remaining, pa.child1, pb.child0)
                            addBranch(remaining, pa.child0, pb.child1)
                            addBranch(remaining, pa.child1, pb.child1)
                        } else {
                            addBranch(remaining, pa.child0, pb)
                            addBranch(remaining, pa.child1, pb)
                        }
                    } else {
                        if (pb.isInternal) {
                            addBranch(remaining, pa, pb.child0)
                            addBranch(remaining, pa, pb.child1)
                        } else {
                            policy.process(pa, pb)
                        }
                    }
                }
            }
            Stack.subList(1)
        }

        /** ///////////////////////////////////////////////////////////////////////// */
        private fun indexOf(node: DbvtNode): Int {
            return if (node.parent!!.child1 == node) 1 else 0
        }

        private fun merge(a: DbvtAabbMm, b: DbvtAabbMm, out: DbvtAabbMm): DbvtAabbMm {
            DbvtAabbMm.Companion.union(a, b, out)
            return out
        }

        private fun deleteNode(pdbvt: Dbvt, node: DbvtNode?) {
            pdbvt.free = node
        }

        private fun recurseDeleteNode(pdbvt: Dbvt, node: DbvtNode) {
            if (node.isBranch) {
                recurseDeleteNode(pdbvt, node.child0!!)
                recurseDeleteNode(pdbvt, node.child1!!)
            }
            if (node == pdbvt.root) {
                pdbvt.root = null
            }
            deleteNode(pdbvt, node)
        }

        private fun createNode(pdbvt: Dbvt, parent: DbvtNode?, volume: DbvtAabbMm, data: Any?): DbvtNode {
            val node: DbvtNode?
            if (pdbvt.free != null) {
                node = pdbvt.free
                pdbvt.free = null
            } else {
                node = DbvtNode()
            }
            node!!.parent = parent
            node.volume.set(volume)
            node.data = data
            node.child1 = null
            return node
        }

        private fun insertLeaf(pdbvt: Dbvt, root: DbvtNode?, leaf: DbvtNode) {
            var root = root
            if (pdbvt.root == null) {
                pdbvt.root = leaf
                leaf.parent = null
            } else {
                while (root!!.isBranch) {
                    root = if (DbvtAabbMm.Companion.proximity(
                            root.child0!!.volume,
                            leaf.volume
                        ) < DbvtAabbMm.Companion.proximity(
                            root.child1!!.volume, leaf.volume
                        )
                    ) {
                        root.child0!!
                    } else {
                        root.child1!!
                    }
                }
                var prev = root.parent
                val volume: DbvtAabbMm = merge(leaf.volume, root.volume, Stack.newDbvtAabbMm())
                var node: DbvtNode? = createNode(pdbvt, prev, volume, null)
                Stack.subDbvtAabbMm(1) // volume
                if (prev != null) {
                    if (indexOf(root) == 0) prev.child0 = node
                    else prev.child1 = node
                    node!!.child0 = root
                    root.parent = node
                    node.child1 = leaf
                    leaf.parent = node
                    do {
                        if (!prev!!.volume.contains(node!!.volume)) {
                            DbvtAabbMm.Companion.union(prev.child0!!.volume, prev.child1!!.volume, prev.volume)
                        } else {
                            break
                        }
                        node = prev
                    } while (null != (node.parent.also { prev = it }))
                } else {
                    node!!.child0 = root
                    root.parent = node
                    node.child1 = leaf
                    leaf.parent = node
                    pdbvt.root = node
                }
            }
        }

        private fun removeLeaf(pdbvt: Dbvt, leaf: DbvtNode): DbvtNode? {
            if (leaf == pdbvt.root) {
                pdbvt.root = null
                return null
            } else {
                val parent = leaf.parent
                var prev = parent!!.parent
                val sibling = if (indexOf(leaf) == 0) parent.child1 else parent.child0
                if (prev != null) {
                    if (indexOf(parent) == 0) prev.child0 = sibling
                    else prev.child1 = sibling
                    sibling!!.parent = prev
                    deleteNode(pdbvt, parent)
                    while (prev != null) {
                        val pb = prev.volume
                        DbvtAabbMm.union(prev.child0!!.volume, prev.child1!!.volume, prev.volume)
                        if (DbvtAabbMm.notEqual(pb, prev.volume)) {
                            prev = prev.parent
                        } else {
                            break
                        }
                    }
                    return (prev ?: pdbvt.root)
                } else {
                    pdbvt.root = sibling
                    sibling!!.parent = null
                    deleteNode(pdbvt, parent)
                    return pdbvt.root
                }
            }
        }

        private fun sort(n: DbvtNode, r: Array<DbvtNode?>): DbvtNode {
            val p = n.parent
            assert(n.isInternal)
            // JAVA TODO: fix this
            if (p != null && p.hashCode() > n.hashCode()) {
                val i: Int = indexOf(n)
                val j = 1 - i
                val s = (if (j == 0) p.child0 else p.child1)!!
                val q = p.parent
                assert(n == (if (i == 0) p.child0 else p.child1))
                if (q != null) {
                    if (indexOf(p) == 0) q.child0 = n
                    else q.child1 = n
                } else {
                    r[0] = n
                }
                s.parent = n
                p.parent = n
                n.parent = q
                p.child0 = n.child0
                p.child1 = n.child1
                n.child0!!.parent = p
                n.child1!!.parent = p

                if (i == 0) {
                    n.child0 = p
                    n.child1 = s
                } else {
                    n.child0 = s
                    n.child1 = p
                }

                DbvtAabbMm.Companion.swap(p.volume, n.volume)
                return p
            }
            return n
        }
    }
}
