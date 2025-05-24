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

fun buildRails(scene: Entity, rails: List<PlacedRailPiece>) {
    val railEntity = Entity("Rail", scene)
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

    val railMap = RailMap()
    railMap.register(rails)
    railMap.link()
}

/**
 * Generate a course with a split and run a train on it
 * */
fun main() {

    OfficialExtensions.initForTests()

    val scene = Entity()

    val outerPieces = listOf(

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

    val i0 = outerPieces.indexOf(splitStraight30)
    val i1 = outerPieces.indexOf(splitStraight30X.reversed)

    val innerPieces = listOf(
        splitParallel30,

        curve40,
        curve40,

        splitParallel30X.reversed,
    )


    val outerPlaced = placeRail(Vector3d(), outerPieces)
    val innerPlaced = placeRail(outerPlaced[i0].position, innerPieces, PI)

    buildRails(scene, outerPlaced + innerPlaced)

    // spawn train
    createTrain(scene, 0, simpleCoalTrain(), outerPlaced).speed = 250.0

    fun addSplit(input: PlacedRailPiece, output0: PlacedRailPiece, output1: PlacedRailPiece, reverse: Boolean) {
        val split = RailSplitController(
            if (reverse) input.reversed else input,
            if (reverse) output0.reversed else output0,
            if (reverse) output1.reversed else output1
        )
        scene.add(split)
    }

    addSplit(outerPlaced.mod(i0 - 1), outerPlaced[i0], innerPlaced.first(), false)
    addSplit(outerPlaced.mod(i1 + 1), outerPlaced[i1], innerPlaced.last(), true)

    testSceneWithUI("Rails with Split", scene)
}