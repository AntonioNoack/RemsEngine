package me.anno.tests.maths

import me.anno.ecs.Component
import me.anno.ecs.Entity
import me.anno.ecs.EntityQuery.forAllChildren
import me.anno.ecs.EntityQuery.getComponent
import me.anno.ecs.annotations.Group
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.ecs.components.mesh.material.Material
import me.anno.ecs.systems.OnDrawGUI
import me.anno.ecs.systems.OnUpdate
import me.anno.engine.ui.LineShapes
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.gpu.pipeline.Pipeline
import me.anno.mesh.Shapes.flatCube
import me.anno.utils.structures.lists.Lists.wrap
import org.joml.AABBd
import org.joml.AABBf
import org.joml.Matrix4x3d
import org.joml.Vector3f

fun main() {

    val scene = Entity()

    val redMaterial = Material.diffuse(0xff0000)
    val greenMaterial = Material.diffuse(0x00ff00)

    val greenMatList = greenMaterial.ref.wrap()
    val redMatList = redMaterial.ref.wrap()

    // a box of test cubes
    val boxes = Entity("Boxes", scene)
    val s = 5
    for (z in -s..s) {
        for (y in -s..s) {
            for (x in -s..s) {
                val t = 10.0
                Entity(boxes)
                    .setPosition(x * t, y * t, z * t)
                    .add(MeshComponent(flatCube.front))
            }
        }
    }

    val tested = object : Component(), OnDrawGUI, OnUpdate {

        @Group("Points")
        var start = Vector3f(0f, 0f, 0f)

        @Group("Points")
        var end = Vector3f(0f, 0f, 50f)

        @Group("Radius")
        var radiusAtOrigin = 0f

        @Group("Radius")
        var radiusPerUnit = 0.5f

        override fun fillSpace(globalTransform: Matrix4x3d, dstUnion: AABBd): Boolean {
            dstUnion.all()
            return true
        }

        override fun onUpdate() {
            updateConeColor() // not really necessary to be executed every frame
        }

        fun updateConeColor() {
            scene.getBounds()
            val tmp = AABBf()
            boxes.forAllChildren(false) { box ->
                val hit = tmp.set(box.getBounds()).testLine(start, end, radiusAtOrigin, radiusPerUnit)
                val matList = if (hit) greenMatList else redMatList
                box.getComponent(MeshComponent::class)?.materials = matList
            }
        }

        override fun onDrawGUI(pipeline: Pipeline, all: Boolean) {
            LineShapes.drawCone(entity, start, end, radiusAtOrigin, radiusPerUnit)
        }
    }
    scene.add(tested)

    testSceneWithUI("Cone-AABB-Intersection", scene)
}