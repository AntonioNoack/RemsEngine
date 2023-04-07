package me.anno.ecs.components.mesh.decal

import me.anno.ecs.components.mesh.decal.DecalMaterial.Companion.sett
import me.anno.engine.ui.render.ECSMeshShader
import me.anno.gpu.deferred.DeferredLayerType
import me.anno.gpu.deferred.DeferredSettingsV2
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.ReverseDepth.depthToPosition
import me.anno.gpu.shader.ReverseDepth.depthToPositionList
import me.anno.gpu.shader.ReverseDepth.rawToDepth
import me.anno.gpu.shader.Shader
import me.anno.gpu.shader.ShaderLib
import me.anno.gpu.shader.ShaderLib.quatRot
import me.anno.gpu.shader.builder.ShaderStage
import me.anno.gpu.shader.builder.Variable
import me.anno.gpu.shader.builder.VariableMode
import me.anno.utils.structures.lists.Lists.any2
import java.util.*
import kotlin.collections.ArrayList

class DecalShader(val layers: ArrayList<DeferredLayerType>) : ECSMeshShader("decal") {
    override fun createFragmentStages(
        isInstanced: Boolean,
        isAnimated: Boolean,
        motionVectors: Boolean
    ): List<ShaderStage> {
        val sett = sett ?: return super.createFragmentStages(isInstanced, isAnimated, motionVectors)
        val loadPart2 = StringBuilder()
        for (layer in sett.layers) {
            layer.appendMapping(loadPart2, "_in2", "_in1", "_in0", "", null, null)
        }
        val original = super.createFragmentStages(isInstanced, isAnimated, motionVectors)
        // can a decal modify the depth? it shouldn't ...
        return listOf(
            // inputs
            ShaderStage(
                "inputs", depthToPositionList + sett.layers2.map { layer ->
                    Variable(GLSLType.S2D, layer.name + "_in0")
                } + listOf(
                    Variable(GLSLType.S2D, "depth_in0"),
                    Variable(GLSLType.M4x3, "invLocalTransform"),
                    Variable(GLSLType.V2F, "windowSize"),
                    Variable(GLSLType.V2F, "uv", VariableMode.OUT),
                    Variable(GLSLType.V3F, "finalPosition", VariableMode.OUT),
                    Variable(GLSLType.V3F, "localPosition", VariableMode.OUT),
                    Variable(GLSLType.V1F, "alphaMultiplier", VariableMode.OUT),
                ), "" +
                        "ivec2 uvz = ivec2(gl_FragCoord.xy);\n" +
                        // load all data
                        sett.layers2.joinToString("") {
                            "vec4 ${it.name}_in1 = texelFetch(${it.name}_in0, uvz, 0);\n"
                        } + loadPart2.toString() +
                        "finalPosition = rawDepthToPosition(gl_FragCoord.xy/windowSize, texelFetch(depth_in0, uvz, 0).x);\n" +
                        "localPosition = invLocalTransform * vec4(finalPosition, 1.0);\n" +
                        "uv = localPosition.xy * vec2(0.5,-0.5) + 0.5;\n" +
                        // automatic blending on edges? alpha should be zero there anyway
                        "alphaMultiplier = abs(uv.x-0.5) < 0.5 && abs(uv.y-0.5) < 0.5 ? max(1.0-abs(localPosition.z), 0.0) : 0.0;\n" +
                        "alphaMultiplier *= -dot(normal, finalNormal_in2);\n" +
                        "if(alphaMultiplier < 0.5/255.0) discard;\n"
            ).add(quatRot).add(rawToDepth).add(depthToPosition).add(ShaderLib.octNormalPacking),
        ) + original + listOf(
            ShaderStage(
                "effect-modulator", listOf(
                    Variable(GLSLType.V1F, "alphaMultiplier", VariableMode.INOUT),
                    Variable(GLSLType.V1F, "finalAlpha", VariableMode.INOUT)
                ), "" +
                        "finalAlpha *= alphaMultiplier;\n" +
                        "if(finalAlpha < 0.5/255.0) discard;\n" +
                        // blend all values with the loaded properties
                        layers.joinToString("") {
                            "${it.glslName} = mix(${it.glslName}_in2, ${it.glslName}, finalAlpha);\n"
                        } +
                        // for all other values, override them completely with the loaded values
                        // todo normal map needs to be applied properly on existing normal, not just copied
                        sett.layerTypes
                            .filter {
                                it !in layers && original.any2 { stage ->
                                    stage.variables.any2 { variable ->
                                        variable.name == it.glslName
                                    }
                                }
                            }
                            .joinToString("") {
                                "${it.glslName} = ${it.glslName}_in2;\n"
                            } +
                        "finalAlpha = 1.0;\n"
            )
        )
    }

    // forward shader isn't really supported

    fun getDisabledLayers(): BitSet? {
        val settings = sett ?: return null
        val disabled = BitSet(settings.layers2.size)
        for (i in settings.layers2.indices) disabled.set(i, true)
        for (layer in layers) {
            val layer1 = settings.findLayer(layer) ?: continue
            disabled.set(layer1.texIndex, false)
        }
        return disabled
    }

    override fun createDeferredShader(
        deferred: DeferredSettingsV2,
        isInstanced: Boolean,
        isAnimated: Boolean,
        motionVectors: Boolean,
        limitedTransform: Boolean,
        postProcessing: ShaderStage?
    ): Shader {
        val base = createBase(
            isInstanced, isAnimated,
            deferred.layerTypes.size > 1 || !motionVectors,
            motionVectors, limitedTransform, postProcessing
        )
        base.outputs = deferred
        base.disabledLayers = getDisabledLayers()
        // build & finish
        val shader = base.create()
        finish(shader)
        return shader
    }
}