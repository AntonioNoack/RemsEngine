package me.anno.engine.debug

object DebugShapes {
    val debugPoints = ArrayList<DebugPoint>()
    val debugLines = ArrayList<DebugLine>()
    val debugRays = ArrayList<DebugRay>()
    fun clear() {
        debugPoints.clear()
        debugLines.clear()
        debugRays.clear()
    }
}