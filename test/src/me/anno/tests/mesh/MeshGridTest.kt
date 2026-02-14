package me.anno.tests.mesh

import me.anno.ecs.Entity
import me.anno.ecs.components.mesh.grid.MeshGrid
import me.anno.engine.DefaultAssets.flatCube
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI

fun main() {
    // todo test placing some things into the grid
    // using a parameterized @DebugAction function -> works :3
    val scene = Entity()
        .add(MeshGrid().apply {
            cellSize.set(2.5)
            fill(0, 0, 2, 3, flatCube)
        })
    testSceneWithUI("Mesh Grid", scene)
}