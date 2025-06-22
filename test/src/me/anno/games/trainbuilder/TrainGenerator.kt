package me.anno.games.trainbuilder

import me.anno.ecs.Entity
import me.anno.ecs.components.mesh.MeshCache
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.engine.OfficialExtensions
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.games.trainbuilder.rail.PlacedRailPiece
import me.anno.games.trainbuilder.rail.RailPiece
import me.anno.games.trainbuilder.rail.RailPieces.curve40
import me.anno.games.trainbuilder.rail.RailPieces.straight10
import me.anno.games.trainbuilder.rail.ReversedPiece
import me.anno.games.trainbuilder.rail.StraightPiece
import me.anno.games.trainbuilder.train.TrainController
import me.anno.games.trainbuilder.train.TrainPoint
import me.anno.games.trainbuilder.train.TrainSegment
import me.anno.io.files.FileReference
import me.anno.maths.Maths.clamp
import me.anno.maths.Maths.fract
import me.anno.utils.assertions.assertTrue
import me.anno.utils.structures.lists.Lists.createList
import org.joml.Vector3d
import kotlin.math.PI
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.random.Random

val couplingDistance = 1.0

/**
 * Merge a sequence of rail pieces into a continuous sequence with positions and rotations.
 * */
fun placeRail(start: Vector3d, pieces: List<RailPiece>, angle: Double = 0.0): List<PlacedRailPiece> {

    val position = Vector3d(start)
    var angle = angle
    val tmp = Vector3d()

    val result = ArrayList<PlacedRailPiece>()
    for (i in pieces.indices) {
        val piece = pieces[i]
        val isBackwards = piece is ReversedPiece
        if (isBackwards) angle -= piece.angle
        val isBackwardsRamp = isBackwards && piece.original is StraightPiece
        if (isBackwardsRamp) angle += PI

        position.sub(piece.getPosition(0.0, tmp).rotateY(angle)) // optional, because all pieces start at 0

        result.add(PlacedRailPiece(piece, Vector3d(position), angle))

        position.add(piece.getPosition(1.0, tmp).rotateY(angle))
        angle += when {
            isBackwardsRamp -> 2.0 * piece.angle + PI
            isBackwards -> 2.0 * piece.angle
            else -> piece.angle
        }
    }

    return result
}

fun getPointAt(index: Double, pieces: List<PlacedRailPiece>): Vector3d {
    val index = clamp(index, 0.0, pieces.size - 0.001)
    val idx = floor(index).toInt()
    val piece = pieces[idx]
    return piece.getPosition(fract(index), Vector3d())
}

fun indexPlusStep(index: Double, step: Double, pieces: List<RailPiece>): Double {
    val index = clamp(index, 0.0, pieces.size - 0.001)
    val idx = floor(index).toInt()
    return index + step / pieces[idx].length
}

fun createTrain(
    scene: Entity, k: Int,
    meshes: List<FileReference>,
    pieces: List<PlacedRailPiece>,
): TrainController {

    val joined = Entity("Train$k", scene)

    var index0 = 0.0
    var idealPos = 0.0
    var lastIdealPos = 0.0

    val controller = TrainController()
    controller.pathSegments.addAll(pieces)

    fun addFractionalPoint(fract: Double, waggonLength: Double): TrainPoint {
        val step = fract * waggonLength
        val indexI = indexPlusStep(index0, step, pieces)
        val idealPosI = idealPos + step
        assertTrue(idealPosI > lastIdealPos)
        val pt = TrainPoint(
            indexI, idealPosI - lastIdealPos,
            getPointAt(indexI, pieces)
        )
        lastIdealPos = idealPosI
        controller.points.add(pt)
        return pt
    }

    fun addPart(name: String, file: FileReference, end: Boolean) {
        val bounds = MeshCache.getEntry(file).waitFor()!!.getBounds()
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

        idealPos += length + couplingDistance
        index0 = indexPlusStep(index0, length + couplingDistance, pieces)
    }

    for (i in meshes.indices) {
        addPart("Train$i", meshes[i], i == meshes.lastIndex)
    }

    joined.add(controller)
    return controller
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
        MeshCache.getEntry(it).waitFor()!!.getBounds().deltaZ.toDouble()
    } * scale + couplingDistance * max(meshes.size - 1, 0)
}

/**
 * Create a series of trains running on their own small circle.
 * */
fun main() {

    OfficialExtensions.initForTests()

    val scene = Entity()

    var k = 0
    val random = Random(2634)

    fun generateRail(meshes: List<FileReference>): List<PlacedRailPiece> {
        val curve = curve40
        val straight = straight10
        val straightLength = calculateTrainLength(meshes) - 4 * curve.length
        val numStraights = max(ceil(0.5f * straightLength / straight.length).toInt(), 0)
        val halfNumRails = numStraights + 2
        val numRails = halfNumRails * 2
        val pieces = createList(numRails) {
            if (it < 2 || it >= halfNumRails && it < halfNumRails + 2) curve
            else straight
        }
        return placeRail(Vector3d(k * 100.0, 0.0, 0.0), pieces)
    }

    val allRails = ArrayList<PlacedRailPiece>()
    fun createTrain(meshes: List<FileReference>) {
        val rail = generateRail(meshes)
        createTrain(scene, k, meshes, rail)
        allRails.addAll(rail)
        k++
    }

    fun createTrain(trains: List<FileReference>, carriers: List<FileReference>) {
        val train = trains.random(random)
        val numWaggons = random.nextInt(5, 10)
        val waggons = (0 until numWaggons).map { carriers.random(random) }
        val meshes = listOf(train) + waggons + train
        createTrain(meshes)
    }

    for (type in cargoCarriers) {
        createTrain(cargoTrainModels, type)
    }

    createTrain(personTrainModels, personCarrierModels)
    createTrain(metroTrains, metroCarriers)

    buildRails(scene, allRails)

    testSceneWithUI("Trains", scene)
}