package me.anno.games.trainbuilder

import me.anno.ecs.Entity
import me.anno.engine.OfficialExtensions
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.games.trainbuilder.rail.PlacedRailPiece
import me.anno.games.trainbuilder.rail.RailPieces.curve40
import me.anno.games.trainbuilder.rail.RailPieces.straight10
import me.anno.games.trainbuilder.rail.RailPieces.straight5
import me.anno.games.trainbuilder.rail.RailPieces.switchCross40
import me.anno.games.trainbuilder.rail.RailPieces.switchCross40X
import me.anno.games.trainbuilder.rail.RailPieces.switchParallel40
import me.anno.games.trainbuilder.rail.RailPieces.switchParallel40X
import me.anno.games.trainbuilder.rail.RailPieces.switchStraight40
import me.anno.games.trainbuilder.rail.RailPieces.switchStraight40X
import me.anno.games.trainbuilder.rail.RailSwitchController
import me.anno.utils.structures.lists.Lists.mod
import org.joml.Vector3d
import kotlin.math.PI

/**
 * Generate a course with a switch and run a train on it
 * */
fun main() {

    OfficialExtensions.initForTests()

    val scene = Entity()

    val outerPieces = listOf(

        curve40,
        straight10,
        curve40,

        straight5,
        switchStraight40,
        straight5,

        curve40,
        straight10,
        curve40,

        straight5,
        switchParallel40X,
        straight5

    )

    val i0 = outerPieces.indexOf(switchStraight40)
    val i1 = outerPieces.indexOf(switchParallel40X)

    val innerPieces = listOf(
        switchParallel40,

        curve40,
        curve40,

        switchStraight40X,

        curve40,
        curve40,
    )

    val j0 = innerPieces.indexOf(switchParallel40)
    val j1 = innerPieces.lastIndexOf(switchStraight40X)

    val outerPlaced = placeRail(Vector3d(), outerPieces)
    val innerPlaced = placeRail(
        outerPlaced[i0].position + Vector3d(5.0, 0.0, 0.0),
        innerPieces, PI
    )

    val switchLane0 = PlacedRailPiece(switchCross40, outerPlaced[i0].position, PI)
    val switchLane1 = PlacedRailPiece(switchCross40X, outerPlaced[i1].position, 0.0)

    buildRails(scene, outerPlaced + innerPlaced + listOf(switchLane0, switchLane1))

    // spawn train
    createTrain(scene, 0, simpleCoalTrain(), innerPlaced).speed = 100.0

    scene.add(
        RailSwitchController(
            outerPlaced.mod(i0 - 1), outerPlaced[i0],
            innerPlaced[j0], innerPlaced.mod(j0 + 1),
            switchLane0
        )
    )

    scene.add(
        RailSwitchController(
            outerPlaced.mod(i1 + 1).reversed, outerPlaced[i1].reversed,
            innerPlaced[j1].reversed, innerPlaced.mod(j1 - 1).reversed,
            switchLane1.reversed
        )
    )

    testSceneWithUI("Rails with Switch", scene)
}