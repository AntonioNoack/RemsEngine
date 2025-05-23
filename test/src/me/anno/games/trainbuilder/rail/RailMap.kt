package me.anno.games.trainbuilder.rail

import me.anno.graph.octtree.OctTree

class RailMap() : OctTree<PlacedRailPiece>(16) {
    override fun createChild() = RailMap()
    override fun getPoint(data: PlacedRailPiece) = data.start

    fun register(placed: List<PlacedRailPiece>) {
        for (i in placed.indices) {
            val pieceI = placed[i]
            add(pieceI)
            add(pieceI.reversed as PlacedRailPiece)
        }
    }
}
