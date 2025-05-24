package me.anno.ecs.components.mesh.utils

import me.anno.ecs.Component
import me.anno.ecs.Entity
import me.anno.ecs.EntityQuery.forAllComponents
import me.anno.ecs.Transform
import me.anno.ecs.components.mesh.IMesh
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.MeshAttributes.color0
import me.anno.ecs.components.mesh.MeshComponentBase
import me.anno.ecs.components.mesh.MeshSpawner
import me.anno.ecs.components.mesh.TransformMesh.transform
import me.anno.ecs.components.mesh.material.Materials.getMaterialRef
import me.anno.io.files.FileReference
import me.anno.utils.algorithms.Recursion
import me.anno.utils.assertions.assertNotSame
import me.anno.utils.structures.lists.Lists.any2
import me.anno.utils.structures.lists.Lists.createList
import org.joml.Matrix4x3f

object StaticMeshJoiner {

    fun findMeshes(instance: Entity): List<Component> {
        val components = ArrayList<Component>()
        Recursion.processRecursive(instance) { entity, remaining ->
            if (entity.isEnabled) {
                entity.validateTransform()
                remaining.addAll(entity.children)
                entity.forAllComponents(false) { comp ->
                    if (comp is MeshComponentBase || comp is MeshSpawner) {
                        components.add(comp)
                    }
                }
            }
        }
        return components
    }

    private fun addMesh(
        meshes: ArrayList<Triple<Mesh, Transform?, List<FileReference>>>,
        mesh: IMesh?, transform: Transform?, compMaterials: List<FileReference>?
    ) {
        if (mesh is Mesh && mesh.proceduralLength <= 0) {
            val meshMaterials = mesh.materials
            val materials = createList(mesh.numMaterials) {
                getMaterialRef(compMaterials, meshMaterials, it)
            }
            meshes.add(Triple(mesh, transform, materials))
        } // else not supported
    }

    private fun collectMeshes(components: List<Component>): List<Triple<Mesh, Transform?, List<FileReference>>> {
        val meshes = ArrayList<Triple<Mesh, Transform?, List<FileReference>>>()
        for (index in components.indices) {
            when (val component = components[index]) {
                is MeshComponentBase -> {
                    addMesh(meshes, component.getMesh(), component.transform, component.materials)
                }
                is MeshSpawner -> {
                    component.forEachMesh { mesh, material, transform ->
                        val materialList = if (material == null) emptyList()
                        else listOf(material.ref)
                        addMesh(meshes, mesh, transform, materialList)
                        false
                    }
                }
            }
        }
        return meshes
    }

    /**
     * this should only be executed for decently small meshes ^^,
     * large meshes may cause OutOfMemoryExceptions
     * */
    fun joinMeshes(list: List<Component>): Mesh? {
        val meshes = collectMeshes(list)
        return when (meshes.size) {
            0 -> null
            1 -> join1Mesh(meshes[0])
            else -> joinNMeshes(meshes)
        }
    }

    private fun join1Mesh(meshes: Triple<Mesh, Transform?, List<FileReference>>): Mesh {
        // special case: no joining required
        val (mesh, transform, materials) = meshes
        return join1Mesh(mesh, transform, materials)
    }

    fun join1Mesh(mesh: Mesh, transform: Transform?, materials: List<FileReference>): Mesh {
        transform?.validate()

        // special case: no joining required
        val matrix = transform?.globalTransform
        val identityTransform = matrix == null || matrix.isIdentity()
        val identityMaterials = materials == mesh.materials
        if (identityTransform && identityMaterials) return mesh

        // transform required
        // only needed for position, normal and tangents
        val clone = mesh.clone() as Mesh
        clone.materials = materials
        clone.unlinkGPUData()
        if (!identityTransform) {
            clone.positions = mesh.positions?.copyOf()
            clone.normals = mesh.normals?.copyOf()
            clone.tangents = mesh.tangents?.copyOf()
            clone.transform(matrix)
        }
        return clone
    }

    fun joinNMeshes(meshes: List<Triple<Mesh, Transform?, List<FileReference>>>): Mesh {
        val hasColors = meshes.any2 { it.first.color0 != null }
        val hasBones = meshes.any2 { it.first.hasBones }
        val hasUVs = meshes.any2 { it.first.uvs != null }

        return object : MeshJoiner<Triple<Mesh, Transform?, List<FileReference>>>(hasColors, hasBones, hasUVs) {
            override fun getMesh(element: Triple<Mesh, Transform?, List<FileReference>>) = element.first
            override fun getMaterials(element: Triple<Mesh, Transform?, List<FileReference>>) = element.third
            override fun getTransform(element: Triple<Mesh, Transform?, List<FileReference>>, dst: Matrix4x3f) {
                val transform = element.second
                if (transform != null) {
                    transform.validate()
                    dst.set(transform.globalTransform)
                } else dst.identity()
            }
        }.join(meshes)
    }
}