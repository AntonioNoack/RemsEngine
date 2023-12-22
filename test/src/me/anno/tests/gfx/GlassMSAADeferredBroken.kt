package me.anno.tests.gfx

import me.anno.ecs.Entity
import me.anno.ecs.components.mesh.Material
import me.anno.engine.ui.render.RenderMode
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.gpu.GFXState.useFrame
import me.anno.gpu.framebuffer.DepthBufferType
import me.anno.gpu.framebuffer.Framebuffer
import me.anno.gpu.framebuffer.TargetType
import me.anno.gpu.hidden.HiddenOpenGLContext
import me.anno.gpu.pipeline.PipelineStage.Companion.TRANSPARENT_PASS
import me.anno.sdf.shapes.SDFSphere

fun main() {
    // glass shader with MSAA was broken
    // todo MSAA is not working?
    // testSimple()
    testComplex()
}

fun testSimple() {
    HiddenOpenGLContext.createOpenGL()
    val base = Framebuffer(
        "base", 1, 1, 2, arrayOf(),
        DepthBufferType.INTERNAL
    )
    val derived = base.attachFramebufferToDepth(
        "derived",
        arrayOf(TargetType.UByteTarget4)
    )
    useFrame(derived) {}
}

fun testComplex() {
    val scene = Entity()
    val sphere = SDFSphere()
    sphere.sdfMaterials = listOf(Material().apply {
        roughnessMinMax.set(0.1f)
        metallicMinMax.set(1f)
        pipelineStage = TRANSPARENT_PASS
    }.ref)
    sphere.material.pipelineStage = TRANSPARENT_PASS
    scene.add(sphere)
    testSceneWithUI("Glass MSAA Deferred", scene) {
        it.renderer.renderMode = RenderMode.MSAA_DEFERRED
    }
}