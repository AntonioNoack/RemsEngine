package me.anno.games.trainbuilder.train

import me.anno.Time
import me.anno.ecs.Component
import me.anno.ecs.systems.OnUpdate
import me.anno.games.trainbuilder.RailMap
import me.anno.games.trainbuilder.rail.PlacedRailPiece
import me.anno.maths.Maths.clamp
import me.anno.utils.pooling.JomlPools
import me.anno.utils.types.Floats.toIntOr
import org.joml.Vector2d.Companion.length
import org.joml.Vector3d
import kotlin.math.atan2
import kotlin.math.ceil
import kotlin.math.floor

class TrainController : Component(), OnUpdate {

    val pathSegments = ArrayList<PlacedRailPiece>()
    val points = ArrayList<TrainPoint>()
    val segments = ArrayList<TrainSegment>()

    var railMap: RailMap? = null

    var speed = 10.0

    override fun onUpdate() {
        moveFirst()
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

    private fun eq(a: Vector3d, b: Vector3d): Boolean {
        return a.distanceSquared(b) < 1.0
    }

    private fun findNextSegment() {
        val first = pathSegments.firstOrNull() ?: return
        val anchor = first.start
        val tolerance = 0.5
        var next: PlacedRailPiece? = null
        railMap?.query(Vector3d(anchor).sub(tolerance), Vector3d(anchor).add(tolerance)) { candidate ->
            if (eq(candidate.start, anchor) && !eq(candidate.end, first.end)) {
                next = candidate.reversed as PlacedRailPiece
            }
            false
        }
        if (next != null) {
            // insert next element at the start
            pathSegments.add(0, next)
            // accordingly, increment all indices by 1
            for (i in points.indices) {
                points[i].index++
            }
        }
    }

    private fun findLastSegment() {
        val first = pathSegments.lastOrNull() ?: return
        val anchor = first.end
        val tolerance = 0.5
        var last: PlacedRailPiece? = null
        railMap?.query(Vector3d(anchor).sub(tolerance), Vector3d(anchor).add(tolerance)) { candidate ->
            if (eq(candidate.start, anchor) && !eq(candidate.end, first.start)) {
                last = candidate
            }
            false
        }
        if (last != null) {
            // insert next element at the start
            pathSegments.add(last)
            // indices can stay the same
        }
    }

    private fun moveFirst() {
        val point = points.firstOrNull() ?: return
        var step = speed * Time.deltaTime
        val maxStep = 20.0 // 20 m/frame * 60 fps = 1200 m/s = 4 mach -> more than enough
        step = clamp(step, -maxStep, maxStep)
        moveIndex(point, step)
    }

    private fun clampRailIndex(index: Int): Int {
        return clamp(index, 0, pathSegments.lastIndex)
    }

    private fun moveIndex(point: TrainPoint, step: Double) {
        if (step > 0) {
            val railIndex = clampRailIndex(ceil(point.index).toInt() - 1)
            val segment = pathSegments[railIndex]
            val fract = point.index - railIndex
            val remainingStep = step - fract * segment.length
            if (remainingStep > 0.0) {
                // skip piece completely
                point.index = railIndex.toDouble()
                if (point.index < 1.0) findNextSegment()
                if (point.index > 0.0) moveIndex(point, remainingStep)
                else point.position.set(segment.start)
            } else moveIndexUnchecked(point, step, segment, railIndex)
        } else if (step < 0) {
            val railIndex = clampRailIndex(floor(point.index).toInt())
            val segment = pathSegments[railIndex]
            val fract = point.index - railIndex
            val remainingStep = step + (1.0 - fract) * segment.length
            if (remainingStep < 0.0) {
                // skip piece completely
                point.index = railIndex.toDouble() + 1.0
                if (point.index > pathSegments.lastIndex) findLastSegment()
                if (point.index < pathSegments.lastIndex) moveIndex(point, remainingStep)
                else point.position.set(segment.end)
            } else moveIndexUnchecked(point, step, segment, railIndex)
        }
    }

    private fun moveIndexUnchecked(point: TrainPoint, step: Double, segment: PlacedRailPiece, railIndex: Int) {
        // just skip a part
        point.index -= step / segment.length
        segment.interpolate(point.index - railIndex, point.position)
    }

    private fun movePoints() {
        var prev = points.firstOrNull() ?: return
        for (i in 1 until points.size) {
            val curr = points[i]

            val step = prev.position.distance(curr.position) - curr.distanceToPrevious
            moveIndex(curr, step)

            prev = curr
        }
    }

    private fun moveSegments() {
        val tmp = JomlPools.vec3d.create()
        for (si in segments.indices) {
            val segment = segments[si]
            val a = segment.pa.position
            val b = segment.pb.position
            val rx = atan2(a.y - b.y, length(a.x - b.x, a.z - b.z)).toFloat()
            val ry = atan2(a.x - b.x, a.z - b.z)
            val offset = segment.offset.rotateY(ry, tmp)
            segment.visuals.setPosition(
                (a.x + b.x) * 0.5 + offset.x,
                (a.y + b.y * 0.5) + offset.y,
                (a.z + b.z) * 0.5 + offset.z
            ).setRotation(rx, ry.toFloat(), 0f)
        }
        JomlPools.vec3d.sub(1)
    }
}