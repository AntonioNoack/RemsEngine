package me.anno.ecs.components.light

import me.anno.ecs.annotations.Range
import me.anno.ecs.components.light.PointLight.Companion.effectiveSpecular
import me.anno.ecs.components.light.PointLight.Companion.falloff
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.engine.serialization.SerializedProperty
import me.anno.engine.ui.LineShapes.drawArrowZ
import me.anno.engine.ui.LineShapes.drawCone
import me.anno.engine.ui.render.RenderState
import me.anno.gpu.drawing.Perspective.setPerspective
import me.anno.gpu.pipeline.Pipeline
import me.anno.maths.Maths
import me.anno.maths.Maths.min
import me.anno.utils.types.Floats.toRadians
import org.joml.Matrix4f
import org.joml.Matrix4x3
import org.joml.Quaternionf
import org.joml.Vector3d
import org.joml.Vector3f
import kotlin.math.atan
import kotlin.math.tan

class SpotLight : LightComponent(LightType.SPOT) {

    @Range(0.0, 179.0)
    var innerConeAngleDegrees = 70f

    @Range(0.0, 179.0)
    var outerConeAngleDegrees = 90f

    @SerializedProperty
    @Range(1e-6, 1.0)
    var near = 0.01f

    val outerConeAtan get() = tan(0.5f * outerConeAngleDegrees.toRadians())

    override fun getShaderV0(): Float = outerConeAtan
    override fun getShaderV1(): Float = shadowMapPower
    override fun getShaderV2(): Float = near
    override fun getShaderV3(): Float {
        val innerConeAngleDegrees = min(innerConeAngleDegrees, 0.999f * outerConeAngleDegrees)
        val innerConeAtan = tan(0.5f * innerConeAngleDegrees.toRadians())
        return innerConeAtan / outerConeAtan
    }

    override fun updateShadowMap(
        cascadeScale: Float,
        dstCameraMatrix: Matrix4f,
        dstCameraPosition: Vector3d,
        cameraRotation: Quaternionf,
        cameraDirection: Vector3f,
        drawTransform: Matrix4x3,
        pipeline: Pipeline,
        resolution: Int
    ) {
        val far = 1f
        val worldScale = (Maths.SQRT3 / drawTransform.getScaleLength()).toFloat()
        val near1 = near / worldScale
        val far1 = far / worldScale
        val aspectRatio = 1f
        val coneAngle = outerConeAtan * cascadeScale
        val fovYRadians = 2f * atan(coneAngle)
        setPerspective(dstCameraMatrix, fovYRadians, aspectRatio, near1, far1, 0f, 0f)
        dstCameraMatrix.rotate(Quaternionf(cameraRotation).invert())
        pipeline.frustum.definePerspective(
            near1, far1, fovYRadians, resolution, aspectRatio,
            dstCameraPosition, cameraRotation
        )
        // required for SDF shapes
        RenderState.fovXRadians = fovYRadians
        RenderState.fovYRadians = fovYRadians
    }

    override fun drawShape(pipeline: Pipeline) {
        val outer = outerConeAtan.toDouble()
        drawCone(entity, outer)
        drawArrowZ(entity, -near.toDouble(), -1.0)
    }

    override fun getLightPrimitive(): Mesh {
        return pyramidNZMesh
    }

    override fun copyInto(dst: PrefabSaveable) {
        super.copyInto(dst)
        if (dst !is SpotLight) return
        dst.innerConeAngleDegrees = innerConeAngleDegrees
        dst.outerConeAngleDegrees = outerConeAngleDegrees
        dst.shadowMapPower = shadowMapPower
        dst.near = near
    }

    companion object {

        private val pyramidNZMesh = Mesh().apply {
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
                    "float outerConeAngle = shaderV0;\n" +
                    "vec2 shadowDir = lightPos.xy/(-lightPos.z * outerConeAngle);\n" +

                    "float ringFalloff = dot(shadowDir,shadowDir);\n" +
                    (if (cutoffContinue != null) "if(ringFalloff >= 1.0) $cutoffContinue;\n" else "") + // outside of light
                    // when we are close to the edge, we blend in
                    "float coneAngleRatio = shaderV3;\n" +
                    "lightColor *= 1.0-smoothstep(coneAngleRatio,1.0,ringFalloff);\n" +
                    (if (withShadows) "" +
                            "if(shadowMapIdx0 < shadowMapIdx1 && receiveShadows){\n" +
                            "   float shadowMapPower = shaderV1;\n" +
                            "   vec2 nextDir = shadowDir * shadowMapPower;\n" +
                            "   float layerIdx = 0.0;\n" +
                            "   while(abs(nextDir.x)<1.0 && abs(nextDir.y)<1.0 && shadowMapIdx0+1<shadowMapIdx1){\n" +
                            "       shadowMapIdx0++;\n" +
                            "       layerIdx++;\n" +
                            "       shadowDir = nextDir;\n" +
                            "       nextDir *= shadowMapPower;\n" +
                            "   }\n" +
                            "   float near = shaderV2;\n" +
                            "   float bias = 0.0004;\n" +
                            "   float depthFromShader = -near / lightPos.z + bias;\n" +
                            "   lightColor *= texture_array_depth_shadowMapPlanar(shadowMapIdx0, vec3(shadowDir.xy,layerIdx), NdotL, depthFromShader);\n" +
                            "}\n"
                    else "") +
                    "effectiveDiffuse = lightColor * $falloff;\n" +
                    effectiveSpecular
        }
    }
}