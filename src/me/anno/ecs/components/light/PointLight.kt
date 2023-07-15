package me.anno.ecs.components.light

import me.anno.Engine
import me.anno.ecs.Entity
import me.anno.ecs.annotations.Range
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.engine.ui.LineShapes.drawBox
import me.anno.engine.ui.LineShapes.drawSphere
import me.anno.engine.ui.render.RenderState
import me.anno.gpu.DepthMode
import me.anno.gpu.GFXState
import me.anno.gpu.drawing.Perspective.setPerspective
import me.anno.gpu.framebuffer.CubemapFramebuffer
import me.anno.gpu.pipeline.Pipeline
import me.anno.gpu.shader.Renderer
import me.anno.gpu.texture.CubemapTexture.Companion.rotateForCubemap
import me.anno.io.serialization.SerializedProperty
import me.anno.maths.Maths.SQRT3
import me.anno.mesh.Shapes
import me.anno.utils.pooling.JomlPools
import me.anno.utils.types.Matrices.getScaleLength
import me.anno.utils.types.Matrices.rotate2
import org.joml.*
import kotlin.math.PI

// todo size of point light: probably either distance or direction needs to be adjusted
// todo - in proximity, the appearance must not stay as a point, but rather be a sphere

class PointLight : LightComponent(LightType.POINT) {

    @Range(0.0, 5.0)
    var lightSize = 0.0

    @SerializedProperty
    @Range(1e-6, 1.0)
    var near = 0.001

    override fun getShaderV0(drawTransform: Matrix4x3d, worldScale: Double): Float {
        // put light size * world scale
        // avg, and then /3
        // but the center really is much smaller -> *0.01
        val scaleX = drawTransform.getScale(JomlPools.vec3d.borrow())
        val lightSize = (scaleX.x + scaleX.y + scaleX.z) * lightSize / 9.0
        return (lightSize * worldScale).toFloat()
    }

    // v1 is not used
    override fun getShaderV2() = near.toFloat()

    override fun invalidateShadows() {
        needsUpdate = true
    }

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

    }

    override fun updateShadowMaps() {

        lastDraw = Engine.nanoTime

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

        val texture = shadowTextures!![0] as CubemapFramebuffer

        val far = 1.0

        val deg90 = PI * 0.5
        val rotInvert = rotation.invert(JomlPools.quat4d.create())
        val rot3 = JomlPools.quat4d.create()

        val cameraMatrix = RenderState.cameraMatrix
        val root = entity.getRoot(Entity::class)
        GFXState.depthMode.use(DepthMode.CLOSER) {
            texture.draw(resolution, Renderer.nothingRenderer) { side ->
                texture.clearDepth()
                setPerspective(cameraMatrix, deg90.toFloat(), 1f, near.toFloat(), far.toFloat(), 0f, 0f)
                rotateForCubemap(rot3.identity(), side)
                rot3.mul(rotInvert)
                cameraMatrix.rotate2(rot3)

                // define camera position and rotation
                val cameraRotation = rot3.invert(RenderState.cameraRotation)
                RenderState.calculateDirections(true)

                pipeline.clear()
                pipeline.frustum.definePerspective(
                    near / worldScale, far / worldScale, deg90, resolution, resolution, 1.0,
                    position, cameraRotation
                )
                pipeline.fillDepth(root, position, worldScale)
                pipeline.defaultStage.drawColors(pipeline)
            }
        }

        JomlPools.quat4d.sub(2)

    }

    override fun copyInto(dst: PrefabSaveable) {
        super.copyInto(dst)
        dst as PointLight
        dst.lightSize = lightSize
        dst.near = near
    }

    override fun drawShape() {
        drawBox(entity, JomlPools.vec3d.borrow().set(near))
        drawSphere(entity, 1.0)
    }

    override fun getLightPrimitive(): Mesh = Shapes.cube11Smooth

    override val className: String get() = "PointLight"

    companion object {

        val falloff = kotlin.run {
            val cutoff = 0.1
            "max(0.0, 1.0/(1.0+9.0*dot(dir,dir)) - $cutoff)*${1.0 / (1.0 - cutoff)}"
        }

        fun getShaderCode(cutoffContinue: String?, withShadows: Boolean, hasLightRadius: Boolean): String {
            return "" +
                    (if (cutoffContinue != null) "if(dot(dir,dir)>1.0) $cutoffContinue;\n" else "") + // outside
                    // when light radius > 0, then adjust the light direction such that it looks as if the light was a sphere
                    "lightDirWS = normalize(-dir);\n" +
                    (if (hasLightRadius) "" +
                            "#define lightRadius data1.x\n" +
                            "if(lightRadius > 0.0){\n" +
                            // todo effect is much more visible in the diffuse part
                            // it's fine for small increased, but we wouldn't really use them...
                            // should be more visible in the specular case...
                            // in the ideal case, we move the light such that it best aligns the sphere...
                            "   vec3 idealLightDirWS = normalize(reflect(finalPosition, finalNormal));\n" +
                            "   lightDirWS = normalize(mix(lightDirWS, idealLightDirWS, clamp(lightRadius/(length(dir)),0.0,1.0)));\n" +
                            "}\n" else "") +
                    "NdotL = dot(lightDirWS, finalNormal);\n" +
                    // shadow maps
                    // shadows can be in every direction -> use cubemaps
                    (if (withShadows) "" +
                            "if(shadowMapIdx0 < shadowMapIdx1 && receiveShadows){\n" +
                            "   float near = data2.a;\n" +
                            "   float maxAbsComponent = max(max(abs(dir.x),abs(dir.y)),abs(dir.z));\n" +
                            "   float depthFromShader = near/maxAbsComponent;\n" +
                            // todo how can we get rid of this (1,-1,-1), what rotation is missing?
                            "   lightColor *= texture_array_depth_shadowMapCubic(shadowMapIdx0, dir*vec3(+1,-1,-1), depthFromShader);\n" +
                            "}\n"
                    else "") +
                    "effectiveDiffuse = lightColor * ${LightType.POINT.falloff};\n" +
                    // "dir *= 0.2;\n" + // less falloff by a factor of 5,
                    // because specular light is more directed and therefore reached farther
                    // nice in theory, but practically, we would need a larger cube for that
                    "effectiveSpecular = effectiveDiffuse;\n"
        }

    }

}