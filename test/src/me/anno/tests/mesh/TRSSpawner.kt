package me.anno.tests.mesh

import me.anno.ecs.Transform
import me.anno.ecs.components.mesh.IMesh
import me.anno.ecs.components.mesh.material.Material
import me.anno.ecs.components.mesh.MeshSpawner
import me.anno.engine.ECSRegistry
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.input.Input
import me.anno.mesh.Shapes
import me.anno.utils.structures.arrays.FloatArrayList

/**
 * tests a mode, where a lot more meshes are supported by limiting their transform, and therefore reducing bandwidth
 * press shift to activate the mode
 * */
fun main() {
    // fixed:
    // - size was missing, so the pipeline wasn't drawn
    // - order of operations was incorrect
    ECSRegistry.init()
    val transforms = Array(25) {
        val x = it % 5
        val y = it / 5
        Transform().apply {
            localPosition = localPosition.set(x * 2.5, 0.0, y * 2.5)
            localRotation = localRotation.identity().rotateX(x * 0.1).rotateY(y * 0.1)
        }
    }
    val mesh = Shapes.flatCube.front
    val spawner = object : MeshSpawner() {
        override fun forEachMesh(run: (IMesh, Material?, Transform) -> Unit) {
            for (tr in transforms) run(mesh, null, tr)
        }

        override fun forEachMeshGroupTRS(run: (IMesh, Material?) -> FloatArrayList): Boolean {
            return if (Input.isShiftDown) {
                val data = run(mesh, null)
                data.ensureExtra(transforms.size * 8)
                for (tr in transforms) {
                    data.add(tr.localPosition)
                    data.add(1f)
                    data.add(tr.localRotation)
                }
                true
            } else false
        }
    }
    testSceneWithUI("TRSSpawner", spawner)
}