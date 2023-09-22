package me.anno.engine.debug

import me.anno.Time

object DebugShapes {

    val debugPoints = ArrayList<DebugPoint>()
    val debugLines = ArrayList<DebugLine>()
    val debugRays = ArrayList<DebugRay>()
    val debugTexts = ArrayList<DebugText>()

    fun clear() {
        debugPoints.clear()
        debugLines.clear()
        debugRays.clear()
        debugTexts.clear()
    }

    fun removeExpired() {
        val time = Time.nanoTime
        debugPoints.removeIf { it.timeOfDeath < time }
        debugLines.removeIf { it.timeOfDeath < time }
        debugRays.removeIf { it.timeOfDeath < time }
        debugTexts.removeIf { it.timeOfDeath < time }
    }
}