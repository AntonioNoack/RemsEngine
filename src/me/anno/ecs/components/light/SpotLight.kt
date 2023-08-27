package me.anno.ecs.components.light

import me.anno.ecs.annotations.Range
import me.anno.ecs.components.light.PointLight.Companion.falloff
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.engine.ui.LineShapes.drawArrowZ
import me.anno.engine.ui.LineShapes.drawCone
import me.anno.gpu.drawing.Perspective.setPerspective2
import me.anno.gpu.pipeline.Pipeline
import me.anno.io.serialization.SerializedProperty
import me.anno.mesh.vox.meshing.BlockBuffer
import me.anno.mesh.vox.meshing.BlockSide
import me.anno.mesh.vox.meshing.VoxelMeshBuildInfo
import me.anno.utils.structures.arrays.ExpandingFloatArray
import org.joml.*
import kotlin.math.atan

class SpotLight() : LightComponent(LightType.SPOT) {

    constructor(src: SpotLight) : this() {
        src.copyInto(this)
    }

    @Range(0.0, 100.0)
    var coneAngle = 1f

    @SerializedProperty
    @Range(1e-6, 1.0)
    var near = 0.01

    override fun getShaderV0(): Float = coneAngle
    override fun getShaderV1(): Float = shadowMapPower.toFloat()
    override fun getShaderV2(): Float = near.toFloat()

    override fun updateShadowMap(
        cascadeScale: Double,
        worldScale: Double,
        dstCameraMatrix: Matrix4f,
        dstCameraPosition: Vector3d,
        dstCameraDirection: Vector3d,
        drawTransform: Matrix4x3d,
        pipeline: Pipeline,
        resolution: Int,
        position: Vector3d,
        rotation: Quaterniond
    ) {
        val far = 1.0
        val coneAngle = coneAngle * cascadeScale
        val fovYRadians = 2.0 * atan(coneAngle)
        setPerspective2(dstCameraMatrix, coneAngle.toFloat(), near.toFloat(), far.toFloat(), 0f, 0f)
        dstCameraMatrix.rotate(Quaternionf(rotation).invert())
        pipeline.frustum.definePerspective(
            near / worldScale, far / worldScale, fovYRadians, resolution, resolution,
            1.0, position, rotation
        )
    }

    override fun drawShape() {
        drawCone(entity, coneAngle.toDouble())
        drawArrowZ(entity, 0.0, -1.0)
    }

    // for deferred rendering, this could be optimized
    override fun getLightPrimitive(): Mesh = halfCubeMesh

    override fun clone() = SpotLight(this)

    override fun copyInto(dst: PrefabSaveable) {
        super.copyInto(dst)
        dst as SpotLight
        dst.coneAngle = coneAngle
        dst.near = near
    }

    override val className: String get() = "SpotLight"

    companion object {

        val halfCubeMesh = Mesh()

        init {

            val vertices = ExpandingFloatArray(6 * 2 * 3 * 3)
            val base = VoxelMeshBuildInfo(intArrayOf(0, -1), vertices, null, null)

            base.color = -1
            base.setOffset(-0.5f, -0.5f, -1f)

            for (side in BlockSide.values) {
                BlockBuffer.addQuad(base, side, 1, 1, 1)
            }

            val positions = vertices.toFloatArray()
            for (i in positions.indices step 3) {
                positions[i + 0] *= 2f
                positions[i + 1] *= 2f
                // z is half as much
            }
            halfCubeMesh.positions = positions
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
                            "   while(abs(nextDir.x)<1.0 && abs(nextDir.y)<1.0 && shadowMapIdx0+1<shadowMapIdx1){\n" +
                            "       shadowMapIdx0++;\n" +
                            "       shadowDir = nextDir;\n" +
                            "       nextDir *= shadowMapPower;\n" +
                            "   }\n" +
                            "   float near = shaderV2;\n" +
                            "   float depthFromShader = -near/lightPos.z;\n" +
                            // do the shadow map function and compare
                            "    lightColor *= texture_array_depth_shadowMapPlanar(shadowMapIdx0, shadowDir.xy, depthFromShader);\n" +
                            "}\n"
                    else "") +
                    "effectiveDiffuse = lightColor * $falloff;\n" +
                    // "dir *= 0.2;\n" + // less falloff by a factor of 5,
                    // because specular light is more directed and therefore reached farther
                    // nice in theory, but practically, we would to render need a larger cube
                    "effectiveSpecular = effectiveDiffuse;//lightColor * falloff;\n"
        }
    }
}