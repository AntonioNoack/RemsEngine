package me.anno.games.trainbuilder.rail

import org.joml.Vector3d

class PlacedRailPiece(val original: RailPiece, val position: Vector3d, val rotationRadians: Double) :
    RailPiece(original.meshFile, original.length, original.angle) {
    override fun interpolate(t: Double, dst: Vector3d): Vector3d {
        return original.interpolate(t, dst)
            .rotateY(rotationRadians)
            .add(position)
    }

    override val reversedImpl: PlacedRailPiece
        get() = PlacedRailPiece(original.reversed, position, rotationRadians)
}