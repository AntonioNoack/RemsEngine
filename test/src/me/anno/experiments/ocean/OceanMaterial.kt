package me.anno.experiments.ocean

import me.anno.Time
import me.anno.ecs.components.mesh.material.Material
import me.anno.gpu.CullMode
import me.anno.gpu.GFXState
import me.anno.gpu.framebuffer.NullFramebuffer.depthTexture
import me.anno.gpu.pipeline.PipelineStage
import me.anno.gpu.shader.DepthTransforms
import me.anno.gpu.shader.GPUShader
import me.anno.gpu.texture.Clamping
import me.anno.gpu.texture.Filtering
import me.anno.gpu.texture.TextureCache
import me.anno.gpu.texture.TextureLib
import me.anno.io.files.FileReference
import me.anno.tests.engine.material.ForceFieldShader.findDepthTexture
import me.anno.utils.OS.res
import org.joml.AABBf
import org.joml.Vector2f

class OceanMaterial : Material() {

    init {
        shader = OceanShader
        cullMode = CullMode.BOTH
        pipelineStage = PipelineStage.GLASS
    }

    var waveHeightMap: FileReference = res.getChild("textures/TileableNoise.png")

    val normal1Offset = Vector2f()
    val normal2Offset = Vector2f()
    val normal3Offset = Vector2f()

    val normal1Speed = Vector2f(-0.14f, 0.058f)
    val normal2Speed = Vector2f(0.015f, -0.07f)
    val normal3Speed = Vector2f(-0.07f, 0.07f)

    val waveBounds = AABBf()

    private var lastFrameIndex = 0
    fun update() {
        if (Time.frameIndex == lastFrameIndex) return

        lastFrameIndex = Time.frameIndex

        val dt = Time.deltaTime.toFloat() * 0.3f
        normal1Offset.fma(dt, normal1Speed).fract()
        normal2Offset.fma(dt, normal2Speed).fract()
        normal3Offset.fma(dt, normal3Speed).fract()
    }

    override fun bind(shader: GPUShader) {
        super.bind(shader)

        update()

        shader.v1f("time", Time.gameTime)
        DepthTransforms.bindDepthUniforms(shader)

        shader.v2f("normal1Offset", normal1Offset)
        shader.v2f("normal2Offset", normal2Offset)
        shader.v2f("normal3Offset", normal3Offset)

        shader.v4f(
            "waveBounds",
            waveBounds.minX, waveBounds.minZ,
            waveBounds.maxX, waveBounds.maxZ,
        )

        val waveHeightMap = TextureCache[waveHeightMap].value ?: TextureLib.grayTexture
        waveHeightMap.bind(shader, "waveHeightMap", Filtering.LINEAR, Clamping.REPEAT)

        val depth = findDepthTexture(GFXState.framebuffer.currentValue)
        (depth?.depthTexture ?: depthTexture).bindTrulyNearest(shader, "depthTex")
    }
}
