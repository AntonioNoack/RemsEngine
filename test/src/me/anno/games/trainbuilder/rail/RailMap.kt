package me.anno.games.trainbuilder.rail

import me.anno.graph.octtree.OctTree
import org.joml.Vector3d

class RailMap() : OctTree<PlacedRailPiece>(16) {

    override fun createChild() = RailMap()
    override fun getPoint(data: PlacedRailPiece) = data.start

    fun register(placed: List<PlacedRailPiece>) {
        for (i in placed.indices) {
            val pieceI = placed[i]
            add(pieceI)
            add(pieceI.reversed)
        }
    }

    fun link() {
        val min = Vector3d()
        val max = Vector3d()
        val tmp0 = Vector3d()
        val tmp1 = Vector3d()
        forEach { piece ->
            val anchor = piece.start
            val tolerance = 0.5
            anchor.sub(tolerance, min)
            anchor.add(tolerance, max)
            query(min, max) { candidate ->
                if (!eq(candidate.end, piece.end) &&
                    dotDir(piece, candidate, tmp0, tmp1) <= 0.0
                ) {
                    candidate.reversed.nextPiece = piece
                    piece.reversed.nextPiece = candidate
                }
                false
            }
        }
    }

    private fun dotDir(a: PlacedRailPiece, b: PlacedRailPiece, tmp0: Vector3d, tmp1: Vector3d): Double {
        val dirA = a.getDirection(1.0, tmp0)
        val dirB = b.getDirection(0.0, tmp1)
        return dirA.dot(dirB)
    }

    private fun eq(a: Vector3d, b: Vector3d): Boolean {
        return a.distanceSquared(b) < 1.0
    }
}
