package me.anno.games.trainbuilder.rail

import me.anno.games.trainbuilder.crossRail10
import me.anno.games.trainbuilder.curvedRail10
import me.anno.games.trainbuilder.curvedRail20
import me.anno.games.trainbuilder.curvedRail40
import me.anno.games.trainbuilder.emptyMesh
import me.anno.games.trainbuilder.rampRail40
import me.anno.games.trainbuilder.splitRail30
import me.anno.games.trainbuilder.splitRail30X
import me.anno.games.trainbuilder.straightRail10
import me.anno.games.trainbuilder.straightRail5
import me.anno.games.trainbuilder.switchRail40
import me.anno.games.trainbuilder.switchRail40X
import org.joml.Vector3d
import kotlin.math.PI

object RailPieces {

    // straight pieces
    val straight5 = StraightPiece(straightRail5, Vector3d(), Vector3d(0.0, 0.0, -5.0))
    val straight10 = StraightPiece(straightRail10, Vector3d(), Vector3d(0.0, 0.0, -10.0))

    // for hills/terrain
    val ramp40 = StraightPiece(rampRail40, Vector3d(), Vector3d(0.0, 10.0, -40.0))

    // curves
    val curve10 = CurvePiece(curvedRail10, Vector3d(-10.0, 0.0, 0.0), 10.0, -PI * 0.5)
    val curve20 = CurvePiece(curvedRail20, Vector3d(-20.0, 0.0, 0.0), 20.0, -PI * 0.5)
    val curve40 = CurvePiece(curvedRail40, Vector3d(-40.0, 0.0, 0.0), 40.0, -PI * 0.5)

    // split, 2 parts each
    val splitStraight30 = StraightPiece(splitRail30, Vector3d(), Vector3d(0.0, 0.0, -30.0))
    val splitStraight30X = StraightPiece(splitRail30X, Vector3d(), Vector3d(0.0, 0.0, -30.0))
    val splitParallel30 = StraightPiece(emptyMesh, Vector3d(), Vector3d(-5.0, 0.0, -30.0))
    val splitParallel30X = StraightPiece(emptyMesh, Vector3d(), Vector3d(+5.0, 0.0, -30.0))

    // switch, 3 parts each
    val switchStraight40 = StraightPiece(switchRail40, Vector3d(), Vector3d(0.0, 0.0, -40.0))
    val switchParallel40 = StraightPiece(emptyMesh, Vector3d(-5.0, 0.0, 0.0), Vector3d(-5.0, 0.0, -40.0))
    val switchCross40 = StraightPiece(emptyMesh, Vector3d(), Vector3d(-5.0, 0.0, -40.0))
    val switchStraight40X = StraightPiece(switchRail40X, Vector3d(), Vector3d(0.0, 0.0, -40.0))
    val switchParallel40X = StraightPiece(emptyMesh, Vector3d(+5.0, 0.0, 0.0), Vector3d(+5.0, 0.0, -40.0))
    val switchCross40X = StraightPiece(emptyMesh, Vector3d(), Vector3d(+5.0, 0.0, -40.0))

    // cross, 2 parts
    val crossStraight10 = StraightPiece(crossRail10, Vector3d(), Vector3d(0.0, 0.0, -10.0))
    val crossOrtho10 = StraightPiece(emptyMesh, Vector3d(0.0, 0.0, 0.0), Vector3d(0.0, 0.0, -10.0))
}