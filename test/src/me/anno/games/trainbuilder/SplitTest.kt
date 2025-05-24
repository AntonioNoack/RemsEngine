package me.anno.games.trainbuilder

import me.anno.ecs.Entity
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.engine.OfficialExtensions
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.games.trainbuilder.rail.PlacedRailPiece
import me.anno.games.trainbuilder.rail.RailMap
import me.anno.games.trainbuilder.rail.RailPieces.curve40
import me.anno.games.trainbuilder.rail.RailPieces.splitParallel30
import me.anno.games.trainbuilder.rail.RailPieces.splitParallel30X
import me.anno.games.trainbuilder.rail.RailPieces.splitStraight30
import me.anno.games.trainbuilder.rail.RailPieces.splitStraight30X
import me.anno.games.trainbuilder.rail.RailPieces.straight10
import me.anno.games.trainbuilder.rail.RailPieces.straight5
import me.anno.games.trainbuilder.rail.RailSplitController
import me.anno.utils.structures.lists.Lists.mod
import org.joml.Vector3d
import kotlin.math.PI

/**
 * Generate a course with a switch and run a train on it
 * */
fun main() {

    OfficialExtensions.initForTests()

    val scene = Entity()
    val railMap = RailMap()

    val pieces = listOf(

        curve40,
        straight10,
        curve40,

        splitStraight30,
        straight5,

        curve40,
        straight10,
        curve40,

        straight5,
        splitStraight30X.reversed

    )

    val i0 = pieces.indexOf(splitStraight30)
    val i1 = pieces.indexOf(splitStraight30X.reversed)

    val innerPieces = listOf(
        splitParallel30,

        curve40,
        curve40,

        splitParallel30X.reversed,
    )

    // todo test switch, too

    val railEntity = Entity("Rail", scene)

    val placed = placeRail(Vector3d(), pieces)
    val innerPlaced = placeRail(placed[i0].position, innerPieces, PI)

    val rails = placed + innerPlaced
    for (i in rails.indices) {
        val rail = rails[i]
        val entity = Entity("Piece$i", railEntity)
            .add(MeshComponent(debugRailMesh(rail.original)))
            .setPosition(rail.position)
            .setRotation(0f, rail.rotationRadians.toFloat(), 0f)
            .setScale(scale)
        if (rail.meshFile != emptyMesh) {
            entity.add(MeshComponent(rail.meshFile))
        }
    }

    railMap.register(rails)
    railMap.link()

    // spawn train
    createTrain(scene, 0, simpleCoalTrain(), placed).speed = 250.0

    val splits = Entity("Splits", scene)
    fun addSplit(input: PlacedRailPiece, output0: PlacedRailPiece, output1: PlacedRailPiece, reverse: Boolean) {
        val split = RailSplitController(
            if (reverse) input.reversed else input,
            if (reverse) output0.reversed else output0,
            if (reverse) output1.reversed else output1
        )
        splits.add(split)
    }

    addSplit(placed.mod(i0 - 1), placed[i0], innerPlaced.first(), false)
    addSplit(placed.mod(i1 + 1), placed[i1], innerPlaced.last(), true)

    testSceneWithUI("Rail", scene)
}