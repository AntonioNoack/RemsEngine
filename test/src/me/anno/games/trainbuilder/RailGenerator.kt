package me.anno.games.trainbuilder

import me.anno.ecs.Entity
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.ecs.components.mesh.material.Material
import me.anno.engine.OfficialExtensions
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.games.trainbuilder.rail.RailMap
import me.anno.games.trainbuilder.rail.RailPiece
import me.anno.games.trainbuilder.rail.RailPieces.curve10
import me.anno.games.trainbuilder.rail.RailPieces.curve20
import me.anno.games.trainbuilder.rail.RailPieces.curve40
import me.anno.games.trainbuilder.rail.RailPieces.ramp40
import me.anno.games.trainbuilder.rail.RailPieces.straight10
import me.anno.games.trainbuilder.rail.RailPieces.straight5
import me.anno.games.trainbuilder.rail.StraightPiece
import me.anno.gpu.buffer.DrawMode
import me.anno.io.files.FileReference
import me.anno.utils.structures.lists.Lists.createList
import org.joml.Vector3d

val debugMaterial = Material().apply {
    emissiveBase.set(1f, 5f, 5f)
}

fun simpleCoalTrain(): List<FileReference> {
    return listOf(cargoTrainModels.first()) + createList(5, coalCarrierModels.first())
}

fun debugRailMesh(piece: RailPiece): Mesh {
    val n = if (piece is StraightPiece) 2 else 10
    val mesh = Mesh()
    mesh.drawMode = DrawMode.LINE_STRIP
    val tmp = Vector3d()
    val positions = FloatArray(3 * n)
    val scale = 1.0 / scale
    for (i in 0 until n) {
        val t = i / (n - 1.0)
        piece.getPosition(t, tmp)
        tmp.y += 3.0
        tmp.mul(scale)
        tmp.get(positions, i * 3)
    }
    mesh.positions = positions
    mesh.materials = listOf(debugMaterial.ref)
    return mesh
}

/**
 * Generate a course and run a train on it
 * */
fun main() {

    OfficialExtensions.initForTests()

    val scene = Entity()
    val railMap = RailMap()

    val pieces = listOf(

        straight5,
        straight10,
        ramp40,
        straight10,
        straight10,

        curve20.reversed,
        curve20.reversed,
        straight10,
        straight5,

        curve20,
        curve20,

        curve10.reversed,
        curve10.reversed,

        straight10,
        ramp40.reversed,
        straight10,

        curve40.reversed,

        straight10,
        straight10,

        curve40.reversed
    )

    val railEntity = Entity("Rail", scene)
    val placed = placeRail(Vector3d(), pieces)
    for (i in placed.indices) {
        val rail = placed[i]
        Entity("Piece$i", railEntity)
            .add(MeshComponent(rail.meshFile))
            .add(MeshComponent(debugRailMesh(rail.original)))
            .setPosition(rail.position)
            .setRotation(0f, rail.rotationRadians.toFloat(), 0f)
            .setScale(scale)
    }

    railMap.register(placed)
    railMap.link()

    // spawn train
    createTrain(scene, 0, simpleCoalTrain(), placed)

    testSceneWithUI("Rail", scene)
}