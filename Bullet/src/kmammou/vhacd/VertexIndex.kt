package kmammou.vhacd

import me.anno.graph.octtree.OctTreeF
import org.joml.Vector3f

class VertexIndex(
    val granularity: Float,
    val offset: Vector3f, // min, not center
    val invScale: Float,
    capacity: Int
) {

    private class TreeNode : OctTreeF<Vector3f>(16) {
        override fun createChild() = TreeNode()
        override fun getMin(data: Vector3f) = data
        override fun getMax(data: Vector3f) = data
        override fun getPoint(data: Vector3f) = data
    }

    val vertices = ArrayList<Vector3f>(capacity)

    private val kdTree = TreeNode()
    private val lookup = HashMap<Vector3f, Int>(capacity)
    private val granularitySq = granularity * granularity

    private val tmpMin = Vector3f()
    private val tmpMax = Vector3f()

    fun map(v: Vector3f): Int {
        val mapped = Vector3f(v).sub(offset).mul(invScale)
        val min = mapped.sub(granularity, tmpMin)
        val max = mapped.add(granularity, tmpMax)
        val found = kdTree.query(min, max) { p ->
            p.distanceSquared(mapped) <= granularitySq
        }
        if (found != null) {
            return lookup[found]!!
        } else {
            val newId = lookup.size
            lookup[mapped] = newId
            kdTree.add(mapped)
            vertices.add(mapped)
            return newId
        }
    }
}