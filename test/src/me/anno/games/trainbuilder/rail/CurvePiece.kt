package me.anno.games.trainbuilder.rail

import me.anno.io.files.FileReference
import me.anno.utils.types.Floats.toDegrees
import org.joml.Vector3d
import kotlin.math.abs

class CurvePiece(meshFile: FileReference, val center: Vector3d, val radius: Double, angle: Double) :
    RailPiece(meshFile, abs(angle) * radius, -angle) {

    override fun getPosition(t: Double, dst: Vector3d): Vector3d {
        return dst.set(0.0, 0.0, -radius)
            .rotateY(angle * t - angle)
            .add(center)
    }

    override fun toString(): String {
        return "Curve[$radius,${angle.toDegrees()}°]"
    }
}