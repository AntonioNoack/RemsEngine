package me.anno.tests.mesh.hexagons

import me.anno.ecs.components.mesh.material.Material
import me.anno.gpu.pipeline.PipelineStage

val diffuseHexMaterial = Material().apply {
    shader = HSMCShader
}

val transparentHexMaterial = Material().apply {
    shader = HSMCShader
    pipelineStage = PipelineStage.GLASS
}
