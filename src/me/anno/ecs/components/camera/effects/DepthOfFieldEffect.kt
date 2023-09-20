package me.anno.ecs.components.camera.effects

import me.anno.ecs.annotations.Range
import me.anno.engine.ui.render.RenderState
import me.anno.engine.ui.render.Renderers.tonemapGLSL
import me.anno.gpu.GFXState.useFrame
import me.anno.gpu.buffer.SimpleBuffer.Companion.flat01
import me.anno.gpu.deferred.DeferredLayerType
import me.anno.gpu.deferred.DeferredSettingsV2
import me.anno.gpu.framebuffer.FBStack
import me.anno.gpu.framebuffer.IFramebuffer
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.DepthTransforms.rawToDepth
import me.anno.gpu.shader.DepthTransforms.depthVars
import me.anno.gpu.shader.Shader
import me.anno.gpu.shader.ShaderLib
import me.anno.gpu.shader.ShaderLib.quatRot
import me.anno.gpu.shader.builder.Variable
import me.anno.gpu.shader.builder.VariableMode
import me.anno.gpu.texture.Clamping
import me.anno.gpu.texture.GPUFiltering
import me.anno.gpu.texture.ITexture2D
import me.anno.graph.render.effects.DepthOfFieldNode
import me.anno.maths.Maths.ceilDiv
import kotlin.math.tan

class DepthOfFieldEffect : CameraEffect() {

    var focusPoint = 1f
    var focusScale = 0.25f
    var radScale = 0.5f // Smaller = nicer blur, larger = faster
    var maxBlurSize = 20f // in pixels

    @Range(0.0, 1.0)
    var spherical = 0f

    var applyToneMapping = false

    override fun render(
        buffer: IFramebuffer,
        format: DeferredSettingsV2,
        layers: MutableMap<DeferredLayerType, IFramebuffer>
    ) {
        val color = layers[DeferredLayerType.SDR_RESULT]!!.getTexture0()
        val depth = layers[DeferredLayerType.DEPTH]!!.getTexture0()
        val output = DepthOfFieldNode.render(color, depth, spherical, focusPoint, focusScale, maxBlurSize, radScale, applyToneMapping)
        write(layers, DeferredLayerType.SDR_RESULT, output)
    }

    override fun listInputs() =
        listOf(DeferredLayerType.SDR_RESULT, DeferredLayerType.DEPTH)

    override fun listOutputs() =
        listOf(DeferredLayerType.SDR_RESULT)

}