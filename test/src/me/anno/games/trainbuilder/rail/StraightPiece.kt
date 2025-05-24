package me.anno.games.trainbuilder.rail

import me.anno.io.files.FileReference
import org.joml.Vector3d

class StraightPiece(meshFile: FileReference, val p0: Vector3d, val p1: Vector3d) :
    RailPiece(meshFile, p0.distance(p1), 0.0) {

    override fun getPosition(t: Double, dst: Vector3d): Vector3d {
        return p0.mix(p1, t, dst)
    }

    override fun toString(): String {
        return "Straight[$p0-$p1]"
    }
}