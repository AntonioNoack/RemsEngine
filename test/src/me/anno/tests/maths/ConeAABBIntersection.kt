package me.anno.tests.maths

import me.anno.ecs.Component
import me.anno.ecs.Entity
import me.anno.ecs.EntityQuery.getComponent
import me.anno.ecs.annotations.Group
import me.anno.ecs.components.mesh.material.Material
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.engine.ui.LineShapes
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.gpu.pipeline.Pipeline
import me.anno.mesh.Shapes.flatCube
import org.joml.AABBd
import org.joml.AABBf
import org.joml.Matrix4x3d
import org.joml.Vector3f

fun main() {

    val scene = Entity()

    val redMaterial = Material()
    redMaterial.diffuseBase.set(1f, 0f, 0f, 1f)
    val greenMaterial = Material()
    greenMaterial.diffuseBase.set(0f, 1f, 0f, 1f)

    val greenMatList = listOf(greenMaterial.ref)
    val redMatList = listOf(redMaterial.ref)

    // a box of test cubes
    val boxes = ArrayList<Entity>()
    val s = 5
    for (z in -s..s) {
        for (y in -s..s) {
            for (x in -s..s) {
                val t = 10.0
                val box = Entity(MeshComponent(flatCube.front))
                box.setPosition(x * t, y * t, z * t)
                boxes.add(box)
            }
        }
    }
    val boxesEntity = Entity("Boxes")
    for (box in boxes) boxesEntity.add(box)
    scene.add(boxesEntity)

    val tested = object : Component() {

        @Group("Points")
        var start = Vector3f(0f, 0f, 0f)

        @Group("Points")
        var end = Vector3f(0f, 0f, 50f)

        @Group("Radius")
        var radiusAtOrigin = 0f

        @Group("Radius")
        var radiusPerUnit = 0.5f

        override fun fillSpace(globalTransform: Matrix4x3d, aabb: AABBd): Boolean {
            aabb.all()
            return true
        }

        fun updateConeColor() {
            scene.getBounds()
            val tmp = AABBf()
            for (box in boxes) {
                val hit = tmp.set(box.aabb).testLine(start, end, radiusAtOrigin, radiusPerUnit)
                val matList = if (hit) greenMatList else redMatList
                box.getComponent(MeshComponent::class)!!.materials = matList
            }
        }

        override fun onDrawGUI(pipeline: Pipeline, all: Boolean) {
            LineShapes.drawCone(entity, start, end, radiusAtOrigin, radiusPerUnit)
            updateConeColor() // not really necessary to be executed every frame
        }
    }
    scene.add(tested)

    testSceneWithUI("Cone-AABB-Intersection", scene)
}