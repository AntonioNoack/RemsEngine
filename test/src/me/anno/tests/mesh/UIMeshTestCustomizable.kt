package me.anno.tests.mesh

import me.anno.config.DefaultConfig.style
import me.anno.ecs.Entity
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.engine.ECSRegistry
import me.anno.engine.OfficialExtensions
import me.anno.engine.ui.render.RenderState
import me.anno.engine.ui.render.Renderers.attributeRenderers
import me.anno.gpu.GFX
import me.anno.gpu.GFXState
import me.anno.gpu.GFXState.useFrame
import me.anno.gpu.deferred.BufferQuality
import me.anno.gpu.deferred.DeferredLayerType
import me.anno.gpu.drawing.DrawTextures
import me.anno.gpu.framebuffer.DepthBufferType
import me.anno.gpu.framebuffer.FBStack
import me.anno.gpu.pipeline.Pipeline
import me.anno.gpu.shader.renderer.Renderer
import me.anno.io.files.FileReference
import me.anno.ui.Panel
import me.anno.ui.debug.TestEngine.Companion.testUI3
import me.anno.utils.OS.res
import kotlin.math.atan2
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
    meshRef: FileReference = res.getChild("meshes/arrowX.obj"),
) : Panel(style) {

    val pipeline = Pipeline(null)
    val rootEntity = Entity()

    init {
        // for loading the mesh
        ECSRegistry.init()
        rootEntity.add(MeshComponent(meshRef))
    }

    override val canDrawOverBorders get() = true

    override fun onUpdate() {
        val window = window ?: return
        val mx = window.mouseX - (x + width / 2) + 0.1f // to avoid 0/0
        val my = window.mouseY - (y + height / 2)
        rootEntity.rotation = rootEntity.rotation
            .identity()
            .rotateZ(atan2(my, mx).toDouble())
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
            if (msaa) FBStack["msaa", width, height, 4, BufferQuality.UINT_8, samples, DepthBufferType.NONE]
            else GFXState.currentBuffer
        useFrame(x, y, width, height, buffer, renderer) {
            buffer.clearColor(backgroundColor, depth = true)
            pipeline.singlePassWithSky(false)
        }
        if (msaa) {
            // if we changed the framebuffer, blit the result onto the target framebuffer
            DrawTextures.drawTexture(x, y, width, height, buffer.getTexture0())
        }
    }
}

fun main() {
    OfficialExtensions.initForTests()
    // the main method is extracted, so it can be easily ported to web
    // a better method may come in the future
    testUI3("UIMesh") { SimpleMeshTest(true, attributeRenderers[DeferredLayerType.COLOR]) }
}