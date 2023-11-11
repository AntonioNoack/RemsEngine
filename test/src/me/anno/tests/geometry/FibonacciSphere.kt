package me.anno.tests.geometry

import me.anno.ecs.Transform
import me.anno.ecs.components.mesh.Material
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.MeshSpawner
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.io.ISaveable.Companion.registerCustomClass
import me.anno.maths.geometry.FibonacciSphere
import me.anno.mesh.Shapes.smoothCube
import kotlin.math.sqrt

/**
 * shows the fibonacci sphere distribution;
 * not optimized for rendering large n
 * */
class FibonacciSpawner : MeshSpawner() {
    var n = 12
    var scale = 0.3
    override fun forEachMesh(run: (Mesh, Material?, Transform) -> Unit) {
        val scale = scale * sqrt(1.0 / n)
        for ((i, pt) in FibonacciSphere.create(n).withIndex()) {
            val tr = getTransform(i)
            tr.localPosition = tr.localPosition.set(pt)
            tr.localScale = tr.localScale.set(scale)
            run(smoothCube.front, null, tr)
        }
    }
}

fun main() {
    registerCustomClass(FibonacciSpawner())
    testSceneWithUI("FibonacciSphere", FibonacciSpawner())
}