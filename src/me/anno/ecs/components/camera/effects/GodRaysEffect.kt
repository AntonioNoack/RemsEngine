package me.anno.ecs.components.camera.effects

import me.anno.ecs.Entity
import me.anno.ecs.annotations.Range
import me.anno.ecs.annotations.Type
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.engine.ui.render.Renderers.tonemapGLSL
import me.anno.gpu.GFXState.useFrame
import me.anno.gpu.buffer.SimpleBuffer.Companion.flat01
import me.anno.gpu.deferred.DeferredLayerType
import me.anno.gpu.deferred.DeferredSettingsV2
import me.anno.gpu.framebuffer.FBStack
import me.anno.gpu.framebuffer.IFramebuffer
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.Shader
import me.anno.gpu.shader.ShaderLib.coordsList
import me.anno.gpu.shader.ShaderLib.coordsVShader
import me.anno.gpu.shader.ShaderLib.uvList
import me.anno.gpu.shader.builder.Variable
import me.anno.gpu.shader.builder.VariableMode
import org.joml.Vector3f

// todo test this
class GodRaysEffect : ToneMappedEffect() {

    @Range(16.0, 1024.0)
    var numSamples = 128

    @Range(0.0, 1.0)
    var falloff = Vector3f(0.01f) // todo can this look nice in the case of non-chromatic?

    @Type("Color3HDR")
    var sunColor = Vector3f()

    // todo set light position via object or manually
    // x, y, depth
    var lightPosition = Vector3f()

    var lightPosObject: Entity? = null

    override fun render(
        buffer: IFramebuffer,
        format: DeferredSettingsV2,
        layers: MutableMap<DeferredLayerType, IFramebuffer>
    ) {
        val color = layers[DeferredLayerType.HDR_RESULT]!!.getTexture0()
        val depth = layers[DeferredLayerType.DEPTH]!!.getTexture0()
        val output = FBStack["god-rays", color.w, color.h, 4, false, 1, false]
        useFrame(output) {
            val shader = shader
            shader.use()
            color.bindTrulyNearest(0)
            depth.bindTrulyNearest(1)
            if (lightPosObject != null) {
                // todo calculate light position
                lightPosition
            }
            shader.v3f("intensity", sunColor)
            shader.v3f("falloff", falloff)
            shader.v1f("samples", numSamples.toFloat())
            shader.v3f("lightPos", lightPosition)
            shader.v1f("maxDensity", 1f) // >= 1f
            shader.v1b("applyToneMapping", applyToneMapping)
            flat01.draw(shader)
        }
        write(layers, dstType, output)
    }

    override fun clone(): BloomEffect {
        val clone = BloomEffect()
        copy(clone)
        return clone
    }

    override fun copy(clone: PrefabSaveable) {
        super.copy(clone)
        clone as GodRaysEffect
        clone.numSamples = numSamples
        clone.falloff.set(falloff)
        clone.sunColor.set(sunColor)
        clone.lightPosition
    }

    override val className get() = "GodRaysEffect"

    companion object {
        val shader = Shader(
            "god-rays", coordsList, coordsVShader, uvList,
            listOf(
                Variable(GLSLType.V3F, "lightPos"),
                Variable(GLSLType.V3F, "falloff"),
                Variable(GLSLType.V3F, "intensity"),
                Variable(GLSLType.V1F, "samples"),
                Variable(GLSLType.V1F, "maxDensity"),
                Variable(GLSLType.S2D, "colorTex"),
                Variable(GLSLType.S2D, "depthTex"),
                Variable(GLSLType.V4F, "result", VariableMode.OUT)
            ), "" +
                    tonemapGLSL +
                    "void main(){\n" +
                    "   vec2 deltaUV = lightPos - uv;\n" +
                    "   vec2 texSize = vec2(textureSize(colorTex,0));\n" +
                    "   float pixelDistance = length(dot(abs(deltaUV), texSize));\n" +
                    "   float dist01 = pixelDistance / texSize.y;\n" + // [0,1]
                    "   int stepsF = max(1, min(pixelDistance * maxDensity, samples));\n" +
                    "   int steps = float(steps);\n" +
                    "   vec2 dir = deltaUV / stepsF;\n" +
                    "   vec2 uv2 = uv;\n" +
                    "   float sum = 0.0;\n" +
                    "   float factor = 1.0;\n" +
                    // walk from light pos to our position
                    // todo use depth buffer to block rays
                    "   for(int i=0;i<steps;i++){\n" +
                    "       sum += factor;\n" + // todo only sun color is of importance
                    "       factor *= falloff;\n" + // todo falloff depends on position
                    "       uv2 += dir;\n" +
                    "   }\n" +
                    "   if(applyToneMapping) color = tonemap(color);\n" +
                    "   result = vec4(texture(colorTex, uv) + sum * intensity, 1.0);\n" +
                    "}\n"
        ).setTextureIndices("colorTex", "depthTex") as Shader
    }

}