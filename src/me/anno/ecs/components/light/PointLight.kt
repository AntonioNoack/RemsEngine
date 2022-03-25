package me.anno.ecs.components.light

import me.anno.ecs.Entity
import me.anno.ecs.annotations.Range
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.engine.gui.LineShapes.drawBox
import me.anno.engine.gui.LineShapes.drawSphere
import me.anno.gpu.DepthMode
import me.anno.gpu.OpenGL
import me.anno.gpu.drawing.Perspective.setPerspective
import me.anno.gpu.framebuffer.CubemapFramebuffer
import me.anno.gpu.framebuffer.Frame
import me.anno.gpu.pipeline.Pipeline
import me.anno.gpu.shader.Renderer
import me.anno.io.serialization.SerializedProperty
import me.anno.maths.Maths.SQRT3
import me.anno.mesh.Shapes
import me.anno.utils.pooling.JomlPools
import me.anno.utils.types.Matrices.getScaleLength
import me.anno.utils.types.Matrices.rotate2
import org.joml.*
import org.lwjgl.opengl.GL11

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

    override fun clone(): PointLight {
        val clone = PointLight()
        copy(clone)
        return clone
    }

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

    }

    override fun updateShadowMaps() {

        val pipeline = pipeline

        val entity = entity!!
        val transform = entity.transform
        val resolution = shadowMapResolution
        val global = transform.globalTransform
        val position = global.getTranslation(JomlPools.vec3d.create())
        val rotation = global.getUnnormalizedRotation(JomlPools.quat4d.create())
        val worldScale = SQRT3 / global.getScaleLength()
        // only fill pipeline once?

        val texture = shadowTextures!![0] as CubemapFramebuffer

        val far = 1.0

        val deg90 = Math.PI * 0.5
        val rotInvert = rotation.invert()
        val rot3 = JomlPools.quat4d.create()

        val cameraMatrix = JomlPools.mat4f.create()
        val root = entity.getRoot(Entity::class)
        OpenGL.depthMode.use(DepthMode.GREATER) {
            texture.draw(resolution, Renderer.depthOnlyRenderer) { side ->
                Frame.bind()
                GL11.glClear(GL11.GL_DEPTH_BUFFER_BIT)
                setPerspective(cameraMatrix, deg90.toFloat(), 1f, near.toFloat(), far.toFloat(), 0f, 0f)
                EnvironmentMap.rotateForCubemap(rot3.identity(), side)
                rot3.mul(rotInvert)
                cameraMatrix.rotate2(rot3)
                pipeline.reset()
                pipeline.frustum.definePerspective(
                    near / worldScale, far / worldScale, deg90, resolution, resolution, 1.0,
                    position, rot3.invert()
                )
                pipeline.fillDepth(root, position, worldScale)
                pipeline.drawDepth(cameraMatrix, position, worldScale)
            }
        }

        JomlPools.vec3d.sub(1)
        JomlPools.mat4f.sub(1)
        JomlPools.quat4d.sub(2)

    }

    override fun copy(clone: PrefabSaveable) {
        super.copy(clone)
        clone as PointLight
        clone.lightSize = lightSize
        clone.near = near
    }

    override fun drawShape() {
        drawBox(entity, JomlPools.vec3d.borrow().set(near))
        drawSphere(entity, 1.0)
    }

    override fun getLightPrimitive(): Mesh = Shapes.cube11Smooth

    override val className: String = "PointLight"

    companion object {

        private const val cutoff = 0.1
        const val falloff = "max(0.0, 1.0/(1.0+9.0*dot(dir,dir)) - $cutoff)*${1.0 / (1.0 - cutoff)}"

        fun getShaderCode(cutoffContinue: String?, withShadows: Boolean, hasLightRadius: Boolean): String {
            return "" +
                    (if (cutoffContinue != null) "if(dot(dir,dir)>1.0) $cutoffContinue;\n"
                    else "") + // outside
                    "lightPosition = data1.rgb;\n" +
                    // when light radius > 0, then adjust the light direction such that it looks as if the light was a sphere
                    "lightDirWS = normalize(lightPosition - finalPosition);\n" +
                    (if (hasLightRadius) "" +
                            "#define lightRadius data1.a\n" +
                            "if(lightRadius > 0.0){\n" +
                            // todo effect is much more visible in the diffuse part
                            // it's fine for small increased, but we wouldn't really use them...
                            // should be more visible in the specular case...
                            // in the ideal case, we move the light such that it best aligns the sphere...
                            "   vec3 idealLightDirWS = normalize(reflect(finalPosition, finalNormal));\n" +
                            "   lightDirWS = normalize(mix(lightDirWS, idealLightDirWS, clamp(lightRadius/(length(lightPosition-finalPosition)),0.0,1.0)));\n" +
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
                            "   float depthFromTex = texture_array_depth_shadowMapCubic(shadowMapIdx0, dir*vec3(+1,-1,-1), depthFromShader);\n" +
                            // "   float val = texture_array_shadowMapCubic(shadowMapIdx0, dir*vec3(+1,-1,-1)).r;\n" +
                            // "   effectiveDiffuse = lightColor * vec3(vec2(val),depthFromShader);\n" + // nice for debugging
                            //"   effectiveDiffuse = lightColor * (dir*.5+.5);\n" +
                            "   lightColor *= 1.0 - depthFromTex;\n" +
                            "}\n"
                    else "") +
                    "effectiveDiffuse = lightColor * ${LightType.POINT.falloff};\n" +
                    // "dir *= 0.2;\n" + // less falloff by a factor of 5,
                    // because specular light is more directed and therefore reached farther
                    // nice in theory, but practically, we would need a larger cube for that
                    "effectiveSpecular = effectiveDiffuse;//lightColor * ${LightType.POINT.falloff};\n"
        }

    }

}