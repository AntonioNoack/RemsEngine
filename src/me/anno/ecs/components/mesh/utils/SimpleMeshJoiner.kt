package me.anno.ecs.components.mesh.utils

import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.material.Material
import me.anno.ecs.components.mesh.material.MaterialCache
import me.anno.io.files.FileReference
import me.anno.utils.Color.toARGB
import me.anno.utils.Color.white
import org.joml.Matrix4x3f

class SimpleMeshJoiner(val joinMaterials: Boolean, hasColors: Boolean, hasUVs: Boolean, hasBones: Boolean) :
    MeshJoiner<Mesh>(hasColors, hasUVs, hasBones) {
    override fun getMesh(element: Mesh): Mesh = element
    override fun getMaterials(element: Mesh): List<FileReference> {
        return if (joinMaterials) emptyList() else element.materials
    }

    override fun getVertexColor(element: Mesh): Int {
        return if (joinMaterials) {
            val mat = MaterialCache[element.material] ?: Material.defaultMaterial
            mat.diffuseBase.toARGB()
        } else white
    }

    override fun getTransform(element: Mesh, dst: Matrix4x3f) {}
}