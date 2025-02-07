package me.anno.gpu.pipeline

import me.anno.cache.ICacheData
import me.anno.ecs.Component
import me.anno.ecs.Entity
import me.anno.ecs.EntityQuery.forAllChildren
import me.anno.ecs.EntityQuery.forAllComponents
import me.anno.ecs.Transform
import me.anno.ecs.components.light.LightComponent
import me.anno.ecs.components.light.PlanarReflection
import me.anno.ecs.components.light.sky.Skybox
import me.anno.ecs.components.light.sky.SkyboxBase
import me.anno.ecs.components.mesh.IMesh
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.ecs.components.mesh.MeshComponentBase
import me.anno.ecs.components.mesh.MeshSpawner
import me.anno.ecs.components.mesh.SimpleMesh
import me.anno.ecs.components.mesh.material.Material
import me.anno.ecs.components.mesh.material.Materials.getMaterial
import me.anno.ecs.interfaces.Renderable
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.ecs.systems.Systems
import me.anno.engine.DefaultAssets
import me.anno.engine.ui.render.ECSShaderLib.pbrModelShader
import me.anno.engine.ui.render.Frustum
import me.anno.engine.ui.render.RenderState
import me.anno.engine.ui.render.RenderView
import me.anno.gpu.CullMode
import me.anno.gpu.DepthMode
import me.anno.gpu.GFX
import me.anno.gpu.M4x3Delta.set4x3delta
import me.anno.gpu.blending.BlendMode
import me.anno.gpu.deferred.DeferredSettings
import me.anno.gpu.framebuffer.CubemapFramebuffer
import me.anno.gpu.pipeline.PipelineStageImpl.Companion.DECAL_PASS
import me.anno.gpu.pipeline.PipelineStageImpl.Companion.OPAQUE_PASS
import me.anno.gpu.pipeline.PipelineStageImpl.Companion.TRANSPARENT_PASS
import me.anno.gpu.pipeline.PipelineStageImpl.Companion.drawCallId
import me.anno.gpu.pipeline.transparency.GlassPass
import me.anno.gpu.pipeline.transparency.TransparentPass
import me.anno.gpu.query.GPUClockNanos
import me.anno.gpu.texture.TextureLib
import me.anno.io.files.FileReference
import me.anno.utils.pooling.JomlPools
import me.anno.utils.structures.Compare.ifSame
import me.anno.utils.structures.Recursion
import me.anno.utils.structures.lists.SmallestKList
import org.apache.logging.log4j.LogManager
import org.joml.AABBd
import org.joml.Planed
import org.joml.Vector3d
import kotlin.math.PI

/**
 * Collects meshes for different passes (opaque, transparency, decals, ...), and for instanced rendering;
 * Makes rendering multiple points of view much cheaper (e.g., for stereo vision for VR)
 * */
class Pipeline(deferred: DeferredSettings?) : ICacheData {

    var ignoredEntity: Entity? = null // e.g. for environment maps
    var ignoredComponent: Component? = null
    var superMaterial: Material? = null

    val stages = ArrayList<PipelineStageImpl>()

    val lightStage = LightPipelineStage(deferred)

    var defaultStage: PipelineStageImpl = PipelineStageImpl(
        "default", 0, null, DepthMode.CLOSE, true,
        CullMode.BOTH, pbrModelShader
    )

    val frustum = Frustum()

    var skybox: SkyboxBase = Skybox.defaultSky
    var bakedSkybox: CubemapFramebuffer? = null

    val planarReflections = ArrayList<PlanarReflection>()

    // vec4(pos,1).dot(prp) > 0.0 -> discard
    // therefore use 0,0,0,0 to disable this plane
    val reflectionCullingPlane = Planed()

    var applyToneMapping = true

    var transparentPass: TransparentPass = GlassPass()

    val skyTimer = GPUClockNanos()
    val skyboxTimer = GPUClockNanos()

    fun disableReflectionCullingPlane() {
        reflectionCullingPlane.set(0.0, 0.0, 0.0, 0.0)
    }

    fun findStage(material: Material): PipelineStageImpl {
        val stage0 = material.pipelineStage.id
        for (i in stages.size..stage0) {
            stages.add(
                when (stages.size) {
                    OPAQUE_PASS.id -> defaultStage
                    TRANSPARENT_PASS.id -> PipelineStageImpl(
                        "transparent", RenderView.MAX_FORWARD_LIGHTS, BlendMode.DEFAULT,
                        DepthMode.CLOSE, false, CullMode.BOTH, pbrModelShader
                    )
                    DECAL_PASS.id -> PipelineStageImpl(
                        "decal", RenderView.MAX_FORWARD_LIGHTS, null,
                        if (GFX.supportsClipControl) DepthMode.FARTHER
                        else DepthMode.FORWARD_FARTHER,
                        false, CullMode.BACK, pbrModelShader
                    )
                    else -> defaultStage.clone()
                }
            )
        }
        return stages[stage0]
    }

    fun getBounds(component: Component): AABBd? {
        return when (component) {
            is MeshSpawner -> component.globalAABB
            is MeshComponentBase -> component.globalAABB
            else -> component.entity?.getBounds()
        }
    }

    fun getClickId(component: Component): Int {
        return getClickId(getBounds(component))
    }

    fun getClickId(bounds: AABBd?): Int {
        return clickIdToBounds.getNextId(bounds)
    }

    fun addMesh(mesh: IMesh, renderer: Component, transform: Transform) {
        val materialOverrides = (renderer as? MeshComponentBase)?.materials
        addMesh(mesh, renderer, materialOverrides, transform)
    }

    fun addMesh(mesh: IMesh, renderer: Component, materialOverrides: List<FileReference>?, transform: Transform) {
        mesh.ensureBuffer()
        val materials = mesh.materials
        val superMaterial = superMaterial
        for (index in 0 until mesh.numMaterials) {
            val material = getMaterial(superMaterial, materialOverrides, materials, index)
            val stage = findStage(material)
            stage.add(renderer, mesh, transform, material, index)
        }
    }

    fun addMeshInstanced(mesh: IMesh, renderer: Component, transform: Transform) {
        val materialOverrides = (renderer as? MeshComponentBase)?.materials
        addMeshInstanced(mesh, renderer, materialOverrides, transform)
    }

    fun addMeshInstanced(
        mesh: IMesh,
        renderer: Component,
        materialOverrides: List<FileReference>?,
        transform: Transform
    ) {
        mesh.ensureBuffer()
        val materials = mesh.materials
        val superMaterial = superMaterial
        for (index in 0 until mesh.numMaterials) {
            val material = getMaterial(superMaterial, materialOverrides, materials, index)
            val stage = findStage(material)
            stage.addInstanced(mesh, renderer, transform, material, index)
        }
    }

    fun addLight(light: LightComponent, entity: Entity) {
        addLight(light, entity.transform)
    }

    fun addLight(light: LightComponent, transform: Transform) {
        // for debugging of the light shapes
        // addMesh(light.getLightPrimitive(), MeshRenderer(), entity, 0)
        val stage = lightStage
        // update light transform
        // its drawn position probably should be smoothed -> we probably should use the drawnMatrix instead of the global one
        // we may want to set a timestamp, so we don't update it twice? no, we fill the pipeline only once
        light.invCamSpaceMatrix
            .set4x3delta(transform.getDrawMatrix(), RenderState.cameraPosition, RenderState.worldScale)
            .invert()
        stage.add(light, transform)
    }

    fun bakeSkybox(resolution: Int) {
        if (resolution <= 0) return
        DrawSky.bakeSkybox(this, resolution)
    }

    fun destroyBakedSkybox() {
        bakedSkybox?.destroy()
        bakedSkybox = null
    }

    override fun destroy() {
        bakedSkybox?.destroy()
        bakedSkybox = null
        transparentPass.destroy()
        skyTimer.destroy()
        skyboxTimer.destroy()
    }

    fun singlePassWithSky(drawSky: Boolean) {
        drawCallId = 0
        var hasDrawnSky = !drawSky
        for (i in stages.indices) {
            val stage = stages[i]
            if (i > 0 && !hasDrawnSky) {
                drawSky()
                hasDrawnSky = true
            }
            if (!stage.isEmpty()) {
                stage.bindDraw(this)
            }
        }
        if (!hasDrawnSky) {
            drawSky()
        }
    }

    fun singlePassWithoutSky() {
        drawCallId = 0
        defaultStage.bind {
            for (i in stages.indices) {
                val stage = stages[i]
                if (!stage.isEmpty()) {
                    stage.draw(this)
                }
            }
        }
    }

    fun drawSky() {
        DrawSky.drawSky(this)
    }

    fun clear() {
        lightStage.clear()
        defaultStage.clear()
        planarReflections.clear()
        lights.fill(null)
        clickIdToBounds.clear()
        for (stageIndex in stages.indices) {
            stages[stageIndex].clear()
        }
    }

    val clickIdToBounds = ClickIdBoundsArray()

    fun fill(rootElement: Entity) {
        // todo reuse the pipeline state for multiple frames
        //  - add a margin, so entities at the screen border can stay visible
        //  - partially populate the pipeline?
        subFill(rootElement)
    }

    fun fill(root: PrefabSaveable) {
        if (root !is Renderable) {
            warnUnknownRenderable(root)
            return
        }

        root.fill(this, sampleEntity.transform)
        if (root is LightComponent) {
            // make lights visible by giving them sth to shine on
            samplePlaneTransform.teleportUpdate()
            addMesh(samplePlane.getMesh()!!, samplePlane, samplePlaneTransform)
        }
        if (root == Systems.world) {
            Systems.forAllSystems(Renderable::class) { system ->
                system.fill(this, sampleEntity.transform)
            }
        }
    }

    private fun warnUnknownRenderable(root: PrefabSaveable) {
        LOGGER.warn(
            "Don't know how to render ${root.className}, " +
                    "please implement me.anno.ecs.interfaces.Renderable, if it is supposed to be renderable"
        )
    }

    val lights = ArrayList<LightRequest?>(RenderView.MAX_FORWARD_LIGHTS)

    init {
        for (i in 0 until RenderView.MAX_FORWARD_LIGHTS) {
            lights.add(null)
        }
    }

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
    fun getClosestRelevantNLights(region: AABBd, numberOfLights: Int, lights: ArrayList<LightRequest?>): Int {
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
                lights.subList(0, size).sortWith { a, b ->
                    val va = a!!.light
                    val vb = b!!.light
                    va.hasShadow.compareTo(vb.hasShadow)
                        .ifSame(va.lightType.shadowMapType.compareTo(vb.lightType.shadowMapType))
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
            lights.subList(0, smallest.size).sortWith { a, b ->
                val va = a!!.light
                val vb = b!!.light
                va.hasShadow.compareTo(vb.hasShadow)
                    .ifSame(va.lightType.shadowMapType.compareTo(vb.lightType.shadowMapType))
            }
            return smallest.size
        }
    }

    private fun subFill(entity0: Entity) {
        // use a list instead of the stack to beautify stack traces a little
        Recursion.processRecursive(entity0) { entity, remaining ->
            entity.forAllComponents(Renderable::class, false) { component ->
                if (component !== ignoredComponent) {
                    if (isVisible(component)) {
                        component.fill(this, entity.transform)
                    }
                }
            }
            entity.forAllChildren(false) { child ->
                if (child !== ignoredEntity && child.isEnabled && frustum.isVisible(child.getBounds())) {
                    remaining.add(child)
                }
            }
        }
    }

    fun traverse(world: PrefabSaveable, callback: (Entity) -> Unit) {
        if (world is Entity) Recursion.processRecursive(world) { entity, remaining ->
            if (entity !== ignoredEntity && entity.isEnabled && frustum.isVisible(entity.getBounds())) {
                callback(entity)
                remaining.addAll(entity.children)
            }
        }
    }

    fun findDrawnSubject(searchedId: Int, world: PrefabSaveable): Any? {
        return Recursion.findRecursive(world) { instance, remaining ->
            if (instance is Component && instance.clickId == searchedId) instance
            else if (isVisible(instance)) {
                for (childType in instance.listChildTypes()) {
                    val children = instance.getChildListByType(childType)
                    remaining.addAll(children)
                }
                null
            } else null
        }
    }

    fun isVisible(instance: Any?): Boolean {
        val bounds = when (instance) {
            is MeshComponentBase -> instance.globalAABB
            is MeshSpawner -> instance.globalAABB
            is Entity -> instance.getBounds()
            else -> return true
        }
        return frustum.isVisible(bounds)
    }

    companion object {

        private val LOGGER = LogManager.getLogger(Pipeline::class)

        private val samplePlane = MeshComponent(
            DefaultAssets.plane,
            Material().apply {
                metallicMinMax.set(1f, 0f)
                roughnessMinMax.set(0.1f, 1f)
                metallicMap = TextureLib.chess8x8Texture.ref
                roughnessMap = metallicMap
                linearFiltering = false
            })

        private val samplePlaneTransform = Transform()
            .setLocalPosition(0.0, 0.0, -0.5)
            .setLocalEulerAngle(PI * 0.5, 0.0, 0.0)

        val sampleEntity = Entity()
        val sampleMeshComponent = MeshComponent()
        val sampleMesh = SimpleMesh.sphereMesh
    }
}
