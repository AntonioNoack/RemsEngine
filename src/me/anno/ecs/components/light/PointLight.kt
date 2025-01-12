package me.anno.ecs.components.light

import me.anno.Time
import me.anno.ecs.Entity
import me.anno.ecs.annotations.Range
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.engine.serialization.SerializedProperty
import me.anno.engine.ui.LineShapes.drawBox
import me.anno.engine.ui.LineShapes.drawSphere
import me.anno.engine.ui.render.RenderState
import me.anno.gpu.GFXState
import me.anno.gpu.GFXState.timeRendering
import me.anno.gpu.drawing.Perspective.setPerspective
import me.anno.gpu.framebuffer.CubemapFramebuffer
import me.anno.gpu.pipeline.Pipeline
import me.anno.gpu.texture.CubemapTexture.Companion.cubemapsAreLeftHanded
import me.anno.gpu.texture.CubemapTexture.Companion.rotateForCubemap
import me.anno.maths.Maths.PIf
import me.anno.maths.Maths.SQRT3
import me.anno.mesh.Shapes
import me.anno.utils.pooling.JomlPools
import org.joml.Matrix4f
import org.joml.Matrix4x3d
import org.joml.Quaterniond
import org.joml.Vector3d
import kotlin.math.PI

// todo add visual light cone to all lights; somehow must be post-processing, or like glass, I'd guess... (cannot show background brighter)

class PointLight : LightComponent(LightType.POINT) {

    @Range(0.0, 5.0)
    var lightSize = 0.0

    @SerializedProperty
    @Range(1e-6, 1.0)
    var near = 0.01

    override fun getShaderV0() = lightSize.toFloat()
    override fun getShaderV2() = near.toFloat()

    override fun invalidateShadows() {
        needsUpdate1 = true
    }

    override fun updateShadowMap(
        cascadeScale: Double,
        worldScale: Double,
        dstCameraMatrix: Matrix4f,
        dstCameraPosition: Vector3d,
        cameraRotation: Quaterniond,
        cameraDirection: Vector3d,
        drawTransform: Matrix4x3d,
        pipeline: Pipeline,
        resolution: Int
    ) {
    }

    override fun updateShadowMaps() {

        lastDrawn = Time.gameTimeN

        val pipeline = pipeline

        val entity = entity!!
        val transform = entity.transform
        val resolution = shadowMapResolution
        val global = transform.globalTransform
        val position = global.getTranslation(RenderState.cameraPosition)
        val rotation = global.getUnnormalizedRotation(RenderState.cameraRotation)
        val worldScale = SQRT3 / global.getScaleLength()
        RenderState.worldScale = worldScale
        // only fill pipeline once?

        val result = shadowTextures!! as CubemapFramebuffer

        val far = 1.0

        val deg90 = PI * 0.5
        val rotInvert = rotation.invert(JomlPools.quat4d.create())
        val rot3 = JomlPools.quat4d.create()

        // important for SDF shapes
        RenderState.fovXRadians = PIf * 0.5f
        RenderState.fovYRadians = PIf * 0.5f

        val cameraMatrix = RenderState.cameraMatrix
        val root = entity.getRoot(Entity::class)
        timeRendering(className, timer) {
            GFXState.depthMode.use(pipeline.defaultStage.depthMode) {
                GFXState.ditherMode.use(ditherMode) {
                    result.draw(resolution, renderer) { side ->
                        result.clearColor(0, depth = true)
                        setPerspective(cameraMatrix, deg90.toFloat(), 1f, near.toFloat(), far.toFloat(), 0f, 0f)
                        rotateForCubemap(rot3.identity(), side)
                        rot3.mul(rotInvert)
                        cameraMatrix.rotate(rot3)

                        // define camera position and rotation
                        val cameraRotation = rot3.invert(RenderState.cameraRotation)
                        RenderState.calculateDirections(true)

                        pipeline.clear()
                        pipeline.frustum.definePerspective(
                            near / worldScale, far / worldScale, deg90, resolution, 1.0, position,
                            cameraRotation
                        )
                        pipeline.fill(root)
                        pipeline.singlePassWithoutSky()
                    }
                }
            }
        }
        JomlPools.quat4d.sub(2)
    }

    override fun copyInto(dst: PrefabSaveable) {
        super.copyInto(dst)
        if (dst !is PointLight) return
        dst.lightSize = lightSize
        dst.near = near
    }

    override fun drawShape(pipeline: Pipeline) {
        drawBox(entity, JomlPools.vec3d.borrow().set(near))
        drawSphere(entity, 1.0)
    }

    override fun getLightPrimitive(): Mesh = Shapes.cube11Smooth

    companion object {

        val falloff = "pow(max(0.0,1.0/length(lightPos)-1.0),2.0)"

        val effectiveSpecular = "" +
                "if(hasSpecular){\n" +
                "   effectiveSpecular = effectiveDiffuse;\n" +
                // 0 = perpendicular, 1 = perfectly aligned for reflection
                "   float dot1 = max(-dot(normalize(lightPos), reflect(viewDir, lightNor)), 0.0);\n" +
                "   float smoothness = finalReflectivity;\n" +
                "   effectiveSpecular *= mix(1.0, 2e4, smoothness) * pow(dot1, 1.0 + 64.0 * smoothness);\n" +
                "}\n"

        fun getShaderCode(cutoffContinue: String?, withShadows: Boolean): String {
            return "" +
                    // light radius
                    "float lightRadius = shaderV0;\n" +
                    (if (cutoffContinue != null) "if(dot(lightPos,lightPos)>1.0) { $cutoffContinue; }\n" else "") + // outside
                    "if(lightRadius > 0.0){\n" +
                    "   lightPos *= max(length(lightPos)-lightRadius, 0.001) * (1.0+lightRadius) / length(lightPos);\n" +
                    "}\n" +
                    "lightDir = normalize(-lightPos);\n" +
                    "NdotL = dot(lightDir, lightNor);\n" + // = cosTheta
                    // shadow maps; shadows can be in every direction -> use cubemaps
                    (if (withShadows) "" +
                            "if(shadowMapIdx0 < shadowMapIdx1 && receiveShadows){\n" +
                            "   float near = shaderV2;\n" +
                            "   float maxAbsComponent = max(max(abs(lightPos.x),abs(lightPos.y)),abs(lightPos.z));\n" +
                            "   float depthFromShader = near/maxAbsComponent;\n" +
                            "   lightColor *= texture_array_depth_shadowMapCubic(shadowMapIdx0, -$cubemapsAreLeftHanded * lightPos, NdotL, depthFromShader);\n" +
                            "}\n"
                    else "") +
                    "effectiveDiffuse = lightColor * $falloff;\n" +
                    effectiveSpecular
        }
    }
}