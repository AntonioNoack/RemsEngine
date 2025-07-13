package me.anno.tests.shader

import me.anno.ecs.Entity
import me.anno.ecs.components.light.sky.SkyboxBase
import me.anno.ecs.components.light.sky.shaders.FixedSkyShader
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.engine.DefaultAssets
import me.anno.engine.DefaultAssets.icoSphere
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI

/**
 * Create a simplified sky shader for preview rendering.
 * */
fun main() {
    testSceneWithUI(
        "FixedSky", Entity()
            .add(MeshComponent(icoSphere, DefaultAssets.silverMaterial))
            .add(SkyboxBase().apply { shader = FixedSkyShader })
    )
}