package me.anno.ecs.components.light

import me.anno.ecs.Component
import me.anno.ecs.Entity
import me.anno.ecs.annotations.HideInInspector
import me.anno.ecs.annotations.Range
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.engine.pbr.DeferredRenderer
import me.anno.engine.ui.render.ECSShaderLib.pbrModelShader
import me.anno.gpu.DepthMode
import me.anno.gpu.RenderState
import me.anno.gpu.RenderState.useFrame
import me.anno.gpu.ShaderLib
import me.anno.gpu.deferred.DeferredLayerType
import me.anno.gpu.framebuffer.Frame
import me.anno.gpu.framebuffer.Framebuffer
import me.anno.gpu.pipeline.Pipeline
import me.anno.gpu.pipeline.PipelineStage
import me.anno.gpu.pipeline.Sorting
import me.anno.gpu.shader.BaseShader
import me.anno.gpu.shader.Renderer
import me.anno.gpu.shader.builder.Variable
import me.anno.io.serialization.NotSerializedProperty
import me.anno.io.serialization.SerializedProperty
import org.joml.*
import org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT
import org.lwjgl.opengl.GL11.glClear
import kotlin.math.pow

abstract class LightComponent(
    val lightType: LightType
) : Component() {

    // todo instead of using the matrix directly, define a matrix
    // todo this matrix then can include the perspective transform if required

    // todo how do we get the normal then?
    // todo just divide by z?


    // todo for shadow mapping, we need this information...

    var shadowMapCascades = 0

    // typical: 2..4
    @Range(1.0, 1000.0)
    @SerializedProperty
    var shadowMapPower = 4.0
    var shadowMapResolution = 1024

    // how much light there is in the shadow
    var shadowBleed = 0.5f

    val hasShadow get() = shadowMapCascades > 0

    var isInstanced = false

    override fun copy(clone: PrefabSaveable) {
        super.copy(clone)
        clone as LightComponent
        clone.isInstanced = isInstanced
        clone.shadowMapCascades = shadowMapCascades
        clone.shadowMapPower = shadowMapPower
        clone.shadowMapResolution = shadowMapResolution
        clone.color = color
        println("cloning light with $shadowMapCascades cascades")
    }

    // black lamp light?
    @SerializedProperty
    var color: Vector3f = Vector3f(1f)

    abstract fun getLightPrimitive(): Mesh

    @HideInInspector
    @NotSerializedProperty
    var shadowTextures: Array<Framebuffer>? = null

    fun ensureShadowBuffers() {
        if (hasShadow) {
            // only a single one is supported,
            // because more makes no sense
            if (lightType == LightType.POINT) shadowMapCascades = 1
            val shadowCascades = shadowTextures
            val targetSize = shadowMapCascades
            val resolution = shadowMapResolution
            if (shadowCascades == null || shadowCascades.size != targetSize || shadowCascades.first().w != resolution) {
                shadowCascades?.forEach { it.destroy() }
                this.shadowTextures = Array(targetSize) {
                    Framebuffer(
                        "Shadow[$it]", resolution, resolution, 1, 0,
                        false, Framebuffer.DepthBufferType.TEXTURE
                    )
                }
            }
        } else {
            shadowTextures?.forEach { it.destroy() }
            shadowTextures = null
        }
    }

    override fun onUpdate() {
        ensureShadowBuffers()
        if (hasShadow) {
            updateShadowMaps()
        }
    }

    // todo the single really large light is the sun, and maybe the moon,
    // todo so theoretically, we could limit cascades to those two...


    fun updateShadowMaps() {
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
        for (i in 0 until shadowMapCascades) {
            val cascadeScale = shadowMapPower.pow(-i.toDouble())
            val texture = shadowTextures!![i]
            when (lightType) {
                LightType.DIRECTIONAL -> {
                    // not times worldScale, because worldScale would need to be divided again
                    val size = worldScale * 20.0 // cascadeScale
                    cameraMatrix.set(Matrix4d(transform.drawTransform).invert())
                    cameraMatrix.setTranslation(0f, 0f, 0f)
                    val sx = (1.0 / (cascadeScale * worldScale)).toFloat()
                    val sz = (1.0 / (worldScale)).toFloat()
                    // z must be mapped from [-1,1] to [0,1]
                    // additionally it must be scaled to match the world size
                    cameraMatrix.scaleLocal(sx, sx, sz * 0.5f)
                    cameraMatrix.m32(0.5f)
                    pipeline.frustum.defineOrthographic(// todo correct frustum
                        size, size, size, resolution,
                        position, rotation
                    )
                }
                LightType.SPOT -> {
                    val near = 1e-16 * cascadeScale // idk...
                    val far = worldScale * cascadeScale
                    this as SpotLight
                    val fovYRadians = fovRadians
                    cameraMatrix.setPerspective(fovYRadians.toFloat(), 1f, near.toFloat(), far.toFloat())
                    cameraMatrix.rotate(Quaternionf(rotation).invert())
                    pipeline.frustum.definePerspective(
                        near, far, fovYRadians, resolution, resolution,
                        1.0, position, rotation,
                    )
                }
                LightType.POINT -> {
                    val near = worldScale * cascadeScale
                    val far = worldScale * cascadeScale
                    cameraMatrix.setPerspective(Math.PI.toFloat() / 2f, 1f, near.toFloat(), far.toFloat())
                    cameraMatrix.rotate(Quaternionf(rotation).invert())
                    TODO("cubemap ... how do we render it?")
                }
            }

            val root = entity.getRoot(Entity::class)
            pipeline.fillDepth(root, position, worldScale)
            RenderState.depthMode.use(DepthMode.GREATER) {
                useFrame(texture, Renderer.depthOnlyRenderer) {
                    Frame.bind()
                    glClear(GL_DEPTH_BUFFER_BIT)
                    pipeline.drawDepth(cameraMatrix, position, worldScale)
                }
            }
        }
    }

    // todo AES lights, and their textures?

    // is set by the pipeline
    @NotSerializedProperty
    val invWorldMatrix = Matrix4x3f()

    companion object {

        val cameraMatrix = Matrix4f()
        val pipeline = Pipeline()

        init {
            pipeline.defaultStage = PipelineStage(
                "", Sorting.NO_SORTING, 0, null, DepthMode.GREATER,
                true, 0, pbrModelShader
            )
        }

        // todo plane of light... how?
        // todo lines of light... how?

        val lightShaders = HashMap<String, BaseShader>()
        fun getShader(sample: LightComponent): BaseShader {
            return lightShaders.getOrPut(sample.className) {
                val deferred = DeferredRenderer.deferredSettings!!
                ShaderLib.createShaderPlus(
                    "PointLight",
                    ShaderLib.v3DBase +
                            "a3 coords;\n" +
                            "uniform mat4x3 localTransform;\n" +
                            "void main(){\n" +
                            "   finalPosition = coords;\n" +
                            "   finalPosition = localTransform * vec4(finalPosition, 1.0);\n" +
                            "   gl_Position = transform * vec4(finalPosition, 1.0);\n" +
                            "   center = localTransform * vec4(0,0,0,1);\n" +
                            "   uvw = gl_Position.xyw;\n" +
                            ShaderLib.positionPostProcessing +
                            "}", ShaderLib.y3D + Variable("vec3", "center"), "" +
                            "float getIntensity(vec3 dir){\n" +
                            "   return 1.0;\n" +
                            // todo fix
                            //"" + sample.getFalloffFunction() +
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