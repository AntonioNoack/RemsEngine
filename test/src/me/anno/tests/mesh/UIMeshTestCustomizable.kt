package me.anno.tests.mesh

import me.anno.config.DefaultConfig.defaultFont
import me.anno.config.DefaultConfig.style
import me.anno.ecs.Entity
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.ecs.components.mesh.shapes.RingMeshModel.createRingMesh
import me.anno.ecs.components.text.TextMeshComponent
import me.anno.engine.DefaultAssets.flatCube
import me.anno.engine.ECSRegistry
import me.anno.engine.OfficialExtensions
import me.anno.engine.ui.render.RenderSize
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
import me.anno.maths.Maths.TAU
import me.anno.maths.Maths.posMod
import me.anno.ui.Panel
import me.anno.ui.base.components.AxisAlignment
import me.anno.ui.debug.TestEngine.Companion.testUI3
import me.anno.utils.OS.res
import java.util.Date
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/**
 * load an existing asset, like the arrow, build a clock from it, animate and draw it
 *
 * like UIMeshTestEasy, just more in-depth and more customizable (like easy MSAA support for rendered normals)
 * */
class SimpleMeshTest(
    var msaa: Boolean,
    var renderer: Renderer = attributeRenderers[DeferredLayerType.NORMAL],
    meshRef: FileReference = res.getChild("meshes/arrowX.obj"),
) : Panel(style) {

    companion object {
        const val ZERO_ANGLE = PI / 2
    }

    val pipeline = Pipeline(null)
    val rootEntity = Entity()

    val hour = Entity(rootEntity)
    val minutes = Entity(rootEntity)
    val seconds = Entity(rootEntity)

    init {
        // for loading the mesh
        ECSRegistry.init()
        val setBackMesh = Entity()
            .setPosition(-0.05, 0.0, 0.0)
            .add(MeshComponent(meshRef)).ref
        hour.add(MeshComponent(setBackMesh)).setScale(0.6, 0.9, 0.9)
        minutes.add(MeshComponent(setBackMesh)).setScale(0.85, 0.58, 1.0)
        seconds.add(MeshComponent(setBackMesh)).setScale(0.95, 0.1, 1.0)
        // create 12 text-meshes
        for (i in 1..12) {
            val angle = -i * TAU / 12 + ZERO_ANGLE
            val radius = 0.81
            Entity(rootEntity)
                .add(TextMeshComponent("$i", defaultFont, AxisAlignment.CENTER))
                .setPosition(cos(angle) * radius, sin(angle) * radius, 0.0)
                .setScale(0.1)
            // add 60 small lines
            for (j in 0 until 5) {
                val angleI = angle + j * TAU / 60.0
                val scaleX = if (j == 0) 0.015 else 0.01
                val scaleY = scaleX * 0.7
                val radiusI = 0.98 - scaleX
                Entity(rootEntity)
                    .add(MeshComponent(flatCube))
                    .setPosition(cos(angleI) * radiusI, sin(angleI) * radiusI, 0.0)
                    .setRotation(0.0, 0.0, angleI)
                    .setScale(scaleX, scaleY, 0.01)
            }
        }
        rootEntity.add(MeshComponent(createRingMesh(12 * 12, 0.98f, 0.99f)))
    }

    override val canDrawOverBorders get() = true

    override fun onUpdate() {
        val secondsRaw = Date().time / 1000
        val daysRem = posMod(secondsRaw, (3600 * 24))
        val hours = daysRem / 3600.0 + 1
        val min = daysRem / 60.0
        val sec = posMod(daysRem, 60)
        hour.setRotation(0.0, 0.0, -hours * TAU / 12.0 + ZERO_ANGLE)
        minutes.setRotation(0.0, 0.0, -min * TAU / 60.0 + ZERO_ANGLE)
        seconds.setRotation(0.0, 0.0, -sec * TAU / 60.0 + ZERO_ANGLE)
        invalidateDrawing()
    }

    private val size = RenderSize()
    override fun onDraw(x0: Int, y0: Int, x1: Int, y1: Int) {
        // super call is not needed, as we draw over the background
        // define camera
        val wf = width.toFloat()
        val hf = height.toFloat()
        size.updateSize(width, height)
        val width = size.renderWidth
        val height = size.renderHeight
        val s = max(1f, wf / hf)
        val t = max(1f, hf / wf)
        RenderState.cameraMatrix.identity()
            .ortho(-s, +s, +t, -t, -1f, 0f)
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
            DrawTextures.drawTexture(x, y, this.width, this.height, buffer.getTexture0())
        }
    }
}

fun main() {
    OfficialExtensions.initForTests()
    // the main method is extracted, so it can be easily ported to web
    // a better method may come in the future
    testUI3("UIMesh") { SimpleMeshTest(true, attributeRenderers[DeferredLayerType.COLOR]) }
}