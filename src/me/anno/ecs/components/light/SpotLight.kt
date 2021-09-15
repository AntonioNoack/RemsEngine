package me.anno.ecs.components.light

import me.anno.ecs.annotations.HideInInspector
import me.anno.ecs.annotations.Range
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.engine.gui.LineShapes.drawArrowZ
import me.anno.engine.gui.LineShapes.drawCone
import me.anno.gpu.drawing.Perspective.setPerspective2
import me.anno.gpu.pipeline.Pipeline
import me.anno.io.serialization.NotSerializedProperty
import me.anno.io.serialization.SerializedProperty
import me.anno.mesh.vox.meshing.BlockBuffer
import me.anno.mesh.vox.meshing.BlockSide
import me.anno.mesh.vox.meshing.VoxelMeshBuildInfo
import me.anno.utils.structures.arrays.ExpandingFloatArray
import org.joml.*
import kotlin.math.atan
import kotlin.math.tan

// a cone light
class SpotLight() : LightComponent(LightType.SPOT) {

    constructor(src: SpotLight) : this() {
        src.copy(this)
    }

    // for a large angle, it just becomes a point light
    @Range(0.0, 100.0)
    var coneAngle = 1.0

    @HideInInspector
    @NotSerializedProperty
    val fovRadians
        get() = 2.0 * atan(coneAngle)

    @HideInInspector
    @NotSerializedProperty
    val halfFovRadians
        get() = tan(coneAngle)

    @SerializedProperty
    @Range(1e-6, 1.0)
    var near = 0.001

    override fun getShaderV0(drawTransform: Matrix4x3d, worldScale: Double): Float {
        return coneAngle.toFloat()
    }

    override fun getShaderV1(): Float = shadowMapPower.toFloat()
    override fun getShaderV2(): Float = near.toFloat()

    override fun updateShadowMap(
        cascadeScale: Double,
        worldScale: Double,
        cameraMatrix: Matrix4f,
        drawTransform: Matrix4x3d,
        pipeline: Pipeline,
        resolution: Int,
        position: Vector3d,
        rotation: Quaterniond
    ) {
        val far = 1.0
        val coneAngle = coneAngle * cascadeScale
        val fovYRadians = 2.0 * atan(coneAngle)
        setPerspective2(cameraMatrix, coneAngle.toFloat(), near.toFloat(), far.toFloat())
        cameraMatrix.rotate(Quaternionf(rotation).invert())
        pipeline.frustum.definePerspective(
            near / worldScale, far / worldScale, fovYRadians, resolution, resolution,
            1.0, position, rotation,
        )
    }

    override fun drawShape() {
        drawCone(entity, coneAngle)
        drawArrowZ(entity, 0.0, -1.0)
    }

    // for deferred rendering, this could be optimized
    override fun getLightPrimitive(): Mesh = halfCubeMesh

    override fun clone() = SpotLight(this)

    override fun copy(clone: PrefabSaveable) {
        super.copy(clone)
        clone as SpotLight
        clone.coneAngle = coneAngle
        clone.near = near
    }

    override val className: String = "SpotLight"

    companion object {

        val coneFunction = "clamp((localNormal.z-(1.0-coneAngle))/coneAngle,0.0,1.0)"
        // val coneFunction = "smoothstep(0.0, 1.0, (localNormal.z-(1.0-coneAngle))/coneAngle)"

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
                    (if (cutoffContinue != null) "if(dir.z >= 0.0) $cutoffContinue;\n" else "") + // backside
                    "lightPosition = data1.rgb;\n" +
                    "lightDirWS = normalize(lightPosition - finalPosition);\n" +
                    "NdotL = dot(lightDirWS, finalNormal);\n" +
                    "float coneAngle = data1.a;\n" +
                    "vec2 shadowDir = dir.xy/(-dir.z * coneAngle);\n" +
                    "float ringFalloff = dot(shadowDir,shadowDir);\n" +
                    (if (cutoffContinue != null) "if(ringFalloff > 1.0) $cutoffContinue;\n" else "") + // outside of light
                    // when we are close to the edge, we blend in
                    "lightColor *= 1.0-ringFalloff;\n" +
                    (if (withShadows) "" +
                            "if(shadowMapIdx0 < shadowMapIdx1){\n" +
                            "   #define shadowMapPower data2.b\n" +
                            "   vec2 nextDir = shadowDir * shadowMapPower;\n" +
                            "   while(abs(nextDir.x)<1.0 && abs(nextDir.y)<1.0 && shadowMapIdx0+1<shadowMapIdx1){\n" +
                            "       shadowMapIdx0++;\n" +
                            "       shadowDir = nextDir;\n" +
                            "       nextDir *= shadowMapPower;\n" +
                            "   }\n" +
                            "   float near = data2.a;\n" +
                            "   float depthFromShader = -near/dir.z;\n" +
                            // do the shadow map function and compare
                            "    float depthFromTex = texture_array_depth_shadowMapPlanar(shadowMapIdx0, shadowDir.xy, depthFromShader);\n" +
                            "    lightColor *= 1.0 - depthFromTex;\n" +
                            "}\n"
                    else "") +
                    "effectiveDiffuse = lightColor * ${LightType.SPOT.falloff};\n" +
                    // "dir *= 0.2;\n" + // less falloff by a factor of 5,
                    // because specular light is more directed and therefore reached farther
                    // nice in theory, but practically, we would to render need a larger cube
                    "effectiveSpecular = effectiveDiffuse;//lightColor * ${LightType.SPOT.falloff};\n"
        }

    }

}