package me.anno.games.trainbuilder.rail

import org.joml.Vector3d

class ReversedPiece(val original: RailPiece) :
    RailPiece(original.meshFile, original.length, -original.angle) {
    override fun interpolate(t: Double, dst: Vector3d): Vector3d {
        return original.interpolate(1.0 - t, dst)
    }
}