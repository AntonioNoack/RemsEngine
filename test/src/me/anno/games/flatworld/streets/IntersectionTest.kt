package me.anno.games.flatworld.streets

import me.anno.engine.ui.render.RenderMode
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.games.flatworld.FlatWorld
import me.anno.utils.types.Floats.toRadians
import org.apache.logging.log4j.LogManager
import org.joml.Vector3d
import kotlin.random.Random

fun main() {
    // generate lots of different intersections, and see where they go wrong
    LogManager.disableLoggers("OpenXRSystem,OpenXRUtils,AudioManager,GPUShader,Saveable,GFX,GFXBase,FontManagerImpl")
    val world = FlatWorld()
    val random = Random(1234)
    for (j in 1 .. 7) {
        val numRotations = 5
        for (ri in 0 until numRotations) {
            val c0 = Vector3d(ri * 50.0, 0.0, j * 50.0)
            val cis = (0 until j).map {
                Vector3d(20.0, 0.0, 0.0)
                    .rotateY((it + ri.toDouble() / numRotations) / j * 360.0.toRadians())
                    .add(c0)
            }
            for (ci in cis.shuffled(random)) {
                world.addStreetSegment(StreetSegment(c0, null, ci))
            }
        }
    }
    world.validateMeshes()
    testSceneWithUI("Intersections", world.scene) {
        it.renderView.renderMode = RenderMode.NORMAL
    }
}