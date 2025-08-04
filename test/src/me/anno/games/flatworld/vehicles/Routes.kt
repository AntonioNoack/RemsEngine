package me.anno.games.flatworld.vehicles

import me.anno.games.flatworld.FlatWorld
import me.anno.games.flatworld.streets.ReversibleSegment
import me.anno.maths.paths.PathFinding

object Routes {
    fun distance(start: ReversibleSegment, end: ReversibleSegment): Double {
        return start.a.distance(end.a)
    }

    fun getChildren(world: FlatWorld, segment: ReversibleSegment): Collection<ReversibleSegment> {
        return world.intersections[segment.c]?.segments ?: emptyList()
    }

    fun findRoute(world: FlatWorld, start: ReversibleSegment, end: ReversibleSegment): List<ReversibleSegment>? {
        return PathFinding<ReversibleSegment>(16).aStar(
            start, end, ::distance, { segment -> getChildren(world, segment) },
            1e9, includeStart = true, includeEnd = true
        )
    }
}