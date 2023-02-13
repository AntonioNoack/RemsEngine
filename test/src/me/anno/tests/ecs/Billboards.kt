package me.anno.tests.ecs

import me.anno.ecs.Entity
import me.anno.ecs.components.mesh.BillboardTransformer
import me.anno.ecs.components.mesh.Material
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.engine.ui.render.SceneView
import me.anno.gpu.texture.Clamping
import me.anno.mesh.Shapes
import me.anno.utils.OS

fun main() {
    val material = Material().apply {
        diffuseMap = OS.pictures.getChild("fav128.ico")
        clamping = Clamping.CLAMP
    }
    SceneView.testSceneWithUI(Entity().apply {
        add(MeshComponent(Shapes.flat11.front).apply {
            materials = listOf(material.ref)
        })
        add(BillboardTransformer())
    })
}