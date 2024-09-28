package me.anno.ecs.components.mesh

import me.anno.Build
import me.anno.ecs.Transform
import me.anno.ecs.annotations.DebugProperty
import me.anno.ecs.annotations.Docs
import me.anno.ecs.components.collider.CollidingComponent
import me.anno.ecs.components.mesh.material.Material
import me.anno.ecs.components.mesh.material.Materials
import me.anno.ecs.components.mesh.material.utils.TypeValue
import me.anno.ecs.interfaces.Renderable
import me.anno.engine.raycast.RayQuery
import me.anno.engine.raycast.Raycast.TRIANGLES
import me.anno.engine.raycast.RaycastMesh
import me.anno.engine.serialization.NotSerializedProperty
import me.anno.gpu.buffer.StaticBuffer
import me.anno.gpu.pipeline.InstancedI32Stack
import me.anno.gpu.pipeline.InstancedStack
import me.anno.gpu.pipeline.InstancedStaticStack
import me.anno.gpu.pipeline.InstancedTRSStack
import me.anno.gpu.pipeline.Pipeline
import me.anno.utils.structures.arrays.FloatArrayList
import me.anno.utils.structures.lists.Lists.firstOrNull2
import me.anno.utils.structures.maps.KeyTripleMap
import org.apache.logging.log4j.LogManager
import org.joml.AABBd
import org.joml.Matrix4x3d
import kotlin.reflect.KClass
import kotlin.reflect.full.createInstance

@Docs("Displays many meshes at once without Entities; can be used for particle systems and such")
abstract class MeshSpawner : CollidingComponent(), Renderable {

    @NotSerializedProperty
    val transforms = ArrayList<Transform>(32)

    /**
     * calls instancedGroupFill, instancedTRSFill, instancedMeshGroupFill, instancedFill,
     * until the first one of them returns true;
     * if you need different behaviour, just override this method :)
     * */
    override fun fill(pipeline: Pipeline, transform: Transform, clickId: Int): Int {
        this.clickId = clickId
        instancedGroupFill(pipeline) ||
                instancedTRSFill(pipeline) ||
                instancedMeshGroupFill(pipeline) ||
                instancedFill(pipeline)
        return clickId + 1
    }

    fun instancedFill(pipeline: Pipeline): Boolean {
        forEachMesh { mesh, materialOverride, transform ->

            transform.validate()

            // check visibility: first transform bounds into global space, then test them
            mesh.getBounds().transformUnion(transform.globalTransform, tmpAABB)
            if (pipeline.frustum.contains(tmpAABB)) {
                for (matIndex in 0 until mesh.numMaterials) {
                    val material0 = materialOverride ?: Materials.getMaterial(null, mesh.materials, matIndex)
                    val material = Materials.getMaterial(pipeline.superMaterial, material0)
                    val stage = pipeline.findStage(material)
                    if (mesh.proceduralLength <= 0) {
                        val stack = stage.instanced.data.getStack(mesh, material, matIndex)
                        stage.addToStack(stack, this, transform)
                    } else {
                        if (Build.isDebug && mesh.numMaterials > 1) {
                            LOGGER.warn("Procedural meshes cannot support multiple materials (in MeshSpawner)")
                        }
                        stage.add(this, mesh, transform, material, matIndex)
                    }
                }
            }
            false
        }
        return true
    }

    fun instancedMeshGroupFill(pipeline: Pipeline): Boolean {
        lastStack = null
        val result = forEachMeshGroup { mesh, material ->
            val material2 = material ?: Material.defaultMaterial
            val stage = pipeline.findStage(material2)
            val stack = stage.instanced.data.getStack(mesh, material2, 0)
            validateLastStack()
            lastStack = stack
            lastStackIndex = stack.size
            stack
        }
        validateLastStack()
        return result
    }

    fun instancedTRSFill(pipeline: Pipeline): Boolean {
        return forEachMeshGroupTRS { mesh, material ->
            val material2 = material ?: Material.defaultMaterial
            val stage = pipeline.findStage(material2)
            val stack = stage.instancedTRS.data.getOrPut(mesh, material2) { _, _ -> InstancedTRSStack.Data() }
            if (stack.gfxIds.isEmpty() || stack.gfxIds.last() != gfxId) {
                stack.gfxIds.add(stack.size)
                stack.gfxIds.add(gfxId)
            }
            stack.posSizeRot
        }
    }

    fun instancedGroupFill(pipeline: Pipeline): Boolean {
        return forEachInstancedGroup { mesh, material, group, overrides ->
            val material2 = material ?: Material.defaultMaterial
            val stage = pipeline.findStage(material2)
            val stack = stage.instancedStatic.data.getOrPut(mesh, material2) { _, _ -> InstancedStaticStack.Data() }
            stack.data.add(group)
            stack.attr.add(overrides)
            stack.clickIds.add(this.clickId)
        }
    }

    private var lastStack: InstancedStack? = null
    private var lastStackIndex = 0

    /**
     * helper function for forEachMeshGroup
     * */
    private fun validateLastStack() {
        val ls = lastStack
        if (ls != null) {
            for (j in lastStackIndex until ls.size) {
                val transform = ls.transforms[j] as Transform
                transform.validate()
            }
        }
        lastStack = null
    }

    private val tmpAABB = AABBd()

    fun getTransform(i: Int): Transform {
        ensureTransforms(i + 1)
        return transforms[i]
    }

    fun ensureTransforms(count: Int) {
        val entity = entity
        for (i in transforms.size until count) {
            val tr = Transform()
            tr.parentEntity = entity
            transforms.add(tr)
        }
    }

    override fun hasRaycastType(typeMask: Int) = (typeMask and TRIANGLES) != 0

    override fun raycastClosestHit(query: RayQuery): Boolean {
        var hit = false
        forEachMesh { mesh, _, transform ->
            if (mesh is Mesh && RaycastMesh.raycastGlobalMeshAnyHit(query, transform, mesh)) {
                query.result.mesh = mesh
                hit = true
            }
            false
        }
        return hit
    }

    override fun raycastAnyHit(query: RayQuery): Boolean {
        var hitSth = false
        forEachMesh { mesh, _, transform ->
            if (mesh is Mesh && RaycastMesh.raycastGlobalMeshAnyHit(query, transform, mesh)) {
                query.result.mesh = mesh
                hitSth = true
            }
            hitSth
        }
        return hitSth
    }

    @DebugProperty
    @NotSerializedProperty
    val localAABB = AABBd()

    @DebugProperty
    @NotSerializedProperty
    val globalAABB = AABBd()

    override fun fillSpace(globalTransform: Matrix4x3d, aabb: AABBd): Boolean {
        // calculate local aabb
        val local = localAABB
        local.clear()
        forEachMesh { mesh, _, transform ->
            transform.validate()
            val lt = transform.localTransform
            mesh.getBounds().transformUnion(lt, local, local)
            false
        }

        // calculate global aabb
        val global = globalAABB
        local.transform(globalTransform, global)

        // add the result to the output
        aabb.union(global)

        // yes, we calculated stuff
        return true
    }

    /**
     * iterates over each mesh, which is actively visible; caller shall call transform.validate() if he needs the transform;
     * will (probably) stop, if you return true
     * */
    abstract fun forEachMesh(run: (IMesh, Material?, Transform) -> Boolean)

    /**
     * iterates over each mesh group, which is actively visible; caller shall call transform.validate();
     * if this is implemented, return true; and forEachMesh just will be a fallback
     *
     * useful, if there are thousands of pre-grouped meshes with the same material; reduced overhead
     * */
    open fun forEachMeshGroup(run: (IMesh, Material?) -> InstancedStack) = false

    /**
     * iterates over each mesh group, which is actively visible;
     * if this is implemented, return true; and forEachMeshGroup just will be a fallback;
     *
     * each element must be added as position (translation; x,y,z), scale (1d), rotation (quaternion; x,y,z,w)
     *
     * useful, if there are thousands of pre-grouped meshes with the same material; and just P+R+S, no shearing, only uniform scaling; reduced overhead
     * */
    open fun forEachMeshGroupTRS(run: (IMesh, Material?) -> FloatArrayList) = false

    /**
     * iterates over each mesh group, which is actively visible; caller shall call transform.validate();
     * if this is implemented, return true; and forEachMeshGroupTRS just will be a fallback;
     *
     * each element must be added as StaticBuffer, and attribute map like Material.shaderOverrides,
     * which then must be interpreted in the Material.shader
     *
     * useful, if you want thousands of instanced meshes, and they don't change their transform dynamically or infrequent;
     * reduced overhead, and potentially reduced memory bandwidth;
     *
     * this is like forEachMeshGroupI32, just generalized
     * */
    open fun forEachInstancedGroup(run: (IMesh, Material?, StaticBuffer, Map<String, TypeValue>) -> Unit) = false

    fun <V : InstancedI32Stack> getOrPutI32Stack(
        pipeline: Pipeline,
        mesh: IMesh,
        material: Material,
        clazz: KClass<V>,
        createInstance: () -> V = { clazz.createInstance() }
    ): InstancedI32Stack.Data {
        val stage = pipeline.findStage(material)
        var list = stage.instances.firstOrNull2 { clazz.isInstance(it) } as? InstancedI32Stack
        if (list == null) {
            list = createInstance()
            stage.instances.add(list)
        }
        return list.data.getOrPut(mesh, material) { _, _ -> InstancedI32Stack.Data() }
    }

    companion object {
        private val LOGGER = LogManager.getLogger(MeshSpawner::class)

        fun KeyTripleMap<IMesh, Material, Int, InstancedStack>.getStack(
            mesh: IMesh, material: Material, matIndex: Int
        ): InstancedStack {
            return getOrPut(mesh, material, matIndex) { meshI, _, _ -> createStack(meshI.hasBonesInBuffer) }
        }

        fun createStack(animated: Boolean): InstancedStack {
            return if (animated) {
                InstancedStack.newAnimStack()
            } else {
                InstancedStack.newInstStack()
            }
        }
    }
}