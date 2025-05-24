package me.anno.games.trainbuilder.rail

import me.anno.io.files.FileReference
import org.joml.Vector3d
import kotlin.math.max
import kotlin.math.min

abstract class RailPiece(val meshFile: FileReference, val length: Double, val angle: Double) {

    val start by lazy { getPosition(0.0, Vector3d()) }
    val end by lazy { getPosition(1.0, Vector3d()) }

    abstract fun getPosition(t: Double, dst: Vector3d): Vector3d

    fun getDirection(t: Double, dst: Vector3d): Vector3d {
        val dt = 1e-3
        val t0 = max(t - dt, 0.0)
        val t1 = min(t + dt, 1.0)
        val tmp = getPosition(t0, Vector3d())
        return getPosition(t1, dst).sub(tmp).normalize()
    }

    open val reversed by lazy { reversedImpl }

    open val reversedImpl get() = if (this is ReversedPiece) original else ReversedPiece(this)
}