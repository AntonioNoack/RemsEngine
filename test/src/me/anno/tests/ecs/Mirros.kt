package me.anno.tests.ecs

import me.anno.ecs.Entity
import me.anno.ecs.components.light.PlanarReflection
import me.anno.ecs.components.mesh.Material
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.ecs.components.mesh.shapes.CylinderModel
import me.anno.ecs.components.mesh.shapes.PlaneModel
import me.anno.ecs.components.shaders.Skybox
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.gpu.CullMode
import me.anno.mesh.Shapes.flatCube
import org.joml.Vector3f
import kotlin.math.PI

fun main() {

    // todo reflections don't work properly inside mirrors (flipped on y?)
    // (recursive reflections, should be easy theoretically)

    // done white floor
    // done x/z planes as mirrors
    // done test object -> maybe a cylinder
    val scene = Entity("Scene")
    val xAxis = Vector3f(-1f, 0f, 0f)
    val yAxis = Vector3f(0f, -1f, 0f)
    val zAxis = Vector3f(0f, 0f, -1f)
    val mirror = Material()

    mirror.cullMode = CullMode.BACK
    mirror.metallicMinMax.set(1f)
    mirror.roughnessMinMax.set(0.15f)

    val white = Material()
    white.cullMode = CullMode.BOTH

    val mirrors = Entity("Mirrors", scene)
    mirrors.add(MeshComponent(
        PlaneModel.createPlane(1, 1, xAxis, yAxis, zAxis)
    ).apply { materials = listOf(mirror.ref) })
    mirrors.add(MeshComponent(
        PlaneModel.createPlane(1, 1, yAxis, zAxis, xAxis)
    ).apply { materials = listOf(white.ref) })
    mirrors.add(MeshComponent(
        PlaneModel.createPlane(1, 1, zAxis, xAxis, yAxis)
    ).apply { materials = listOf(mirror.ref) })

    val mirror0 = Entity("Mirror0", scene)
    mirror0.add(PlanarReflection())
    mirror0.setPosition(0.0, 0.0, -1.0)
    mirror0.setScale(10.0)

    val mirror1 = Entity("Mirror1", scene)
    mirror1.add(PlanarReflection())
    mirror1.setPosition(-1.0, 0.0, 0.0)
    mirror1.setRotation(0.0, PI / 2, 0.0)
    mirror1.setScale(10.0)

    val sample = Entity("Cylinder", scene)
    sample.add(MeshComponent(CylinderModel.createMesh(32, 2, top = true, bottom = true, null, 3f, Mesh())))
    sample.setPosition(0.0, -0.35, 0.0)
    sample.setScale(0.2)

    val sample2 = Entity("Cube", scene)
    sample2.add(MeshComponent(flatCube.front))
    sample2.setPosition(0.0, +0.35, -2.0)
    sample2.setScale(0.2)

    scene.add(Skybox())
    testSceneWithUI("Mirrors", scene)
}