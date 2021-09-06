package me.anno.ecs.components.light

import me.anno.ecs.Component
import me.anno.ecs.Entity
import me.anno.ecs.annotations.HideInInspector
import me.anno.ecs.annotations.Range
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.engine.pbr.DeferredRenderer
import me.anno.engine.ui.render.ECSShaderLib.pbrModelShader
import me.anno.engine.ui.render.RenderView
import me.anno.gpu.DepthMode
import me.anno.gpu.RenderState
import me.anno.gpu.RenderState.useFrame
import me.anno.gpu.ShaderLib
import me.anno.gpu.deferred.DeferredLayerType
import me.anno.gpu.framebuffer.*
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

    // todo AES lights, and their textures?

    // todo plane of light: how?
    // todo lines of light: how?
    // todo circle/sphere of light: how?

    var shadowMapCascades = 0

    // typical: 2..4
    @Range(1.0, 1000.0)
    @SerializedProperty
    var shadowMapPower = 4.0
    var shadowMapResolution = 1024

    val hasShadow get() = shadowMapCascades > 0

    /** for deferred rendering, no shadows
     * improves rendering speed when drawing lots of lights without shadows */
    var isInstanced = false

    var needsUpdate = true
    var autoUpdate = true

    // black lamp light?
    @SerializedProperty
    var color: Vector3f = Vector3f(1f)

    override fun copy(clone: PrefabSaveable) {
        super.copy(clone)
        clone as LightComponent
        clone.isInstanced = isInstanced
        clone.shadowMapCascades = shadowMapCascades
        clone.shadowMapPower = shadowMapPower
        clone.shadowMapResolution = shadowMapResolution
        clone.color = color
    }

    override fun onDrawGUI(view: RenderView) {
        if (isSelectedIndirectly) {
            drawShape()
        }
    }

    abstract fun drawShape()

    abstract fun getLightPrimitive(): Mesh

    @HideInInspector
    @NotSerializedProperty
    var shadowTextures: Array<IFramebuffer>? = null

    fun ensureShadowBuffers() {
        if (hasShadow) {
            // only a single one is supported,
            // because more makes no sense
            val isPointLight = lightType == LightType.POINT
            if (isPointLight) shadowMapCascades = 1
            val shadowCascades = shadowTextures
            val targetSize = shadowMapCascades
            val resolution = shadowMapResolution
            if (shadowCascades == null || shadowCascades.size != targetSize || shadowCascades.first().w != resolution) {
                shadowCascades?.forEach { it.destroy() }
                // we currently use a depth bias of 0.005,
                // which is equal to ~ 1/255
                // so a 8 bit depth buffer would be enough
                val depthBufferType = DepthBufferType.TEXTURE_16
                this.shadowTextures = Array(targetSize) {
                    if (isPointLight) {
                        CubemapFramebuffer(
                            "ShadowCubemap[$it]", resolution, 0,
                            false, depthBufferType
                        )
                    } else {
                        Framebuffer(
                            "Shadow[$it]", resolution, resolution, 1, 0,
                            false,depthBufferType
                        )
                    }
                }
            }
        } else {
            shadowTextures?.forEach { it.destroy() }
            shadowTextures = null
        }
    }

    override fun onUpdate() {
        if (autoUpdate || needsUpdate) {
            needsUpdate = autoUpdate
            ensureShadowBuffers()
            if (hasShadow) {
                updateShadowMaps()
            }
        }
    }

    abstract fun updateShadowMap(
        cascadeScale: Double, worldScale: Double,
        cameraMatrix: Matrix4f,
        drawTransform: Matrix4x3d, pipeline: Pipeline,
        resolution: Int, position: Vector3d, rotation: Quaterniond
    )

    open fun updateShadowMaps() {
        val pipeline = pipeline
        pipeline.reset()
        val entity = entity!!
        val transform = entity.transform
        val drawTransform = transform.drawTransform
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
            updateShadowMap(
                cascadeScale, worldScale,
                cameraMatrix, drawTransform,
                pipeline, resolution,
                position, rotation
            )
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

    // is set by the pipeline
    @NotSerializedProperty
    val invWorldMatrix = Matrix4x3f()

    override fun destroy() {
        super.destroy()
        shadowTextures?.forEach { it.destroy() }
        shadowTextures = null
    }

    companion object {

        val cameraMatrix = Matrix4f()
        val pipeline = Pipeline()

        init {
            pipeline.defaultStage = PipelineStage(
                "", Sorting.NO_SORTING, 0, null, DepthMode.GREATER,
                true, 0, pbrModelShader
            )
        }

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