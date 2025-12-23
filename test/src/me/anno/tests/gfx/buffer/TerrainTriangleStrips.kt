package me.anno.tests.gfx.buffer

import me.anno.ecs.Entity
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.ecs.components.mesh.terrain.RectangleTerrainModel
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI

fun main() {
    val meshEven = RectangleTerrainModel.generateRegularQuadHeightMesh(
        8, 12, false, 1f,
        Mesh(), true
    )
    val meshOdd = RectangleTerrainModel.generateRegularQuadHeightMesh(
        9, 9, false, 1f,
        Mesh(), true
    )
    val scene = Entity()
    Entity(scene)
        .add(MeshComponent(meshEven))
    Entity(scene)
        .add(MeshComponent(meshOdd))
        .setPosition(0.0, 10.0, 0.0)
    testSceneWithUI("triangleStripTerrain", scene)
}