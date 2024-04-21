package me.anno.tests.engine.material

import me.anno.ecs.Entity
import me.anno.ecs.components.mesh.LookAtComponent
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.ecs.components.mesh.material.Material
import me.anno.engine.EngineBase.Companion.enableVSync
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.gpu.texture.Clamping
import me.anno.io.files.Reference.getReference
import me.anno.mesh.Shapes

fun main() {
    // todo this lags behind one frame, can we get it perfect?
    val source = getReference("res://icon.png")
    val material = Material().apply {
        // for alpha
        diffuseMap = source
        diffuseBase.set(0f, 0f, 0f, 1f)
        // actual color
        emissiveMap = source
        emissiveBase.set(2f)
        linearFiltering = false
        clamping = Clamping.CLAMP
    }
    testSceneWithUI("Billboards", Entity().apply {
        add(Entity().apply {
            add(MeshComponent(Shapes.flat11.front).apply {
                materials = listOf(material.ref)
            })
            add(LookAtComponent())
        })
    }) {
        enableVSync = true // seeing the lag of one frame is easier at 60 fps than at 200 ^^
    }
}