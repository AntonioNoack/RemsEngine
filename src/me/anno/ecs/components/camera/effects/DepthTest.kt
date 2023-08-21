package me.anno.ecs.components.camera.effects

import me.anno.engine.ui.render.RenderState
import me.anno.gpu.GFXState.useFrame
import me.anno.gpu.buffer.SimpleBuffer.Companion.flat01
import me.anno.gpu.deferred.DeferredLayerType
import me.anno.gpu.deferred.DeferredSettingsV2
import me.anno.gpu.framebuffer.FBStack
import me.anno.gpu.framebuffer.IFramebuffer
import me.anno.gpu.shader.DepthTransforms
import me.anno.gpu.shader.DepthTransforms.depthToPosition
import me.anno.gpu.shader.DepthTransforms.depthVars
import me.anno.gpu.shader.DepthTransforms.rawToDepth
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.Shader
import me.anno.gpu.shader.ShaderLib
import me.anno.gpu.shader.ShaderLib.quatRot
import me.anno.gpu.shader.builder.Variable
import me.anno.gpu.shader.builder.VariableMode
import me.anno.gpu.texture.ITexture2D

class DepthTest : CameraEffect() {

    override fun render(
        buffer: IFramebuffer,
        format: DeferredSettingsV2,
        layers: MutableMap<DeferredLayerType, IFramebuffer>
    ) {
        val depth = layers[DeferredLayerType.DEPTH]!!.getTexture0()
        val output = FBStack["depthTest", depth.width, depth.height, 4, false, 1, false]
        useFrame(output) {
            val shader = shader
            shader.use()
            shader.v1f("worldScale", RenderState.worldScale)
            shader.v3f("cameraPosition", RenderState.cameraPosition)
            DepthTransforms.bindDepthToPosition(shader)
            depth.bindTrulyNearest(shader, "depthTex")
            flat01.draw(shader)
        }
        write(layers, DeferredLayerType.SDR_RESULT, output)
    }

    override fun listInputs() =
        listOf(DeferredLayerType.DEPTH)

    override fun listOutputs() =
        listOf(DeferredLayerType.SDR_RESULT)

    override fun clone() = DepthTest()

    companion object {

        val shader = Shader(
            "dof", ShaderLib.coordsList, ShaderLib.coordsVShader, ShaderLib.uvList,
            listOf(
                Variable(GLSLType.V1F,"worldScale"),
                Variable(GLSLType.V3F,"cameraPosition"),
                Variable(GLSLType.S2D, "depthTex"),
                Variable(GLSLType.V4F, "result", VariableMode.OUT)
            ) + depthVars, "" +
                    quatRot +
                    rawToDepth +
                    depthToPosition +
                    "void main() {\n" +
                    "   vec3 pos = cameraPosition + rawDepthToPosition(uv,texture(depthTex,uv).r) / worldScale;\n" +
                    "   result = vec4(fract(pos - 0.001),1.0);\n" +
                    "}\n"
        )

    }
}