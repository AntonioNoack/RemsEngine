package me.anno.tests.mesh

import me.anno.Time
import me.anno.config.DefaultConfig.style
import me.anno.ecs.Entity
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.engine.ECSRegistry
import me.anno.engine.ui.render.RenderState
import me.anno.engine.ui.render.Renderers.attributeRenderers
import me.anno.gpu.GFX
import me.anno.gpu.GFXState
import me.anno.gpu.GFXState.useFrame
import me.anno.gpu.deferred.BufferQuality
import me.anno.gpu.deferred.DeferredLayerType
import me.anno.gpu.drawing.DrawTextures
import me.anno.gpu.framebuffer.FBStack
import me.anno.gpu.pipeline.Pipeline
import me.anno.gpu.shader.Renderer
import me.anno.io.ISaveable
import me.anno.io.files.FileReference
import me.anno.io.files.FileReference.Companion.getReference
import me.anno.ui.Panel
import me.anno.ui.debug.TestStudio.Companion.testUI3
import kotlin.math.max
import kotlin.math.min

/**
 * load an existing asset, like the arrow, and draw it :)
 *
 * like UIMeshTestEasy, just more in-depth and more customizable (like easy MSAA support for rendered normals)
 * */
class SimpleMeshTest(
    var msaa: Boolean,
    var renderer: Renderer = attributeRenderers[DeferredLayerType.NORMAL],
    meshRef: FileReference = getReference("res://mesh/arrowX.obj"),
) : Panel(style) {

    val pipeline = Pipeline(null)
    val rootEntity = Entity()

    init {
        // for loading the mesh
        if ("Mesh" !in ISaveable.objectTypeRegistry) ECSRegistry.initMeshes()
        rootEntity.add(MeshComponent(meshRef))
    }

    override val canDrawOverBorders get() = true

    override fun onUpdate() {
        rootEntity.rotation = rootEntity.rotation.rotateZ(Time.deltaTime * 1.0)
        invalidateDrawing()
    }

    override fun onDraw(x0: Int, y0: Int, x1: Int, y1: Int) {
        // super call is not needed, as we draw over the background
        // define camera
        val wf = width.toFloat()
        val hf = height.toFloat()
        val s = max(1f, wf / hf)
        val t = max(1f, hf / wf)
        RenderState.cameraMatrix.identity().ortho(-s, +s, -t, +t, -1f, 0f)
        pipeline.frustum.defineOrthographic(
            2.0 * s, 2.0 * t, 2.0, height,
            RenderState.cameraPosition,
            RenderState.cameraRotation
        )
        // fill/update pipeline
        pipeline.clear()
        rootEntity.validateTransform()
        pipeline.fill(rootEntity)
        // render
        val samples = min(GFX.maxSamples, 8)
        val msaa = msaa && GFXState.currentBuffer.samples < samples
        val buffer =
            if (msaa) FBStack["msaa", width, height, 4, BufferQuality.LOW_8, samples, false] else GFXState.currentBuffer
        useFrame(x, y, width, height, buffer, renderer) {
            buffer.clearColor(backgroundColor, depth = true)
            pipeline.draw()
        }
        if (msaa) {
            // if we changed the framebuffer, blit the result onto the target framebuffer
            DrawTextures.drawTexture(x, y, width, height, buffer.getTexture0())
        }
    }
}

fun main() {
    // the main method is extracted, so it can be easily ported to web
    // a better method may come in the future
    testUI3("UIMesh") { SimpleMeshTest(true, attributeRenderers[DeferredLayerType.COLOR]) }
}