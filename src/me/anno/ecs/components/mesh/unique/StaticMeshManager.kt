package me.anno.ecs.components.mesh.unique

import me.anno.Time
import me.anno.ecs.Component
import me.anno.ecs.Entity
import me.anno.ecs.EntityQuery.forAllComponents
import me.anno.ecs.Transform
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.ecs.components.mesh.MeshComponentBase
import me.anno.ecs.components.mesh.material.Material
import me.anno.ecs.interfaces.Renderable
import me.anno.ecs.systems.OnUpdate
import me.anno.gpu.buffer.Attribute
import me.anno.gpu.buffer.AttributeType
import me.anno.gpu.buffer.DrawMode
import me.anno.gpu.pipeline.Pipeline
import me.anno.gpu.pipeline.Pipeline.Companion.getMaterial

// todo collect all transforms/meshes that have been idle for X iterations
// todo make this like Physics, because it's kind of a global system...
// todo we need to do effective culling, so somehow add bounds metadata, and do GPU culling
//  - min, max,                      |  6
//  - index of data? or start+length |  2
//  - format of data??? -> flags     |  1
//  - transform                      | 12

class StaticMeshManager : Component(), Renderable, OnUpdate {

    val managers = HashMap<Material, UniqueMeshRenderer<Mesh, SMMKey>>()
    val meshes = HashSet<MeshComponent>(1024)

    override fun fill(pipeline: Pipeline, transform: Transform, clickId: Int): Int {
        this.clickId = clickId
        for ((_, manager) in managers) {
            manager.fill(pipeline, transform, clickId)
        }
        return clickId + 1
    }

    var numIdleFrames = 3
    var scanLimit = 5000
    private var fullScan = true
    private val collectStackE = ArrayList<Entity>()

    override fun onUpdate() {
        // this entity must only operate on Root level
        if (entity?.parentEntity != null) return
        // todo regularly check whether all transforms are still static
        //  do this more spread out: respect scanLimit
        collect()
    }

    fun collect() {
        if (collectStackE.isEmpty()) {
            collectStackE.add(entity ?: return)
        }
        val limit = if (fullScan) Int.MAX_VALUE else scanLimit
        val time = Time.frameIndex + numIdleFrames
        fullScan = false
        for (i in 0 until limit) {
            val idx = collectStackE.size - 1
            if (idx < 0) break
            val entity = collectStackE.removeAt(idx)
            val transform = entity.transform
            if (transform.lastUpdateFrameIndex <= time) {
                collectStackE.addAll(entity.children)
                entity.forAllComponents(MeshComponentBase::class) { comp ->
                    if (comp.manager == null) {
                        val mesh = comp.getMesh()
                        if (mesh is Mesh && supportsMesh(mesh)) {
                            register(comp, mesh)
                        }
                    }
                }
            }
        }
    }

    fun supportsMesh(mesh: Mesh): Boolean {
        return !mesh.hasBones && supportsDrawMode(mesh.drawMode) &&
                mesh.proceduralLength <= 0 && mesh.positions != null
    }

    fun supportsDrawMode(drawMode: DrawMode): Boolean {
        return when (drawMode) {
            DrawMode.TRIANGLES -> true
            else -> false
        }
    }

    fun register(comp: MeshComponentBase, mesh: Mesh) {
        for (i in 0 until mesh.numMaterials) {
            val material = getMaterial(comp.materials, mesh.materials, i)
            val umr = managers.getOrPut(material) { SMMMeshRenderer(material) }
            val key = SMMKey(comp, mesh, i)
            val buffer = umr.getData(key, mesh)
            if (buffer != null) {
                umr.add(key, MeshEntry(mesh, mesh.getBounds(), buffer))
            }
        }
        comp.manager = this
    }

    fun unregister(comp: MeshComponentBase) {
        if (meshes.remove(comp)) {
            comp.manager = null
            val mesh = comp.getMesh()
            if (mesh is Mesh) {
                for (i in 0 until mesh.numMaterials) {
                    val material = getMaterial(comp.materials, mesh.materials, i)
                    managers[material]?.remove(SMMKey(comp, mesh, i), true)
                }
            }
        }
    }

    companion object {
        val attributes = listOf(
            // total size: 32 bytes
            Attribute("coords", 3),
            Attribute("normals", AttributeType.SINT8_NORM, 4),
            Attribute("uvs", 2),
            Attribute("tangents", AttributeType.SINT8_NORM, 4),
            Attribute("colors0", AttributeType.UINT8_NORM, 4),
        )
    }
}