package me.anno.tests.geometry

import me.anno.ecs.Transform
import me.anno.ecs.components.mesh.IMesh
import me.anno.ecs.components.mesh.material.Material
import me.anno.ecs.components.mesh.MeshSpawner
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.io.Saveable.Companion.registerCustomClass
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
    override fun forEachMesh(run: (IMesh, Material?, Transform) -> Unit) {
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