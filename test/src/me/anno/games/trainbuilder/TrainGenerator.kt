package me.anno.games.trainbuilder

import me.anno.ecs.Entity
import me.anno.ecs.components.mesh.MeshCache
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.engine.OfficialExtensions
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.games.trainbuilder.rail.PlacedRailPiece
import me.anno.games.trainbuilder.rail.RailPiece
import me.anno.games.trainbuilder.rail.ReversedPiece
import me.anno.games.trainbuilder.train.TrainController
import me.anno.games.trainbuilder.train.TrainPoint
import me.anno.games.trainbuilder.train.TrainSegment
import me.anno.io.files.FileReference
import me.anno.maths.Maths.clamp
import me.anno.maths.Maths.fract
import me.anno.utils.structures.lists.Lists.createList
import org.joml.Vector3d
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.random.Random

val padding = 1.0
val width = 100.0

fun placeRail(start: Vector3d, pieces: List<RailPiece>): List<PlacedRailPiece> {

    val tmp = Vector3d()
    var angle = 0.0

    val result = ArrayList<PlacedRailPiece>()
    for (i in pieces.indices) {
        val piece = pieces[i]
        val isBackwards = piece is ReversedPiece
        if (isBackwards) angle -= piece.angle
        start.sub(piece.interpolate(0.0, tmp).rotateY(angle)) // optional, because all pieces start at 0

        result.add(PlacedRailPiece(piece, Vector3d(start), angle))

        start.add(piece.interpolate(1.0, tmp).rotateY(angle))
        angle += if (!isBackwards) piece.angle
        else 2.0 * piece.angle
    }

    return result
}

fun getPointAt(index: Double, pieces: List<PlacedRailPiece>): Vector3d {
    val index = clamp(index, 0.0, pieces.size - 0.001)
    val idx = floor(index).toInt()
    val piece = pieces[idx]
    return piece.interpolate(fract(index), Vector3d())
}

fun indexPlusStep(index: Double, step: Double, pieces: List<RailPiece>): Double {
    val index = clamp(index, 0.0, pieces.size - 0.001)
    val idx = floor(index).toInt()
    return index + step / pieces[idx].length
}

fun createTrain(
    scene: Entity, k: Int,
    meshes: List<FileReference>,
    controller: TrainController,
    pieces: List<PlacedRailPiece>
) {

    val joined = Entity("Train$k", scene)

    var index0 = 0.0
    var idealPos = 0.0
    var lastIdealPos = 0.0

    controller.pathSegments.addAll(pieces)

    fun addFractionalPoint(fract: Double, waggonLength: Double): TrainPoint {
        val step = fract * waggonLength
        val indexI = indexPlusStep(index0, step, pieces)
        val idealPosI = idealPos + step
        val pt = TrainPoint(
            indexI, idealPosI - lastIdealPos,
            getPointAt(indexI, pieces)
        )
        lastIdealPos = idealPosI
        controller.points.add(pt)
        return pt
    }

    fun addPart(name: String, file: FileReference, end: Boolean) {
        val bounds = MeshCache[file]!!.getBounds()
        val length = bounds.deltaZ * scale.toDouble()

        val p0 = addFractionalPoint(0.2, length)
        val p1 = addFractionalPoint(0.8, length)

        val visuals = Entity(name, joined)
            .add(MeshComponent(file))
            .setScale(scale)

        val dz = bounds.centerZ * scale.toDouble()
        val offset = Vector3d(0.0, 0.0, -dz)
        controller.segments.add(
            if (end) TrainSegment(p1, p0, visuals, offset)
            else TrainSegment(p0, p1, visuals, offset)
        )

        idealPos += length + padding
        index0 = indexPlusStep(index0, length + padding, pieces)
    }

    for (i in meshes.indices) {
        addPart("Train$i", meshes[i], i == meshes.lastIndex)
    }

    joined.add(controller)
}

fun createRail(scene: Entity, name: String, pieces: List<PlacedRailPiece>) {
    // place rail below them
    val rail = Entity(name, scene)
    for (i in pieces.indices) {
        val piece = pieces[i]
        Entity("Piece$i", rail)
            .add(MeshComponent(piece.meshFile))
            .setPosition(piece.position)
            .setRotation(0f, piece.rotationRadians.toFloat(), 0f)
            .setScale(scale)
    }
}

fun calculateTrainLength(meshes: List<FileReference>): Double {
    return meshes.sumOf {
        MeshCache[it]!!.getBounds().deltaZ.toDouble()
    } * scale + padding * max(meshes.size - 1, 0)
}

/**
 * create a series of trains programmatically
 * */
fun main() {

    OfficialExtensions.initForTests()

    val scene = Entity()

    var k = 0
    val random = Random(2634)

    val map = RailMap()

    fun createTrain(trains: List<FileReference>, carriers: List<FileReference>) {
        val train = trains.random(random)
        val numWaggons = random.nextInt(5, 10)
        val waggons = (0 until numWaggons).map { carriers.random(random) }
        val railPiece = curve40
        val meshes = listOf(train) + waggons + train
        val numRails1 = ceil(calculateTrainLength(meshes) / railPiece.length).toInt()
        val straights = max((numRails1 - 4) * 3, 0)
        val numRails2 = 4 + straights * 2
        val pieces = createList(numRails2) {
            if (it < 2 || it >= numRails2 / 2 && it < numRails2 / 2 + 2) curve40
            else straight10
        }
        val rail = placeRail(Vector3d(k * width, 0.0, 0.0), pieces)
        val controller = TrainController()
        createTrain(scene, k, meshes, controller, rail)
        createRail(scene, "Rail$k", rail)
        controller.railMap = map
        map.register(rail)
        k++
    }

    for (type in cargoCarriers) {
        createTrain(cargoTrainModels, type)
    }
    createTrain(personTrainModels, personCarrierModels)
    createTrain(metroTrains, metroCarriers)

    testSceneWithUI("Trains", scene)
}