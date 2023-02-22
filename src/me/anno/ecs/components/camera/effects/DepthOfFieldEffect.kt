package me.anno.ecs.components.camera.effects

import me.anno.ecs.components.mesh.sdf.SDFComponent.Companion.quatRot
import me.anno.gpu.GFXState
import me.anno.gpu.buffer.SimpleBuffer
import me.anno.gpu.deferred.DeferredLayerType
import me.anno.gpu.deferred.DeferredSettingsV2
import me.anno.gpu.framebuffer.FBStack
import me.anno.gpu.framebuffer.IFramebuffer
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.ReverseDepth.bindDepthToPosition
import me.anno.gpu.shader.ReverseDepth.depthToPosition
import me.anno.gpu.shader.ReverseDepth.depthToPositionList
import me.anno.gpu.shader.Shader
import me.anno.gpu.shader.ShaderLib
import me.anno.gpu.shader.builder.Variable
import me.anno.gpu.shader.builder.VariableMode
import me.anno.gpu.texture.ITexture2D

class DepthOfFieldEffect : CameraEffect() {

    override fun render(
        buffer: IFramebuffer,
        format: DeferredSettingsV2,
        layers: MutableMap<DeferredLayerType, IFramebuffer>
    ) {
        val color = layers[DeferredLayerType.SDR_RESULT]!!.getTexture0()
        val depth = layers[DeferredLayerType.DEPTH]!!.getTexture0()
        val position = layers[DeferredLayerType.POSITION]!!.getTexture0()
        val output = render(color, depth, position)
        write(layers, DeferredLayerType.SDR_RESULT, output)
    }

    override fun listInputs() =
        listOf(DeferredLayerType.HDR_RESULT, DeferredLayerType.DEPTH, DeferredLayerType.POSITION)

    override fun listOutputs() =
        listOf(DeferredLayerType.SDR_RESULT)

    fun render(color: ITexture2D, depth: ITexture2D, position: ITexture2D): IFramebuffer {
        val buffer = FBStack["dof", color.w, color.h, 4, true, 1, false]
        GFXState.useFrame(buffer) {
            val shader = shader
            shader.use()
            bindDepthToPosition(shader)
            color.bindTrulyNearest(shader, "colorTex")
            depth.bindTrulyNearest(shader, "depthTex")
            position.bindTrulyNearest(shader, "positionTex")
            SimpleBuffer.flat01.draw(shader)
        }
        return buffer
    }

    override fun clone() = DepthOfFieldEffect()

    companion object {

        val shader = Shader(
            "dof", ShaderLib.coordsList, ShaderLib.coordsVShader, ShaderLib.uvList,
            listOf(
                Variable(GLSLType.S2D, "colorTex"),
                Variable(GLSLType.S2D, "depthTex"),
                Variable(GLSLType.S2D, "positionTex"),
                Variable(GLSLType.V4F, "result", VariableMode.OUT)
            ) + depthToPositionList, "" +
                    quatRot +
                    depthToPosition +
                    "void main() {\n" +
                    "   float depth = texture(depthTex,uv).r;\n" +
                    "   vec3 pos = depthToPosition(depth);\n" +
                    "   vec3 pos0 = texture(positionTex,uv).xyz;\n" +
                    "   result = uv.x < 0.333 ? vec4(vec3(fract(log2(depth))),1.0) :\n" +
                    "            uv.x < 0.667 ? vec4(pos,1.0) : vec4(pos0,1.0);\n" +
                    "   result = vec4(pos/pos0,1.0);\n" +
                    "}\n"
        )
    }
}