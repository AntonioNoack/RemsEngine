package me.anno.graph.knn

import me.anno.graph.octtree.HexDecTree
import me.anno.utils.structures.lists.Lists.smallestKElementsBy
import org.joml.Vector3d
import org.joml.Vector4d

@Suppress("unused") // probably is useful in games
class KNearestNeighbors<V> : HexDecTree<KNearestNeighbors.Element<V>>(16) {

    data class Element<V>(val posSize: Vector4d, val element: V)

    override fun getPoint(data: Element<V>) = data.posSize
    override fun createChild() = KNearestNeighbors<V>()

    fun find1(position: Vector3d, minSize: Double, maxSize: Double, maxDistance: Double): Element<V>? {
        val min = Vector4d(position, minSize).sub(maxDistance, maxDistance, maxDistance, 0.0)
        val max = Vector4d(position, maxSize).add(maxDistance, maxDistance, maxDistance, 0.0)
        var bestScore = maxDistance * maxDistance
        var bestV: Element<V>? = null
        query(min, max) {
            val score = it.posSize.distanceSquared(position.x, position.y, position.z, it.posSize.w)
            if (score <= bestScore) {
                bestScore = score
                bestV = it
            }
            false
        }
        return bestV
    }

    fun findK(position: Vector3d, minSize: Double, maxSize: Double, maxDistance: Double, k: Int): List<Element<V>> {
        val min = Vector4d(position, minSize).sub(maxDistance, maxDistance, maxDistance, 0.0)
        val max = Vector4d(position, maxSize).add(maxDistance, maxDistance, maxDistance, 0.0)
        val bestScore = maxDistance * maxDistance
        val candidates = ArrayList<Element<V>>()
        query(min, max) {
            val score = it.posSize.distanceSquared(position.x, position.y, position.z, it.posSize.w)
            if (score <= bestScore) {
                candidates.add(it)
            }
            false
        }
        return candidates.smallestKElementsBy(k) {
            it.posSize.distanceSquared(position.x, position.y, position.z, it.posSize.w)
        }
    }
}