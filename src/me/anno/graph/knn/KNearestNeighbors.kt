package me.anno.graph.knn

import me.anno.utils.structures.lists.Lists.smallestKElementsBy
import org.joml.Vector3d

// todo generally: oct trees for fast finding of things

// todo find the k nearest neighbors

// todo also respect the size: sometimes we look for huge things, sometimes for very small stuff

class KNearestNeighbors<V> {

    // stupid, simple implementation:
    class Element<V>(val position: Vector3d, val size: Double, val element: V)

    val elements = ArrayList<Element<V>>()

    fun clear() {
        elements.clear()
    }

    fun remove(element: V) {
        elements.removeIf { it.element == element }
    }

    fun addOrUpdate(position: Vector3d, size: Double, element: V) {
        remove(element)
        elements.add(Element(position, size, element))
    }

    fun find1(position: Vector3d, minSize: Double, maxSize: Double, maxDistance: Double): V? {
        val maxDSq = maxDistance * maxDistance
        return elements
            .filter { it.position.distanceSquared(position) < maxDSq && it.size in minSize..maxSize }
            .minByOrNull { it.position.distanceSquared(position) }?.element
    }

    fun findK(position: Vector3d, minSize: Double, maxSize: Double, maxDistance: Double, k: Int): List<V> {
        val maxDSq = maxDistance * maxDistance
        return elements
            .filter { it.position.distanceSquared(position) < maxDSq && it.size in minSize..maxSize }
            // mmh, it would be nice to have a better function
            // todo generic function to find the k smallest/largest values
            .smallestKElementsBy(k) { it.position.distanceSquared(position) }
            .map { it.element }
    }


}