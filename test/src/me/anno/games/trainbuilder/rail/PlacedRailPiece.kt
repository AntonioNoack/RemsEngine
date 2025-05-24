package me.anno.games.trainbuilder.rail

import me.anno.utils.types.Floats.toDegrees
import org.joml.Vector3d

class PlacedRailPiece(
    val original: RailPiece, val position: Vector3d, val rotationRadians: Double,
    private val reversedZero: PlacedRailPiece? = null
) : RailPiece(original.meshFile, original.length, original.angle) {

    override fun getPosition(t: Double, dst: Vector3d): Vector3d {
        return original.getPosition(t, dst)
            .rotateY(rotationRadians)
            .add(position)
    }

    override val reversedImpl: PlacedRailPiece
        get() = reversedZero ?: PlacedRailPiece(original.reversed, position, rotationRadians, this)

    var nextPiece: PlacedRailPiece? = null

    val prevPiece: PlacedRailPiece?
        get() = reversed.nextPiece?.reversed

    override val reversed: PlacedRailPiece
        get() = super.reversed as PlacedRailPiece

    override fun toString(): String {
        return "Placed[$original,$position,${rotationRadians.toDegrees()}°]"
    }
}