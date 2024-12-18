package me.anno.tests.mesh.spline

import me.anno.ecs.Entity
import me.anno.ecs.components.mesh.material.Material
import me.anno.ecs.components.mesh.spline.SplineControlPoint
import me.anno.ecs.components.mesh.spline.SplineMesh
import me.anno.ecs.components.mesh.spline.SplineProfile
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.utils.OS.res
import me.anno.utils.structures.arrays.FloatArrayList
import org.joml.Vector2f

/**
 * test that splines properly generate UV along their run length
 * */
fun main() {
    val uvProfile = SplineProfile(
        listOf(Vector2f(-0.5f, 0f), Vector2f(+0.5f, 0f)),
        FloatArrayList(floatArrayOf(0f, 1f)),
        null, false
    )
    // todo bug: the repetition of the pattern is along the wrong axis... how???
    val texture = res.getChild("icon.png")
    val material = Material().apply { diffuseMap = texture }
    val scene = Entity()
        .add(Entity().add(SplineControlPoint()))
        .add(Entity().add(SplineControlPoint()).setPosition(0.0, 0.0, -3.0))
        .add(SplineMesh().apply { materials = listOf(material.ref); profile = uvProfile })
    testSceneWithUI("SplinesWithUVs", scene)
}