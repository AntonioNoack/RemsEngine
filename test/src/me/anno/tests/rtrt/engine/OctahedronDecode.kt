package me.anno.tests.rtrt.engine

import me.anno.ecs.Entity
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.ecs.components.mesh.shapes.IcosahedronModel
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.maths.Maths.clamp
import org.joml.Vector2f
import org.joml.Vector3f
import kotlin.math.abs

fun main() {
    // https://knarkowicz.wordpress.com/2014/04/16/octahedron-normal-vector-encoding/
    fun decode(f: Vector2f): Vector3f {
        f.mul(2f).sub(1f, 1f)
        // https://twitter.com/Stubbesaurus/status/937994790553227264
        val n = Vector3f(f.x, f.y, 1f - abs(f.x) - abs(f.y))
        val t = clamp(-n.z)
        n.add(
            if (n.x >= 0f) -t else +t,
            if (n.y >= 0f) -t else +t,
            0f
        )
        return n.normalize()
    }

    val entity = Entity()
    val s = 75
    val meshFile = IcosahedronModel.createIcosphere(2)
    for (y in 0 until s) {
        val yf = y / (s - 1f)
        for (x in 0 until s) {
            val xf = x / (s - 1f)
            val child = Entity()
            val meshComp = MeshComponent(meshFile)
            meshComp.isInstanced = true
            child.add(meshComp)
            child.position = child.position
                .set(decode(Vector2f(xf, yf)))
            child.scale = child.scale.set(1.0 / s)
            entity.add(child)
        }
    }
    testSceneWithUI("Octahedron Decode", entity)
}