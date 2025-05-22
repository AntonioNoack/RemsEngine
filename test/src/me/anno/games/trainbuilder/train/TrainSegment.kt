package me.anno.games.trainbuilder.train

import me.anno.ecs.Entity
import org.joml.Vector3d

class TrainSegment(
    val pa: TrainPoint, val pb: TrainPoint,
    val visuals: Entity, val offset: Vector3d
)