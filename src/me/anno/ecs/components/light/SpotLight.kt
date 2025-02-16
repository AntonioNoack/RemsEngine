package me.anno.ecs.components.light

import me.anno.ecs.annotations.Docs
import me.anno.ecs.annotations.Range
import me.anno.ecs.components.light.PointLight.Companion.effectiveSpecular
import me.anno.ecs.components.light.PointLight.Companion.falloff
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.engine.serialization.SerializedProperty
import me.anno.engine.ui.LineShapes.drawArrowZ
import me.anno.engine.ui.LineShapes.drawCone
import me.anno.engine.ui.render.RenderState
import me.anno.gpu.drawing.Perspective.setPerspectiveSpotLight
import me.anno.gpu.pipeline.Pipeline
import org.joml.Matrix4f
import org.joml.Matrix4x3m
import org.joml.Quaternionf
import org.joml.Vector3d
import org.joml.Vector3f
import kotlin.math.atan

// todo can/do-we-want-to support inner cone angle?
//  - a section, in which the brightness is constant
/*
* https://github.com/KhronosGroup/glTF/blob/main/extensions/2.0/Khronos/KHR_lights_punctual/README.md#inner-and-outer-cone-angles:
Inner and Outer Cone Angles
There should be a smooth attenuation of brightness between the innerConeAngle and outerConeAngle angles. In reality, this "angular" attenuation is very complex as it depends on the physical size of the spotlight and the shape of the sheath around the bulb.
Conforming implementations will model this angular attenuation with a curve that follows a steeper decline in brightness before leveling off when moving from the inner to the outer angle.
It is common to model this falloff by interpolating between the cosine of each angle. This is an efficient approximation that provides decent results.

Reference code:
// These two values can be calculated on the CPU and passed into the shader
float lightAngleScale = 1.0f / max(0.001f, cos(innerConeAngle) - cos(outerConeAngle));
float lightAngleOffset = -cos(outerConeAngle) * lightAngleScale;

// Then, in the shader:
float cd = dot(spotlightDir, normalizedLightVector);
float angularAttenuation = saturate(cd * lightAngleScale + lightAngleOffset);
angularAttenuation *= angularAttenuation;
* */
class SpotLight : LightComponent(LightType.SPOT) {

    @Docs("Actually atan(angle), so coneAngle=1.0 means a 90° opening")
    @Range(0.0, 100.0)
    var coneAngle = 1f

    @SerializedProperty
    @Range(1e-6, 1.0)
    var near = 0.01f

    override fun getShaderV0(): Float = coneAngle
    override fun getShaderV1(): Float = shadowMapPower
    override fun getShaderV2(): Float = near

    override fun updateShadowMap(
        cascadeScale: Float,
        worldScale: Float,
        dstCameraMatrix: Matrix4f,
        dstCameraPosition: Vector3d,
        cameraRotation: Quaternionf,
        cameraDirection: Vector3f,
        drawTransform: Matrix4x3m,
        pipeline: Pipeline,
        resolution: Int
    ) {
        val far = 1f
        val coneAngle = coneAngle * cascadeScale
        val fovYRadians = 2f * atan(coneAngle)
        setPerspectiveSpotLight(dstCameraMatrix, coneAngle, near, far, 0f, 0f)
        dstCameraMatrix.rotate(Quaternionf(cameraRotation).invert())
        pipeline.frustum.definePerspective(
            near / worldScale, far / worldScale, fovYRadians, resolution, 1f,
            dstCameraPosition, cameraRotation
        )
        // required for SDF shapes
        RenderState.fovXRadians = fovYRadians
        RenderState.fovYRadians = fovYRadians
    }

    override fun drawShape(pipeline: Pipeline) {
        drawCone(entity, coneAngle.toDouble())
        drawArrowZ(entity, 0.0, -1.0)
    }

    override fun getLightPrimitive(): Mesh {
        return pyramidMesh
    }

    override fun copyInto(dst: PrefabSaveable) {
        super.copyInto(dst)
        if (dst !is SpotLight) return
        dst.coneAngle = coneAngle
        dst.near = near
    }

    companion object {

        private val pyramidMesh = Mesh().apply {
            positions = floatArrayOf(
                -1f, -1f, -1f,
                -1f, +1f, -1f,
                +1f, -1f, -1f,
                +1f, +1f, -1f,
                +0f, +0f, +0f
            )
            indices = intArrayOf(
                0, 1, 3,
                0, 3, 2,
                0, 4, 1,
                1, 4, 3,
                3, 4, 2,
                2, 4, 0
            )
        }

        fun getShaderCode(cutoffContinue: String?, withShadows: Boolean): String {
            return "" +
                    (if (cutoffContinue != null) "if(lightPos.z >= 0.0) $cutoffContinue;\n" else "") + // backside
                    "lightDir = normalize(-lightPos);\n" +
                    "NdotL = dot(lightDir, lightNor);\n" +
                    "float coneAngle = shaderV0;\n" +
                    "vec2 shadowDir = lightPos.xy/(-lightPos.z * coneAngle);\n" +
                    "float ringFalloff = dot(shadowDir,shadowDir);\n" +
                    (if (cutoffContinue != null) "if(ringFalloff >= 1.0) $cutoffContinue;\n" else "") + // outside of light
                    // when we are close to the edge, we blend in
                    "lightColor *= max(1.0-ringFalloff, 0.0);\n" +
                    (if (withShadows) "" +
                            "if(shadowMapIdx0 < shadowMapIdx1 && receiveShadows){\n" +
                            "   #define shadowMapPower shaderV1\n" +
                            "   vec2 nextDir = shadowDir * shadowMapPower;\n" +
                            "   float layerIdx = 0.0;\n" +
                            "   while(abs(nextDir.x)<1.0 && abs(nextDir.y)<1.0 && shadowMapIdx0+1<shadowMapIdx1){\n" +
                            "       shadowMapIdx0++;\n" +
                            "       layerIdx++;\n" +
                            "       shadowDir = nextDir;\n" +
                            "       nextDir *= shadowMapPower;\n" +
                            "   }\n" +
                            "   float near = shaderV2;\n" +
                            "   float depthFromShader = -near/lightPos.z;\n" +
                            // do the shadow map function and compare
                            "    lightColor *= texture_array_depth_shadowMapPlanar(shadowMapIdx0, vec3(shadowDir.xy,layerIdx), NdotL, depthFromShader);\n" +
                            "}\n"
                    else "") +
                    "effectiveDiffuse = lightColor * $falloff;\n" +
                    effectiveSpecular
        }
    }
}