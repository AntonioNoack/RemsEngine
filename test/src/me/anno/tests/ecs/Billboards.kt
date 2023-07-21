package me.anno.tests.ecs

import me.anno.ecs.Entity
import me.anno.ecs.components.mesh.BillboardTransformer
import me.anno.ecs.components.mesh.Material
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.gpu.texture.Clamping
import me.anno.mesh.Shapes
import me.anno.utils.OS.pictures

fun main() {
    val material = Material().apply {
        // for alpha
        diffuseMap = pictures.getChild("fav128.ico")
        diffuseBase.set(0f, 0f, 0f, 1f)
        // actual color
        emissiveMap = pictures.getChild("fav128.ico")
        emissiveBase.set(5f)
        linearFiltering = false
        clamping = Clamping.CLAMP
    }
    testSceneWithUI("Billboards", Entity().apply {
        add(Entity().apply {
            add(MeshComponent(Shapes.flat11.front).apply {
                materials = listOf(material.ref)
            })
            add(BillboardTransformer())
        })
    })
}