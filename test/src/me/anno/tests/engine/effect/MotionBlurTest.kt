package me.anno.tests.engine.effect

import me.anno.ecs.Component
import me.anno.ecs.Entity
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.ecs.components.mesh.material.Material
import me.anno.ecs.components.mesh.shapes.CylinderModel
import me.anno.ecs.systems.Updatable
import me.anno.engine.ui.render.RenderMode
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.utils.OS.res
import kotlin.math.PI

fun main() {
    val scene = Entity()
    val box = Entity(scene)
    box.add(object : Component(), Updatable {
        override fun update(instances: Collection<Component>) {
            Thread.sleep(50) // simulate low fps, so the result is better visible
            box.rotation = box.rotation.rotateY(PI / 6) // rotate quickly
        }
    })
    val cyl = CylinderModel.createMesh(50, 2, top = true, bottom = true, null, 3f, Mesh())
    box.add(MeshComponent(cyl).apply {
        materials = listOf(Material().apply {
            diffuseMap = res.getChild("icon.png")
            linearFiltering = false
        }.ref)
    })
    testSceneWithUI("MotionBlur", scene) {
        it.renderView.renderMode = RenderMode.MOTION_BLUR
    }
}