package me.anno.engine.debug

import me.anno.Time
import me.anno.utils.InternalAPI

object DebugShapes {

    @InternalAPI
    val debugPoints = ArrayList<DebugPoint>()

    @InternalAPI
    val debugLines = ArrayList<DebugLine>()

    @InternalAPI
    val debugArrows = ArrayList<DebugLine>()

    @InternalAPI
    val debugRays = ArrayList<DebugRay>()

    @InternalAPI
    val debugTexts = ArrayList<DebugText>()

    @InternalAPI
    val debugAABBs = ArrayList<DebugAABB>()

    @InternalAPI
    val debugTriangles = ArrayList<DebugTriangle>()

    val collections = arrayListOf(
        debugPoints,
        debugLines,
        debugArrows,
        debugRays,
        debugTexts,
        debugAABBs,
        debugTriangles
    )

    fun showDebugPoint(point: DebugPoint) = addItem(debugPoints, point)
    fun showDebugLine(line: DebugLine) = addItem(debugLines, line)
    fun showDebugArrow(arrow: DebugLine) = addItem(debugArrows, arrow)
    fun showDebugRay(ray: DebugRay) = addItem(debugRays, ray)
    fun showDebugText(text: DebugText) = addItem(debugTexts, text)
    fun showDebugAABB(aabb: DebugAABB) = addItem(debugAABBs, aabb)
    fun showDebugTriangle(triangle: DebugTriangle) = addItem(debugTriangles, triangle)

    private fun <V> addItem(list: ArrayList<V>, item: V) {
        synchronized(list) { list.add(item) }
    }

    fun removeExpired() {
        val time = Time.nanoTime
        for (i in collections.indices) {
            val collection = collections[i]
            synchronized(collection) {
                collection.removeAll { it.timeOfDeath < time }
            }
        }
    }

    fun clear() {
        for (i in collections.indices) {
            val collection = collections[i]
            synchronized(collection) {
                collection.clear()
            }
        }
    }
}