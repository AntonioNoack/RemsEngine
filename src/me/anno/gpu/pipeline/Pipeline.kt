package me.anno.gpu.pipeline

import me.anno.Engine
import me.anno.ecs.Component
import me.anno.ecs.Entity
import me.anno.ecs.Transform
import me.anno.ecs.annotations.Type
import me.anno.ecs.components.anim.Animation
import me.anno.ecs.components.anim.Skeleton
import me.anno.ecs.components.cache.MaterialCache
import me.anno.ecs.components.cache.SkeletonCache
import me.anno.ecs.components.light.AmbientLight
import me.anno.ecs.components.light.LightComponent
import me.anno.ecs.components.light.PlanarReflection
import me.anno.ecs.components.mesh.*
import me.anno.ecs.components.mesh.Mesh.Companion.defaultMaterial
import me.anno.ecs.components.mesh.sdf.SDFComponent
import me.anno.ecs.components.mesh.sdf.SDFGroup
import me.anno.ecs.components.mesh.shapes.Icosahedron
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.engine.ui.render.Frustum
import me.anno.engine.ui.render.RenderState
import me.anno.engine.ui.render.RenderView
import me.anno.gpu.GFX
import me.anno.gpu.deferred.DeferredSettingsV2
import me.anno.gpu.pipeline.M4x3Delta.set4x3delta
import me.anno.gpu.texture.Texture2D
import me.anno.io.ISaveable
import me.anno.io.Saveable
import me.anno.io.base.BaseWriter
import me.anno.io.files.thumbs.Thumbs.threadLocalBoneMatrices
import me.anno.io.serialization.SerializedProperty
import me.anno.utils.pooling.JomlPools
import me.anno.utils.sorting.MergeSort.mergeSort
import me.anno.utils.structures.Compare.ifSame
import me.anno.utils.structures.lists.SmallestKList
import me.anno.utils.types.Matrices.distanceSquared
import org.apache.logging.log4j.LogManager
import org.joml.AABBd
import org.joml.Vector3d
import org.joml.Vector3f
import org.joml.Vector4d
import kotlin.math.min

/**
 * collects meshes for sorting (transparency, overdraw), and for instanced rendering
 * todo instead of a pipeline, maybe the best would be a render graph...
 * */
class Pipeline(val deferred: DeferredSettingsV2) : Saveable() {

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

    val lightPseudoStage = LightPipelineStage(deferred)

    lateinit var defaultStage: PipelineStage

    var lastClickId = 0

    val frustum = Frustum()

    val ambient = Vector3f()

    val planarReflections = ArrayList<PlanarReflection>()

    // vec4(pos,1).dot(prp) > 0.0 -> discard
    // therefore use 0,0,0,0 to disable this plane
    val reflectionCullingPlane = Vector4d()

    var applyToneMapping = true

    fun disableReflectionCullingPlane() {
        reflectionCullingPlane.set(0.0)
    }

    fun hasTooManyLights(): Boolean {
        return lightPseudoStage.size > RenderView.MAX_FORWARD_LIGHTS
    }

    private fun getDefaultStage(mesh: Mesh, material: Material?): PipelineStage {
        // todo analyse, whether the material has transparency, and if so,
        // todo add it to the transparent pass
        return defaultStage
    }

    private fun addMesh(mesh: Mesh, renderer: MeshComponentBase, entity: Entity, clickId: Int) {
        mesh.ensureBuffer()
        val materials = mesh.materials
        val materialOverrides = renderer.materials
        for (index in 0 until mesh.numMaterials) {
            val m0 = materialOverrides.getOrNull(index)?.nullIfUndefined()
            val m1 = m0 ?: materials.getOrNull(index)
            val material = MaterialCache[m1, defaultMaterial]
            val stage = material.pipelineStage ?: getDefaultStage(mesh, material)
            stage.add(renderer, mesh, entity, index, clickId)
        }
    }

    private fun addMeshDepth(mesh: Mesh, renderer: MeshComponentBase, entity: Entity) {
        defaultStage.add(renderer, mesh, entity, 0, 0)
    }

    private fun addMeshInstanced(mesh: Mesh, renderer: MeshComponentBase, entity: Entity, clickId: Int) {
        mesh.ensureBuffer()
        val materials = mesh.materials
        val materialOverrides = renderer.materials
        for (index in 0 until mesh.numMaterials) {
            val m0 = materialOverrides.getOrNull(index)?.nullIfUndefined()
            val m1 = m0 ?: materials.getOrNull(index)
            val material = MaterialCache[m1, defaultMaterial]
            val stage = material.pipelineStage ?: defaultStage
            stage.addInstanced(mesh, renderer, entity, material, index, clickId)
        }
    }

    private fun addMeshInstancedDepth(
        mesh: Mesh,
        component: MeshComponentBase,
        entity: Entity,
        material: Material,
        materialIndex: Int
    ) {
        defaultStage.addInstanced(mesh, component, entity, material, materialIndex, 0)
    }

    private fun addLight(light: LightComponent, entity: Entity, cameraPosition: Vector3d, worldScale: Double) {
        // for debugging of the light shapes
        // addMesh(light.getLightPrimitive(), MeshRenderer(), entity, 0)
        val stage = lightPseudoStage
        // update light transform
        // its drawn position probably should be smoothed -> we probably should use the drawnMatrix instead of the global one
        // we may want to set a timestamp, so we don't update it twice? no, we fill the pipeline only once
        val invWorldTransform = light.invWorldMatrix
        val drawTransform = entity.transform.getDrawMatrix()
        invWorldTransform
            .set4x3delta(drawTransform, cameraPosition, worldScale)
            .invert()
        stage.add(light, entity)
    }

    fun draw() {
        for (stage in stages) {
            stage.bindDraw(this)
        }
    }

    /**
     * drawing only the depth of a scene;
     * for light-shadows or pre-depth
     * */
    fun drawDepth() {
        GFX.check()
        defaultStage.drawDepths(this)
        GFX.check()
    }

    fun clear() {
        ambient.set(0f)
        lightPseudoStage.clear()
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

    fun fill(rootElement: Entity, cameraPosition: Vector3d, worldScale: Double) {
        // todo more complex traversal:
        // done exclude static entities by their AABB
        // done exclude entities, if they contain no meshes
        // done exclude entities, if they are off-screen
        // todo reuse the pipeline state for multiple frames
        //  - add a margin, so entities at the screen border can stay visible
        //  - partially populate the pipeline?
        rootElement.validateAABBs()
        lastClickId = subFill(rootElement, lastClickId, cameraPosition, worldScale)
        // LOGGER.debug("$contained/$nonContained")
    }

    fun fill(root: PrefabSaveable, cameraPosition: Vector3d, worldScale: Double) {
        val clickId = lastClickId
        when (root) {
            is Entity -> fill(root, cameraPosition, worldScale)
            is MeshComponentBase -> {
                root.clickId = clickId
                val mesh = root.getMesh()
                if (mesh != null) addMesh(mesh, root, sampleEntity, root.gfxId)
            }
            is Mesh -> addMesh(root, sampleMeshComponent, sampleEntity, clickId)
            is Material -> {
                val mesh = sampleMesh
                val stage = root.pipelineStage ?: getDefaultStage(mesh, root)
                val materialSource = root.prefab!!.source // should be defined
                if (!materialSource.exists) throw IllegalArgumentException("Material must have source")
                mesh.material = materialSource
                stage.add(sampleMeshComponent, mesh, sampleEntity, 0, clickId)
            }
            is LightComponent -> {
                // todo add floor, so we can see the light?
                addLight(root, root.entity ?: sampleEntity, cameraPosition, worldScale)
            }
            is Animation -> {
                // todo optimize this (avoid allocations)
                // todo use AnimRenderer for motion vectors
                val skeleton = SkeletonCache[root.skeleton] ?: return
                val bones = skeleton.bones
                val mesh = Mesh()
                val (skinningMatrices, animPositions) = threadLocalBoneMatrices.get()
                val size = (bones.size - 1) * Skeleton.boneMeshVertices.size
                mesh.positions = Texture2D.floatArrayPool[size, false, true]
                mesh.normals = Texture2D.floatArrayPool[size, true, true]
                val time = Engine.gameTimeF % root.duration
                // generate the matrices
                root.getMatrices(null, time, skinningMatrices)
                // apply the matrices to the bone positions
                for (i in 0 until min(animPositions.size, bones.size)) {
                    val position = animPositions[i].set(bones[i].bindPosition)
                    skinningMatrices[i].transformPosition(position)
                }
                Skeleton.generateSkeleton(bones, animPositions, mesh.positions!!, null)
                mesh.invalidateGeometry()
                fill(mesh, cameraPosition, worldScale)
                GFX.addGPUTask("free", 1) {
                    Texture2D.floatArrayPool.returnBuffer(mesh.positions)
                    Texture2D.floatArrayPool.returnBuffer(mesh.normals)
                    mesh.destroy()
                }
            }
            is Skeleton -> {
                // todo optimize this (avoid allocations)
                val bones = root.bones
                if (bones.isEmpty()) return
                val mesh = Mesh()
                // in a tree with N nodes, there is N-1 lines
                val size = (bones.size - 1) * Skeleton.boneMeshVertices.size
                mesh.positions = Texture2D.floatArrayPool[size, false, true]
                mesh.normals = Texture2D.floatArrayPool[size, true, true]
                val bonePositions = Array(bones.size) { bones[it].bindPosition }
                Skeleton.generateSkeleton(bones, bonePositions, mesh.positions!!, null)
                fill(mesh, cameraPosition, worldScale)
                GFX.addGPUTask("free", 1) {
                    Texture2D.floatArrayPool.returnBuffer(mesh.positions)
                    Texture2D.floatArrayPool.returnBuffer(mesh.normals)
                    mesh.destroy()
                }
            }
            else -> {
                LOGGER.warn("Don't know how to draw ${root.className}")
            }
        }
    }

    fun fillDepth(rootElement: Entity, cameraPosition: Vector3d, worldScale: Double) {
        rootElement.validateAABBs()
        subFillDepth(rootElement, cameraPosition, worldScale)
    }

    // todo fix deferred rendering for scenes with many lights

    val lights = arrayOfNulls<LightRequest<*>>(RenderView.MAX_FORWARD_LIGHTS)

    val center = Vector3d()
    private val lightList = SmallestKList<LightRequest<*>>(16) { a, b ->
        // todo also use the size, and relative size to the camera
        val at = a.transform.getDrawMatrix()
        val bt = b.transform.getDrawMatrix()
        val cam = RenderState.cameraPosition
        val scale = JomlPools.vec3d.borrow()
        val da = (at.distanceSquared(center) + at.distanceSquared(cam)) * bt.getScale(scale).lengthSquared()
        val db = (bt.distanceSquared(center) + bt.distanceSquared(cam)) * at.getScale(scale).lengthSquared()
        da.compareTo(db)
    }

    /**
     * creates a list of relevant lights for a forward-rendering draw call of a mesh or region
     * */
    fun getClosestRelevantNLights(region: AABBd, numberOfLights: Int, lights: Array<LightRequest<*>?>): Int {
        val lightStage = lightPseudoStage
        if (numberOfLights <= 0) return 0
        val size = lightStage.size
        if (size < numberOfLights) {
            // check if already filled:
            if (lights[0] == null) {
                for (i in 0 until size) {
                    lights[i] = lightStage[i]
                }
                // sort by type, and whether they have a shadow
                mergeSort(lights, 0, size) { a, b ->
                    val va = a!!.light
                    val vb = b!!.light
                    va.hasShadow.compareTo(vb.hasShadow).ifSame {
                        va.lightType.shadowMapType.compareTo(vb.lightType.shadowMapType)
                    }
                }
            }// else done
            return size
        } else {
            val center = center.set(region.avgX(), region.avgY(), region.avgZ())
            if (!center.isFinite) center.set(0.0)
            lightList.clear()
            lightPseudoStage.listOfAll(lightList)
            val smallest = lightList
            for (i in 0 until smallest.size) {
                lights[i] = smallest[i]
            }
            // sort by type, and whether they have a shadow
            mergeSort(lights, 0, smallest.size) { a, b ->
                val va = a!!.light
                val vb = b!!.light
                va.hasShadow.compareTo(vb.hasShadow).ifSame {
                    va.lightType.shadowMapType.compareTo(vb.lightType.shadowMapType)
                }
            }
            return smallest.size
        }
    }

    private fun assignClickIds(sdf: SDFGroup, clickId0: Int): Int {
        var clickId = clickId0
        val children = sdf.children
        for (i in children.indices) {
            val child = children[i]
            if (child.isEnabled) {
                child.clickId = clickId++
                if (child is SDFGroup) {
                    clickId = assignClickIds(child, clickId)
                }
            }
        }
        return clickId
    }

    private fun subFill(entity: Entity, clickId0: Int, cameraPosition: Vector3d, worldScale: Double): Int {
        entity.hasBeenVisible = true
        var clickId = clickId0
        val components = entity.components
        for (i in components.indices) {
            val component = components[i]
            if (component.isEnabled && component !== ignoredComponent) {
                when (component) {
                    is SDFComponent -> {
                        val mesh = component.getMesh()
                        component.clickId = clickId
                        if (component.isInstanced && mesh.proceduralLength <= 0) {
                            addMeshInstanced(mesh, component, entity, clickId)
                        } else {
                            addMesh(mesh, component, entity, component.gfxId)
                        }
                        clickId++
                        if (component is SDFGroup) {
                            clickId = assignClickIds(component, clickId)
                        }
                    }
                    is MeshComponentBase -> {
                        val mesh = component.getMesh()
                        if (mesh != null) {
                            component.clickId = clickId
                            if (component.isInstanced && mesh.proceduralLength <= 0) {
                                addMeshInstanced(mesh, component, entity, clickId)
                            } else {
                                addMesh(mesh, component, entity, component.gfxId)
                            }
                            clickId++
                        }
                    }
                    is MeshSpawner -> {
                        component.clickId = clickId
                        lastClickId0 = clickId
                        tmpComponent = component
                        component.forEachMesh(::subFill1)
                        clickId++
                    }
                    is LightComponent -> addLight(component, entity, cameraPosition, worldScale)
                    is AmbientLight -> ambient.add(component.color)
                    is PlanarReflection -> planarReflections.add(component)
                }
            }
        }
        val children = entity.children
        for (i in children.indices) {
            val child = children[i]
            if (child !== ignoredEntity && child.isEnabled && frustum.isVisible(child.aabb)) {
                clickId = subFill(child, clickId, cameraPosition, worldScale)
            }
        }
        return clickId
    }

    private var tmpComponent: MeshSpawner? = null
    private var lastMesh: Mesh? = null
    private var lastMat: Material? = null
    private var lastStage: PipelineStage? = null
    private var lastStack: InstancedStack? = null
    private var lastClickId0 = 0


    private val tmpAABB = AABBd()
    fun subFill1(mesh: Mesh, material: Material?, transform: Transform) {

        mesh.ensureBuffer()

        // check visibility
        val bounds = tmpAABB
        bounds.set(mesh.aabb)
        bounds.transform(transform.globalTransform)
        if (!frustum.contains(bounds)) return

        // quick path for lots of repeating meshes
        if (mesh === lastMesh && material === lastMat) {
            lastStack!!.add(transform, lastClickId0)
            lastStage!!.instancedSize++
            return
        }

        if (mesh.proceduralLength <= 0) {

            val material2 = material ?: defaultMaterial
            val stage = material2.pipelineStage ?: defaultStage
            val stack = stage.instancedMeshes2.getOrPut(mesh, material2) { mesh1, _ ->
                if (mesh1.hasBones) InstancedAnimStack() else InstancedStack()
            }

            val clickId = lastClickId0
            stage.addToStack(stack, null, transform, clickId)

            // cache
            lastMesh = mesh
            lastMat = material
            lastStage = stage
            lastStack = stack

        } else {
            if (mesh.numMaterials > 1) {
                LOGGER.warn("Procedural meshes in MeshSpawner cannot support multiple materials")
            }
            val material2 = material ?: defaultMaterial
            val stage = material2.pipelineStage ?: defaultStage
            val component = tmpComponent!!
            val clickId = lastClickId0
            stage.add(component, mesh, component.entity!!, 0, clickId)
        }
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

    private fun subFillDepth(entity: Entity, cameraPosition: Vector3d, worldScale: Double) {
        entity.hasBeenVisible = true
        val components = entity.components
        for (i in components.indices) {
            val component = components[i]
            if (component.isEnabled && component !== ignoredComponent) {
                if (component is MeshComponentBase && component.castShadows) {
                    val mesh = component.getMesh()
                    if (mesh != null) {
                        if (component.isInstanced) {
                            for (materialIndex in 0 until mesh.numMaterials) {
                                val material = MaterialCache[component.materials.getOrNull(materialIndex)
                                    ?: mesh.materials.getOrNull(materialIndex), defaultMaterial]
                                addMeshInstancedDepth(mesh, component, entity, material, materialIndex)
                            }
                        } else {
                            addMeshDepth(mesh, component, entity)
                        }
                    }
                }
            }
        }
        val children = entity.children
        for (i in children.indices) {
            val child = children[i]
            if (child !== ignoredEntity && child.isEnabled && frustum.isVisible(child.aabb)) {
                subFillDepth(child, cameraPosition, worldScale)
            }
        }
    }

    fun findDrawnSubject(searchedId: Int, entity: Entity): Any? {
        // LOGGER.debug("[E] ${entity.clickId.toString(16)} vs ${searchedId.toString(16)}")
        if (entity.clickId == searchedId) return entity
        val components = entity.components
        for (i in components.indices) {
            val c = components[i]
            if (c.isEnabled) {
                // this probably should be more generic...
                if (c is MeshComponentBase) {
                    // LOGGER.debug("[C] ${c.clickId.toString(16)} vs ${searchedId.toString(16)}")
                    if (c.clickId == searchedId) return c
                }
                if (c is SDFGroup) {
                    // also visit all children
                    val found = findDrawnSubject(searchedId, c)
                    if (found != null) return found
                }
            }
        }
        val children = entity.children
        for (i in children.indices) {
            val child = children[i]
            if (child.isEnabled) {
                val aabb = child.aabb
                val needsDrawing = frustum.isVisible(aabb)
                if (needsDrawing) {
                    val found = findDrawnSubject(searchedId, child)
                    if (found != null) return found
                }
            }
        }
        return null
    }

    fun findDrawnSubject(searchedId: Int, group: SDFGroup): Any? {
        val children = group.children
        for (i in children.indices) {
            val child = children[i]
            if (child.isEnabled) {
                // LOGGER.debug("[S] ${child.clickId.toString(16)} vs ${searchedId.toString(16)}")
                if (child.clickId == searchedId) return child
                if (child is SDFGroup) {
                    val found = findDrawnSubject(searchedId, child)
                    if (found != null) return found
                }
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

    override val className: String = "Pipeline"
    override val approxSize: Int = 10
    override fun isDefaultValue(): Boolean = false

    companion object {
        private val LOGGER = LogManager.getLogger(Pipeline::class)
        private val sampleEntity = Entity()
        private val sampleMeshComponent = MeshComponent()
        private val sampleMesh = Icosahedron.createMesh(60, 60)
    }

}
