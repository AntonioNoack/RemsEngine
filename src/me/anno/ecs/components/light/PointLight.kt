package me.anno.ecs.components.light

import me.anno.ecs.Entity
import me.anno.ecs.annotations.Range
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.engine.gui.LineShapes.drawBox
import me.anno.gpu.DepthMode
import me.anno.gpu.RenderState
import me.anno.gpu.drawing.Perspective.setPerspective
import me.anno.gpu.framebuffer.CubemapFramebuffer
import me.anno.gpu.framebuffer.Frame
import me.anno.gpu.pipeline.Pipeline
import me.anno.gpu.shader.Renderer
import me.anno.io.serialization.SerializedProperty
import me.anno.mesh.vox.meshing.BlockBuffer
import me.anno.mesh.vox.meshing.BlockSide
import me.anno.mesh.vox.meshing.VoxelMeshBuildInfo
import me.anno.utils.structures.arrays.ExpandingFloatArray
import org.joml.*
import org.lwjgl.opengl.GL11
import kotlin.math.PI

// todo size of point light: probably either distance or direction needs to be adjusted
// todo - in proximity, the appearance must not stay as a point, but rather be a sphere

class PointLight : LightComponent(LightType.POINT) {

    @Range(0.0, 5.0)
    var lightSize = 0.0

    @SerializedProperty
    @Range(1e-6, 1.0)
    var near = 0.001


    /* good idea, but typically, the precision is good enough,
    and motion is fast enough, that the results flicker way too much
    @SerializedProperty
    var updateIndividually = false

    // not important enough to be serialized
    @NotSerializedProperty
    var nextFaceIndex = 0*/

    override fun getShaderV0(drawTransform: Matrix4x3d, worldScale: Double): Float {
        // put light size * world scale
        // avg, and then /3
        // but the center really is much smaller -> *0.01
        val lightSize = drawTransform.getScale(Vector3d()).dot(1.0, 1.0, 1.0) * lightSize / 9.0
        return (lightSize * worldScale).toFloat()
    }

    // v1 is not used
    override fun getShaderV2() = near.toFloat()

    override fun invalidateShadows() {
        needsUpdate = true // 6
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
        pipeline.reset()
        val entity = entity!!
        val transform = entity.transform
        val resolution = shadowMapResolution
        val global = transform.globalTransform
        val position = global.getTranslation(Vector3d())
        val rotation = global.getUnnormalizedRotation(Quaterniond())
        val sqrt3 = 1.7320508075688772
        val worldScale = sqrt3 / global.getScale(Vector3d()).length()
        // only fill pipeline once? probably better...

        val texture = shadowTextures!![0] as CubemapFramebuffer

        val far = 1.0

        val deg90 = Math.PI * 0.5
        val rot2 = Quaterniond(rotation).invert()
        val rot3 = Quaterniond()

        val root = entity.getRoot(Entity::class)
        RenderState.depthMode.use(DepthMode.GREATER) {
            texture.draw(Renderer.depthOnlyRenderer) { side ->
                // if (!updateIndividually || side == nextFaceIndex) {
                Frame.bind()
                GL11.glClear(GL11.GL_DEPTH_BUFFER_BIT)
                setPerspective(cameraMatrix, deg90.toFloat(), 1f, near.toFloat(), far.toFloat())
                rot3.identity()
                // rotate based on direction
                /*
                * POSITIVE_X = 34069;
                * NEGATIVE_X = 34070;
                * POSITIVE_Y = 34071;
                * NEGATIVE_Y = 34072;
                * POSITIVE_Z = 34073;
                * NEGATIVE_Z = 34074;
                * */
                when (side) {
                    0 -> rot3.rotateY(+PI * 0.5)
                    1 -> rot3.rotateY(-PI * 0.5)
                    2 -> rot3.rotateX(+PI * 0.5)
                    3 -> rot3.rotateX(-PI * 0.5)
                    5 -> rot3.rotateY(PI)
                    4 -> {
                        rot3//.rotateZ(PI*1.0)
                    } // done
                }
                rot3.mul(rot2)
                cameraMatrix.rotate(Quaternionf(rot3))
                pipeline.frustum.definePerspective(
                    near / worldScale, far / worldScale, deg90, resolution, resolution, 1.0,
                    position, rot3.invert() // needs to be the inverse again
                )
                pipeline.fillDepth(root, position, worldScale)
                pipeline.drawDepth(cameraMatrix, position, worldScale)
                // }
            }
        }

        // if(updateIndividually) nextFaceIndex = (nextFaceIndex + 1) % 6

    }

    override fun copy(clone: PrefabSaveable) {
        super.copy(clone)
        clone as PointLight
        clone.lightSize = lightSize
        clone.near = near
        // clone.updateIndividually = updateIndividually
    }

    override fun drawShape() {
        // todo draw sphere or view-aligned-circle
        drawBox(entity)
    }

    override fun getLightPrimitive(): Mesh = cubeMesh

    /*override fun onDrawGUI(view: RenderView) {
        super.onDrawGUI(view)
        stack.pushMatrix()
        stack.scale(1f / 3f)
        Grid.drawBuffer(stack, white4, Grid.sphereBuffer)
        stack.popMatrix()
    }*/

    override val className: String = "PointLight"

    companion object {

        private const val cutoff = 0.1
        const val falloff = "max(0.0, 1.0/(1.0+9.0*dot(dir,dir)) - $cutoff)*${1.0 / (1.0 - cutoff)}"
        // val falloff2d = "max(0.0, 1.0/(1.0+9.0*dir.z*dir.z) - $cutoff)*${1.0 / (1.0 - cutoff)}"

        val cubeMesh = Mesh()

        init {

            val vertices = ExpandingFloatArray(6 * 2 * 3 * 3)
            val base = VoxelMeshBuildInfo(intArrayOf(0, -1), vertices, null, null)

            base.color = -1

            for (side in BlockSide.values) {
                BlockBuffer.addQuad(base, side, 1, 1, 1)
            }

            // [0,1]³ -> [-1,+1]³
            val positions = vertices.toFloatArray()
            for (i in positions.indices) {
                positions[i] = positions[i] * 2f - 1f
            }
            cubeMesh.positions = positions

        }

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
                            "   lightDirWS = normalize(mix(lightDirWS, idealLightDirWS, clamp(lightRadius/(length(lightPosition-finalPosition)),0,1)));\n" +
                            "}\n" else "") +
                    "NdotL = dot(lightDirWS, finalNormal);\n" +
                    // shadow maps
                    // shadows can be in every direction -> use cubemaps
                    (if (withShadows) "" +
                            "if(shadowMapIdx0 < shadowMapIdx1){\n" +
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
                    "dir *= 0.2;\n" + // less falloff by a factor of 5,
                    // because specular light is more directed and therefore reached farther
                    "effectiveSpecular = lightColor * ${LightType.POINT.falloff};\n"
        }

    }

}