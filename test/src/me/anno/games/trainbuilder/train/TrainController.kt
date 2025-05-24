package me.anno.games.trainbuilder.train

import me.anno.Time
import me.anno.ecs.Component
import me.anno.ecs.systems.OnUpdate
import me.anno.games.trainbuilder.rail.PlacedRailPiece
import me.anno.games.trainbuilder.rail.RailPiece
import me.anno.maths.Maths.clamp
import me.anno.utils.pooling.JomlPools
import me.anno.utils.structures.lists.SegmentLists.segmentStep
import me.anno.utils.types.Booleans.toInt
import me.anno.utils.types.Floats.toIntOr
import kotlin.math.atan2

class TrainController : Component(), OnUpdate {

    val pathSegments = ArrayList<PlacedRailPiece>()
    val points = ArrayList<TrainPoint>()
    val segments = ArrayList<TrainSegment>()

    var speed = 10.0

    override fun onUpdate() {
        movePoints()
        moveSegments()
        removeTrailingPathSegments()
    }

    private fun removeTrailingPathSegments() {
        val lastPoint = points.lastOrNull() ?: return
        val maxAllowedIndex = (lastPoint.index + 1.5).toIntOr()
        while (pathSegments.size > maxAllowedIndex) {
            pathSegments.removeLast()
        }
    }

    private fun addFirstSegment(): Int {
        val oldFirst = pathSegments.firstOrNull()
        val newFirst = oldFirst?.prevPiece
        if (newFirst != null) {
            // insert next element at the start
            pathSegments.add(0, newFirst)
            // accordingly, increment all indices by 1
            for (i in points.indices) {
                points[i].index++
            }
        }
        return (newFirst != null).toInt()
    }

    private fun addLastSegment(): Int {
        val oldLast = pathSegments.lastOrNull()
        val newLast: PlacedRailPiece? = oldLast?.nextPiece
        if (newLast != null) {
            // insert next element at the start
            pathSegments.add(newLast)
            // indices can stay the same
        }
        return (newLast != null).toInt()
    }

    private fun moveIndex(point: TrainPoint, step: Double) {
        point.index = segmentStep(
            point.index, -step, pathSegments, RailPiece::length,
            ::addFirstSegment, ::addLastSegment, true
        )
        val railIndex = clamp(point.index.toIntOr(), 0, pathSegments.lastIndex)
        pathSegments[railIndex].getPosition(point.index - railIndex, point.position)
    }

    private fun movePoints() {
        var step = speed * Time.deltaTime
        val maxStep = 200.0 // 200 m/frame * 60 fps = 12000 m/s = 40 mach -> more than enough
        step = clamp(step, -maxStep, maxStep)

        if (step > 0.0) { // moving forwards
            var prev = points.firstOrNull() ?: return
            moveIndex(prev, step)
            for (i in 1 until points.size) {
                val curr = points[i]
                curr.index = prev.index
                moveIndex(curr, -curr.distanceToPrevious)
                prev = curr
            }
        } else { // moving backwards
            var last = points.lastOrNull() ?: return
            moveIndex(last, step)
            for (i in points.size - 2 downTo 0) {
                val curr = points[i]
                curr.index = last.index
                moveIndex(curr, last.distanceToPrevious)
                last = curr
            }
        }
    }

    private fun moveSegments() {
        val tmp = JomlPools.vec3d.create()
        for (si in segments.indices) {
            val segment = segments[si]
            val front = segment.frontAnchor.position
            val back = segment.backAnchor.position
            val rx = atan2(back.y - front.y, front.distanceXZ(back))
            val ry = atan2(front.x - back.x, front.z - back.z)
            val offset = segment.offset
                .rotateX(rx, tmp)
                .rotateY(ry, tmp)
            segment.visuals.setPosition(
                (front.x + back.x) * 0.5 + offset.x,
                (front.y + back.y) * 0.5 + offset.y,
                (front.z + back.z) * 0.5 + offset.z
            ).setRotation(rx.toFloat(), ry.toFloat(), 0f)
        }
        JomlPools.vec3d.sub(1)
    }
}