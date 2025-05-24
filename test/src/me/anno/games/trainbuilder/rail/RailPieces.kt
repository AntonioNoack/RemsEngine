package me.anno.games.trainbuilder.rail

import me.anno.games.trainbuilder.curvedRail10
import me.anno.games.trainbuilder.curvedRail20
import me.anno.games.trainbuilder.curvedRail40
import me.anno.games.trainbuilder.rampRail40
import me.anno.games.trainbuilder.straightRail10
import me.anno.games.trainbuilder.straightRail5
import org.joml.Vector3d
import kotlin.math.PI

object RailPieces {
    val straight5 = StraightPiece(straightRail5, Vector3d(), Vector3d(0.0, 0.0, -5.0))
    val straight10 = StraightPiece(straightRail10, Vector3d(), Vector3d(0.0, 0.0, -10.0))
    val ramp40 = StraightPiece(rampRail40, Vector3d(), Vector3d(0.0, 10.0, -40.0))

    val curve10 = CurvePiece(curvedRail10, Vector3d(-10.0, 0.0, 0.0), 10.0, -PI * 0.5)
    val curve20 = CurvePiece(curvedRail20, Vector3d(-20.0, 0.0, 0.0), 20.0, -PI * 0.5)
    val curve40 = CurvePiece(curvedRail40, Vector3d(-40.0, 0.0, 0.0), 40.0, -PI * 0.5)

}