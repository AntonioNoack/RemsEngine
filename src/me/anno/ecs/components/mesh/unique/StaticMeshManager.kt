package me.anno.ecs.components.mesh.unique

import me.anno.Time
import me.anno.ecs.Component
import me.anno.ecs.Entity
import me.anno.ecs.Transform
import me.anno.ecs.components.mesh.*
import me.anno.ecs.components.mesh.Material.Companion.defaultMaterial
import me.anno.ecs.interfaces.Renderable
import me.anno.gpu.buffer.Attribute
import me.anno.gpu.buffer.AttributeType
import me.anno.gpu.buffer.DrawMode
import me.anno.gpu.buffer.StaticBuffer
import me.anno.gpu.pipeline.Pipeline
import org.joml.AABBf
import org.joml.Matrix4x3f

/**
 * todo use this to render lots of instances more fluently
 * */
class StaticMeshManager : Component(), Renderable {

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

    data class Key(val component: Component, val mesh: Mesh, val materialIndex: Int)

    val byMaterial = HashMap<Material, UniqueMeshRenderer<Key>>()

    fun canAdd(mesh: Mesh): Boolean {
        return !(mesh.drawMode != DrawMode.TRIANGLES || mesh.proceduralLength > 0)
    }

    fun add(component: MeshComponentBase): Boolean {
        val mesh = component.getMesh() ?: return false
        if (!canAdd(mesh)) return false
        // todo only accept meshes, where everything is fine
        for (i in 0 until mesh.numMaterials) {
            val materialRef = component.materials.getOrNull(i) ?: mesh.materials.getOrNull(i)
            val material = MaterialCache[materialRef] ?: defaultMaterial
            add(component, mesh, material, i)
        }
        return true
    }

    fun add(component: Component, mesh: Mesh, material: Material, materialIndex: Int) {
        val umr = byMaterial.getOrPut(material) {
            object : UniqueMeshRenderer<Key>(attributes, MeshVertexData.DEFAULT, material, DrawMode.TRIANGLES) {
                override fun forEachMesh(run: (Mesh, Material?, Transform) -> Unit) {
                    throw NotImplementedError()
                }

                override fun getData(key: Key, mesh: Mesh): StaticBuffer? {
                    val numTris = mesh.numPrimitives.toInt()
                    val buffer = StaticBuffer("umr-${mesh.name}", attributes, numTris)
                    mesh.forEachTriangleIndex { ai, bi, ci ->

                    }
                    TODO("Not yet implemented")
                }
            }
        }
        val key = Key(component, mesh, materialIndex)
        val data = umr.getData(key, mesh)
        if (data != null) {
            // calculate bounds;
            // not really necessary for now, but might be used for frustum checks in the future
            val transform = component.transform
            var bounds = mesh.getBounds()
            if (transform != null) {
                bounds = bounds.transform(Matrix4x3f().set(transform.getDrawMatrix()), AABBf())
            }
            umr.add(key, MeshEntry(mesh, bounds, data))
        }
    }

    fun remove(component: MeshComponentBase): Boolean {
        val mesh = component.getMesh() ?: return false
        if (!canAdd(mesh)) return false
        // todo only accept meshes, where everything is fine
        for (i in 0 until mesh.numMaterials) {
            val materialRef = component.materials.getOrNull(i) ?: mesh.materials.getOrNull(i)
            val material = MaterialCache[materialRef] ?: defaultMaterial
            remove(component, mesh, material, i)
        }
        return true
    }

    fun remove(component: Component, mesh: Mesh, material: Material, materialIndex: Int): Boolean {
        val umr = byMaterial[material] ?: return false
        val key = Key(component, mesh, materialIndex)
        return umr.remove(key)
    }

    override fun fill(pipeline: Pipeline, entity: Entity, clickId: Int): Int {
        this.clickId = clickId
        this.lastDrawn = Time.gameTimeN
        for ((_, v) in byMaterial) { // too easy xD
            v.fill(pipeline, entity, clickId)
        }
        return clickId + 1
    }
}