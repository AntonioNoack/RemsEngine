package me.anno.tests.mesh

import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.terrain.HexagonTerrainModel.createHexagonTerrain
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.maths.chunks.hexagon.HexagonGridMaths

fun main() {
    testSceneWithUI(
        "HexagonTerrain",
        createHexagonTerrain(12, 0.2f, HexagonGridMaths.corners, Mesh())
    )
}