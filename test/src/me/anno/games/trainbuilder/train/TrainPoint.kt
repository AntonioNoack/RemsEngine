package me.anno.games.trainbuilder.train

import org.joml.Vector3d

class TrainPoint(
    var index: Double,
    val distanceToPrevious: Double,
    val position: Vector3d
)