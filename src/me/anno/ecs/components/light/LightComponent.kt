package me.anno.ecs.components.light

import me.anno.ecs.Component
import me.anno.ecs.annotations.Range
import me.anno.ecs.components.mesh.Mesh
import me.anno.engine.pbr.DeferredRenderer
import me.anno.gpu.ShaderLib
import me.anno.gpu.deferred.DeferredLayerType
import me.anno.gpu.shader.BaseShader
import me.anno.io.serialization.SerializedProperty
import org.joml.Vector3f

abstract class LightComponent : Component() {

    enum class ShadowMapType {
        CUBEMAP,
        PLANE
    }

    var shadowMapCascades = 0
    var shadowMapPower = 4f

    // black lamp light?
    @SerializedProperty
    var color: Vector3f = Vector3f(1f)

    @Range(1e-300, 1e300)
    @SerializedProperty
    var radius = 1f

    abstract fun getLightPrimitive(): Mesh

    // todo glsl?
    // todo AES lights, and their textures?
    // todo shadow map type
    abstract fun getFalloffFunction(): String

    // todo update/calculate shadow maps

    companion object {

        // todo plane of light... how?
        // todo lines of light... how?

        val lightShaders = HashMap<String, BaseShader>()
        fun getShader(sample: LightComponent): BaseShader {
            val falloff = sample.getFalloffFunction()
            return lightShaders.getOrPut(falloff) {
                val deferred = DeferredRenderer.deferredSettings!!
                ShaderLib.createShaderPlus(
                    "PointLight",
                    ShaderLib.v3DBase +
                            "a3 coords;\n" +
                            "uniform mat4x3 localTransform;\n" +
                            "void main(){\n" +
                            "   localPosition = coords;\n" +
                            "   localPosition = localTransform * vec4(localPosition, 1.0);\n" +
                            "   gl_Position = transform * vec4(localPosition, 1.0);\n" +
                            "   center = localTransform * vec4(0,0,0,1);\n" +
                            "   uvw = gl_Position.xyw;\n" +
                            ShaderLib.positionPostProcessing +
                            "}", ShaderLib.y3D + "" +
                            "varying vec3 center;\n", "" +
                            "float getIntensity(vec3 dir){\n" +
                            "" + sample.getFalloffFunction() +
                            "}\n" +
                            "uniform vec3 uColor;\n" +
                            "uniform mat3 invLocalTransform;\n" +
                            "uniform sampler2D ${deferred.settingsV1.layerNames.joinToString()};\n" +
                            // roughness / metallic + albedo defines reflected light points -> needed as input as well
                            // todo roughness / metallic + albedo defines reflected light points -> compute them
                            // todo environment map is required for brilliant results as well
                            "void main(){\n" +
                            "   vec2 uv = uvw.xy/uvw.z*.5+.5;\n" +
                            "   vec3 globalDelta = ${DeferredLayerType.POSITION.getValue(deferred)} - center;\n" +
                            "   vec3 localDelta = invLocalTransform * globalDelta;\n" + // transform into local coordinates
                            "   vec3 nor = ${DeferredLayerType.NORMAL.getValue(deferred)};\n" +
                            "   float intensity = getIntensity(localDelta * 5.0) * max(0.0, -dot(globalDelta, nor));\n" +
                            "   vec3 finalColor = uColor * intensity;\n" +
                            "   float finalAlpha = 0.125;\n" +
                            "}", deferred.settingsV1.layerNames
                )
            }
        }

    }

}