package me.anno.games.trainbuilder

import me.anno.ecs.Entity
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.ecs.components.mesh.material.Material
import me.anno.engine.OfficialExtensions
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.games.trainbuilder.rail.CurvePiece
import me.anno.games.trainbuilder.rail.PlacedRailPiece
import me.anno.games.trainbuilder.rail.RailPiece
import me.anno.games.trainbuilder.rail.StraightPiece
import me.anno.gpu.buffer.DrawMode
import me.anno.graph.octtree.OctTree
import org.joml.Vector3d
import kotlin.math.PI

val debugMaterial = Material().apply {
    emissiveBase.set(1f, 5f, 5f)
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
        piece.interpolate(t, tmp)
        tmp.y += 3.0
        tmp.mul(scale)
        tmp.get(positions, i * 3)
    }
    mesh.positions = positions
    mesh.materials = listOf(debugMaterial.ref)
    return mesh
}

val straight5 = StraightPiece(straightRail5, Vector3d(), Vector3d(0.0, 0.0, -5.0))
val straight10 = StraightPiece(straightRail10, Vector3d(), Vector3d(0.0, 0.0, -10.0))

val curve10 = CurvePiece(curvedRail10, Vector3d(-10.0, 0.0, 0.0), 10.0, -PI * 0.5)
val curve20 = CurvePiece(curvedRail20, Vector3d(-20.0, 0.0, 0.0), 20.0, -PI * 0.5)
val curve40 = CurvePiece(curvedRail40, Vector3d(-40.0, 0.0, 0.0), 40.0, -PI * 0.5)

class RailMap() : OctTree<PlacedRailPiece>(16) {
    override fun createChild() = RailMap()
    override fun getPoint(data: PlacedRailPiece) = data.start

    fun register(placed: List<PlacedRailPiece>) {
        for (i in placed.indices) {
            val pieceI = placed[i]
            add(pieceI)
            add(pieceI.reversed as PlacedRailPiece)
        }
    }
}

fun main() {

    // generate a simple course
    // todo run a train on it

    OfficialExtensions.initForTests()

    val scene = Entity()

    val map = RailMap()

    val pieces = listOf(

        straight5,
        straight10,
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
        straight10,

        curve40.reversed,

        straight10,
        straight10,

        curve40.reversed
    )

    val placed = placeRail(Vector3d(), pieces)
    for (i in placed.indices) {
        val rail = placed[i]
        Entity(scene)
            .add(MeshComponent(rail.meshFile))
            .add(MeshComponent(debugRailMesh(rail.original)))
            .setPosition(rail.position)
            .setRotation(0f, rail.rotationRadians.toFloat(), 0f)
            .setScale(scale)
    }

    map.register(placed)

    // todo spawn train
    // todo generate route for it...

    testSceneWithUI("Rail", scene)
}