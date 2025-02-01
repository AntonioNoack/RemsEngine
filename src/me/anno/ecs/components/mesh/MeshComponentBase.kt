package me.anno.ecs.components.mesh

import me.anno.ecs.Entity
import me.anno.ecs.Transform
import me.anno.ecs.annotations.DebugProperty
import me.anno.ecs.annotations.Docs
import me.anno.ecs.annotations.Type
import me.anno.ecs.components.collider.CollidingComponent
import me.anno.ecs.components.mesh.unique.StaticMeshManager
import me.anno.ecs.interfaces.Renderable
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.engine.raycast.RayQuery
import me.anno.engine.raycast.Raycast
import me.anno.engine.raycast.RaycastMesh
import me.anno.engine.serialization.NotSerializedProperty
import me.anno.engine.serialization.SerializedProperty
import me.anno.gpu.pipeline.Pipeline
import me.anno.gpu.query.OcclusionQuery
import me.anno.gpu.shader.Shader
import me.anno.io.files.FileReference
import me.anno.maths.Maths
import org.joml.AABBd
import org.joml.Matrix4x3d

abstract class MeshComponentBase : CollidingComponent(), Renderable {

    /**
     * whether an object will receive shadows from shadow-mapped lights;
     * may be ignored for instanced meshes (to save bandwidth/computations)
     * */
    @SerializedProperty
    var receiveShadows = true

    @SerializedProperty
    var castShadows = true

    // idk... good for large scenes, bad for accurate lighting in forward rendering
    // automatic instancing based on if shader is default??...
    // check what the performance is in the large dungeon sample scene from Synty
    // 20 non-instanced, 3000 draw calls
    // 30 instanced, 300 draw calls
    // -> idk...
    @SerializedProperty
    var isInstanced = true

    @NotSerializedProperty
    var manager: StaticMeshManager? = null

    @Docs("Abstract function for you to define your mesh; you may return null, if you're not yet ready")
    abstract fun getMeshOrNull(): IMesh?

    @Docs("Overrides the mesh materials")
    @Type("List<Material/Reference>")
    @SerializedProperty
    var materials: List<FileReference> = emptyList()

    @Docs("For displaying random triangle colors")
    @NotSerializedProperty
    val randomTriangleId = (Maths.random() * 1e9).toInt()

    @DebugProperty
    @NotSerializedProperty
    val localAABB = AABBd()

    @DebugProperty
    @NotSerializedProperty
    val globalAABB = AABBd()

    open fun getMesh(): IMesh? {
        return getMeshOrNull()
    }

    override fun hasRaycastType(typeMask: Int): Boolean {
        return typeMask.and(Raycast.TRIANGLES) != 0
    }

    override fun raycast(query: RayQuery): Boolean {
        val mesh = getMeshOrNull() as? Mesh
        return if (mesh != null) {
            val wasHit = RaycastMesh.raycastGlobalMesh(query, transform, mesh)
            if (wasHit) {
                query.result.mesh = mesh
                true
            } else false
        } else false
    }

    override fun onChangeStructure(entity: Entity) {
        super.onChangeStructure(entity)
        entity.invalidateCollisionMask()
        getMesh()
    }

    /**
     * set this parameter, if you want this object to be only drawn, if it is really visible;
     * visibility is checked every <everyNthFrame> frames
     *
     * you also can use this option to check whether an option was drawn;
     * if it shall be drawn every frame, but you want check visibility, set <everyNthFrame> to 0
     *
     * only works, if not isInstanced
     * */
    @DebugProperty
    @NotSerializedProperty
    var occlusionQuery: OcclusionQuery? = null

    override fun fill(pipeline: Pipeline, transform: Transform, clickId: Int): Int {
        if (manager != null) return clickId
        val mesh = getMeshOrNull() ?: return clickId
        if (isInstanced && mesh.proceduralLength <= 0) {
            pipeline.addMeshInstanced(mesh, this, transform)
        } else {
            val oc = occlusionQuery
            if (oc == null || oc.wasVisible || oc.frameCounter++ > 0) {
                pipeline.addMesh(mesh, this, transform)
            }
        }
        this.clickId = clickId
        return clickId + 1
    }

    override fun findDrawnSubject(searchedId: Int): Any? {
        return if (clickId == searchedId) this else null
    }

    override fun fillSpace(globalTransform: Matrix4x3d, aabb: AABBd): Boolean {
        val mesh = getMesh()
        if (mesh != null) {
            fillSpaceSet(mesh, globalTransform, aabb)
        }
        return true
    }

    fun fillSpaceStart() {
        localAABB.clear()
    }

    fun fillSpaceAdd(mesh: IMesh) {
        localAABB.union(mesh.getBounds())
    }

    fun fillSpaceEnd(globalTransform: Matrix4x3d?, aabb: AABBd) {
        if (globalTransform != null) localAABB.transform(globalTransform, globalAABB)
        else globalAABB.set(localAABB)
        aabb.union(globalAABB)
    }

    fun fillSpaceSet(mesh: IMesh, globalTransform: Matrix4x3d?, aabb: AABBd) {
        // fillSpaceStart()
        // fillSpaceAdd(mesh)
        localAABB.set(mesh.getBounds()) // optimization
        fillSpaceEnd(globalTransform, aabb)
    }

    fun fillSpace(mesh: IMesh, transform: Matrix4x3d?, dst: AABBd) {
        // add aabb of that mesh with the transform
        val bounds = mesh.getBounds()
        if (transform != null) bounds.transformUnion(transform, dst, dst)
        else dst.union(bounds)
    }

    fun fillSpace(bounds: AABBd, transform: Matrix4x3d?, dst: AABBd) {
        // add aabb of that mesh with the transform
        if (transform != null) bounds.transformUnion(transform, dst, dst)
        else dst.union(bounds)
    }

    open fun hasAnimation(async: Boolean): Boolean = false

    open fun defineVertexTransform(shader: Shader, transform: Transform, mesh: IMesh): Boolean {
        return false
    }

    fun draw(pipeline: Pipeline, shader: Shader, materialIndex: Int) {
        getMeshOrNull()?.draw(pipeline, shader, materialIndex, Mesh.drawDebugLines)
    }

    override fun copyInto(dst: PrefabSaveable) {
        super.copyInto(dst)
        if (dst !is MeshComponentBase) return
        dst.materials = materials
        dst.castShadows = castShadows
        dst.receiveShadows = receiveShadows
        dst.isInstanced = isInstanced
    }
}