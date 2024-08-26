package me.anno.engine.debug

import me.anno.Time

object DebugShapes {

    val debugPoints = ArrayList<DebugPoint>()
    val debugLines = ArrayList<DebugLine>()
    val debugArrows = ArrayList<DebugLine>()
    val debugRays = ArrayList<DebugRay>()
    val debugTexts = ArrayList<DebugText>()
    val debugAABBs = ArrayList<DebugAABB>()
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

    fun clear() {
        for (i in collections.indices) {
            collections[i].clear()
        }
    }

    fun removeExpired() {
        val time = Time.nanoTime
        for (i in collections.indices) {
            collections[i].removeAll { it.timeOfDeath < time }
        }
    }
}