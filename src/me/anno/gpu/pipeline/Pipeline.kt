package me.anno.gpu.pipeline

import me.anno.Time
import me.anno.cache.ICacheData
import me.anno.ecs.Component
import me.anno.ecs.Entity
import me.anno.ecs.annotations.Type
import me.anno.ecs.components.light.LightComponent
import me.anno.ecs.components.light.PlanarReflection
import me.anno.ecs.components.mesh.*
import me.anno.ecs.components.mesh.Material.Companion.defaultMaterial
import me.anno.ecs.components.shaders.Skybox
import me.anno.ecs.components.shaders.SkyboxBase
import me.anno.ecs.interfaces.Renderable
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.engine.ui.render.*
import me.anno.engine.ui.render.ECSShaderLib.pbrModelShader
import me.anno.gpu.CullMode
import me.anno.gpu.DepthMode
import me.anno.gpu.GFXState
import me.anno.gpu.M4x3Delta.set4x3delta
import me.anno.gpu.blending.BlendMode
import me.anno.gpu.deferred.DeferredLayerType
import me.anno.gpu.deferred.DeferredSettings
import me.anno.gpu.drawing.Perspective
import me.anno.gpu.framebuffer.CubemapFramebuffer
import me.anno.gpu.framebuffer.DepthBufferType
import me.anno.gpu.framebuffer.TargetType
import me.anno.gpu.pipeline.PipelineStage.Companion.DECAL_PASS
import me.anno.gpu.pipeline.PipelineStage.Companion.OPAQUE_PASS
import me.anno.gpu.pipeline.PipelineStage.Companion.TRANSPARENT_PASS
import me.anno.gpu.pipeline.transparency.GlassPass
import me.anno.gpu.pipeline.transparency.TransparentPass
import me.anno.gpu.texture.CubemapTexture
import me.anno.gpu.texture.GPUFiltering
import me.anno.io.ISaveable
import me.anno.io.Saveable
import me.anno.io.base.BaseWriter
import me.anno.io.files.FileReference
import me.anno.io.files.thumbs.Thumbs
import me.anno.io.serialization.SerializedProperty
import me.anno.maths.Maths
import me.anno.utils.OS
import me.anno.utils.pooling.JomlPools
import me.anno.utils.structures.Compare.ifSame
import me.anno.utils.structures.lists.Lists.any2
import me.anno.utils.structures.lists.SmallestKList
import org.apache.logging.log4j.LogManager
import org.joml.AABBd
import org.joml.Vector3d
import org.joml.Vector3f
import org.joml.Vector4d
import java.util.*
import kotlin.math.max

/**
 * Collects meshes for different passes (opaque, transparency, decals, ...), and for instanced rendering;
 * Makes rendering multiple points of view much cheaper (e.g., for stereo vision for VR)
 * */
class Pipeline(deferred: DeferredSettings?) : Saveable(), ICacheData {

    var deferred: DeferredSettings? = deferred
        set(value) {
            field = value
            lightStage.deferred = value
        }

    // pipelines, that we need:
    //  - 3d world,
    //  - transparency
    //  - 2d ui,
    //  - ...
    // to do we can sort by material and shaders; or by distance

    var ignoredEntity: Entity? = null // e.g. for environment maps
    var ignoredComponent: Component? = null

    @Type("List<PipelineStage>")
    @SerializedProperty
    val stages = ArrayList<PipelineStage>()

    val lightStage = LightPipelineStage(deferred)

    var defaultStage: PipelineStage = PipelineStage(
        "default", Sorting.NO_SORTING, 0, null, DepthMode.CLOSE,
        true, CullMode.BOTH, pbrModelShader
    )

    var lastClickId = 0

    val frustum = Frustum()

    var skybox: SkyboxBase = Skybox.defaultSky
    var bakedSkybox: CubemapFramebuffer? = null

    val planarReflections = ArrayList<PlanarReflection>()

    // vec4(pos,1).dot(prp) > 0.0 -> discard
    // therefore use 0,0,0,0 to disable this plane
    val reflectionCullingPlane = Vector4d()

    var applyToneMapping = true

    fun disableReflectionCullingPlane() {
        reflectionCullingPlane.set(0.0)
    }

    fun findStage(material: Material): PipelineStage {
        val stage0 = material.pipelineStage
        if (stage0 < 0) return defaultStage
        while (stages.size <= stage0) {
            stages.add(
                when (stages.size) {
                    OPAQUE_PASS -> defaultStage
                    TRANSPARENT_PASS -> PipelineStage(
                        "transparent",
                        Sorting.BACK_TO_FRONT, 64,
                        BlendMode.DEFAULT, DepthMode.CLOSE, false, CullMode.BOTH,
                        pbrModelShader
                    )
                    DECAL_PASS -> PipelineStage(
                        "decal", Sorting.NO_SORTING, 64,
                        null, DepthMode.FARTHER, false, CullMode.FRONT,
                        // todo default decal shader? :) would be nice :D
                        pbrModelShader
                    )
                    else -> defaultStage.clone()
                }
            )
        }
        return stages[stage0]
        // todo analyse, whether the material has transparency, and if so,
        // todo add it to the transparent pass
    }

    private fun getMaterial(
        materialOverrides: List<FileReference>?,
        materials: List<FileReference>,
        index: Int
    ): Material {
        val m0 = materialOverrides?.getOrNull(index)?.nullIfUndefined()
        val m1 = m0 ?: materials.getOrNull(index)
        return MaterialCache[m1, defaultMaterial]
    }

    fun addMesh(mesh: IMesh, renderer: Component, entity: Entity) {
        val materialOverrides = (renderer as? MeshComponentBase)?.materials
        addMesh(mesh, renderer, materialOverrides, entity)
    }

    fun addMesh(mesh: IMesh, renderer: Component, materialOverrides: List<FileReference>?, entity: Entity) {
        mesh.ensureBuffer()
        val materials = mesh.materials
        for (index in 0 until mesh.numMaterials) {
            val material = getMaterial(materialOverrides, materials, index)
            val stage = findStage(material)
            stage.add(renderer, mesh, entity, material, index)
        }
    }

    fun addMeshInstanced(mesh: Mesh, renderer: Component, entity: Entity) {
        val materialOverrides = (renderer as? MeshComponentBase)?.materials
        addMeshInstanced(mesh, renderer, materialOverrides, entity)
    }

    fun addMeshInstanced(mesh: Mesh, renderer: Component, materialOverrides: List<FileReference>?, entity: Entity) {
        mesh.ensureBuffer()
        val materials = mesh.materials
        for (index in 0 until mesh.numMaterials) {
            val material = getMaterial(materialOverrides, materials, index)
            val stage = findStage(material)
            stage.addInstanced(mesh, renderer, entity, material, index)
        }
    }

    fun addLight(light: LightComponent, entity: Entity) {
        // for debugging of the light shapes
        // addMesh(light.getLightPrimitive(), MeshRenderer(), entity, 0)
        val stage = lightStage
        // update light transform
        // its drawn position probably should be smoothed -> we probably should use the drawnMatrix instead of the global one
        // we may want to set a timestamp, so we don't update it twice? no, we fill the pipeline only once
        light.invCamSpaceMatrix
            .set4x3delta(entity.transform.getDrawMatrix(), RenderState.cameraPosition, RenderState.worldScale)
            .invert()
        stage.add(light, entity)
    }

    var transparentPass: TransparentPass = GlassPass()

    fun bakeSkybox(resolution: Int) {
        if (resolution <= 0) {
            return
        }

        val self = RenderView.currentInstance
        val renderMode = self?.renderMode
        if (renderMode == RenderMode.LINES || renderMode == RenderMode.LINES_MSAA) {
            self.renderMode = RenderMode.DEFAULT
        }
        // todo only update skybox every n frames
        //  maybe even only one side at a time
        val framebuffer = bakedSkybox ?: CubemapFramebuffer(
            "skyBox", resolution, 1,
            arrayOf(TargetType.FP16Target3), DepthBufferType.NONE
        )
        val renderer = Renderers.rawAttributeRenderers[DeferredLayerType.EMISSIVE]
        framebuffer.draw(renderer) { side ->
            val skyRot = JomlPools.quat4f.create()
            val cameraMatrix = JomlPools.mat4f.create()
            val sky = skybox
            // draw sky
            // could be optimized to draw a single triangle instead of a full cube for each side
            CubemapTexture.rotateForCubemap(skyRot.identity(), side)
            val shader = (sky.shader ?: pbrModelShader).value
            shader.use()
            Perspective.setPerspective(
                cameraMatrix, Maths.PIf * 0.5f, 1f,
                0.1f, 10f, 0f, 0f
            )
            cameraMatrix.rotate(skyRot)
            shader.m4x4("transform", cameraMatrix)
            if (side == 0) {
                shader.v1i("hasVertexColors", 0)
                sky.material.bind(shader)
            }// else already set
            shader.v3f("cameraPosition", RenderState.cameraPosition)
            shader.v4f("cameraRotation", RenderState.cameraRotation)
            shader.v1f("camScale", RenderState.worldScale.toFloat())
            shader.v1f("meshScale", 1f)
            shader.v1b("isPerspective", false)
            shader.v1b("reversedDepth", false) // depth doesn't matter
            sky.getMesh().draw(shader, 0)
            JomlPools.quat4f.sub(1)
            JomlPools.mat4f.sub(1)
        }
        if (!OS.isAndroid) {
            // performance impact of this: 230->210 fps, so 0.4ms on RTX 3070
            framebuffer.textures[0].bind(0, GPUFiltering.LINEAR)
        }
        bakedSkybox = framebuffer
        if (renderMode != null) {
            self.renderMode = renderMode
        }
    }

    fun destroyBakedSkybox() {
        bakedSkybox?.destroy()
        bakedSkybox = null
    }

    override fun destroy() {
        bakedSkybox?.destroy()
        bakedSkybox = null
        transparentPass.destroy()
    }

    fun hasTransparentPart(): Boolean {
        return GFXState.currentRenderer.deferredSettings != null &&
                stages.any2 { it.blendMode != null && it.size > 0 } &&
                GFXState.currentBuffer.numTextures >= 2
    }

    fun draw(drawSky: Boolean = true) {
        if (hasTransparentPart()) {
            transparentPass.draw0(this, drawSky)
        } else {
            var hasDrawnSky = !drawSky
            for (i in stages.indices) {
                val stage = stages[i]
                if (stage.blendMode != null && !hasDrawnSky) {
                    drawSky()
                    hasDrawnSky = true
                }
                if (stage.size > 0) {
                    stage.bindDraw(this)
                }
            }
            if (!hasDrawnSky) {
                drawSky()
            }
        }
    }

    fun drawWithoutSky() {
        if (hasTransparentPart()) {
            // todo test this
            transparentPass.draw0(this, false)
        } else {
            GFXState.currentBuffer.clearColor(0)
            for (i in stages.indices) {
                val stage = stages[i]
                if (stage.size > 0) {
                    stage.bindDraw(this)
                }
            }
        }
    }

    fun drawSky() {
        GFXState.depthMode.use(defaultStage.depthMode) {
            GFXState.depthMask.use(false) {
                GFXState.blendMode.use(null) {
                    drawSky0(defaultStage)
                }
            }
        }
    }

    fun drawSky0(stage: PipelineStage) {
        val sky = skybox
        val mesh = sky.getMesh()
        val allAABB = JomlPools.aabbd.create()
        val scale = if (RenderState.isPerspective) 1f
        else 2f * max(RenderState.fovXRadians, RenderState.fovYRadians)
        allAABB.all()
        for (i in 0 until mesh.numMaterials) {
            val material = MaterialCache[sky.materials.getOrNull(i)] ?: defaultMaterial
            val shader = (material.shader ?: pbrModelShader).value
            shader.use()
            stage.initShader(shader, this)
            stage.bindRandomness(shader)
            stage.setupLights(this, shader, allAABB, false)
            PipelineStage.setupLocalTransform(shader, sky.transform, Time.gameTimeN)
            shader.v1b("hasAnimation", false)
            shader.v4f("tint", -1)
            shader.v1f("finalAlpha", 1f)
            shader.v1i("hasVertexColors", 0)
            shader.v2i("randomIdData", 6, 123456)
            shader.v1f("meshScale", scale)
            material.bind(shader)
            mesh.draw(shader, i)
        }
        JomlPools.aabbd.sub(1)
    }

    fun clear() {
        lightStage.clear()
        defaultStage.clear()
        planarReflections.clear()
        lights.fill(null)
        for (stageIndex in stages.indices) {
            stages[stageIndex].clear()
        }
    }

    fun resetClickId() {
        lastClickId = 1
    }

    fun fill(rootElement: Entity): Int {
        // more complex traversal:
        // done exclude static entities by their AABB
        // done exclude entities, if they contain no meshes
        // done exclude entities, if they are off-screen
        // todo reuse the pipeline state for multiple frames
        //  - add a margin, so entities at the screen border can stay visible
        //  - partially populate the pipeline?
        rootElement.validateAABBs()
        val lastClickId = subFill(rootElement, lastClickId)
        this.lastClickId = lastClickId
        return lastClickId
    }

    fun fill(root: PrefabSaveable) {
        val clickId = lastClickId
        if (root is Renderable) {
            root.fill(this, sampleEntity, clickId)
        } else {
            LOGGER.warn(
                "Don't know how to render ${root.className}, " +
                        "please implement me.anno.ecs.interfaces.Renderable, if it is supposed to be renderable"
            )
        }
    }

    // todo fix deferred rendering for scenes with many lights

    val lights = arrayOfNulls<LightRequest>(RenderView.MAX_FORWARD_LIGHTS)

    val center = Vector3d()
    private val lightList = SmallestKList<LightRequest>(16) { a, b ->
        // todo also use the size, and relative size to the camera
        val at = a.drawMatrix
        val bt = b.drawMatrix
        val cam = RenderState.cameraPosition
        val scale = JomlPools.vec3d.borrow()
        val da = (at.distanceSquared(center) + at.distanceSquared(cam)) * bt.getScale(scale).lengthSquared()
        val db = (bt.distanceSquared(center) + bt.distanceSquared(cam)) * at.getScale(scale).lengthSquared()
        da.compareTo(db)
    }

    /**
     * creates a list of relevant lights for a forward-rendering draw call of a mesh or region
     * */
    fun getClosestRelevantNLights(region: AABBd, numberOfLights: Int, lights: Array<LightRequest?>): Int {
        val lightStage = lightStage
        if (numberOfLights <= 0) return 0
        val size = lightStage.size
        if (size < numberOfLights) {
            // check if already filled:
            if (lights[0] == null) {
                for (i in 0 until size) {
                    lights[i] = lightStage[i]
                }
                // sort by type, and whether they have a shadow
                Arrays.sort(lights, 0, size) { a, b ->
                    val va = a!!.light
                    val vb = b!!.light
                    va.hasShadow.compareTo(vb.hasShadow).ifSame {
                        va.lightType.shadowMapType.compareTo(vb.lightType.shadowMapType)
                    }
                }
            }// else done
            return size
        } else {
            val center = center.set(region.centerX, region.centerY, region.centerZ)
            if (!center.isFinite) center.set(0.0)
            lightList.clear()
            this.lightStage.listOfAll(lightList)
            val smallest = lightList
            for (i in 0 until smallest.size) {
                lights[i] = smallest[i]
            }
            // sort by type, and whether they have a shadow
            Arrays.sort(lights, 0, smallest.size) { a, b ->
                val va = a!!.light
                val vb = b!!.light
                va.hasShadow.compareTo(vb.hasShadow).ifSame {
                    va.lightType.shadowMapType.compareTo(vb.lightType.shadowMapType)
                }
            }
            return smallest.size
        }
    }

    private fun subFill(entity: Entity, clickId0: Int): Int {
        var clickId = clickId0
        val components = entity.components
        for (i in components.indices) {
            val component = components[i]
            if (component.isEnabled && component !== ignoredComponent && component is Renderable) {
                if (component !is MeshComponentBase || frustum.isVisible(component.globalAABB)) {
                    clickId = component.fill(this, entity, clickId)
                }
            }
        }
        val children = entity.children
        for (i in children.indices) {
            val child = children[i]
            if (child !== ignoredEntity && child.isEnabled && frustum.isVisible(child.aabb)) {
                clickId = subFill(child, clickId)
            }
        }
        return clickId
    }

    fun traverse(world: PrefabSaveable, run: (Entity) -> Unit) {
        if (world is Entity) traverse(world, run)
    }

    fun traverse(world: Entity, run: (Entity) -> Unit) {
        run(world)
        val children = world.children
        for (i in children.indices) {
            val child = children[i]
            if (child !== ignoredEntity && child.isEnabled && frustum.isVisible(child.aabb)) {
                traverse(child, run)
            }
        }
    }

    @Suppress("unused")
    fun traverseConditionally(entity: Entity, run: (Entity) -> Boolean) {
        if (run(entity)) {
            val children = entity.children
            for (i in children.indices) {
                val child = children[i]
                if (child !== ignoredEntity && child.isEnabled && frustum.isVisible(child.aabb)) {
                    traverseConditionally(child, run)
                }
            }
        }
    }

    fun findDrawnSubject(searchedId: Int, entity: Entity): Any? {
        // LOGGER.debug("[E] ${entity.clickId.toString(16)} vs ${searchedId.toString(16)}")
        if (entity.clickId == searchedId) return entity
        val components = entity.components
        for (i in components.indices) {
            val c = components[i]
            if (c.isEnabled && c is Renderable) {
                val found = c.findDrawnSubject(searchedId)
                if (found != null) return found
            }
        }
        val children = entity.children
        for (i in children.indices) {
            val child = children[i]
            if (child.isEnabled && frustum.isVisible(child.aabb)) {
                val found = findDrawnSubject(searchedId, child)
                if (found != null) return found
            }
        }
        return null
    }

    override fun save(writer: BaseWriter) {
        super.save(writer)
        writer.writeBoolean("applyToneMapping", applyToneMapping)
        writer.writeObjectList(this, "stages", stages)
    }

    override fun readBoolean(name: String, value: Boolean) {
        when (name) {
            "applyToneMapping" -> applyToneMapping = value
            else -> super.readBoolean(name, value)
        }
    }

    override fun readObject(name: String, value: ISaveable?) {
        when (name) {
            "stages" -> stages.add(value as? PipelineStage ?: return)
            else -> super.readObject(name, value)
        }
    }

    override fun readObjectArray(name: String, values: Array<ISaveable?>) {
        when (name) {
            "stages" -> {
                stages.clear()
                stages.addAll(values.filterIsInstance<PipelineStage>())
            }
            else -> super.readObjectArray(name, values)
        }
    }

    override val className: String get() = "Pipeline"
    override val approxSize get() = 10
    override fun isDefaultValue() = false

    companion object {
        private val LOGGER = LogManager.getLogger(Pipeline::class)
        val sampleEntity = Entity()
        val sampleMeshComponent = MeshComponent()
        val sampleMesh = Thumbs.sphereMesh
    }
}
