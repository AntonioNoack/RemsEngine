package me.anno.tests.gfx.nanite

import me.anno.ecs.Component
import me.anno.ecs.Transform
import me.anno.ecs.components.mesh.IMesh
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.material.Material
import me.anno.ecs.components.mesh.material.Materials
import me.anno.gpu.Blitting
import me.anno.gpu.CullMode
import me.anno.gpu.GFXState
import me.anno.gpu.GFXState.renderPurely
import me.anno.gpu.GFXState.useFrame
import me.anno.gpu.buffer.Buffer
import me.anno.gpu.buffer.DrawMode
import me.anno.gpu.buffer.IndexBuffer
import me.anno.gpu.framebuffer.DepthBufferType
import me.anno.gpu.framebuffer.FBStack
import me.anno.gpu.framebuffer.IFramebuffer
import me.anno.gpu.framebuffer.TargetType
import me.anno.gpu.pipeline.Pipeline
import me.anno.gpu.pipeline.PipelineStageImpl.Companion.bindRandomness
import me.anno.gpu.pipeline.PipelineStageImpl.Companion.initShader
import me.anno.gpu.pipeline.PipelineStageImpl.Companion.setupLights
import me.anno.gpu.pipeline.PipelineStageImpl.Companion.setupLocalTransform
import me.anno.gpu.shader.ComputeShader
import me.anno.gpu.shader.ComputeTextureMode
import me.anno.gpu.shader.Shader
import me.anno.gpu.shader.builder.Variable
import me.anno.gpu.texture.ITexture2D
import me.anno.gpu.texture.Texture2D
import me.anno.gpu.texture.TextureLib.blackTexture
import me.anno.input.Input
import org.joml.AABBd
import org.joml.AABBf

class ComputeShaderMesh(val mesh: Mesh) : IMesh {

    companion object {
        fun useTraditionalRendering(): Boolean {
            return Input.isShiftDown
        }
    }

    lateinit var component: Component

    override val numPrimitives: Long
        get() = mesh.numPrimitives

    override fun ensureBuffer() {
        mesh.ensureBuffer()
    }

    override fun getBounds(): AABBf {
        return mesh.getBounds()
    }

    override fun draw(pipeline: Pipeline?, shader: Shader, materialIndex: Int, drawLines: Boolean) {
        if (useTraditionalRendering()) {
            mesh.draw(pipeline, shader, materialIndex, drawLines)
        } else {
            drawInstanced0(pipeline!!, shader, materialIndex, null, drawLines)
        }
    }

    override fun drawInstanced(
        pipeline: Pipeline, shader: Shader, materialIndex: Int,
        instanceData: Buffer, drawLines: Boolean
    ) {
        if (useTraditionalRendering()) {
            mesh.drawInstanced(pipeline, shader, materialIndex, instanceData, drawLines)
        } else {
            drawInstanced0(pipeline, shader, materialIndex, instanceData, drawLines)
        }
    }

    fun drawInstanced0(
        pipeline: Pipeline,
        shader: Shader,
        materialIndex: Int,
        instanceData: Buffer?,
        drawLines: Boolean
    ) {
        GFXState.cullMode.use(CullMode.BOTH) {
            renderPurely {
                drawInstanced1(pipeline, shader, materialIndex, instanceData, drawLines)
            }
        }
    }

    fun drawInstanced1(
        pipeline: Pipeline,
        shader: Shader,
        materialIndex: Int,
        instanceData: Buffer?,
        drawLines: Boolean
    ) {

        mesh.ensureBuffer()
        val triBuffer = mesh.triBuffer
        val target = GFXState.currentBuffer

        // copy depth to color
        val depthAsColor = getDepthTarget(target)

        val deferredSettings = GFXState.currentRenderer.deferredSettings
        val key = ComputeShaderKey(
            shader, deferredSettings, mesh.buffer!!.attributes,
            instanceData?.attributes ?: emptyList(),
            triBuffer?.elementsType,
            if (drawLines) DrawMode.LINES else mesh.drawMode
        )

        val (rasterizer, outputs) = ComputeShaders.shaders[key]
        rasterizer.use()

        val numPrimitives = mesh.numPrimitives.toInt()

        bindBuffers(rasterizer, instanceData, triBuffer)
        bindUniforms(pipeline, rasterizer, materialIndex, instanceData, target, numPrimitives)
        bindTargets(rasterizer, depthAsColor, outputs, target)

        rasterizer.runBySize(numPrimitives * (instanceData?.drawLength ?: 1))

        writeDepth(target, depthAsColor)
    }

    private fun getDepthTarget(target: IFramebuffer): Texture2D {
        val depthAsColor = FBStack[
            "depthAsColor", target.width, target.height,
            listOf(TargetType.Float32x1), 1, DepthBufferType.NONE
        ]
        useFrame(depthAsColor) {
            Blitting.copy(target.depthTexture!!, false)
        }
        return depthAsColor.getTexture0() as Texture2D
    }

    private fun writeDepth(target: IFramebuffer, depthAsColor: ITexture2D) {
        // copy depth from writable depth
        // disable all colors being written
        useFrame(null, target) {
            Blitting.copyColorAndDepth(blackTexture, depthAsColor, 0, false) // is 0 as mask correct???
        }
    }

    private fun bindBuffers(rasterizer: ComputeShader, instanceBuffer: Buffer?, indexBuffer: IndexBuffer?) {
        rasterizer.bindBuffer(0, mesh.buffer!!)
        if (instanceBuffer != null) {
            instanceBuffer.ensureBuffer()
            rasterizer.bindBuffer(1, instanceBuffer)
        }
        if (indexBuffer != null) {
            rasterizer.bindBuffer(2, indexBuffer)
        }
    }

    private fun bindUniforms(
        pipeline: Pipeline, shader: ComputeShader, materialIndex: Int,
        instanceData: Buffer?, target: IFramebuffer,
        numPrimitives: Int,
    ) {
        val material = Materials.getMaterial(null, mesh.materials, materialIndex)
        material.bind(shader)
        initShader(shader, false)
        bindRandomness(shader)
        setupLocalTransform(shader, null, 0L)
        setupLights(pipeline, shader, AABBd(), true)
        shader.v1i("numPrimitives", numPrimitives)
        shader.v1i("numInstances", instanceData?.drawLength ?: 1)
        shader.v2i("viewportSize", target.width, target.height)
    }

    private fun bindTargets(
        rasterizer: ComputeShader, depthAsColor: Texture2D,
        outputs: List<Variable>, target: IFramebuffer
    ) {
        rasterizer.bindTexture(0, depthAsColor, ComputeTextureMode.READ_WRITE)
        for (i in outputs.indices) {
            rasterizer.bindTexture(i + 1, target.getTextureI(i) as Texture2D, ComputeTextureMode.WRITE)
        }
    }

    override fun fill(pipeline: Pipeline, transform: Transform, clickId: Int): Int {
        val material = Material.defaultMaterial
        pipeline.findStage(material)
            .add(component, this, transform, material, 0)
        return clickId + 1
    }
}