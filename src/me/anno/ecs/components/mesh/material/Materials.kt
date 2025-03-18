package me.anno.ecs.components.mesh.material

import me.anno.ecs.components.mesh.material.Material.Companion.defaultMaterial
import me.anno.gpu.FinalRendering
import me.anno.io.files.FileReference
import me.anno.io.files.InvalidRef

object Materials {
    fun getMaterial(
        materialOverrides: List<FileReference>?,
        materials: List<FileReference>, index: Int
    ): Material {
        val ref = getMaterialRef(materialOverrides, materials, index)
        val async = !FinalRendering.isFinalRendering
        return MaterialCache[ref, async] ?: defaultMaterial
    }

    fun getMaterial(
        materialOverride: Material?,
        materialOverrides: List<FileReference>?,
        materials: List<FileReference>,
        index: Int
    ): Material {
        val mat1 = getMaterial(materialOverrides, materials, index)
        return getMaterial(materialOverride, mat1)
    }

    fun getMaterial(
        materialOverride: Material?,
        material: Material,
    ): Material {
        return if (materialOverride != null && material.shader == null) materialOverride
        else material
    }

    fun getMaterialRef(
        materialOverrides: List<FileReference>?,
        materials: List<FileReference>,
        index: Int
    ): FileReference {
        val m0 = materialOverrides?.getOrNull(index)?.nullIfUndefined()
        return m0 ?: materials.getOrNull(index)?.nullIfUndefined() ?: InvalidRef
    }
}