package me.anno.games.trainbuilder.rail

import me.anno.io.files.FileReference
import org.joml.Vector3d

abstract class RailPiece(val meshFile: FileReference, val length: Double, val angle: Double) {

    val start by lazy { interpolate(0.0, Vector3d()) }
    val end by lazy { interpolate(1.0, Vector3d()) }

    abstract fun interpolate(t: Double, dst: Vector3d): Vector3d

    val reversed by lazy { reversedImpl }

    open val reversedImpl get() = if (this is ReversedPiece) original else ReversedPiece(this)
}