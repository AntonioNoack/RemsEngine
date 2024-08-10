package me.anno.tests.game.flatworld.streets

data class StreetIntersection(val segments: ArrayList<ReversibleSegment>) {
    data class ReversibleSegment(val segment: StreetSegment, val reversed: Boolean)
}