package me.anno.games.trainbuilder

import me.anno.ecs.Entity
import me.anno.engine.OfficialExtensions
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.games.trainbuilder.rail.RailPieces.crossOrtho10
import me.anno.games.trainbuilder.rail.RailPieces.crossStraight10
import me.anno.games.trainbuilder.rail.RailPieces.curve20
import me.anno.games.trainbuilder.rail.RailPieces.straight10
import me.anno.games.trainbuilder.rail.RailPieces.straight5
import org.joml.Vector3d

/**
 * Generate a course with a cross and run a train on it
 * */
fun main() {

    OfficialExtensions.initForTests()

    val scene = Entity()
    val pieces = listOf(

        straight5,
        straight10,

        curve20,
        curve20,
        curve20,

        straight10,
        straight5,

        crossStraight10,

        straight5,
        straight10,

        curve20.reversed,
        curve20.reversed,
        curve20.reversed,

        straight10,
        straight5,

        crossOrtho10
    )

    val placed = placeRail(Vector3d(), pieces)
    buildRails(scene, placed)

    // spawn train
    createTrain(scene, 0, simpleCoalTrain(), placed).speed = 100.0

    testSceneWithUI("Rails with Cross", scene)
}