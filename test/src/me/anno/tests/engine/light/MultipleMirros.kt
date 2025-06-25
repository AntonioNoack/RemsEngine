package me.anno.tests.engine.light

import me.anno.ecs.Entity
import me.anno.ecs.components.light.PlanarReflection
import me.anno.ecs.components.mesh.material.Material
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.ecs.components.mesh.shapes.CylinderModel
import me.anno.ecs.components.mesh.shapes.PlaneModel
import me.anno.ecs.components.light.sky.Skybox
import me.anno.ecs.components.mesh.shapes.IcosahedronModel
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.gpu.CullMode
import me.anno.gpu.pipeline.PipelineStage
import me.anno.maths.Maths.PIf
import me.anno.mesh.Shapes.flatCube
import org.joml.Vector3f

/**
 * recursive mirrors don't work as easy as that, because we could look behind things
 * */
fun main() {

    // done white floor
    // done x/z planes as mirrors
    // done test object -> maybe a cylinder
    val scene = Entity("Scene")
    val xAxis = Vector3f(-1f, 0f, 0f)
    val yAxis = Vector3f(0f, -1f, 0f)
    val zAxis = Vector3f(0f, 0f, -1f)
    val mirror = Material().apply {
        cullMode = CullMode.BACK
        metallicMinMax.set(1f)
        roughnessMinMax.set(0.15f)
    }

    val white = Material()
    white.cullMode = CullMode.BOTH

    Entity("Mirrors", scene)
        .add(MeshComponent(PlaneModel.createPlane(1, 1, xAxis, yAxis, zAxis), mirror))
        .add(MeshComponent(PlaneModel.createPlane(1, 1, yAxis, zAxis, xAxis), mirror))
        .add(MeshComponent(PlaneModel.createPlane(1, 1, zAxis, xAxis, yAxis), mirror))

    Entity("Mirror0", scene)
        .add(PlanarReflection())
        .setPosition(0.0, 0.0, -1.0)
        .setScale(10f)

    Entity("Mirror1", scene)
        .add(PlanarReflection())
        .setPosition(-1.0, 0.0, 0.0)
        .setRotation(0f, PIf / 2, 0f)
        .setScale(10f)

    val glassMaterial = Material.metallic(-1, 0f)
        .apply { pipelineStage = PipelineStage.GLASS }
    Entity("Sphere", scene)
        .add(MeshComponent(IcosahedronModel.createIcosphere(3), glassMaterial))
        .setPosition(0.0, 0.35, 0.0)
        .setScale(0.2f)

    Entity("Cylinder", scene)
        .add(MeshComponent(CylinderModel.createCylinder(32, 2, top = true, bottom = true, null, 3f, Mesh())))
        .setPosition(0.0, -0.35, 0.0)
        .setScale(0.2f)

    Entity("Cube", scene)
        .add(MeshComponent(flatCube.front))
        .setPosition(0.0, +0.35, -2.0)
        .setScale(0.2f)

    scene.add(Skybox())
    testSceneWithUI("Mirrors", scene)
}